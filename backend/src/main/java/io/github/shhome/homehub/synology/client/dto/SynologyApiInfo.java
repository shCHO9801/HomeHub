package io.github.shhome.homehub.synology.client.dto;

public record SynologyApiInfo(
        String path,
        int minVersion,
        int maxVersion,
        String requestFormat
) {
}