// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcagentad.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instantaccessprovider.arcagentad.config.AgentConfig;
import com.instantaccessprovider.arcagentad.config.AgentProperties;
import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;
import com.instantaccessprovider.arcagentad.domain.port.ProvisioningHandlerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

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
                "order-1",
                "ASSIGN",
                "id-1",
                "jdupont",
                "ad://AD01",
                "ent-1",
                "GRP-FIN",
                "EMEA",
                List.of(new ProvisioningCommand.ChangedAttribute("ou", null, "Finance")),
                OffsetDateTime.parse("2026-06-01T08:00:00+02:00"),
                "corr-1",
                null,
                null,
                null,
                null
        );
        String payload = objectMapper.writeValueAsString(command);

        processor.process(payload, "arc.provisioning.assignment");

        verify(handler, times(1)).execute(argThat(cmd ->
            cmd != null
                && "order-1".equals(cmd.orderId())
                && "ASSIGN".equals(cmd.action())
                && "ad://AD01".equals(cmd.targetSystem())
        ), argThat(t -> "arc.provisioning.assignment".equals(t)));
    }

    @Test
    void shouldIgnoreCommandWhenTargetDoesNotMatch() throws Exception {
        ProvisioningCommand command = new ProvisioningCommand(
                "order-2",
                "ASSIGN",
                "id-1",
                "jdupont",
                "ad://AD02",
                "ent-1",
                "GRP-FIN",
                "EMEA",
                List.of(),
                OffsetDateTime.parse("2026-06-01T08:00:00+02:00"),
                "corr-2",
                null,
                null,
                null,
                null
        );
        String payload = objectMapper.writeValueAsString(command);

        processor.process(payload, "arc.provisioning.assignment");

        verify(handler, never()).execute(argThat(cmd ->
            cmd != null && "order-2".equals(cmd.orderId())
        ), org.mockito.ArgumentMatchers.any());
    }
}
