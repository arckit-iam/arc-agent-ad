package com.instantaccessprovider.arcagentad.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcagentad.config.AgentConfig;
import com.instantaccessprovider.arcagentad.config.AgentProperties;
import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;
import com.instantaccessprovider.arcagentad.domain.port.ProvisioningHandlerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProvisioningCommandProcessorTest {

    private ProvisioningHandlerPort handler;
    private ProvisioningCommandProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = mock(ProvisioningHandlerPort.class);
        objectMapper = new AgentConfig().objectMapper();
        AgentProperties properties = new AgentProperties(
                "ad://AD01",
                new AgentProperties.Topics("arc.provisioning.identity", "arc.provisioning.assignment", "arc.provisioning.entitlement"),
                true,
                null
        );
        processor = new ProvisioningCommandProcessor(objectMapper, properties, handler);
    }

    @Test
    void shouldHandleCommandWhenTargetMatches() throws Exception {
        ProvisioningCommand command = new ProvisioningCommand(
                "job-1",
                "ASSIGN",
                "id-1",
                "jdupont",
                "ad://AD01",
                "ent-1",
                "GRP-FIN",
                "EMEA",
                Map.of("ou", "Finance"),
                OffsetDateTime.parse("2026-06-01T08:00:00+02:00"),
                "corr-1"
        );
        String payload = objectMapper.writeValueAsString(command);

        processor.process(payload, "arc.provisioning.assignment");

        verify(handler, times(1)).execute(argThat(cmd ->
            cmd != null
                && "job-1".equals(cmd.jobId())
                && "ASSIGN".equals(cmd.action())
                && "ad://AD01".equals(cmd.targetSystem())
        ));
    }

    @Test
    void shouldIgnoreCommandWhenTargetDoesNotMatch() throws Exception {
        ProvisioningCommand command = new ProvisioningCommand(
                "job-2",
                "ASSIGN",
                "id-1",
                "jdupont",
                "ad://AD02",
                "ent-1",
                "GRP-FIN",
                "EMEA",
                Map.of(),
                OffsetDateTime.parse("2026-06-01T08:00:00+02:00"),
                "corr-2"
        );
        String payload = objectMapper.writeValueAsString(command);

        processor.process(payload, "arc.provisioning.assignment");

        verify(handler, never()).execute(argThat(cmd ->
            cmd != null && "job-2".equals(cmd.jobId())
        ));
    }
}
