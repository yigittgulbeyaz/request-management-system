package com.yigit.requestms.user.enums;

// The database stores the constant name, not the question text, so the
// question can be rendered in whichever language the user has selected.
public enum SecurityQuestion {

    FIRST_PET,
    BIRTH_CITY,
    PRIMARY_SCHOOL_TEACHER,
    FAVOURITE_BOOK;

    public String messageKey() {
        return "security.question." + name().toLowerCase();
    }
}