/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.authc.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Utilities for JWT validation.
 */
public class JwtValidateUtil {
    private static final Logger LOGGER = LogManager.getLogger(JwtValidateUtil.class);

    private static final JOSEObjectTypeVerifier<SecurityContext> JWT_HEADER_TYPE_VERIFIER = new DefaultJOSEObjectTypeVerifier<>(
        JOSEObjectType.JWT,
        null
    );

    /**
     * Validate a SignedJWT. Use iss/aud/alg filters for those claims, JWKSet for signature, and skew seconds for time claims.
     * @param jwt Signed JWT to be validated.
     * @param allowedIssuer Filter for the "iss" claim.
     * @param allowedAudiences Filter for the "aud" claim.
     * @param allowedClockSkewSeconds Skew tolerance for the "auth_time", "iat", "nbf", and "exp" claims.
     * @param allowedSignatureAlgorithms Filter for the "aud" header.
     * @param jwks JWKs of HMAC secret keys or RSA/EC public keys.
     * @throws Exception Error for the first validation to fail.
     */
    public static void validate(
        final SignedJWT jwt,
        final String allowedIssuer,
        final List<String> allowedAudiences,
        final long allowedClockSkewSeconds,
        final List<String> allowedSignatureAlgorithms,
        final List<JWK> jwks
    ) throws Exception {
        final Date now = new Date();
        LOGGER.debug(
            "Validating JWT, now ["
                + now
                + "], alg ["
                + jwt.getHeader().getAlgorithm()
                + "], issuer ["
                + jwt.getJWTClaimsSet().getIssuer()
                + "], audiences ["
                + jwt.getJWTClaimsSet().getAudience()
                + "], typ ["
                + jwt.getHeader().getType()
                + "], auth_time ["
                + jwt.getJWTClaimsSet().getDateClaim("auth_time")
                + "], iat ["
                + jwt.getJWTClaimsSet().getIssueTime()
                + "], nbf ["
                + jwt.getJWTClaimsSet().getIssueTime()
                + "], exp ["
                + jwt.getJWTClaimsSet().getExpirationTime()
                + "], kid ["
                + jwt.getHeader().getKeyID()
                + "], jti ["
                + jwt.getJWTClaimsSet().getJWTID()
        );
        // validate claims before signature, because log messages about rejected claims can be more helpful than rejected signatures
        JwtValidateUtil.validateType(jwt);
        JwtValidateUtil.validateIssuer(jwt, allowedIssuer);
        JwtValidateUtil.validateAudiences(jwt, allowedAudiences);
        JwtValidateUtil.validateSignatureAlgorithm(jwt, allowedSignatureAlgorithms);
        JwtValidateUtil.validateAuthTime(jwt, now, allowedClockSkewSeconds);
        JwtValidateUtil.validateIssuedAtTime(jwt, now, allowedClockSkewSeconds);
        JwtValidateUtil.validateNotBeforeTime(jwt, now, allowedClockSkewSeconds);
        JwtValidateUtil.validateExpiredTime(jwt, now, allowedClockSkewSeconds);
        JwtValidateUtil.validateSignature(jwt, jwks);
    }

    public static void validateType(final SignedJWT jwt) throws Exception {
        final JOSEObjectType jwtHeaderType = jwt.getHeader().getType();
        try {
            JwtValidateUtil.JWT_HEADER_TYPE_VERIFIER.verify(jwtHeaderType, null);
        } catch (Exception e) {
            throw new Exception("Invalid JWT type [" + jwtHeaderType + "].", e);
        }
    }

    public static void validateIssuer(final SignedJWT jwt, String allowedIssuer) throws Exception {
        final String issuer = jwt.getJWTClaimsSet().getIssuer();
        if ((issuer == null) || (allowedIssuer.equals(issuer) == false)) {
            throw new Exception("Rejected issuer [" + issuer + "]. Allowed [" + allowedIssuer + "]");
        }
    }

