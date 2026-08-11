package com.yigit.requestms.user.enums;

public enum Role {

    CUSTOMER,
    PRODUCT_OWNER,
    DEVELOPER,
    ADMIN;

    // Spring Security's hasRole() strips a ROLE_ prefix before comparing,
    // so authorities must carry it. Kept here to write it in one place.
    public String asAuthority() {
        return "ROLE_" + name();
    }
}