/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.security.authc.jwt;

import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xpack.core.security.authc.RealmSettings;
import org.elasticsearch.xpack.core.security.authc.support.ClaimSetting;
import org.elasticsearch.xpack.core.security.authc.support.DelegatedAuthorizationSettings;
import org.elasticsearch.xpack.core.ssl.SSLConfigurationSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Settings for JWT realms.
 */
public class JwtRealmSettings {

    private JwtRealmSettings() {}

    public static final String TYPE = "jwt";

    // Signature algorithms
    public static final List<String> SUPPORTED_SIGNATURE_ALGORITHMS_HMAC = List.of("HS256", "HS384", "HS512");
    public static final List<String> SUPPORTED_SIGNATURE_ALGORITHMS_RSA = List.of("RS256", "RS384", "RS512", "PS256", "PS384", "PS512");
    public static final List<String> SUPPORTED_SIGNATURE_ALGORITHMS_EC = List.of("ES256", "ES384", "ES512");
    public static final List<String> SUPPORTED_SIGNATURE_ALGORITHMS_PKC = Stream.of(
        SUPPORTED_SIGNATURE_ALGORITHMS_RSA,
        SUPPORTED_SIGNATURE_ALGORITHMS_EC
    ).flatMap(Collection::stream).toList();
    public static final List<String> SUPPORTED_SIGNATURE_ALGORITHMS = Stream.of(
        SUPPORTED_SIGNATURE_ALGORITHMS_HMAC,
        SUPPORTED_SIGNATURE_ALGORITHMS_PKC
    ).flatMap(Collection::stream).toList();

    // Header names
    public static final String CLIENT_AUTHENTICATION_TYPE_SHARED_SECRET = "SharedSecret";
    public static final String CLIENT_AUTHENTICATION_TYPE_NONE = "None";
    public static final List<String> CLIENT_AUTHENTICATION_TYPES = List.of(
        CLIENT_AUTHENTICATION_TYPE_SHARED_SECRET,
        CLIENT_AUTHENTICATION_TYPE_NONE
    );

    // Default values and min/max constraints

    private static final TimeValue DEFAULT_ALLOWED_CLOCK_SKEW = TimeValue.timeValueSeconds(60);
    private static final List<String> DEFAULT_ALLOWED_SIGNATURE_ALGORITHMS = Collections.singletonList("RS256");
    private static final boolean DEFAULT_POPULATE_USER_METADATA = true;
    private static final String DEFAULT_CLIENT_AUTHENTICATION_TYPE = CLIENT_AUTHENTICATION_TYPE_SHARED_SECRET;
    private static final String DEFAULT_JWT_VALIDATION_CACHE_HASH_ALGO = "ssha256";
    private static final TimeValue DEFAULT_JWT_VALIDATION_CACHE_TTL = TimeValue.timeValueMinutes(20);
    private static final int DEFAULT_JWT_VALIDATION_CACHE_MAX_USERS = 100_000;
    private static final int MIN_JWT_VALIDATION_CACHE_MAX_USERS = 0;
    private static final TimeValue DEFAULT_ROLES_LOOKUP_CACHE_TTL = TimeValue.timeValueMinutes(20);
    private static final int DEFAULT_ROLES_LOOKUP_CACHE_MAX_USERS = 100_000;
    private static final int MIN_ROLES_LOOKUP_CACHE_MAX_USERS = 0;
    private static final TimeValue DEFAULT_HTTP_CONNECT_TIMEOUT = TimeValue.timeValueSeconds(5);
    private static final TimeValue DEFAULT_HTTP_CONNECTION_READ_TIMEOUT = TimeValue.timeValueSeconds(5);
    private static final TimeValue DEFAULT_HTTP_SOCKET_TIMEOUT = TimeValue.timeValueSeconds(5);
    private static final int DEFAULT_HTTP_MAX_CONNECTIONS = 200;
    private static final int MIN_HTTP_MAX_CONNECTIONS = 0;
    private static final int DEFAULT_HTTP_MAX_ENDPOINT_CONNECTIONS = 200;
    private static final int MIN_HTTP_MAX_ENDPOINT_CONNECTIONS = 0;

    // All settings

    /**
     * Get all secure and non-secure settings.
     * @return All secure and non-secure settings.
     */
    public static Set<Setting.AffixSetting<?>> getSettings() {
        final Set<Setting.AffixSetting<?>> set = Sets.newHashSet();
        set.addAll(JwtRealmSettings.getNonSecureSettings());
        set.addAll(JwtRealmSettings.getSecureSettings());
        return set;
    }

