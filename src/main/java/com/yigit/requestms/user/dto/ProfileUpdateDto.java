package com.yigit.requestms.user.dto;

// Name and email only. Anything else someone could change about themselves
// would either be a role they granted themselves or a credential they already
// have a dedicated flow for.
public record ProfileUpdateDto(
        String nameSurname,
        String email
) {
}