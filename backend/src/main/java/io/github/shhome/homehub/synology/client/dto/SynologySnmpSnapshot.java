package io.github.shhome.homehub.synology.client.dto;

import java.util.List;

public record SynologySnmpSnapshot(
        List<SnmpVolumeData> volumes,
        List<SnmpStorageItemData> storageItems
) {
}