    /**
     * Get all non-secure settings.
     * @return All non-secure settings.
     */
    public static Set<Setting.AffixSetting<?>> getNonSecureSettings() {
        final Set<Setting.AffixSetting<?>> set = Sets.newHashSet();
        // Standard realm settings: order, enabled
        set.addAll(RealmSettings.getStandardSettings(TYPE));
        // JWT Issuer settings
        set.addAll(List.of(ALLOWED_ISSUER, ALLOWED_SIGNATURE_ALGORITHMS, ALLOWED_CLOCK_SKEW, PKC_JWKSET_PATH));
        // JWT Audience settings
        set.addAll(List.of(ALLOWED_AUDIENCES));
        // JWT End-user settings
        set.addAll(
            List.of(
                CLAIMS_PRINCIPAL.getClaim(),
                CLAIMS_PRINCIPAL.getPattern(),
                CLAIMS_GROUPS.getClaim(),
                CLAIMS_GROUPS.getPattern(),
                POPULATE_USER_METADATA
            )
        );
        // JWT Client settings
        set.addAll(List.of(CLIENT_AUTHENTICATION_TYPE));
        // Standard HTTP settings for outgoing connections to get JWT issuer jwkset_path
        set.addAll(
            List.of(
                HTTP_CONNECT_TIMEOUT,
                HTTP_CONNECTION_READ_TIMEOUT,
                HTTP_SOCKET_TIMEOUT,
                HTTP_MAX_CONNECTIONS,
                HTTP_MAX_ENDPOINT_CONNECTIONS
            )
        );
        // Standard TLS connection settings for outgoing connections to get JWT issuer jwkset_path
        set.addAll(SSL_CONFIGURATION_SETTINGS);
        // JWT End-user delegated authorization settings: authorization_realms
        set.addAll(DELEGATED_AUTHORIZATION_REALMS_SETTINGS);
        return set;
    }

    /**
     * Get all secure settings.
     * @return All secure settings.
     */
    public static List<Setting.AffixSetting<SecureString>> getSecureSettings() {
        return List.of(HMAC_JWKSET, HMAC_KEY, CLIENT_AUTHENTICATION_SHARED_SECRET);
    }

