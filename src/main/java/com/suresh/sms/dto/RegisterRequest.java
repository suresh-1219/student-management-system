package com.suresh.sms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for public self-registration.
 *
 * <p>Deliberately has NO role field: a client must never be able to choose
 * its own privileges. Anything named "role" in the JSON body is ignored.
 */
public record RegisterRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        String email,

        // BCrypt only uses the first 72 bytes, so longer passwords are rejected.
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
