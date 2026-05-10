package com.instantaccessprovider.arcagentad.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvisioningCommand(
        String jobId,
        @JsonProperty("eventType") String action,
        @JsonProperty("externalId") String identityId,
        @JsonProperty("displayName") String identityName,
        @JsonProperty("system") String targetSystem,
        String entitlementId,
        String entitlementName,
        String scope,
        @JsonProperty("changedAttributes") List<ChangedAttribute> attributes,
        @JsonProperty("timestamp") OffsetDateTime executeAt,
        String correlationId,
        String type,
        NestedTarget identity,
        NestedTarget entitlement
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangedAttribute(String name, Object oldValue, Object newValue) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NestedTarget(String name, String displayName, String externalId, String type, String system) {}
}
