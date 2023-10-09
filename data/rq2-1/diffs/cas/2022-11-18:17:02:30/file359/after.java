package org.apereo.cas.web.support;

import org.apereo.cas.audit.RedisAuditTrailManager;
import org.apereo.cas.redis.core.CasRedisTemplate;


import lombok.val;
import org.apereo.inspektr.audit.AuditActionContext;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.springframework.data.redis.core.BoundValueOperations;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Works in conjunction with a redis database to
 * block attempts to dictionary attack users.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@SuppressWarnings("JavaUtilDate")
public class RedisThrottledSubmissionHandlerInterceptorAdapter extends AbstractInspektrAuditHandlerInterceptorAdapter {
    private final transient CasRedisTemplate<String, Object> redisTemplate;

    private final long scanCount;

    public RedisThrottledSubmissionHandlerInterceptorAdapter(
        final ThrottledSubmissionHandlerConfigurationContext configurationContext,
        final CasRedisTemplate<String, Object> redisTemplate,
        final long scanCount) {
        super(configurationContext);
        this.redisTemplate = redisTemplate;
        this.scanCount = scanCount;
    }

    @Override
    public boolean exceedsThreshold(final HttpServletRequest request) {
        val clientInfo = ClientInfoHolder.getClientInfo();
        val remoteAddress = clientInfo.getClientIpAddress();
        val throttle = getConfigurationContext().getCasProperties().getAuthn().getThrottle();
        val keys = redisTemplate.scan(RedisAuditTrailManager.CAS_AUDIT_CONTEXT_PREFIX + '*', this.scanCount);
        val username = getUsernameParameterFromRequest(request);
        val failures = keys
            .map((Function<String, BoundValueOperations>) this.redisTemplate::boundValueOps)
            .map(BoundValueOperations::get)
            .map(AuditActionContext.class::cast)
            .filter(audit ->
                audit.getPrincipal().equalsIgnoreCase(username)
                && audit.getClientIpAddress().equalsIgnoreCase(remoteAddress)
                && audit.getActionPerformed().equalsIgnoreCase(throttle.getFailure().getCode())
                && audit.getApplicationCode().equalsIgnoreCase(throttle.getCore().getAppCode())
                && audit.getWhenActionWasPerformed().compareTo(getFailureInRangeCutOffDate()) >= 0)
            .sorted(Comparator.comparing(AuditActionContext::getWhenActionWasPerformed).reversed())
            .limit(2)
            .map(this::toThrottledSubmission)
            .collect(Collectors.toList());
        return calculateFailureThresholdRateAndCompare(failures);
    }

    @Override
    public String getName() {
        return "RedisThrottle";
    }
}
