const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

/**
 * Cloud Function triggered when a new broadcast alert document is created or updated in Firestore.
 * Securely fetches recipient FCM tokens and dispatches push notifications via Firebase Admin SDK.
 */
exports.onBroadcastAlertCreated = functions.firestore
    .document('broadcastAlerts/{alertId}')
    .onWrite(async (change, context) => {
        const afterData = change.after.exists ? change.after.data() : null;
        const beforeData = change.before.exists ? change.before.data() : null;

        if (!afterData) return null; // Document deleted

        // Send push notification ONLY when status becomes "PUBLISHED"
        if (afterData.status !== 'PUBLISHED') return null;
        if (beforeData && beforeData.status === 'PUBLISHED') return null; // Already sent

        const alertId = context.params.alertId;
        const title = afterData.title || 'GradeXpert Alert';
        const message = afterData.message || 'You have a new academic alert.';
        const type = afterData.type || 'General Announcement';
        const priority = afterData.priority || 'Normal';
        const targetType = afterData.targetType || 'ALL_STUDENTS';
        const department = afterData.department || '';
        const semester = afterData.semester || '';
        const studentId = afterData.studentId || '';

        console.log(`Processing FCM notification for Alert [${alertId}], Target: ${targetType}`);

        try {
            // Determine destination screen based on alert type
            let destination = 'STUDENT_DASHBOARD';
            if (type === 'Exam') destination = 'EXAMS';
            else if (type === 'Assignment') destination = 'ASSIGNMENTS';
            else if (type === 'Attendance') destination = 'ATTENDANCE';
            else if (type === 'Result') destination = 'RESULTS';

            // Query target FCM tokens
            let tokens = [];

            if (targetType === 'SPECIFIC_STUDENT' && studentId) {
                const tokenDocs = await admin.firestore()
                    .collection('studentDevices')
                    .doc(studentId)
                    .collection('tokens')
                    .get();
                tokenDocs.forEach(doc => tokens.push(doc.id));
            } else {
                let studentRef = admin.firestore().collection('students').where('status', '==', 'ACTIVE');
                if (targetType === 'DEPARTMENT' && department) {
                    studentRef = studentRef.where('department', '==', department);
                } else if (targetType === 'DEPARTMENT_SEMESTER') {
                    if (department) studentRef = studentRef.where('department', '==', department);
                    if (semester) studentRef = studentRef.where('semester', '==', semester);
                }

                const activeStudentsSnap = await studentRef.get();
                const studentIds = [];
                activeStudentsSnap.forEach(doc => studentIds.push(doc.id));

                // Fetch tokens for active target students
                for (const id of studentIds) {
                    const tSnap = await admin.firestore()
                        .collection('studentDevices')
                        .doc(id)
                        .collection('tokens')
                        .get();
                    tSnap.forEach(tDoc => tokens.push(tDoc.id));
                }
            }

            if (tokens.length === 0) {
                console.log(`No registered FCM tokens found for Alert [${alertId}]`);
                return null;
            }

            // Construct FCM Multicast payload
            const payload = {
                tokens: tokens,
                notification: {
                    title: title,
                    body: message
                },
                data: {
                    alertId: alertId,
                    type: type,
                    priority: priority,
                    targetType: targetType,
                    destination: destination
                }
            };

            const response = await admin.messaging().sendEachForMulticast(payload);
            console.log(`Successfully dispatched FCM notifications. Success: ${response.successCount}, Failure: ${response.failureCount}`);
            return response;

        } catch (error) {
            console.error(`Error sending FCM broadcast for Alert [${alertId}]`, error);
            return null;
        }
    });

/**
 * Helper to verify that the caller is an authenticated System Admin.
 * Checks Firebase Auth Custom Claims or queries Firestore /users/{uid}.
 */
async function verifyAdminCaller(context) {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            'unauthenticated',
            'Authentication is required to perform this administrative operation.'
        );
    }

    // 1. Check custom claims if set
    if (context.auth.token && (context.auth.token.role === 'ADMIN' || context.auth.token.role === 'admin')) {
        return true;
    }

    // 2. Query Firestore /users/{uid}
    const callerUid = context.auth.uid;
    const userDoc = await admin.firestore().collection('users').doc(callerUid).get();
    if (userDoc.exists) {
        const userData = userDoc.data();
        const role = userData.role ? userData.role.toUpperCase() : '';
        if (role === 'ADMIN') {
            return true;
        }
    }

    throw new functions.https.HttpsError(
        'permission-denied',
        'Permission denied: Only authorized System Administrators can manage user passwords.'
    );
}

