package com.instantaccessprovider.arcagentad.domain;

import java.time.OffsetDateTime;
import java.util.Map;

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
