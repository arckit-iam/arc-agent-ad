// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcagentad.infrastructure.ad;

import com.instantaccessprovider.arcagentad.config.AgentProperties;
import com.instantaccessprovider.arcagentad.domain.ProvisioningCommand;
import com.instantaccessprovider.arcagentad.domain.port.ProvisioningHandlerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Component;

import javax.naming.directory.*;
import javax.naming.ldap.LdapName;

@Component
public class AdProvisioningHandler implements ProvisioningHandlerPort {

    private static final Logger log = LoggerFactory.getLogger(AdProvisioningHandler.class);

    private final AgentProperties properties;
    private final LdapTemplate ldapTemplate;

    public AdProvisioningHandler(AgentProperties properties, LdapTemplate ldapTemplate) {
        this.properties = properties;
        this.ldapTemplate = ldapTemplate;
    }

    @Override
    public void execute(ProvisioningCommand command, String topic) {
        if (properties.dryRun()) {
            log.info("[DRY-RUN] jobId={} action={} topic={} identity={} targetSystem={} executeAt={}",
                    command.jobId(), command.action(), topic, command.identityName(),
                    command.targetSystem(), command.executeAt());
            return;
        }

        log.info("Execution AD jobId={} action={} topic={} identity={}",
                command.jobId(), command.action(), topic, command.identityId());

        AgentProperties.Topics topics = properties.topics();
        String action = command.action() != null ? command.action().toUpperCase() : "";

        if (topic.equals(topics.identity())) {
            switch (action) {
                case "CREATE" -> createUser(command);
                case "MODIFY" -> modifyUser(command);
                case "DELETE" -> deleteUser(command);
                default -> log.warn("Action inconnue sur topic identity: action={} jobId={}", command.action(), command.jobId());
            }
        } else if (topic.equals(topics.assignment())) {
            switch (action) {
                case "CREATE" -> addGroupMember(command);
                case "DELETE" -> removeGroupMember(command);
                default -> log.warn("Action inconnue sur topic assignment: action={} jobId={}", command.action(), command.jobId());
            }
        } else if (topic.equals(topics.entitlement())) {
            switch (action) {
                case "CREATE" -> createGroup(command);
                case "MODIFY" -> modifyGroup(command);
                case "DELETE" -> deleteGroup(command);
                default -> log.warn("Action inconnue sur topic entitlement: action={} jobId={}", command.action(), command.jobId());
            }
        } else {
            log.warn("Topic inconnu ignore: topic={} jobId={}", topic, command.jobId());
        }
    }

