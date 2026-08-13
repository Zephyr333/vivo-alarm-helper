package com.example.vivoalarmhelper;

final class PendingExecution {
    final String token;
    final String profileId;
    final long requestedAt;

    PendingExecution(String token, String profileId, long requestedAt) {
        this.token = token;
        this.profileId = profileId;
        this.requestedAt = requestedAt;
    }
}
