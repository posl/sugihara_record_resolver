package org.apereo.cas.gauth.credential;

import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.redis.core.CasRedisTemplate;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.crypto.CipherExecutor;

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is {@link RedisGoogleAuthenticatorTokenCredentialRepository}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@ToString
@Getter
public class RedisGoogleAuthenticatorTokenCredentialRepository extends BaseGoogleAuthenticatorTokenCredentialRepository {
    private static final String KEY_SEPARATOR = ":";

    private static final String CAS_PREFIX = RedisGoogleAuthenticatorTokenCredentialRepository.class.getSimpleName();

    private final CasRedisTemplate<String, List<? extends OneTimeTokenAccount>> template;

    private final long scanCount;

    public RedisGoogleAuthenticatorTokenCredentialRepository(
        final IGoogleAuthenticator googleAuthenticator,
        final CasRedisTemplate<String, List<? extends OneTimeTokenAccount>> template,
        final CipherExecutor<String, String> tokenCredentialCipher,
        final CipherExecutor<Number, Number> scratchCodesCipher,
        final long scanCount) {
        super(tokenCredentialCipher, scratchCodesCipher, googleAuthenticator);
        this.template = template;
        this.scanCount = scanCount;
    }

    private static String getGoogleAuthenticatorRedisKey(final OneTimeTokenAccount account) {
        return CAS_PREFIX + KEY_SEPARATOR + account.getUsername().trim().toLowerCase(Locale.ENGLISH) + KEY_SEPARATOR + account.getId();
    }

    @Override
    public OneTimeTokenAccount get(final String username, final long id) {
        try (val keys = getGoogleAuthenticatorTokenKeys(username, String.valueOf(id))) {
            val keySet = keys.collect(Collectors.toSet());
            if (keySet.size() == 1) {
                val r = this.template.boundValueOps(keySet.iterator().next()).get();
                if (r != null && !r.isEmpty()) {
                    return decode(r.get(0));
                }
            }
            return null;
        }
    }

    @Override
    public OneTimeTokenAccount get(final long id) {
        try (val keys = getGoogleAuthenticatorTokenKeys("*", String.valueOf(id))) {
            val keySet = keys.collect(Collectors.toSet());
            if (keySet.size() == 1) {
                val r = this.template.boundValueOps(keySet.iterator().next()).get();
                if (r != null && !r.isEmpty()) {
                    return decode(r.get(0));
                }
            }
            return null;
        }
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> get(final String username) {
        try (val keys = getGoogleAuthenticatorTokenKeys(username, "*")) {
            return keys
                .map(key -> this.template.boundValueOps(key).get())
                .filter(Objects::nonNull)
                .map(this::decode)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> load() {
        try (val keys = getGoogleAuthenticatorTokenKeys()) {
            return (Collection) keys
                .map(redisKey -> this.template.boundValueOps(redisKey).get())
                .filter(Objects::nonNull)
                .map(this::decode)
                .collect(Collectors.toList());
        }
    }

    @Override
    public OneTimeTokenAccount save(final OneTimeTokenAccount account) {
        return update(account);
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount account) {
        val encodedAccount = encode(account);
        val redisKey = getGoogleAuthenticatorRedisKey(account);
        LOGGER.trace("Saving [{}] using key [{}]", encodedAccount, redisKey);
        val ops = this.template.boundValueOps(redisKey);
        ops.set(CollectionUtils.wrapList(encodedAccount));
        return encodedAccount;
    }

    @Override
    public void deleteAll() {
        try (val keys = getGoogleAuthenticatorTokenKeys()) {
            val redisKey = keys.collect(Collectors.toSet());
            LOGGER.trace("Deleting tokens using key [{}]", redisKey);
            this.template.delete(redisKey);
            LOGGER.trace("Deleted tokens");
        }
    }

    @Override
    public void delete(final String username) {
        try (val keys = getGoogleAuthenticatorTokenKeys(username, "*")) {
            val redisKey = keys.collect(Collectors.toSet());
            LOGGER.trace("Deleting tokens using key [{}]", redisKey);
            this.template.delete(redisKey);
            LOGGER.trace("Deleted tokens");
        }
    }

    @Override
    public void delete(final long id) {
        try (val keys = getGoogleAuthenticatorTokenKeys("*", String.valueOf(id))) {
            val redisKey = keys.collect(Collectors.toSet());
            LOGGER.trace("Deleting tokens using key [{}]", redisKey);
            this.template.delete(redisKey);
            LOGGER.trace("Deleted tokens");
        }
    }

    @Override
    public long count() {
        try (val keys = getGoogleAuthenticatorTokenKeys()) {
            return keys.count();
        }
    }

    @Override
    public long count(final String username) {
        try (val keys = getGoogleAuthenticatorTokenKeys(username, "*")) {
            return keys.count();
        }
    }

    private Stream<String> getGoogleAuthenticatorTokenKeys(final String username, final String id) {
        val key = CAS_PREFIX + KEY_SEPARATOR + username.trim().toLowerCase(Locale.ENGLISH) + KEY_SEPARATOR + id;
        LOGGER.trace("Fetching Google Authenticator records based on key [{}]", key);
        return template.scan(key, this.scanCount);
    }

    private Stream<String> getGoogleAuthenticatorTokenKeys() {
        val key = CAS_PREFIX + KEY_SEPARATOR + "*:*";
        LOGGER.trace("Fetching Google Authenticator records based on key [{}]", key);
        return template.scan(key, this.scanCount);
    }
}