/**
 * Secure HTTPS Callable Cloud Function for Admin to change another user's Firebase Auth password.
 * 
 * Validates:
 * 1. Caller is authenticated.
 * 2. Caller has verified ADMIN role.
 * 3. New password is at least 8 characters.
 * 4. Target user exists in Firebase Auth.
 * 5. Updates Firebase Auth password via Admin SDK.
 * 6. Never exposes or logs password.
 */
exports.adminChangeUserPassword = functions.https.onCall(async (data, context) => {
    // Verify Admin authorization
    await verifyAdminCaller(context);

    const { targetUid, targetEmail, newPassword } = data || {};

    if (!newPassword || typeof newPassword !== 'string' || newPassword.trim().length < 8) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Password must contain at least 8 characters.'
        );
    }

    let authUid = targetUid;

    // If targetUid not supplied directly or need resolution by email
    if (!authUid && targetEmail) {
        try {
            const userRecord = await admin.auth().getUserByEmail(targetEmail.trim());
            authUid = userRecord.uid;
        } catch (err) {
            throw new functions.https.HttpsError(
                'not-found',
                `Target user account for email (${targetEmail}) was not found in Firebase Authentication.`
            );
        }
    }

    if (!authUid) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Target user ID or email is required.'
        );
    }

    try {
        // Update user password in Firebase Authentication
        await admin.auth().updateUser(authUid, {
            password: newPassword.trim()
        });

        // Optionally mark passwordResetRequired: false in Firestore if present
        try {
            await admin.firestore().collection('users').doc(authUid).update({
                passwordResetRequired: false,
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (ignore) {
            // Firestore doc may have custom ID
        }

        console.log(`[AdminAuth] Password successfully updated for target user [${authUid}] by Admin [${context.auth.uid}].`);

        return {
            success: true,
            message: 'Password updated successfully.'
        };
    } catch (error) {
        console.error(`[AdminAuth] Error updating password for target user [${authUid}]:`, error.message);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to update user password in Firebase Authentication.'
        );
    }
});

/**
 * Secure HTTPS Callable Cloud Function for Admin to create a new user account in Firebase Auth.
 */
