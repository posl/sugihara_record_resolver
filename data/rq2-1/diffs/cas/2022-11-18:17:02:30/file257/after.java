package org.apereo.cas.pm.impl;

import org.apereo.cas.pm.PasswordChangeRequest;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordValidationService;
import org.apereo.cas.util.RegexUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * This is {@link DefaultPasswordValidationService}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPasswordValidationService implements PasswordValidationService {
    private final String policyPattern;

    private final PasswordHistoryService passwordHistoryService;

    @Override
    public boolean isValid(final PasswordChangeRequest bean) {
        if (!StringUtils.hasText(bean.toPassword())) {
            LOGGER.error("Provided password is blank");
            return false;
        }
        if (bean.getCurrentPassword() != null && bean.toPassword().equals(bean.toCurrentPassword())) {
            LOGGER.error("Provided password cannot be the same as the current password");
            return false;
        }
        if (!bean.toPassword().equals(bean.toConfirmedPassword())) {
            LOGGER.error("Provided password does not match the confirmed password");
            return false;
        }
        if (!RegexUtils.find(policyPattern, bean.toPassword())) {
            LOGGER.error("Provided password does not match the pattern required for password policy [{}]", policyPattern);
            return false;
        }
        if (passwordHistoryService.exists(bean)) {
            LOGGER.error("Recycled password from password history is not allowed for [{}]", bean.getUsername());
            return false;
        }
        return validatePassword(bean);
    }

    /**
     * Validate password.
     *
     * @param bean the bean
     * @return true/false
     */
    protected boolean validatePassword(final PasswordChangeRequest bean) {
        return true;
    }
}