    private void createUser(ProvisioningCommand command) {
        LdapName dn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityId())
                .build();
        Attributes attrs = new BasicAttributes(true);
        Attribute objClass = new BasicAttribute("objectClass");
        objClass.add("top");
        objClass.add("person");
        objClass.add("organizationalPerson");
        objClass.add("user");
        attrs.put(objClass);
        attrs.put(new BasicAttribute("sAMAccountName", command.identityId()));
        attrs.put(new BasicAttribute("cn", command.identityId()));
        if (command.identityName() != null && !command.identityName().isBlank()) {
            attrs.put(new BasicAttribute("displayName", command.identityName()));
        }
        ldapTemplate.bind(dn, null, attrs);
        log.info("Utilisateur cree: dn={}", dn);
        applyAttributesSafely(dn, command);
    }

    private void modifyUser(ProvisioningCommand command) {
        LdapName dn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityId())
                .build();
        applyAttributesSafely(dn, command);
    }

    private static final java.util.Map<String, String> ATTR_MAPPING = java.util.Map.of(
            "email", "mail",
            "firstname", "givenName",
            "lastname", "sn",
            "phone", "telephoneNumber",
            "mobile", "mobile",
            "title", "title",
            "department", "department",
            "company", "company"
    );

    private void applyAttributesSafely(LdapName dn, ProvisioningCommand command) {
        if (command.attributes() == null) return;
        command.attributes().stream()
                .filter(a -> a.newValue() != null)
                .forEach(a -> {
                    String ldapAttr = ATTR_MAPPING.getOrDefault(a.name(), a.name());
                    try {
                        ModificationItem mod = new ModificationItem(
                                DirContext.REPLACE_ATTRIBUTE,
                                new BasicAttribute(ldapAttr, a.newValue().toString()));
                        ldapTemplate.modifyAttributes(dn, new ModificationItem[]{mod});
                    } catch (Exception e) {
                        log.warn("Attribut ignore (schema inconnu?): name={} ldapAttr={} dn={} error={}", a.name(), ldapAttr, dn, e.getMessage());
                    }
                });
    }

    private void deleteUser(ProvisioningCommand command) {
        LdapName dn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityId())
                .build();
        ldapTemplate.unbind(dn);
        log.info("Utilisateur supprime: dn={}", dn);
    }

    private String resolveEntitlementId(ProvisioningCommand command) {
        // Pour les événements entitlement plats, externalId/name sont à la racine → mappés sur identityId
        return firstNonBlank(command.entitlementId(), command.identityId());
    }

    private String resolveEntitlementName(ProvisioningCommand command) {
        return firstNonBlank(command.entitlementName(), command.identityName());
    }

    private LdapName resolveGroupDn(ProvisioningCommand command) {
        return LdapNameBuilder.newInstance(resolveGroupOu(command))
                .add("CN", resolveEntitlementId(command))
                .build();
    }

    private String resolveGroupOu(ProvisioningCommand command) {
        return switch (command.scope() != null ? command.scope().toUpperCase() : "") {
            case "TIER0" -> properties.ad().tier0Ou();
            case "TIER1" -> properties.ad().tier1Ou();
            case "TIER2" -> properties.ad().tier2Ou();
            default -> {
                log.warn("Scope inconnu '{}', fallback sur Tier2 pour jobId={}", command.scope(), command.jobId());
                yield properties.ad().tier2Ou();
            }
        };
    }

    private void addGroupMember(ProvisioningCommand command) {
        LdapName groupDn = resolveGroupDnFromAssignment(command);
        LdapName userDn = resolveUserDnFromAssignment(command);
        ModificationItem mod = new ModificationItem(
                DirContext.ADD_ATTRIBUTE,
                new BasicAttribute("member", userDn.toString())
        );
        ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{mod});
        log.info("Membre ajoute: user={} group={}", userDn, groupDn);
    }

    private void removeGroupMember(ProvisioningCommand command) {
        LdapName groupDn = resolveGroupDnFromAssignment(command);
        LdapName userDn = resolveUserDnFromAssignment(command);
        ModificationItem mod = new ModificationItem(
                DirContext.REMOVE_ATTRIBUTE,
                new BasicAttribute("member", userDn.toString())
        );
        ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{mod});
        log.info("Membre retire: user={} group={}", userDn, groupDn);
    }

    private LdapName resolveUserDnFromAssignment(ProvisioningCommand command) {
        String cn = command.identity() != null
                ? firstNonBlank(command.identity().externalId(), command.identity().name(), command.identity().displayName(), command.identityId())
                : command.identityId();
        return LdapNameBuilder.newInstance(properties.ad().usersOu()).add("CN", cn).build();
    }

    private LdapName resolveGroupDnFromAssignment(ProvisioningCommand command) {
        String cn = command.entitlement() != null
                ? firstNonBlank(command.entitlement().externalId(), command.entitlement().name(), command.entitlement().displayName(), command.entitlementId())
                : command.entitlementId();
        return LdapNameBuilder.newInstance(resolveGroupOu(command)).add("CN", cn).build();
    }

    private void createGroup(ProvisioningCommand command) {
        LdapName dn = resolveGroupDn(command);
        String id = resolveEntitlementId(command);
        String displayName = resolveEntitlementName(command);
        Attributes attrs = new BasicAttributes(true);
        Attribute objClass = new BasicAttribute("objectClass");
        objClass.add("top");
        objClass.add("group");
        attrs.put(objClass);
        attrs.put(new BasicAttribute("sAMAccountName", id));
        attrs.put(new BasicAttribute("cn", id));
        if (displayName != null && !displayName.isBlank()) {
            attrs.put(new BasicAttribute("displayName", displayName));
        }
        ldapTemplate.bind(dn, null, attrs);
        log.info("Groupe cree: dn={}", dn);
    }

    private void modifyGroup(ProvisioningCommand command) {
        LdapName dn = resolveGroupDn(command);
        if (command.attributes() != null) {
            ModificationItem[] mods = command.attributes().stream()
                    .filter(a -> a.newValue() != null)
                    .map(a -> new ModificationItem(
                            DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute(a.name(), a.newValue().toString())))
                    .toArray(ModificationItem[]::new);
            ldapTemplate.modifyAttributes(dn, mods);
            log.info("Groupe modifie: dn={}", dn);
        }
    }

    private void deleteGroup(ProvisioningCommand command) {
        LdapName dn = resolveGroupDn(command);
        ldapTemplate.unbind(dn);
        log.info("Groupe supprime: dn={}", dn);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

