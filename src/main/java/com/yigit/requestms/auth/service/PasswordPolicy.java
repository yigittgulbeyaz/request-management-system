package com.yigit.requestms.auth.service;

import com.yigit.requestms.auth.exception.WeakPasswordException;

// The rule lives here rather than in each screen that sets a password, because
// a rule enforced in two places is a rule that will eventually differ between
// them.
//
// Length and a mix of characters, and nothing else. Rules that demand symbols
// and forbid repeats push people towards writing passwords down, which trades
// a guessable password for a discoverable one.
public final class PasswordPolicy {

    private static final int MINIMUM_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void require(String password) {
        if (!isAcceptable(password)) {
            throw new WeakPasswordException();
        }
    }

    public static boolean isAcceptable(String password) {
        return password != null
                && password.length() >= MINIMUM_LENGTH
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }

    public static String describe() {
        return "At least " + MINIMUM_LENGTH
                + " characters, including a letter and a digit.";
    }
}