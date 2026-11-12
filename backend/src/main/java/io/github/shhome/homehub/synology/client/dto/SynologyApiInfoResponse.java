package io.github.shhome.homehub.synology.client.dto;

import java.util.Map;

public record SynologyApiInfoResponse(
        boolean success,
        Map<String, SynologyApiInfo> data,
        SynologyApiError error
) {
}