exports.adminCreateUserAccount = functions.https.onCall(async (data, context) => {
    await verifyAdminCaller(context);

    const {
        email,
        password,
        displayName,
        role,
        identifier,
        loginId,
        department,
        departmentId,
        departmentShortName,
        programLevel,
        semester,
        phone,
        designation,
        qualification,
        dateOfJoining
    } = data || {};

    if (!email || !password || password.length < 8) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Valid email and a password with at least 8 characters are required.'
        );
    }

    const assignedRole = (role || 'STUDENT').toUpperCase();
    const cleanLevel = (programLevel && programLevel.toUpperCase() === 'PG') ? 'PG' : 'UG';
    const finalLoginId = (loginId && loginId.trim().length > 0) ? loginId.trim().toUpperCase() : (identifier || '').trim().toUpperCase();

    try {
        // Create user in Firebase Auth
        const userRecord = await admin.auth().createUser({
            email: email.trim(),
            password: password.trim(),
            displayName: displayName || '',
            disabled: false
        });

        // Set custom claims for role
        await admin.auth().setCustomUserClaims(userRecord.uid, {
            role: assignedRole
        });

        const batch = admin.firestore().batch();

        // 1. Create Firestore user document in users/{uid}
        const userDocRef = admin.firestore().collection('users').doc(userRecord.uid);
        const userDoc = {
            id: userRecord.uid,
            uid: userRecord.uid,
            name: displayName || '',
            email: email.trim(),
            identifier: identifier || finalLoginId,
            loginId: finalLoginId,
            role: assignedRole,
            status: 'ACTIVE',
            department: department || '',
            departmentName: department || '',
            departmentId: departmentId || '',
            departmentShortName: departmentShortName || '',
            programLevel: cleanLevel,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        };

        if (phone) userDoc.phone = phone;
        if (semester) userDoc.semester = parseInt(semester, 10) || 1;

        batch.set(userDocRef, userDoc);

        // 2. Reserve Login ID in userLoginIds/{normalizedLoginId}
        if (finalLoginId) {
            const normalizedLoginId = finalLoginId.toLowerCase();
            const loginIdRef = admin.firestore().collection('userLoginIds').doc(normalizedLoginId);
            batch.set(loginIdRef, {
                loginId: finalLoginId,
                normalizedLoginId: normalizedLoginId,
                uid: userRecord.uid,
                role: assignedRole,
                email: email.trim().toLowerCase(),
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }

        // 3. If STUDENT, create dedicated students/{uid} document
        if (assignedRole === 'STUDENT') {
            const studentDocRef = admin.firestore().collection('students').doc(userRecord.uid);
            const studentDoc = {
                uid: userRecord.uid,
                loginId: finalLoginId,
                name: displayName || '',
                registerNo: identifier || finalLoginId,
                email: email.trim(),
                phone: phone || '',
                role: 'STUDENT',
                department: department || '',
                departmentName: department || '',
                departmentId: departmentId || '',
                departmentShortName: departmentShortName || '',
                programLevel: cleanLevel,
                semester: semester ? (parseInt(semester, 10) || 1) : 1,
                status: 'ACTIVE',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                createdBy: context.auth.uid
            };
            batch.set(studentDocRef, studentDoc);
        }

        // 4. If TEACHER, create dedicated teachers/{uid} document
        if (assignedRole === 'TEACHER') {
            const teacherDocRef = admin.firestore().collection('teachers').doc(userRecord.uid);
            const teacherDoc = {
                uid: userRecord.uid,
                loginId: finalLoginId,
                name: displayName || '',
                employeeId: identifier || finalLoginId,
                email: email.trim(),
                phone: phone || '',
                departmentName: department || '',
                departmentId: departmentId || '',
                departmentShortName: departmentShortName || '',
                department: department || '',
                programLevel: cleanLevel,
                designation: designation || 'Faculty',
                qualification: qualification || '',
                dateOfJoining: dateOfJoining || '',
                assignedSubjectIds: [],
                assignedSubjectNames: [],
                status: 'ACTIVE',
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                createdBy: context.auth.uid
            };
            batch.set(teacherDocRef, teacherDoc);
        }

        await batch.commit();

        console.log(`[AdminAuth] Created new user [${userRecord.uid}], loginId: ${finalLoginId}, role: ${assignedRole} by Admin [${context.auth.uid}].`);

        return {
            success: true,
            uid: userRecord.uid,
            loginId: finalLoginId,
            message: `${assignedRole} account created successfully with Login ID: ${finalLoginId}`
        };
    } catch (error) {
        console.error(`[AdminAuth] Error creating user:`, error.message);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create user account in Firebase Authentication.'
        );
    }
});

/**
 * Cloud Function triggered when a new password reset request is submitted.
 * Alerts System Administrators via FCM push notification and in-app alert.
 */
