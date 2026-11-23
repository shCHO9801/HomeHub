package io.github.shhome.homehub.synology.client;

import io.github.shhome.homehub.synology.client.dto.SnmpStorageItemData;
import io.github.shhome.homehub.synology.client.dto.SnmpVolumeData;
import io.github.shhome.homehub.synology.client.dto.SynologySnmpSnapshot;
import io.github.shhome.homehub.synology.config.SynologySnmpProperties;
import lombok.RequiredArgsConstructor;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "homehub.synology.snmp",
        name = "enabled",
        havingValue = "true"
)
public class SynologySnmpClient {

    private static final Pattern VOLUME_PATH_PATTERN =
            Pattern.compile("^/volume\\d+$");

    private static final OID[] VOLUME_COLUMNS = {
            new OID("1.3.6.1.2.1.25.2.3.1.3"),
            new OID("1.3.6.1.2.1.25.2.3.1.4"),
            new OID("1.3.6.1.2.1.25.2.3.1.5"),
            new OID("1.3.6.1.2.1.25.2.3.1.6")
    };

    private static final OID[] STORAGE_ITEM_COLUMNS = {
            new OID("1.3.6.1.4.1.6574.3.1.1.2"),
            new OID("1.3.6.1.4.1.6574.3.1.1.3")
    };

    private final Snmp snmp;
    private final UserTarget<UdpAddress> synologySnmpTarget;
    private final SynologySnmpProperties properties;

    public SynologySnmpSnapshot getSnapshot() {
        TableUtils tableUtils = new TableUtils(
                snmp,
                new DefaultPDUFactory(PDU.GETBULK)
        );

        return new SynologySnmpSnapshot(
                getVolumes(tableUtils),
                getStorageItems(tableUtils)
        );
    }

    private List<SnmpVolumeData> getVolumes(
            TableUtils tableUtils
    ) {
        List<TableEvent> events = tableUtils.getTable(
                synologySnmpTarget,
                VOLUME_COLUMNS,
                null,
                null,
                getTableTimeoutSeconds()
        );

        List<SnmpVolumeData> volumes =
                new ArrayList<>();

        for (TableEvent event : events) {
            VariableBinding[] columns =
                    getColumns(event, VOLUME_COLUMNS.length);

            String path =
                    columns[0].getVariable().toString();

            if (!VOLUME_PATH_PATTERN.matcher(path).matches()) {
                continue;
            }

            volumes.add(
                    new SnmpVolumeData(
                            path,
                            columns[1].getVariable().toLong(),
                            columns[2].getVariable().toLong(),
                            columns[3].getVariable().toLong()
                    )
            );
        }

        return List.copyOf(volumes);
    }

    private List<SnmpStorageItemData> getStorageItems(
            TableUtils tableUtils
    ) {
        List<TableEvent> events = tableUtils.getTable(
                synologySnmpTarget,
                STORAGE_ITEM_COLUMNS,
                null,
                null,
                getTableTimeoutSeconds()
        );

        List<SnmpStorageItemData> storageItems =
                new ArrayList<>();

        for (TableEvent event : events) {
            VariableBinding[] columns =
                    getColumns(
                            event,
                            STORAGE_ITEM_COLUMNS.length
                    );

            storageItems.add(
                    new SnmpStorageItemData(
                            columns[0]
                                    .getVariable()
                                    .toString(),
                            columns[1]
                                    .getVariable()
                                    .toInt()
                    )
            );
        }

        return List.copyOf(storageItems);
    }

    private VariableBinding[] getColumns(
            TableEvent event,
            int expectedColumnCount
    ) {
        if (event.isError()) {
            throw new IllegalStateException(
                    "Synology SNMP 테이블 조회에 실패했습니다."
                            + " status=" + event.getStatus()
                            + ", message=" + event.getErrorMessage()
            );
        }

        VariableBinding[] columns = event.getColumns();

        if (columns == null
                || columns.length != expectedColumnCount) {
            throw new IllegalStateException(
                    "Synology SNMP 응답 형식이 올바르지 않습니다."
            );
        }

        for (VariableBinding column : columns) {
            if (column == null
                    || column.getVariable() == null) {
                throw new IllegalStateException(
                        "Synology SNMP 응답 값이 누락되었습니다."
                );
            }
        }

        return columns;
    }

    private long getTableTimeoutSeconds() {
        long requestCount = properties.retries() + 1L;
        long timeoutMillis =
                properties.timeout().toMillis()
                        * requestCount;

        return Math.max(
                1L,
                (timeoutMillis + 999L) / 1_000L
        );
    }
}
