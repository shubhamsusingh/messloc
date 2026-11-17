package com.example.messloc.data.model;

public class OwnerSignupResponse {

    private boolean success;
    private String message;
    private String token;
    private String tokenType;
    private Long expiresInMs;
    private User user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresInMs() {
        return expiresInMs;
    }

    public User getUser() {
        return user;
    }

    public static class User {

        private String id;
        private String name;
        private String email;
        private String phone;
        private String city;
        private String role;
        private String status;
        private String profileImage;
        private String createdAt;
        private String accountStatus;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getCity() {
            return city;
        }

        public String getRole() {
            return role;
        }

        public String getStatus() {
            return status;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getAccountStatus() {
            return accountStatus;
        }
    }
}