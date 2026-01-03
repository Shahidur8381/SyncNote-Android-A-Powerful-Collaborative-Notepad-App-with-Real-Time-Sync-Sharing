package com.example.syncnote.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {
    
    private static final int SALT_LENGTH = 16;

    /**
     * Hash a password with a randomly generated salt
     * @param password The plain text password
     * @return The hashed password in format: salt$hash
     */
    public static String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash the password with the salt
            String saltStr = bytesToHex(salt);
            String hash = hashWithSalt(password, saltStr);
            
            // Return salt$hash
            return saltStr + "$" + hash;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verify a password against a stored hash
     * @param password The plain text password to verify
     * @param storedHash The stored hash in format: salt$hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 2) {
                return false;
            }
            
            String salt = parts[0];
            String hash = parts[1];
            
            String computedHash = hashWithSalt(password, salt);
            return hash.equals(computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static String hashWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