exports.onPasswordResetRequestCreated = functions.firestore
    .document('passwordResetRequests/{requestId}')
    .onCreate(async (snap, context) => {
        const data = snap.data();
        if (!data || data.status !== 'PENDING') return null;

        const requestId = context.params.requestId;
        const userName = data.userName || 'A user';
        const role = data.role || 'User';
        const identifier = data.identifier || data.registerNo || data.employeeId || '';

        console.log(`[PasswordReset] New request [${requestId}] from ${role} ${userName} (${identifier})`);

        try {
            // Find all Admin users
            const adminUsersSnap = await admin.firestore().collection('users')
                .where('role', '==', 'ADMIN')
                .get();

            const adminUids = [];
            const adminTokens = [];

            adminUsersSnap.forEach(doc => {
                adminUids.push(doc.id);
                const uData = doc.data();
                if (uData && uData.fcmToken) {
                    adminTokens.push(uData.fcmToken);
                }
            });

            const notifTitle = 'Password Reset Request';
            const notifMessage = `${userName} (${role}, ${identifier}) has requested a password reset.`;

            // 1. Create Top-level notification document
            await admin.firestore().collection('notifications').doc(requestId).set({
                type: 'PASSWORD_RESET_REQUEST',
                title: notifTitle,
                message: notifMessage,
                target_role: 'ADMIN',
                targetRole: 'ADMIN',
                category: 'SECURITY',
                requestId: requestId,
                sender: userName,
                read: false,
                is_read: false,
                date: new Date().toISOString(),
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });

            // 2. Create in-app notification for each Admin in their subcollection
            for (const adminUid of adminUids) {
                await admin.firestore().collection('users').doc(adminUid).collection('notifications').doc(requestId).set({
                    title: notifTitle,
                    message: notifMessage,
                    type: 'PASSWORD_RESET_REQUEST',
                    targetUserId: adminUid,
                    targetRole: 'ADMIN',
                    requestId: requestId,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    read: false
                }, { merge: true });

                // Check studentDevices tokens as well
                const tokenDocs = await admin.firestore().collection('studentDevices').doc(adminUid).collection('tokens').get();
                tokenDocs.forEach(tDoc => {
                    if (!adminTokens.includes(tDoc.id)) {
                        adminTokens.push(tDoc.id);
                    }
                });
            }

            // 3. Dispatch FCM Push Notifications to Admins
            if (adminTokens.length > 0) {
                const payload = {
                    tokens: adminTokens,
                    notification: {
                        title: notifTitle,
                        body: notifMessage
                    },
                    data: {
                        requestId: requestId,
                        role: role,
                        destination: 'PASSWORD_RESET_REQUESTS'
                    }
                };
                await admin.messaging().sendEachForMulticast(payload);
                console.log(`[PasswordReset] Dispatched admin push notifications for [${requestId}] to ${adminTokens.length} devices.`);
            }
        } catch (error) {
            console.error(`[PasswordReset] Error processing notification for [${requestId}]:`, error);
        }
        return null;
    });

/**
 * Cloud Function triggered when a password reset request status changes (COMPLETED / REJECTED).
 * Notifies the requester that their request has been processed.
 */
exports.onPasswordResetRequestUpdated = functions.firestore
    .document('passwordResetRequests/{requestId}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();

        if (!after || !before) return null;
        if (before.status === after.status) return null;

        const requestId = context.params.requestId;
        const targetUserId = after.userId;
        const targetEmail = after.email;
        const newStatus = after.status;

        console.log(`[PasswordReset] Request [${requestId}] status updated: ${before.status} -> ${newStatus}`);

        let notifTitle = 'Password Request Update';
        let notifMessage = 'Your password reset request status has been updated.';

        if (newStatus === 'COMPLETED') {
            notifTitle = 'Password Updated';
            notifMessage = 'Your GradeXpert password has been updated by the administrator.';
        } else if (newStatus === 'REJECTED') {
            notifTitle = 'Password Reset Request Rejected';
            notifMessage = 'Your password reset request was reviewed and rejected. Please contact the administrator for assistance.';
        } else {
            return null;
        }

        try {
            // Write in-app notification to requester
            if (targetUserId) {
                await admin.firestore().collection('users').doc(targetUserId).collection('notifications').add({
                    title: notifTitle,
                    message: notifMessage,
                    type: 'SECURITY_ALERT',
                    requestId: requestId,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    read: false
                });

                // Send FCM push if token is registered
                const tokenDocs = await admin.firestore().collection('studentDevices').doc(targetUserId).collection('tokens').get();
                const tokens = [];
                tokenDocs.forEach(tDoc => tokens.push(tDoc.id));

                if (tokens.length > 0) {
                    await admin.messaging().sendEachForMulticast({
                        tokens: tokens,
                        notification: {
                            title: notifTitle,
                            body: notifMessage
                        },
                        data: {
                            requestId: requestId,
                            status: newStatus,
                            destination: 'LOGIN'
                        }
                    });
                    console.log(`[PasswordReset] Dispatched status update push notification to user [${targetUserId}].`);
                }
            }
        } catch (error) {
            console.error(`[PasswordReset] Error sending status notification for [${requestId}]:`, error);
        }

        return null;
    });


