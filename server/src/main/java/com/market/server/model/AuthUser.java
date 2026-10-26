package com.market.server.model;

import java.util.UUID;

public record AuthUser(
        UUID id,
        String email,
        String passwordHash,
        String name,
        String phone,
        String role
) {
}
