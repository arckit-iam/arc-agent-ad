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
    public void execute(ProvisioningCommand command) {
        if (properties.dryRun()) {
            log.info("[DRY-RUN] jobId={} action={} identity={} entitlement={} targetSystem={} executeAt={}",
                    command.jobId(), command.action(), command.identityName(),
                    command.entitlementName(), command.targetSystem(), command.executeAt());
            return;
        }

        log.info("Execution AD jobId={} action={} identity={} entitlement={}",
                command.jobId(), command.action(), command.identityId(), command.entitlementId());

        switch (command.action().toUpperCase()) {
            case "CREATE_IDENTITY" -> createUser(command);
            case "DELETE_IDENTITY" -> deleteUser(command);
            case "ASSIGN"          -> addGroupMember(command);
            case "REVOKE"          -> removeGroupMember(command);
            case "CREATE_GROUP"    -> createGroup(command);
            case "DELETE_GROUP"    -> deleteGroup(command);
            default -> log.warn("Action inconnue ignoree: action={} jobId={}", command.action(), command.jobId());
        }
    }

    private void createUser(ProvisioningCommand command) {
        LdapName dn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityName())
                .build();
        Attributes attrs = new BasicAttributes(true);
        attrs.put(new BasicAttribute("objectClass", "user"));
        attrs.put(new BasicAttribute("sAMAccountName", command.identityId()));
        attrs.put(new BasicAttribute("cn", command.identityName()));
        if (command.attributes() != null) {
            command.attributes().forEach((k, v) -> attrs.put(new BasicAttribute(k, v.toString())));
        }
        ldapTemplate.bind(dn, null, attrs);
        log.info("Utilisateur cree: dn={}", dn);
    }

    private void deleteUser(ProvisioningCommand command) {
        LdapName dn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityName())
                .build();
        ldapTemplate.unbind(dn);
        log.info("Utilisateur supprime: dn={}", dn);
    }

    private LdapName resolveGroupDn(ProvisioningCommand command) {
        String tierOu = switch (command.scope() != null ? command.scope().toUpperCase() : "") {
            case "TIER0" -> properties.ad().tier0Ou();
            case "TIER1" -> properties.ad().tier1Ou();
            case "TIER2" -> properties.ad().tier2Ou();
            default -> {
                log.warn("Scope inconnu '{}', fallback sur Tier2 pour jobId={}", command.scope(), command.jobId());
                yield properties.ad().tier2Ou();
            }
        };
        return LdapNameBuilder.newInstance(tierOu)
                .add("CN", command.entitlementId())
                .build();
    }

    private void addGroupMember(ProvisioningCommand command) {
        LdapName groupDn = resolveGroupDn(command);
        LdapName userDn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityName())
                .build();
        ModificationItem mod = new ModificationItem(
                DirContext.ADD_ATTRIBUTE,
                new BasicAttribute("member", userDn.toString())
        );
        ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{mod});
        log.info("Membre ajoute: user={} group={}", userDn, groupDn);
    }

    private void removeGroupMember(ProvisioningCommand command) {
        LdapName groupDn = resolveGroupDn(command);
        LdapName userDn = LdapNameBuilder.newInstance(properties.ad().usersOu())
                .add("CN", command.identityName())
                .build();
        ModificationItem mod = new ModificationItem(
                DirContext.REMOVE_ATTRIBUTE,
                new BasicAttribute("member", userDn.toString())
        );
        ldapTemplate.modifyAttributes(groupDn, new ModificationItem[]{mod});
        log.info("Membre retire: user={} group={}", userDn, groupDn);
    }

    private void createGroup(ProvisioningCommand command) {
        LdapName dn = resolveGroupDn(command);
        Attributes attrs = new BasicAttributes(true);
        Attribute objClass = new BasicAttribute("objectClass");
        objClass.add("top");
        objClass.add("group");
        attrs.put(objClass);
        attrs.put(new BasicAttribute("sAMAccountName", command.entitlementId()));
        attrs.put(new BasicAttribute("cn", command.entitlementName()));
        ldapTemplate.bind(dn, null, attrs);
        log.info("Groupe cree: dn={}", dn);
    }

    private void deleteGroup(ProvisioningCommand command) {
        LdapName dn = resolveGroupDn(command);
        ldapTemplate.unbind(dn);
        log.info("Groupe supprime: dn={}", dn);
    }
}

