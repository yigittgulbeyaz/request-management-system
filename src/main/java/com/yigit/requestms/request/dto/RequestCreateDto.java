package com.yigit.requestms.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Binding target for the submission form. Deliberately not the entity: the
// entity carries fields the customer must never set (status, customer), and
// binding to it would make them reachable from the form.
public record RequestCreateDto(

        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 4000, message = "Description must be between 20 and 4000 characters")
        String description
) {
}