package io.github.shhome.homehub.synology.config;

import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(
        prefix = "homehub.synology.snmp",
        name = "enabled",
        havingValue = "true"
)
public class SynologySnmpConfig {

    @Bean(destroyMethod = "close")
    public Snmp synologySnmp(
            SynologySnmpProperties properties
    ) throws IOException {
        SecurityProtocols securityProtocols =
                SecurityProtocols.getInstance();
        securityProtocols.addDefaultProtocols();
        securityProtocols.addAuthenticationProtocol(
                new AuthSHA()
        );

        USM usm = new USM(
                securityProtocols,
                new OctetString(MPv3.createLocalEngineID()),
                0
        );

        SecurityModels.getInstance()
                .addSecurityModel(usm);

        Snmp snmp = new Snmp(
                new DefaultUdpTransportMapping()
        );

        OctetString securityName =
                new OctetString(properties.username());

        snmp.getUSM().addUser(
                new UsmUser(
                        securityName,
                        AuthSHA.ID,
                        new OctetString(
                                properties.authPassword()
                        ),
                        PrivAES128.ID,
                        new OctetString(
                                properties.privPassword()
                        )
                )
        );

        snmp.listen();

        return snmp;
    }

    @Bean
    public UserTarget<UdpAddress> synologySnmpTarget(
            SynologySnmpProperties properties
    ) {
        UserTarget<UdpAddress> target =
                new UserTarget<>();

        target.setAddress(
                new UdpAddress(
                        properties.host()
                        + "/"
                        + properties.port()
                )
        );
        target.setVersion(SnmpConstants.version3);
        target.setSecurityName(
                new OctetString(properties.username())
        );
        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        target.setTimeout(
                properties.timeout().toMillis()
        );
        target.setRetries(properties.retries());

        return target;
    }
}