    public static void validateAudiences(final SignedJWT jwt, List<String> allowedAudiences) throws Exception {
        final List<String> audiences = jwt.getJWTClaimsSet().getAudience();
        if ((audiences == null) || (allowedAudiences.stream().anyMatch(audiences::contains) == false)) {
            final String audiencesString = (audiences == null) ? "null" : String.join(",", audiences);
            throw new Exception("Rejected audiences [" + audiencesString + "]. Allowed [" + allowedAudiences + "]");
        }
    }

    public static void validateSignatureAlgorithm(final SignedJWT jwt, final List<String> allowedAlgorithms) throws Exception {
        final JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
        if ((algorithm == null) || (allowedAlgorithms.contains(algorithm.getName()) == false)) {
            throw new Exception("Rejected algorithm [" + algorithm + "]. Allowed [" + String.join(",", allowedAlgorithms) + "]");
        }
    }

    public static void validateAuthTime(final SignedJWT jwt, final Date now, final long allowedClockSkewSeconds) throws Exception {
        JwtValidateUtil.validateAuthTime(jwt.getJWTClaimsSet().getDateClaim("auth_time"), now, allowedClockSkewSeconds);
    }

    // package private, so this logic can be called from unit tests without constructing a SignedJWT
    static void validateAuthTime(final Date authTime, final Date now, final long allowedClockSkewSeconds) throws Exception {
        if (authTime == null) {
            return; // optional
        } else if (now == null) {
            throw new Exception("Invalid now [null].");
        } else if (allowedClockSkewSeconds < 0L) {
            throw new Exception("Invalid negative allowedClockSkewSeconds [" + allowedClockSkewSeconds + "].");
        }
        // skewSec=0 auth_time=3:00:00.000 now=2:59:59.999 --> fail
        // skewSec=0 auth_time=3:00:00.000 now=3:00:00.000 --> pass
        // skewSec=1 auth_time=3:00:00.000 now=2:59:59.999 --> pass (subtract skew from auth_time)
        if ((authTime.getTime() - (allowedClockSkewSeconds * 1000L)) > now.getTime()) {
            throw new Exception(
                "Invalid auth_time ["
                    + authTime.getTime()
                    + "ms/"
                    + authTime
                    + "] > now ["
                    + now.getTime()
                    + "ms/"
                    + now
                    + "] with skew ["
                    + (allowedClockSkewSeconds * 1000L)
                    + "ms]."
            );
        }
    }

    public static void validateIssuedAtTime(final SignedJWT jwt, final Date now, final long allowedClockSkewSeconds) throws Exception {
        JwtValidateUtil.validateIssuedAtTime(jwt.getJWTClaimsSet().getIssueTime(), now, allowedClockSkewSeconds);
    }

    // package private, so this logic can be called from unit tests without constructing a SignedJWT
    static void validateIssuedAtTime(final Date iat, final Date now, final long allowedClockSkewSeconds) throws Exception {
        if (iat == null) {
            throw new Exception("Invalid iat [null].");
        } else if (now == null) {
            throw new Exception("Invalid now [null].");
        } else if (allowedClockSkewSeconds < 0L) {
            throw new Exception("Invalid negative allowedClockSkewSeconds [" + allowedClockSkewSeconds + "].");
        }
        // skewSec=0 iat=3:00:00.000 now=2:59:59.999 --> fail
        // skewSec=0 iat=3:00:00.000 now=3:00:00.000 --> pass
        // skewSec=1 iat=3:00:00.000 now=2:59:59.999 --> pass (subtract skew from iat)
        if ((iat.getTime() - (allowedClockSkewSeconds * 1000L)) > now.getTime()) {
            throw new Exception(
                "Invalid iat ["
                    + iat.getTime()
                    + "ms/"
                    + iat
                    + "] > now ["
                    + now.getTime()
                    + "ms/"
                    + now
                    + "] with skew ["
                    + (allowedClockSkewSeconds * 1000L)
                    + "ms]."
            );
        }
    }

