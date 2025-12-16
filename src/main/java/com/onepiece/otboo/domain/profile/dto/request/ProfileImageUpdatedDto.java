package com.onepiece.otboo.domain.profile.dto.request;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageUpdatedDto(
    UUID userId,
    String profileImageUrl,
    Instant updatedAt
) {

}
