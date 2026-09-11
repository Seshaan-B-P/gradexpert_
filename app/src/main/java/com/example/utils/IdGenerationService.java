package com.example.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for atomic, safe, collision-free Login ID generation, validation,
 * lookup management (userLoginIds collection), and existing user identity migration.
 *
 * Distinguishes:
 * 1. Login ID (Human-readable, e.g. MCA001, TCH001)
 * 2. Firebase Auth UID (Internal auth identity, e.g. "xYz123...")
 * 3. Firestore Document ID (Firebase Auth UID)
 * 4. SQLite ID (Local auto-increment integer)
 */
public class IdGenerationService {

    private static final String TAG = "IdGenerationService";

    public static final String COLLECTION_COUNTERS = "counters";
    public static final String COLLECTION_USER_LOGIN_IDS = "userLoginIds";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_TEACHERS = "teachers";

    public interface OnIdGeneratedListener {
        void onSuccess(String loginId);
        void onError(String errorMessage);
    }

    public interface OnAvailabilityListener {
        void onResult(boolean isAvailable);
        void onError(String errorMessage);
    }

    public interface OnMigrationCompleteListener {
        void onComplete(int migratedCount, String message);
    }

    private static IdGenerationService instance;
    private final FirebaseFirestore db;

    private IdGenerationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized IdGenerationService getInstance() {
        if (instance == null) {
            instance = new IdGenerationService();
        }
        return instance;
    }

    /**
     * Checks if the device has an active network connection.
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "Network check error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Normalizes Login ID for case-insensitive lookup (e.g. "MCA001" -> "mca001").
     */
    public static String normalizeLoginId(String loginId) {
        if (loginId == null) return "";
        return loginId.trim().toLowerCase(Locale.US);
    }

    /**
     * Formats canonical Login ID (e.g. "mca001" -> "MCA001").
     */
    public static String canonicalLoginId(String loginId) {
        if (loginId == null) return "";
        return loginId.trim().toUpperCase(Locale.US);
    }

