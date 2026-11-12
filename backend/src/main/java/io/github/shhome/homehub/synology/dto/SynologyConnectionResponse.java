package io.github.shhome.homehub.synology.dto;

import java.time.OffsetDateTime;

public record SynologyConnectionResponse(
        SynologyConnectionStatus status,
        OffsetDateTime checkedAt
) {
}