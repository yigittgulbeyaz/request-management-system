package com.yigit.requestms.user.service;

import com.yigit.requestms.user.exception.InvalidEmailException;

import java.util.regex.Pattern;

// The rule lives here rather than in the two forms that collect an address,
// because a rule enforced in two places is a rule that will eventually differ
// between them. The forms use it to explain the problem; the services use it to
// refuse, which is the one that counts.
//
// Deliberately permissive. A pattern strict enough to reject every invalid
// address also rejects valid ones nobody expected, and the only real proof an
// address works is sending to it.
public final class EmailPolicy {

    private static final Pattern SHAPE =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private EmailPolicy() {
    }

    public static void require(String email) {
        if (!isWellFormed(email)) {
            throw new InvalidEmailException();
        }
    }

    public static boolean isWellFormed(String email) {
        return email != null && SHAPE.matcher(email.trim()).matches();
    }

    public static String describe() {
        return "Something of the form name@example.com.";
    }
}