    public static void validateNotBeforeTime(final SignedJWT jwt, final Date now, final long allowedClockSkewSeconds) throws Exception {
        JwtValidateUtil.validateNotBeforeTime(jwt.getJWTClaimsSet().getNotBeforeTime(), now, allowedClockSkewSeconds);
    }

    // package private, so this logic can be called from unit tests without constructing a SignedJWT
    static void validateNotBeforeTime(final Date nbf, final Date now, final long allowedClockSkewSeconds) throws Exception {
        if (nbf == null) {
            return; // optional
        } else if (now == null) {
            throw new Exception("Invalid now [null].");
        } else if (allowedClockSkewSeconds < 0L) {
            throw new Exception("Invalid negative allowedClockSkewSeconds [" + allowedClockSkewSeconds + "].");
        }
        // skewSec=0 nbf=3:00:00.000 now=2:59:59.999 --> fail
        // skewSec=0 nbf=3:00:00.000 now=3:00:00.000 --> pass
        // skewSec=1 nbf=3:00:00.000 now=2:59:59.999 --> pass (subtract skew from nbf)
        if ((nbf.getTime() - (allowedClockSkewSeconds * 1000L)) > now.getTime()) {
            throw new Exception(
                "Invalid nbf ["
                    + nbf.getTime()
                    + "ms/"
                    + nbf
                    + "] > now ["
                    + now.getTime()
                    + "ms/"
                    + now
                    + "] with skew ["
                    + (allowedClockSkewSeconds * 1000L)
                    + "ms]."
            );
        }
    }

    public static void validateExpiredTime(final SignedJWT jwt, final Date now, final long allowedClockSkewSeconds) throws Exception {
        JwtValidateUtil.validateExpiredTime(jwt.getJWTClaimsSet().getExpirationTime(), now, allowedClockSkewSeconds);
    }

    // package private, so this logic can be called from unit tests without constructing a SignedJWT
    static void validateExpiredTime(final Date exp, final Date now, final long allowedClockSkewSeconds) throws Exception {
        if (exp == null) {
            throw new Exception("Invalid exp [null].");
        } else if (now == null) {
            throw new Exception("Invalid now [null].");
        } else if (allowedClockSkewSeconds < 0L) {
            throw new Exception("Invalid allowedClockSkewSeconds [" + allowedClockSkewSeconds + "] < 0.");
        }
        // skewSec=0 now=2:59:59.999 exp=3:00:00.000 --> pass
        // skewSec=0 now=3:00:00.000 exp=3:00:00.000 --> fail
        // skewSec=1 now=3:00:00.000 exp=3:00:00.000 --> pass (subtract skew from now)
        if (now.getTime() - (allowedClockSkewSeconds * 1000L) >= exp.getTime()) {
            throw new Exception(
                "Invalid exp ["
                    + exp.getTime()
                    + "ms/"
                    + exp
                    + "] < now ["
                    + now.getTime()
                    + "ms/"
                    + now
                    + "] with skew ["
                    + (allowedClockSkewSeconds * 1000L)
                    + "ms]."
            );
        }
    }