    /**
     * Extracts a clean uppercase alphanumeric prefix from department code/name.
     * E.g.:
     * "MCA" -> "MCA"
     * "B.Sc CS" -> "BSCCS"
     * "B.Com" -> "BCOM"
     * "TCH" -> "TCH"
     */
    public static String extractPrefix(String rawCodeOrName, String defaultPrefix) {
        if (rawCodeOrName == null || rawCodeOrName.trim().isEmpty()) {
            return defaultPrefix != null ? defaultPrefix.toUpperCase(Locale.US) : "STD";
        }
        String cleaned = rawCodeOrName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.US);
        if (cleaned.isEmpty()) {
            return defaultPrefix != null ? defaultPrefix.toUpperCase(Locale.US) : "STD";
        }
        // Limit prefix length to reasonable 6 characters
        if (cleaned.length() > 6) {
            cleaned = cleaned.substring(0, 6);
        }
        return cleaned;
    }

    /**
     * Checks whether a Login ID is available across all student and teacher accounts.
     * Checks userLoginIds/{normalizedLoginId} and fallback users collection.
     */
    public void isLoginIdAvailable(String loginId, @NonNull OnAvailabilityListener listener) {
        if (loginId == null || loginId.trim().isEmpty()) {
            listener.onResult(false);
            return;
        }

        final String normalized = normalizeLoginId(loginId);
        final String canonical = canonicalLoginId(loginId);

        // 1. Check userLoginIds collection lookup
        db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Document already exists in lookup -> taken
                        listener.onResult(false);
                    } else {
                        // 2. Check users collection for legacy accounts where loginId or identifier matches
                        db.collection(COLLECTION_USERS)
                                .whereEqualTo("loginId", canonical)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                        listener.onResult(false);
                                    } else {
                                        // Also check students collection
                                        db.collection(COLLECTION_STUDENTS)
                                                .whereEqualTo("loginId", canonical)
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener(stdSnap -> {
                                                    if (stdSnap != null && !stdSnap.isEmpty()) {
                                                        listener.onResult(false);
                                                    } else {
                                                        listener.onResult(true);
                                                    }
                                                })
                                                .addOnFailureListener(e -> listener.onError(e.getMessage()));
                                    }
                                })
                                .addOnFailureListener(e -> listener.onError(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Atomically generates the next unique Login ID using a Firestore transaction on counters/{counterKey}.
     * Ensures concurrency safety if multiple Admins create accounts at the same time.
     * Does NOT use count + 1, avoiding duplicate ID reuse upon account deletion.
     *
     * @param prefix Department short code for students (e.g. "MCA") or "TCH" for teachers.
     * @param isTeacher true if creating Teacher ID, false for Student.
     * @param listener Callback returning unique generated ID (e.g. "MCA001" or "TCH001").
     */
    public void generateNextLoginId(String prefix, boolean isTeacher, @NonNull OnIdGeneratedListener listener) {
        final String cleanPrefix = extractPrefix(prefix, isTeacher ? "TCH" : "STD");
        final String counterKey = isTeacher ? "teacher_TCH" : "student_" + cleanPrefix;
        final DocumentReference counterRef = db.collection(COLLECTION_COUNTERS).document(counterKey);

        // First find current max in case counters doc is not initialized yet
        findHighestExistingSuffix(cleanPrefix, isTeacher, highestExisting -> {
            db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(counterRef);

                long lastNum = highestExisting;
                if (snapshot.exists() && snapshot.contains("lastNumber")) {
                    Long storedLast = snapshot.getLong("lastNumber");
                    if (storedLast != null && storedLast > lastNum) {
                        lastNum = storedLast;
                    }
                }

                long nextNum = lastNum + 1;
                String candidateId = String.format(Locale.US, "%s%03d", cleanPrefix, nextNum);
                String normalized = normalizeLoginId(candidateId);

                // Check collision in lookup inside transaction if possible
                DocumentReference lookupRef = db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized);
                DocumentSnapshot lookupSnap = transaction.get(lookupRef);
                while (lookupSnap.exists()) {
                    nextNum++;
                    candidateId = String.format(Locale.US, "%s%03d", cleanPrefix, nextNum);
                    normalized = normalizeLoginId(candidateId);
                    lookupRef = db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized);
                    lookupSnap = transaction.get(lookupRef);
                }

                // Update counter in transaction
                Map<String, Object> counterData = new HashMap<>();
                counterData.put("lastNumber", nextNum);
                counterData.put("prefix", cleanPrefix);
                counterData.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(counterRef, counterData, SetOptions.merge());

                return candidateId;
            }).addOnSuccessListener(listener::onSuccess)
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed transaction to generate Login ID: " + e.getMessage(), e);
                listener.onError("ID generation failed: " + e.getMessage());
            });
        });
    }

    /**
     * Finds highest numeric suffix in existing userLoginIds/students/teachers for this prefix.
     */
    private void findHighestExistingSuffix(String prefix, boolean isTeacher, SuffixCallback callback) {
        db.collection(COLLECTION_USER_LOGIN_IDS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long maxFound = 0;
                    Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix.toLowerCase(Locale.US)) + "(\\d+)$");

                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String docId = doc.getId().toLowerCase(Locale.US);
                            Matcher m = pattern.matcher(docId);
                            if (m.matches()) {
                                try {
                                    long val = Long.parseLong(m.group(1));
                                    if (val > maxFound) maxFound = val;
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    // Also check users collection if userLoginIds was empty
                    if (maxFound == 0) {
                        checkExistingUsersCollectionMax(prefix, isTeacher, callback);
                    } else {
                        callback.onMaxFound(maxFound);
                    }
                })
                .addOnFailureListener(e -> checkExistingUsersCollectionMax(prefix, isTeacher, callback));
    }

    private void checkExistingUsersCollectionMax(String prefix, boolean isTeacher, SuffixCallback callback) {
        String targetCollection = isTeacher ? COLLECTION_TEACHERS : COLLECTION_STUDENTS;
        db.collection(targetCollection)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long maxFound = 0;
                    Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix.toUpperCase(Locale.US)) + "(\\d+)$");

                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String loginId = doc.getString("loginId");
                            if (loginId == null || loginId.isEmpty()) {
                                loginId = isTeacher ? doc.getString("employeeId") : doc.getString("registerNo");
                            }
                            if (loginId != null) {
                                Matcher m = pattern.matcher(loginId.trim().toUpperCase(Locale.US));
                                if (m.matches()) {
                                    try {
                                        long val = Long.parseLong(m.group(1));
                                        if (val > maxFound) maxFound = val;
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                    callback.onMaxFound(maxFound);
                })
                .addOnFailureListener(e -> callback.onMaxFound(0));
    }

    private interface SuffixCallback {
        void onMaxFound(long max);
    }

    /**
     * Adds the Login ID reservation operation to a Firestore WriteBatch.
     */
    public void addLoginIdReservationToBatch(WriteBatch batch, String loginId, String firebaseUid, String role, String email) {
        if (batch == null || loginId == null || loginId.trim().isEmpty() || firebaseUid == null) {
            return;
        }

        String canonical = canonicalLoginId(loginId);
        String normalized = normalizeLoginId(loginId);

        DocumentReference ref = db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized);
        Map<String, Object> data = new HashMap<>();
        data.put("loginId", canonical);
        data.put("normalizedLoginId", normalized);
        data.put("uid", firebaseUid);
        data.put("role", role != null ? role.toUpperCase(Locale.US) : "STUDENT");
        data.put("email", email != null ? email.trim().toLowerCase(Locale.US) : "");
        data.put("createdAt", FieldValue.serverTimestamp());

        batch.set(ref, data);
    }

    /**
     * Resolves a Login ID to associated user email for Firebase Authentication signIn.
     * Looks up userLoginIds first, with fallback to users collection.
     */
    public void resolveEmailForLoginId(String loginIdOrIdentifier, @NonNull OnResolvedListener listener) {
        if (loginIdOrIdentifier == null || loginIdOrIdentifier.trim().isEmpty()) {
            listener.onError("Please enter a valid Login ID or Email.");
            return;
        }

        final String raw = loginIdOrIdentifier.trim();
        final String normalized = normalizeLoginId(raw);
        final String canonical = canonicalLoginId(raw);

        // 1. Check userLoginIds collection
        db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("email") != null && !doc.getString("email").trim().isEmpty()) {
                        String email = doc.getString("email").trim();
                        String uid = doc.getString("uid");
                        String role = doc.getString("role");
                        listener.onSuccess(email, uid != null ? uid : "", canonical, role != null ? role : "");
                    } else {
                        // 2. Query users collection by loginId
                        queryUsersCollectionForResolution(canonical, raw, listener);
                    }
                })
                .addOnFailureListener(e -> queryUsersCollectionForResolution(canonical, raw, listener));
    }

    private void queryUsersCollectionForResolution(String canonical, String raw, @NonNull OnResolvedListener listener) {
        db.collection(COLLECTION_USERS)
                .whereEqualTo("loginId", canonical)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String email = doc.getString("email");
                        if (email != null && !email.trim().isEmpty()) {
                            listener.onSuccess(email.trim(), doc.getId(), canonical, doc.getString("role"));
                            return;
                        }
                    }

                    // Fallback to identifier (registerNo or employeeId)
                    db.collection(COLLECTION_USERS)
                            .whereEqualTo("identifier", raw)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(idSnap -> {
                                if (idSnap != null && !idSnap.isEmpty()) {
                                    DocumentSnapshot doc = idSnap.getDocuments().get(0);
                                    String email = doc.getString("email");
                                    if (email != null && !email.trim().isEmpty()) {
                                        listener.onSuccess(email.trim(), doc.getId(), canonical, doc.getString("role"));
                                        return;
                                    }
                                }
                                listener.onError("No account found matching Login ID: " + raw);
                            })
                            .addOnFailureListener(e -> listener.onError("Failed to lookup Login ID: " + e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError("Failed to lookup Login ID: " + e.getMessage()));
    }

    public interface OnResolvedListener {
        void onSuccess(String email, String uid, String canonicalLoginId, String role);
        void onError(String errorMessage);
    }

    /**
     * Safe migration of existing accounts in Firestore:
     * - Preserves Firebase Auth UID.
     * - Creates missing userLoginIds entries.
     * - Ensures loginId field exists in users and students/teachers collections.
     * - Updates counters so future generated IDs do not collide.
     */
    public void migrateExistingAccountsIfNeeded(@NonNull OnMigrationCompleteListener listener) {
        db.collection(COLLECTION_USERS).get().addOnSuccessListener(userDocs -> {
            if (userDocs == null || userDocs.isEmpty()) {
                listener.onComplete(0, "No existing user accounts to migrate.");
                return;
            }

            WriteBatch batch = db.batch();
            int count = 0;

            Map<String, Long> counterMaxMap = new HashMap<>();

            for (DocumentSnapshot userDoc : userDocs.getDocuments()) {
                String uid = userDoc.getId();
                String role = userDoc.getString("role");
                String email = userDoc.getString("email");
                String identifier = userDoc.getString("identifier");
                String existingLoginId = userDoc.getString("loginId");

                String assignedLoginId = existingLoginId;
                if (assignedLoginId == null || assignedLoginId.trim().isEmpty()) {
                    if (identifier != null && !identifier.trim().isEmpty()) {
                        assignedLoginId = identifier.trim();
                    }
                }

                if (assignedLoginId != null && !assignedLoginId.trim().isEmpty()) {
                    String canonical = canonicalLoginId(assignedLoginId);
                    String normalized = normalizeLoginId(assignedLoginId);

                    // Add to userLoginIds lookup
                    DocumentReference lookupRef = db.collection(COLLECTION_USER_LOGIN_IDS).document(normalized);
                    Map<String, Object> lookupData = new HashMap<>();
                    lookupData.put("loginId", canonical);
                    lookupData.put("normalizedLoginId", normalized);
                    lookupData.put("uid", uid);
                    lookupData.put("role", role != null ? role.toUpperCase(Locale.US) : "STUDENT");
                    lookupData.put("email", email != null ? email.trim().toLowerCase(Locale.US) : "");
                    lookupData.put("migratedAt", FieldValue.serverTimestamp());
                    batch.set(lookupRef, lookupData, SetOptions.merge());

                    // Update user doc if loginId missing
                    if (existingLoginId == null || existingLoginId.trim().isEmpty()) {
                        batch.update(userDoc.getReference(), "loginId", canonical, "uid", uid);
                    }

                    // Track highest numeric suffix for counters
                    Pattern p = Pattern.compile("^([A-Z]+)(\\d+)$");
                    Matcher m = p.matcher(canonical);
                    if (m.matches()) {
                        String prefix = m.group(1);
                        try {
                            long num = Long.parseLong(m.group(2));
                            Long currentMax = counterMaxMap.get(prefix);
                            if (currentMax == null || num > currentMax) {
                                counterMaxMap.put(prefix, num);
                            }
                        } catch (Exception ignored) {}
                    }

                    count++;
                }
            }

            // Also synchronize counters
            for (Map.Entry<String, Long> entry : counterMaxMap.entrySet()) {
                String prefix = entry.getKey();
                long maxNum = entry.getValue();
                boolean isTch = "TCH".equalsIgnoreCase(prefix);
                String counterKey = isTch ? "teacher_TCH" : "student_" + prefix;
                DocumentReference cRef = db.collection(COLLECTION_COUNTERS).document(counterKey);
                Map<String, Object> cData = new HashMap<>();
                cData.put("lastNumber", maxNum);
                cData.put("prefix", prefix);
                cData.put("updatedAt", FieldValue.serverTimestamp());
                batch.set(cRef, cData, SetOptions.merge());
            }

            final int totalMigrated = count;
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Identity architecture migration completed. Migrated: " + totalMigrated);
                        listener.onComplete(totalMigrated, "Successfully synchronized " + totalMigrated + " accounts with Login ID lookup.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Migration commit failed: " + e.getMessage(), e);
                        listener.onComplete(0, "Migration warning: " + e.getMessage());
                    });
        }).addOnFailureListener(e -> listener.onComplete(0, "Migration failed: " + e.getMessage()));
    }
}
