package com.yigit.requestms.admin.service;

import java.security.SecureRandom;

// Generates the one-time code an administrator hands over. SecureRandom rather
// than Random: this code opens an account until it is used, and a predictable
// sequence would let one code suggest the next.
//
// The alphabet leaves out characters that are read wrong when a code is spoken
// aloud or copied off a screen: no O and 0, no l and 1 and I. Grouped in fours
// because a code that has to be read out loud is read out in chunks.
final class SetupCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GROUPS = 3;
    private static final int GROUP_LENGTH = 4;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        StringBuilder code = new StringBuilder();
        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                code.append('-');
            }
            for (int i = 0; i < GROUP_LENGTH; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return code.toString();
    }
}