    /**
     * Look through each JWK in the JWKSet to see if they can validate the Signed JWT signature.
     * Apply JWT kid and JWT alg filters to the JWKs to skip unnecessary signature checking.
     *
     * If JWT kid is present, and any JWK kid matches, only use the matching subset of JWKs. Ignore the rest.
     * Note: JWK kid should be unique. However, this method does not assume they are unique. Each match will be tried.
     *
     * Depending on the JWT alg, certain HMAC/RSA/EC JWKs can be excluded.
     * HMAC JWKs that do not meet the minimum length requirement are ignored.
     * RSA JWKs that do not meet the minimum length requirement are ignored.
     * EC JWKs that do not meet the exact curve requirement are ignored.
     *
     * @param jwt Signed JWT to be validated.
     * @param jwks JWKSet of HMAC/RSA/EC JWKs. At least one JWK is required to succeed.
     * @throws Exception Error if JWKs fail to validate the Signed JWT.
     */
    public static void validateSignature(final SignedJWT jwt, final List<JWK> jwks) throws Exception {
        final String id = jwt.getHeader().getKeyID();
        final JWSAlgorithm alg = jwt.getHeader().getAlgorithm();
        LOGGER.trace("JWKs [" + jwks.size() + "], JWT KID [ + id + ], and JWT Algorithm [" + alg.getName() + "] before filters.");

        final List<JWK> jwksKid = jwks.stream().filter(j -> ((id == null) || (j.getKeyID() == null) || (id.equals(j.getKeyID())))).toList();
        LOGGER.trace("JWKs [" + jwksKid.size() + "] after KID [" + id + "||null] filter.");

        final List<JWK> jwksAlg = jwksKid.stream().filter(j -> (j.getAlgorithm() == null) || (alg.equals(j.getAlgorithm()))).toList();
        LOGGER.trace("JWKs [" + jwksAlg.size() + " after Algorithm [" + alg.getName() + "||null] filter.");

        final List<JWK> jwksStrength = jwksAlg.stream().filter(j -> JwkValidateUtil.isMatch(j, alg.getName())).toList();
        LOGGER.trace("JWKs [" + jwksStrength.size() + "] after Algorithm [" + alg + "] match filter.");

        for (final JWK jwk : jwksStrength) {
            if (jwt.verify(JwtValidateUtil.createJwsVerifier(jwk))) {
                return; // VERIFY SUCCEEDED
            }
        }
        throw new Exception("Verify failed using " + jwksStrength.size() + " of " + jwks.size() + " provided JWKs.");
    }

    public static JWSVerifier createJwsVerifier(final JWK jwk) throws JOSEException {
        if (jwk instanceof RSAKey rsaKey) {
            return new RSASSAVerifier(rsaKey);
        } else if (jwk instanceof ECKey ecKey) {
            return new ECDSAVerifier(ecKey);
        } else if (jwk instanceof OctetSequenceKey octetSequenceKey) {
            return new MACVerifier(octetSequenceKey);
        }
        throw JwtValidateUtil.createExceptionInvalidJwkClass(jwk);
    }

    public static JWSSigner createJwsSigner(final JWK jwk) throws JOSEException {
        if (jwk instanceof RSAKey rsaKey) {
            return new RSASSASigner(rsaKey);
        } else if (jwk instanceof ECKey ecKey) {
            return new ECDSASigner(ecKey);
        } else if (jwk instanceof OctetSequenceKey octetSequenceKey) {
            return new MACSigner(octetSequenceKey);
        }
        throw JwtValidateUtil.createExceptionInvalidJwkClass(jwk);
    }

    private static JOSEException createExceptionInvalidJwkClass(final JWK jwk) {
        return new JOSEException(
            "Unsupported JWK class ["
                + (jwk == null ? "null" : jwk.getClass().getCanonicalName())
                + "]. Supported classes are ["
                + RSAKey.class.getCanonicalName()
                + ", "
                + ECKey.class.getCanonicalName()
                + ", "
                + OctetSequenceKey.class.getCanonicalName()
                + "]."
        );
    }

    public static boolean verifyJWT(final JWSVerifier jwtVerifier, final SignedJWT signedJwt) throws Exception {
        return signedJwt.verify(jwtVerifier);
    }

    public static SignedJWT signJwt(final JWSSigner jwtSigner, final SignedJWT unsignedJwt) throws JOSEException, ParseException {
        final SignedJWT signedJwt = new SignedJWT(unsignedJwt.getHeader(), unsignedJwt.getJWTClaimsSet());
        signedJwt.sign(jwtSigner);
        return signedJwt;
    }
}
