/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.whispersystems.textsecuregcm.configuration.dynamic.DynamicConfiguration;
import org.whispersystems.textsecuregcm.redis.FaultTolerantRedisCluster;
import org.whispersystems.textsecuregcm.storage.DynamicConfigurationManager;
import org.whispersystems.textsecuregcm.util.MockUtils;

@SuppressWarnings("unchecked")
public class RateLimitersTest {

  private final DynamicConfiguration configuration = mock(DynamicConfiguration.class);

  private final DynamicConfigurationManager<DynamicConfiguration> dynamicConfig =
      MockUtils.buildMock(DynamicConfigurationManager.class, cfg -> when(cfg.getConfiguration()).thenReturn(configuration));

  private final FaultTolerantRedisCluster redisCluster = mock(FaultTolerantRedisCluster.class);

  private static final String BAD_YAML = """
      limits:
        smsVoicePrefix:
          bucketSize: 150
          leakRatePerMinute: 10
        unexpected:
          bucketSize: 4
          leakRatePerMinute: 2
      """;

  private static final String GOOD_YAML = """
      limits:
        smsVoicePrefix:
          bucketSize: 150
          leakRatePerMinute: 10
        attachmentCreate:
          bucketSize: 4
          leakRatePerMinute: 2
      """;

  public record GenericHolder(@Valid @NotNull @JsonProperty Map<String, RateLimiterConfig> limits) {
  }

  @Test
  public void testValidateConfigs() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      final GenericHolder cfg = DynamicConfigurationManager.parseConfiguration(BAD_YAML, GenericHolder.class).orElseThrow();
      final RateLimiters rateLimiters = new RateLimiters(cfg.limits(), dynamicConfig, redisCluster);
      rateLimiters.validateValuesAndConfigs();
    });

    final GenericHolder cfg = DynamicConfigurationManager.parseConfiguration(GOOD_YAML, GenericHolder.class).orElseThrow();
    final RateLimiters rateLimiters = new RateLimiters(cfg.limits(), dynamicConfig, redisCluster);
    rateLimiters.validateValuesAndConfigs();
  }

  @Test
  public void testValidateDuplicates() throws Exception {
    final TestDescriptor td1 = new TestDescriptor("id1");
    final TestDescriptor td2 = new TestDescriptor("id2");
    final TestDescriptor td3 = new TestDescriptor("id3");
    final TestDescriptor tdDup = new TestDescriptor("id1");

    assertThrows(IllegalStateException.class, () -> new BaseRateLimiters<>(
        new TestDescriptor[] { td1, td2, td3, tdDup },
        Collections.emptyMap(),
        dynamicConfig,
        redisCluster) {});

    new BaseRateLimiters<>(
        new TestDescriptor[] { td1, td2, td3 },
        Collections.emptyMap(),
        dynamicConfig,
        redisCluster) {};
  }

  @Test
  void testUnchangingConfiguration() {
    final RateLimiters rateLimiters = new RateLimiters(Collections.emptyMap(), dynamicConfig, redisCluster);
    final RateLimiter limiter = rateLimiters.getRateLimitResetLimiter();
    final RateLimiterConfig expected = RateLimiters.For.RATE_LIMIT_RESET.defaultConfig();
    assertEquals(expected, limiter.config());
  }

  @Test
  void testChangingConfiguration() {
    final RateLimiterConfig initialRateLimiterConfig = new RateLimiterConfig(4, 1);
    final RateLimiterConfig updatedRateLimiterCongig = new RateLimiterConfig(17, 19);
    final RateLimiterConfig baseConfig = new RateLimiterConfig(1, 1);

    final Map<String, RateLimiterConfig> limitsConfigMap = new HashMap<>();

    limitsConfigMap.put(RateLimiters.For.RECAPTCHA_CHALLENGE_ATTEMPT.id(), baseConfig);
    limitsConfigMap.put(RateLimiters.For.RECAPTCHA_CHALLENGE_SUCCESS.id(), baseConfig);

    when(configuration.getLimits()).thenReturn(limitsConfigMap);

    final RateLimiters rateLimiters = new RateLimiters(Collections.emptyMap(), dynamicConfig, redisCluster);
    final RateLimiter limiter = rateLimiters.getRateLimitResetLimiter();

    limitsConfigMap.put(RateLimiters.For.RATE_LIMIT_RESET.id(), initialRateLimiterConfig);
    assertEquals(initialRateLimiterConfig, limiter.config());

    assertEquals(baseConfig, rateLimiters.getRecaptchaChallengeAttemptLimiter().config());
    assertEquals(baseConfig, rateLimiters.getRecaptchaChallengeSuccessLimiter().config());

    limitsConfigMap.put(RateLimiters.For.RATE_LIMIT_RESET.id(), updatedRateLimiterCongig);
    assertEquals(updatedRateLimiterCongig, limiter.config());

    assertEquals(baseConfig, rateLimiters.getRecaptchaChallengeAttemptLimiter().config());
    assertEquals(baseConfig, rateLimiters.getRecaptchaChallengeSuccessLimiter().config());
  }

  @Test
  public void testRateLimiterHasItsPrioritiesStraight() throws Exception {
    final RateLimiters.For descriptor = RateLimiters.For.RECAPTCHA_CHALLENGE_ATTEMPT;
    final RateLimiterConfig configForDynamic = new RateLimiterConfig(1, 1);
    final RateLimiterConfig configForStatic = new RateLimiterConfig(2, 2);
    final RateLimiterConfig defaultConfig = descriptor.defaultConfig();

    final Map<String, RateLimiterConfig> mapForDynamic = new HashMap<>();
    final Map<String, RateLimiterConfig> mapForStatic = new HashMap<>();

    when(configuration.getLimits()).thenReturn(mapForDynamic);

    final RateLimiters rateLimiters = new RateLimiters(mapForStatic, dynamicConfig, redisCluster);
    final RateLimiter limiter = rateLimiters.forDescriptor(descriptor);

    // test only default is present
    mapForDynamic.remove(descriptor.id());
    mapForStatic.remove(descriptor.id());
    assertEquals(defaultConfig, limiter.config());

    // test dynamic and no static
    mapForDynamic.put(descriptor.id(), configForDynamic);
    mapForStatic.remove(descriptor.id());
    assertEquals(configForDynamic, limiter.config());

    // test dynamic and static
    mapForDynamic.put(descriptor.id(), configForDynamic);
    mapForStatic.put(descriptor.id(), configForStatic);
    assertEquals(configForDynamic, limiter.config());

    // test static, but no dynamic
    mapForDynamic.remove(descriptor.id());
    mapForStatic.put(descriptor.id(), configForStatic);
    assertEquals(configForStatic, limiter.config());
  }

  private record TestDescriptor(String id) implements RateLimiterDescriptor {

    @Override
    public boolean isDynamic() {
      return false;
    }

    @Override
    public RateLimiterConfig defaultConfig() {
      return new RateLimiterConfig(1, 1);
    }
  }
}
