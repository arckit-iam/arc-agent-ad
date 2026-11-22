// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcagentad.infrastructure.ad;

import com.instantaccessprovider.arcagentad.config.AgentProperties;
import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdProvisioningHandlerTest {

    private static final String IDENTITY_TOPIC = "arc.provisioning.identity";

    private LdapTemplate ldapTemplate;
    private AdProvisioningHandler handler;

    @BeforeEach
    void setUp() {
        ldapTemplate = mock(LdapTemplate.class);
        AgentProperties.Ad ad = new AgentProperties.Ad(
                "ldaps://localhost:636", "DC=corp,DC=local", "cn=svc", "secret",
                "OU=Users,DC=corp,DC=local",
                "OU=Tier0,DC=corp,DC=local", "OU=Tier1,DC=corp,DC=local", "OU=Tier2,DC=corp,DC=local",
                Map.of("standard", "OU=Standard,OU=Tier2,DC=corp,DC=local",
                        "admin", "OU=Admins,OU=Tier0,DC=corp,DC=local"));
        AgentProperties properties = new AgentProperties("ad://AD01",
                new AgentProperties.Topics(IDENTITY_TOPIC, "arc.provisioning.assignment", "arc.provisioning.entitlement"),
                false, ad);
        handler = new AdProvisioningHandler(properties, ldapTemplate);
    }

    private static ProvisioningCommand command(String action, String accountType) {
        return new ProvisioningCommand("order-1", action, "jdupont", "Jean Dupont", "ad://AD01",
                null, null, null, List.of(), null, "account:jdupont", "user", accountType, null, null);
    }

    @Test
    void createUser_isPlacedInTheOuOfItsAccountType() {
        handler.execute(command("CREATE", "standard"), IDENTITY_TOPIC);

        ArgumentCaptor<LdapName> dn = ArgumentCaptor.forClass(LdapName.class);
        verify(ldapTemplate).bind(dn.capture(), isNull(), any(Attributes.class));
        assertThat(dn.getValue().toString()).isEqualTo("CN=jdupont,OU=Standard,OU=Tier2,DC=corp,DC=local");
    }

    @Test
    void createAdminUser_goesToTheAdminOu() {
        handler.execute(command("CREATE", "ADMIN"), IDENTITY_TOPIC);

        ArgumentCaptor<LdapName> dn = ArgumentCaptor.forClass(LdapName.class);
        verify(ldapTemplate).bind(dn.capture(), isNull(), any(Attributes.class));
        assertThat(dn.getValue().toString()).isEqualTo("CN=jdupont,OU=Admins,OU=Tier0,DC=corp,DC=local");
    }

    @Test
    void deleteUser_targetsTheOuOfItsAccountType() {
        handler.execute(command("DELETE", "standard"), IDENTITY_TOPIC);

        ArgumentCaptor<LdapName> dn = ArgumentCaptor.forClass(LdapName.class);
        verify(ldapTemplate).unbind(dn.capture());
        assertThat(dn.getValue().toString()).isEqualTo("CN=jdupont,OU=Standard,OU=Tier2,DC=corp,DC=local");
    }

    @Test
    void withoutAccountType_fallsBackOnDefaultUsersOu() {
        handler.execute(command("CREATE", null), IDENTITY_TOPIC);

        ArgumentCaptor<LdapName> dn = ArgumentCaptor.forClass(LdapName.class);
        verify(ldapTemplate).bind(dn.capture(), isNull(), any(Attributes.class));
        assertThat(dn.getValue().toString()).isEqualTo("CN=jdupont,OU=Users,DC=corp,DC=local");
    }

    @Test
    void unknownAccountType_isRefusedInsteadOfBeingStoredSomewhereElse() {
        assertThatThrownBy(() -> handler.execute(command("CREATE", "contractor"), IDENTITY_TOPIC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contractor");
    }

    @Test
    void withoutAccountTypeAndWithoutDefaultOu_isRefused() {
        AgentProperties.Ad ad = new AgentProperties.Ad("ldaps://x", "DC=corp", "cn=svc", "s",
                null, null, null, null, Map.of());
        AdProvisioningHandler noDefault = new AdProvisioningHandler(
                new AgentProperties("ad://AD01", new AgentProperties.Topics(IDENTITY_TOPIC, "a", "e"), false, ad),
                ldapTemplate);

        assertThatThrownBy(() -> noDefault.execute(command("CREATE", null), IDENTITY_TOPIC))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
