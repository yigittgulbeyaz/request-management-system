package com.yigit.requestms.admin.dto;

import com.yigit.requestms.user.enums.Role;

// What an administrator supplies to open an account: who the person is and
// what they may do. Nothing about how they will prove it is theirs.
//
// No password and no security question. An administrator who chose either
// would hold the account open indefinitely; the person who will use it
// chooses both, once, with the code they were handed.
public record CreateUserDto(
        String nameSurname,
        String email,
        Role role
) {
}