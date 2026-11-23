package io.github.shhome.homehub.synology.dto;

public record SynologyStorageItemResponse(
        String name,
        SynologyStorageItemType type,
        SynologyStorageHealth health
) {
}