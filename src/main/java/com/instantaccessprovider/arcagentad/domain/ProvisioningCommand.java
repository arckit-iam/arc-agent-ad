package com.instantaccessprovider.arcagentad.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvisioningCommand(
        String jobId,
        String action,
        String identityId,
        String identityName,
        String targetSystem,
        String entitlementId,
        String entitlementName,
        String scope,
        Map<String, Object> attributes,
        OffsetDateTime executeAt,
        String correlationId
) {
}
