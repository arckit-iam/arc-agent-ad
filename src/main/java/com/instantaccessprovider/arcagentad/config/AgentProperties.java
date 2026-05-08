package com.instantaccessprovider.arcagentad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arc.agent")
public record AgentProperties(
        String targetSystem,
        String topics,
        boolean dryRun
) {
}
