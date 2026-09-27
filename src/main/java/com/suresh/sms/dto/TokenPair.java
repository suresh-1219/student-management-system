package com.suresh.sms.dto;

/** An access token plus the refresh token issued alongside it. */
public record TokenPair(String accessToken, String refreshToken) {
}
