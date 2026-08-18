package com.yigit.requestms.admin.service;

import java.security.SecureRandom;

// Generates the password an administrator hands over once. SecureRandom rather
// than Random: this value briefly guards an account, and a predictable sequence
// would let one guessed password suggest the next.
//
// The alphabet leaves out characters that are read wrong when a password is
// spoken aloud or copied off a screen: no O and 0, no l and 1 and I.
final class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}