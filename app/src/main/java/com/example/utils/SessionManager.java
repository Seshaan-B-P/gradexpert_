package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager handles SharedPreferences for maintaining login sessions,
 * user roles, remember me flags, and logout state.
 */
public class SessionManager {

    private static final String PREF_NAME = "GradeXpertSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole"; // "TEACHER" or "STUDENT"
    private static final String KEY_IDENTIFIER = "identifier"; // RegNo or Employee ID
    private static final String KEY_FIREBASE_UID = "firebaseUid";
    private static final String KEY_LOGIN_ID = "loginId";
    private static final String KEY_DARK_MODE = "isDarkMode";

    // Remember Me Keys
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_SAVED_USERNAME = "savedUsername";
    private static final String KEY_SAVED_PASSWORD = "savedPassword";
    private static final String KEY_SAVED_ROLE = "savedRole";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int id, String name, String email, String role, String identifier) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_IDENTIFIER, identifier);
        editor.apply();
    }

    public void saveLoginSession(int id, String name, String email, String role, String identifier) {
        createLoginSession(id, name, email, role, identifier);
    }

    public void saveRememberMe(boolean remember, String username, String password, String role) {
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        if (remember) {
            editor.putString(KEY_SAVED_USERNAME, username);
            editor.putString(KEY_SAVED_PASSWORD, PasswordUtils.encryptString(password));
            editor.putString(KEY_SAVED_ROLE, role);
        } else {
            editor.remove(KEY_SAVED_USERNAME);
            editor.remove(KEY_SAVED_PASSWORD);
            editor.remove(KEY_SAVED_ROLE);
        }
        editor.apply();
    }

    public boolean isRememberMeEnabled() {
        return pref.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getSavedUsername() {
        return pref.getString(KEY_SAVED_USERNAME, "");
    }

    public String getSavedPassword() {
        String encrypted = pref.getString(KEY_SAVED_PASSWORD, "");
        return PasswordUtils.decryptString(encrypted);
    }

    public String getSavedRole() {
        return pref.getString(KEY_SAVED_ROLE, "TEACHER");
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "User");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "");
    }

    public String getIdentifier() {
        return pref.getString(KEY_IDENTIFIER, "");
    }

    public boolean isDarkMode() {
        return pref.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public void updateUserProfile(String name, String email) {
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public void setFirebaseUid(String uid) {
        editor.putString(KEY_FIREBASE_UID, uid);
        editor.apply();
    }

    public String getFirebaseUid() {
        return pref.getString(KEY_FIREBASE_UID, "");
    }

    public void setLoginId(String loginId) {
        editor.putString(KEY_LOGIN_ID, loginId);
        editor.apply();
    }

    public String getLoginId() {
        return pref.getString(KEY_LOGIN_ID, "");
    }

    public void logout() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_IDENTIFIER);
        editor.remove(KEY_FIREBASE_UID);
        editor.remove(KEY_LOGIN_ID);
        editor.apply();
    }
}
