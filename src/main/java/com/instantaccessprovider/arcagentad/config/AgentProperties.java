// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcagentad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "arc.agent")
public record AgentProperties(
        String targetSystem,
        Topics topics,
        boolean dryRun,
        Ad ad
) {
    public record Topics(String identity, String assignment, String entitlement) {}

    public record Ad(
            String url,
            String baseDn,
            String bindDn,
            String bindPassword,
            String usersOu,
            String tier0Ou,
            String tier1Ou,
            String tier2Ou,
            Map<String, String> accountTypeOus
    ) {}
}
