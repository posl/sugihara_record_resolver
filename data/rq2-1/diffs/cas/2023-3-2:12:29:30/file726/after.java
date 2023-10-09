package org.apereo.cas.impl.account;

import org.apereo.cas.api.PasswordlessUserAccount;
import org.apereo.cas.api.PasswordlessUserAccountStore;
import org.apereo.cas.configuration.model.support.passwordless.account.PasswordlessAuthenticationLdapAccountsProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapConnectionFactory;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.LoggingUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * This is {@link LdapPasswordlessUserAccountStore}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@Slf4j
@RequiredArgsConstructor
public class LdapPasswordlessUserAccountStore implements PasswordlessUserAccountStore {
    private final LdapConnectionFactory connectionFactory;

    private final PasswordlessAuthenticationLdapAccountsProperties ldapProperties;

    @Override
    public Optional<PasswordlessUserAccount> findUser(final String username) {
        try {
            val filter = LdapUtils.newLdaptiveSearchFilter(ldapProperties.getSearchFilter(),
                LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME,
                CollectionUtils.wrap(username));

            LOGGER.debug("Constructed LDAP filter [{}] to locate passwordless account", filter);
            val response = connectionFactory.executeSearchOperation(ldapProperties.getBaseDn(), filter, ldapProperties.getPageSize());
            LOGGER.debug("LDAP response for passwordless account is [{}]", response);

            if (LdapUtils.containsResultEntry(response)) {
                val entry = response.getEntry();
                val acctBuilder = PasswordlessUserAccount.builder().username(username).name(username);

                if (entry.getAttribute(ldapProperties.getNameAttribute()) != null) {
                    acctBuilder.name(entry.getAttribute(ldapProperties.getNameAttribute()).getStringValue());
                }
                if (entry.getAttribute(ldapProperties.getEmailAttribute()) != null) {
                    acctBuilder.email(entry.getAttribute(ldapProperties.getEmailAttribute()).getStringValue());
                }
                if (entry.getAttribute(ldapProperties.getPhoneAttribute()) != null) {
                    acctBuilder.phone(entry.getAttribute(ldapProperties.getPhoneAttribute()).getStringValue());
                }
                if (entry.getAttribute(ldapProperties.getRequestPasswordAttribute()) != null) {
                    val value = entry.getAttribute(ldapProperties.getRequestPasswordAttribute()).getStringValue();
                    acctBuilder.requestPassword(BooleanUtils.toBoolean(value));
                }
                val attributes = new LinkedHashMap<String, List<String>>(entry.getAttributes().size());
                entry.getAttributes().forEach(attr -> attributes.put(attr.getName(), new ArrayList<>(attr.getStringValues())));
                val acct = acctBuilder.attributes(attributes).build();
                return Optional.of(acct);
            }
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return Optional.empty();
    }
}
