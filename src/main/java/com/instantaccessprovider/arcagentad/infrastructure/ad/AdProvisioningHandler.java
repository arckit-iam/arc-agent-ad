package com.instantaccessprovider.arcagentad.infrastructure.ad;

import com.instantaccessprovider.arcagentad.config.AgentProperties;
import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;
import com.instantaccessprovider.arcagentad.domain.port.ProvisioningHandlerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdProvisioningHandler implements ProvisioningHandlerPort {

    private static final Logger log = LoggerFactory.getLogger(AdProvisioningHandler.class);

    private final AgentProperties properties;

    public AdProvisioningHandler(AgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void execute(ProvisioningCommand command) {
        if (properties.dryRun()) {
            log.info("[DRY-RUN] jobId={} action={} identity={} entitlement={} targetSystem={} executeAt={}",
                    command.jobId(),
                    command.action(),
                    command.identityName(),
                    command.entitlementName(),
                    command.targetSystem(),
                    command.executeAt());
            return;
        }

        // Point d'extension: remplacer ce bloc par l'appel AD reel (LDAP/PowerShell/Graph).
        log.info("Execution AD jobId={} action={} identityId={} entitlementId={} targetSystem={}",
                command.jobId(),
                command.action(),
                command.identityId(),
                command.entitlementId(),
                command.targetSystem());
    }
}
