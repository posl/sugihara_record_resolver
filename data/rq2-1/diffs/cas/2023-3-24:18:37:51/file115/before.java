package org.apereo.cas.gauth.token;

import org.apereo.cas.couchdb.gauth.token.CouchDbGoogleAuthenticatorToken;
import org.apereo.cas.couchdb.gauth.token.GoogleAuthenticatorTokenCouchDbRepository;
import org.apereo.cas.otp.repository.token.BaseOneTimeTokenRepository;
import org.apereo.cas.util.LoggingUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.ektorp.UpdateConflictException;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * This is {@link GoogleAuthenticatorCouchDbTokenRepository}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@Slf4j
@RequiredArgsConstructor
@Deprecated(since = "7.0.0")
public class GoogleAuthenticatorCouchDbTokenRepository extends BaseOneTimeTokenRepository<GoogleAuthenticatorToken> {

    private final GoogleAuthenticatorTokenCouchDbRepository couchDb;
    private final long expireTokensInSeconds;
    
    @Override
    public void store(final GoogleAuthenticatorToken token) {
        couchDb.add(new CouchDbGoogleAuthenticatorToken(token));
    }

    @Override
    public GoogleAuthenticatorToken get(final String uid, final Integer otp) {
        return couchDb.findOneByUidForOtp(uid, otp);
    }

    @Override
    public void remove(final String uid, final Integer otp) {
        couchDb.findByUidForOtp(uid, otp).forEach(couchDb::deleteToken);
    }

    @Override
    public void remove(final String uid) {
        couchDb.findByUserId(uid).forEach(couchDb::deleteToken);
    }

    @Override
    public void remove(final Integer otp) {
        couchDb.findByToken(otp).forEach(couchDb::deleteToken);
    }

    @Override
    public void removeAll() {
        couchDb.getAll().forEach(couchDb::deleteToken);
    }

    @Override
    public long count(final String uid) {
        return couchDb.countByUserId(uid);
    }

    @Override
    public long count() {
        return couchDb.count();
    }

    @Override
    protected void cleanInternal() {
        try {
            val since = LocalDateTime.now(ZoneId.systemDefault()).minusSeconds(expireTokensInSeconds);
            LOGGER.debug("Removing tokens older than [{}]", since);
            couchDb.findByIssuedDateTimeBefore(since).forEach(couchDb::remove);
        } catch (final UpdateConflictException e) {
            LoggingUtils.warn(LOGGER, e);
        }
    }
}