    // JWT issuer settings
    public static final Setting.AffixSetting<String> ALLOWED_ISSUER = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "allowed_issuer",
        key -> Setting.simpleString(key, value -> verifyNonNullNotEmpty(key, value, null), Setting.Property.NodeScope)
    );
    public static final Setting.AffixSetting<TimeValue> ALLOWED_CLOCK_SKEW = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "allowed_clock_skew",
        key -> Setting.timeSetting(key, DEFAULT_ALLOWED_CLOCK_SKEW, Setting.Property.NodeScope)
    );
    public static final Setting.AffixSetting<List<String>> ALLOWED_SIGNATURE_ALGORITHMS = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "allowed_signature_algorithms",
        key -> Setting.listSetting(
            key,
            DEFAULT_ALLOWED_SIGNATURE_ALGORITHMS,
            Function.identity(),
            values -> verifyNonNullNotEmpty(key, values, SUPPORTED_SIGNATURE_ALGORITHMS),
            Setting.Property.NodeScope
        )
    );

    public static final Setting.AffixSetting<String> PKC_JWKSET_PATH = RealmSettings.simpleString(
        TYPE,
        "pkc_jwkset_path",
        Setting.Property.NodeScope
    );

    public static final Setting.AffixSetting<SecureString> HMAC_JWKSET = RealmSettings.secureString(TYPE, "hmac_jwkset");
    public static final Setting.AffixSetting<SecureString> HMAC_KEY = RealmSettings.secureString(TYPE, "hmac_key");

    // JWT audience settings

    public static final Setting.AffixSetting<List<String>> ALLOWED_AUDIENCES = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "allowed_audiences",
        key -> Setting.stringListSetting(key, values -> verifyNonNullNotEmpty(key, values, null), Setting.Property.NodeScope)
    );

    // JWT end-user settings

    // Note: ClaimSetting is a wrapper for two individual settings: getClaim(), getPattern()
    public static final ClaimSetting CLAIMS_PRINCIPAL = new ClaimSetting(TYPE, "principal");
    public static final ClaimSetting CLAIMS_GROUPS = new ClaimSetting(TYPE, "groups");

    public static final Setting.AffixSetting<Boolean> POPULATE_USER_METADATA = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "populate_user_metadata",
        key -> Setting.boolSetting(key, DEFAULT_POPULATE_USER_METADATA, Setting.Property.NodeScope)
    );

    // Client authentication settings for incoming connections

    public static final Setting.AffixSetting<String> CLIENT_AUTHENTICATION_TYPE = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "client_authentication.type",
        key -> Setting.simpleString(key, DEFAULT_CLIENT_AUTHENTICATION_TYPE, value -> {
            if (CLIENT_AUTHENTICATION_TYPES.contains(value) == false) {
                throw new IllegalArgumentException(
                    "Invalid value [" + value + "] for [" + key + "]. Allowed values are " + CLIENT_AUTHENTICATION_TYPES + "."
                );
            }
        }, Setting.Property.NodeScope)
    );

    public static final Setting.AffixSetting<SecureString> CLIENT_AUTHENTICATION_SHARED_SECRET = RealmSettings.secureString(
        TYPE,
        "client_authentication.shared_secret"
    );

    // Individual outgoing HTTP settings

    public static final Setting.AffixSetting<TimeValue> HTTP_CONNECT_TIMEOUT = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "http.connect_timeout",
        key -> Setting.timeSetting(key, DEFAULT_HTTP_CONNECT_TIMEOUT, Setting.Property.NodeScope)
    );

    public static final Setting.AffixSetting<TimeValue> HTTP_CONNECTION_READ_TIMEOUT = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "http.connection_read_timeout",
        key -> Setting.timeSetting(key, DEFAULT_HTTP_CONNECTION_READ_TIMEOUT, Setting.Property.NodeScope)
    );
    public static final Setting.AffixSetting<TimeValue> HTTP_SOCKET_TIMEOUT = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "http.socket_timeout",
        key -> Setting.timeSetting(key, DEFAULT_HTTP_SOCKET_TIMEOUT, Setting.Property.NodeScope)
    );
    public static final Setting.AffixSetting<Integer> HTTP_MAX_CONNECTIONS = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "http.max_connections",
        key -> Setting.intSetting(key, DEFAULT_HTTP_MAX_CONNECTIONS, MIN_HTTP_MAX_CONNECTIONS, Setting.Property.NodeScope)
    );
    public static final Setting.AffixSetting<Integer> HTTP_MAX_ENDPOINT_CONNECTIONS = Setting.affixKeySetting(
        RealmSettings.realmSettingPrefix(TYPE),
        "http.max_endpoint_connections",
        key -> Setting.intSetting(key, DEFAULT_HTTP_MAX_ENDPOINT_CONNECTIONS, MIN_HTTP_MAX_ENDPOINT_CONNECTIONS, Setting.Property.NodeScope)
    );

    // SSL Configuration settings

    public static final Collection<Setting.AffixSetting<?>> SSL_CONFIGURATION_SETTINGS = SSLConfigurationSettings.getRealmSettings(TYPE);
    public static final SSLConfigurationSettings ssl = SSLConfigurationSettings.withoutPrefix(true);

    // Delegated Authorization Realms settings

    public static final Collection<Setting.AffixSetting<?>> DELEGATED_AUTHORIZATION_REALMS_SETTINGS = DelegatedAuthorizationSettings
        .getSettings(TYPE);

    private static void verifyNonNullNotEmpty(final String key, final String value, final List<String> allowedValues) {
        assert value != null : "Invalid null value for [" + key + "].";
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty value for [" + key + "].");
        }
        if (allowedValues != null) {
            if (allowedValues.contains(value) == false) {
                throw new IllegalArgumentException(
                    "Invalid value [" + value + "] for [" + key + "]. Allowed values are " + allowedValues + "."
                );
            }
        }
    }

    private static void verifyNonNullNotEmpty(final String key, final List<String> values, final List<String> allowedValues) {
        assert values != null : "Invalid null list of values for [" + key + "].";
        if (values.isEmpty()) {
            if (allowedValues == null) {
                throw new IllegalArgumentException("Invalid empty list for [" + key + "].");
            } else {
                throw new IllegalArgumentException("Invalid empty list for [" + key + "]. Allowed values are " + allowedValues + ".");
            }
        }
        for (final String value : values) {
            verifyNonNullNotEmpty(key, value, allowedValues);
        }
    }
}
