package com.fortydash.infrastructure.security;
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(10));
    }
    public static boolean verify(String plain, String hashed) {
        if (hashed == null || !hashed.startsWith("$2a$")) return false;
        return BCrypt.checkpw(plain, hashed);
    }
}