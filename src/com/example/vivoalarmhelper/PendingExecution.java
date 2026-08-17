package com.example.vivoalarmhelper;

final class PendingExecution {
    final String token;
    final String profileId;
    final long requestedAt;
    final long runtimeBaseTargetAt;

    PendingExecution(String token, String profileId, long requestedAt,
            long runtimeBaseTargetAt) {
        this.token = token;
        this.profileId = profileId;
        this.requestedAt = requestedAt;
        this.runtimeBaseTargetAt = runtimeBaseTargetAt;
    }
}
