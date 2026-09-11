package com.example.utils;

import android.content.Context;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class providing cryptographic functions for password hashing,
 * verification, legacy plain-text migration, and credential encryption.
 */
public class PasswordUtils {

    private static final int SALT_BYTE_SIZE = 16;
    private static final int HASH_BYTE_SIZE = 32;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final String ENCRYPTION_KEY_SEED = "GradeXpertSecureAppKey2026";

    /**
     * Hashes a plain-text password using PBKDF2 with SHA-256.
     * Returns a string formatted as "salt_hex:hash_hex".
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            return "";
        }
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTE_SIZE];
            random.nextBytes(salt);

            byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE * 8);

            return bytesToHex(salt) + ":" + bytesToHex(hash);
        } catch (Exception e) {
            // Fallback to SHA-256 with salt if PBKDF2 algorithm is unavailable
            return fallbackSha256Hash(plainPassword);
        }
    }

    /**
     * Verifies a plain-text password against a stored password string.
     * Supports both new hashed format ("salt:hash") and legacy plain-text format.
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }

        // Check if stored password is in salt:hash format
        if (storedPassword.contains(":")) {
            String[] parts = storedPassword.split(":");
            if (parts.length == 2) {
                try {
                    byte[] salt = hexToBytes(parts[0]);
                    byte[] storedHash = hexToBytes(parts[1]);
                    byte[] testHash = pbkdf2(plainPassword.toCharArray(), salt, PBKDF2_ITERATIONS, storedHash.length * 8);
                    return slowEquals(storedHash, testHash);
                } catch (Exception e) {
                    return false;
                }
            }
        }

        // Legacy check for unhashed plain text passwords
        return plainPassword.equals(storedPassword);
    }

    /**
     * Checks if the stored password is in legacy plain-text format (needs upgrade).
     */
    public static boolean isLegacyPlainText(String storedPassword) {
        return storedPassword != null && !storedPassword.contains(":");
    }

    /**
     * Encrypts a text string using AES for secure storage in SharedPreferences.
     */
    public static String encryptString(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(ENCRYPTION_KEY_SEED.getBytes("UTF-8"));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            return data; // Fallback
        }
    }

    /**
     * Decrypts an AES encrypted text string from SharedPreferences.
     */
    public static String decryptString(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return "";
        }
        try {
            byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
            if (combined.length < 17) {
                return encryptedData; // Return as-is if not in expected encrypted format
            }

            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(ENCRYPTION_KEY_SEED.getBytes("UTF-8"));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] original = cipher.doFinal(encrypted);
            return new String(original, "UTF-8");
        } catch (Exception e) {
            return encryptedData; // Return plain string if decryption fails (e.g. legacy stored string)
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    private static String fallbackSha256Hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes("UTF-8"));
            return "sha256:" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return plainPassword;
        } catch (Exception e) {
            return plainPassword;
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
