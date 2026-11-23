package io.github.shhome.homehub.synology.client.dto;

public record SnmpVolumeData(
        String path,
        long allocationUnit,
        long totalUnits,
        long usedUnits
) {
}