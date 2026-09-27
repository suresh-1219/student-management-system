package com.suresh.sms.dto;

public class LoginResponse {

    private String token;

    public LoginResponse() {
    }

    private String refreshToken;

    public LoginResponse(String token) {
        this.token = token;
    }

    public LoginResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}