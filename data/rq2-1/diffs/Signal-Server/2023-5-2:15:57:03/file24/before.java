/*
 * Copyright 2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vdurmont.semver4j.Semver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.whispersystems.textsecuregcm.captcha.Action;
import org.whispersystems.textsecuregcm.limits.RateLimiterConfig;
import org.whispersystems.textsecuregcm.limits.RateLimiters;
import org.whispersystems.textsecuregcm.storage.DynamicConfigurationManager;
import org.whispersystems.textsecuregcm.util.ua.ClientPlatform;

class DynamicConfigurationTest {

  private static final String REQUIRED_CONFIG = """
      captcha:
        scoreFloor: 1.0
      """;

  @Test
  void testParseExperimentConfig() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertFalse(emptyConfig.getExperimentEnrollmentConfiguration("test").isPresent());
    }

    {
      final String experimentConfigYaml = REQUIRED_CONFIG.concat("""
          experiments:
            percentageOnly:
              enrollmentPercentage: 12
            uuidsAndPercentage:
              enrolledUuids:
                - 717b1c09-ed0b-4120-bb0e-f4697534b8e1
                - 279f264c-56d7-4bbf-b9da-de718ff90903
              enrollmentPercentage: 77
            uuidsOnly:
              enrolledUuids:
                - 71618739-114c-4b1f-bb0d-6478a44eb600
            uuids-with-dash:
              enrolledUuids:
                - 71618739-114c-4b1f-bb0d-6478ffffffff
          """);

      final DynamicConfiguration config =
          DynamicConfigurationManager.parseConfiguration(experimentConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertFalse(config.getExperimentEnrollmentConfiguration("unconfigured").isPresent());

      assertTrue(config.getExperimentEnrollmentConfiguration("percentageOnly").isPresent());
      assertEquals(12, config.getExperimentEnrollmentConfiguration("percentageOnly").get().getEnrollmentPercentage());
      assertEquals(Collections.emptySet(),
          config.getExperimentEnrollmentConfiguration("percentageOnly").get().getEnrolledUuids());

      assertTrue(config.getExperimentEnrollmentConfiguration("uuidsAndPercentage").isPresent());
      assertEquals(77,
          config.getExperimentEnrollmentConfiguration("uuidsAndPercentage").get().getEnrollmentPercentage());
      assertEquals(Set.of(UUID.fromString("717b1c09-ed0b-4120-bb0e-f4697534b8e1"),
          UUID.fromString("279f264c-56d7-4bbf-b9da-de718ff90903")),
          config.getExperimentEnrollmentConfiguration("uuidsAndPercentage").get().getEnrolledUuids());

      assertTrue(config.getExperimentEnrollmentConfiguration("uuidsOnly").isPresent());
      assertEquals(0, config.getExperimentEnrollmentConfiguration("uuidsOnly").get().getEnrollmentPercentage());
      assertEquals(Set.of(UUID.fromString("71618739-114c-4b1f-bb0d-6478a44eb600")),
          config.getExperimentEnrollmentConfiguration("uuidsOnly").get().getEnrolledUuids());

      assertTrue(config.getExperimentEnrollmentConfiguration("uuids-with-dash").isPresent());
      assertEquals(0, config.getExperimentEnrollmentConfiguration("uuids-with-dash").get().getEnrollmentPercentage());
      assertEquals(Set.of(UUID.fromString("71618739-114c-4b1f-bb0d-6478ffffffff")),
          config.getExperimentEnrollmentConfiguration("uuids-with-dash").get().getEnrolledUuids());
    }
  }

  @Test
  void testParsePreRegistrationExperiments() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertFalse(emptyConfig.getPreRegistrationEnrollmentConfiguration("test").isPresent());
    }

    {
      final String experimentConfigYaml = REQUIRED_CONFIG.concat("""
          preRegistrationExperiments:
            percentageOnly:
              enrollmentPercentage: 17
            e164sCountryCodesAndPercentage:
              enrolledE164s:
                - +120255551212
                - +3655323174
              excludedE164s:
                - +120255551213
                - +3655323175
              enrollmentPercentage: 46
              excludedCountryCodes:
                - 47
              includedCountryCodes:
                - 56
            e164sAndExcludedCodes:
              enrolledE164s:
                - +120255551212
              excludedCountryCodes:
                - 47
          """);

      final DynamicConfiguration config =
          DynamicConfigurationManager.parseConfiguration(experimentConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertFalse(config.getPreRegistrationEnrollmentConfiguration("unconfigured").isPresent());

      {
        final Optional<DynamicPreRegistrationExperimentEnrollmentConfiguration> percentageOnly = config
            .getPreRegistrationEnrollmentConfiguration("percentageOnly");
        assertTrue(percentageOnly.isPresent());
        assertEquals(17,
            percentageOnly.get().getEnrollmentPercentage());
        assertEquals(Collections.emptySet(),
            percentageOnly.get().getEnrolledE164s());
        assertEquals(Collections.emptySet(),
            percentageOnly.get().getExcludedE164s());
      }

      {
        final Optional<DynamicPreRegistrationExperimentEnrollmentConfiguration> e164sCountryCodesAndPercentage = config
            .getPreRegistrationEnrollmentConfiguration("e164sCountryCodesAndPercentage");

        assertTrue(e164sCountryCodesAndPercentage.isPresent());
        assertEquals(46,
            e164sCountryCodesAndPercentage.get().getEnrollmentPercentage());
        assertEquals(Set.of("+120255551212", "+3655323174"),
            e164sCountryCodesAndPercentage.get().getEnrolledE164s());
        assertEquals(Set.of("+120255551213", "+3655323175"),
            e164sCountryCodesAndPercentage.get().getExcludedE164s());
        assertEquals(Set.of("47"),
            e164sCountryCodesAndPercentage.get().getExcludedCountryCodes());
        assertEquals(Set.of("56"),
            e164sCountryCodesAndPercentage.get().getIncludedCountryCodes());
      }

      {
        final Optional<DynamicPreRegistrationExperimentEnrollmentConfiguration> e164sAndExcludedCodes = config
            .getPreRegistrationEnrollmentConfiguration("e164sAndExcludedCodes");
        assertTrue(e164sAndExcludedCodes.isPresent());
        assertEquals(0, e164sAndExcludedCodes.get().getEnrollmentPercentage());
        assertEquals(Set.of("+120255551212"),
            e164sAndExcludedCodes.get().getEnrolledE164s());
        assertTrue(e164sAndExcludedCodes.get().getExcludedE164s().isEmpty());
        assertEquals(Set.of("47"),
            e164sAndExcludedCodes.get().getExcludedCountryCodes());
      }
    }
  }

  @Test
  void testParseRemoteDeprecationConfig() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertNotNull(emptyConfig.getRemoteDeprecationConfiguration());
    }

    {
      final String remoteDeprecationConfig = REQUIRED_CONFIG.concat("""
          remoteDeprecation:
            minimumVersions:
              IOS: 1.2.3
              ANDROID: 4.5.6
            versionsPendingDeprecation:
              DESKTOP: 7.8.9
            blockedVersions:
              DESKTOP:
                - 1.4.0-beta.2
          """);

      final DynamicConfiguration config =
          DynamicConfigurationManager.parseConfiguration(remoteDeprecationConfig, DynamicConfiguration.class).orElseThrow();

      final DynamicRemoteDeprecationConfiguration remoteDeprecationConfiguration = config
          .getRemoteDeprecationConfiguration();

      assertEquals(Map.of(ClientPlatform.IOS, new Semver("1.2.3"), ClientPlatform.ANDROID, new Semver("4.5.6")),
          remoteDeprecationConfiguration.getMinimumVersions());
      assertEquals(Map.of(ClientPlatform.DESKTOP, new Semver("7.8.9")),
          remoteDeprecationConfiguration.getVersionsPendingDeprecation());
      assertEquals(Map.of(ClientPlatform.DESKTOP, Set.of(new Semver("1.4.0-beta.2"))),
          remoteDeprecationConfiguration.getBlockedVersions());
      assertTrue(remoteDeprecationConfiguration.getVersionsPendingBlock().isEmpty());
    }
  }

  @Test
  void testParsePaymentsConfiguration() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertTrue(emptyConfig.getPaymentsConfiguration().getDisallowedPrefixes().isEmpty());
    }

    {
      final String paymentsConfigYaml = REQUIRED_CONFIG.concat("""
          payments:
            disallowedPrefixes:
              - +44
          """);

      final DynamicPaymentsConfiguration config =
          DynamicConfigurationManager.parseConfiguration(paymentsConfigYaml, DynamicConfiguration.class).orElseThrow()
              .getPaymentsConfiguration();

      assertEquals(List.of("+44"), config.getDisallowedPrefixes());
    }
  }

  @Test
  void testParseCaptchaConfiguration() throws JsonProcessingException {
    {
      final String emptyConfigYaml = "test: true";

      assertTrue(DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).isEmpty(),
          "empty config should not validate");
    }

    {
      final String captchaConfig = """
          captcha:
            signupCountryCodes:
              - 1
            scoreFloor: null
          """;

      assertTrue(DynamicConfigurationManager.parseConfiguration(captchaConfig, DynamicConfiguration.class).isEmpty(),
          "score floor must not be null");
    }

    {
      final String captchaConfig = """
          captcha:
            signupCountryCodes:
              - 1
            scoreFloor: 0.9
            scoreFloorByAction:
              challenge: 0.1
              registration: 0.2
            hCaptchaSiteKeys:
              challenge:
                - ab317f2a-2b76-4098-84c9-ecdf8ea44f53
              registration:
                - e4ddb6ff-05e7-497b-9a29-b76e7331789c
                - 52fdbc88-f246-4705-a7dd-05ad85b93420
            recaptchaSiteKeys:
              challenge:
                - 299068b6-ac78-4288-a90b-2e2ce5a6ddfe
          """;

      final DynamicCaptchaConfiguration config =
          DynamicConfigurationManager.parseConfiguration(captchaConfig, DynamicConfiguration.class).orElseThrow()
              .getCaptchaConfiguration();

      assertEquals(Set.of("1"), config.getSignupCountryCodes());
      assertEquals(0.9f, config.getScoreFloor().floatValue());
      assertEquals(0.1f, config.getScoreFloorByAction().get(Action.CHALLENGE).floatValue());
      assertEquals(0.2f, config.getScoreFloorByAction().get(Action.REGISTRATION).floatValue());

      assertThat(config.getHCaptchaSiteKeys().get(Action.CHALLENGE)).contains("ab317f2a-2b76-4098-84c9-ecdf8ea44f53");
      assertThat(config.getHCaptchaSiteKeys().get(Action.REGISTRATION)).contains("e4ddb6ff-05e7-497b-9a29-b76e7331789c");
      assertThat(config.getHCaptchaSiteKeys().get(Action.REGISTRATION)).contains("52fdbc88-f246-4705-a7dd-05ad85b93420");

      assertThat(config.getRecaptchaSiteKeys().get(Action.CHALLENGE)).contains("299068b6-ac78-4288-a90b-2e2ce5a6ddfe");
      assertThat(config.getRecaptchaSiteKeys().get(Action.REGISTRATION)).isNull();
    }
  }

  @Test
  void testParseLimits() throws JsonProcessingException {
    final String limitsConfig = REQUIRED_CONFIG.concat("""
          limits:
            rateLimitReset:
              bucketSize: 17
              leakRatePerMinute: 44
          """);

    final RateLimiterConfig resetRateLimiterConfig =
        DynamicConfigurationManager.parseConfiguration(limitsConfig, DynamicConfiguration.class).orElseThrow()
            .getLimits().get(RateLimiters.For.RATE_LIMIT_RESET.id());

    assertThat(resetRateLimiterConfig.bucketSize()).isEqualTo(17);
    assertThat(resetRateLimiterConfig.leakRatePerMinute()).isEqualTo(44);
  }

  @Test
  void testParseRateLimitReset() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertThat(emptyConfig.getRateLimitChallengeConfiguration().getClientSupportedVersions()).isEmpty();
    }

    {
      final String rateLimitChallengeConfig = REQUIRED_CONFIG.concat("""
          rateLimitChallenge:
            clientSupportedVersions:
              IOS: 5.1.0
              ANDROID: 5.2.0
              DESKTOP: 5.0.0
          """);

      DynamicRateLimitChallengeConfiguration rateLimitChallengeConfiguration =
          DynamicConfigurationManager.parseConfiguration(rateLimitChallengeConfig, DynamicConfiguration.class).orElseThrow()
              .getRateLimitChallengeConfiguration();

      final Map<ClientPlatform, Semver> clientSupportedVersions = rateLimitChallengeConfiguration.getClientSupportedVersions();

      assertThat(clientSupportedVersions.get(ClientPlatform.IOS)).isEqualTo(new Semver("5.1.0"));
      assertThat(clientSupportedVersions.get(ClientPlatform.ANDROID)).isEqualTo(new Semver("5.2.0"));
      assertThat(clientSupportedVersions.get(ClientPlatform.DESKTOP)).isEqualTo(new Semver("5.0.0"));
    }
  }

  @Test
  void testParseDirectoryReconciler() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertThat(emptyConfig.getDirectoryReconcilerConfiguration().isEnabled()).isTrue();
    }

    {
      final String directoryReconcilerConfig = REQUIRED_CONFIG.concat("""
          directoryReconciler:
            enabled: false
          """);

      DynamicDirectoryReconcilerConfiguration directoryReconcilerConfiguration =
          DynamicConfigurationManager.parseConfiguration(directoryReconcilerConfig, DynamicConfiguration.class).orElseThrow()
              .getDirectoryReconcilerConfiguration();

      assertThat(directoryReconcilerConfiguration.isEnabled()).isFalse();
    }
  }

  @Test
  void testParseTurnConfig() throws JsonProcessingException {
    {
      final String config = REQUIRED_CONFIG.concat("""
          turn:
            secret: bloop
            uriConfigs:
                - uris:
                    - turn:test.org
                  weight: -1
           """);
      assertThat(DynamicConfigurationManager.parseConfiguration(config, DynamicConfiguration.class)).isEmpty();
    }
    {
      final String config = REQUIRED_CONFIG.concat("""
          turn:
            secret: bloop
            uriConfigs:
                - uris:
                    - turn:test0.org
                    - turn:test1.org
                - uris:
                    - turn:test2.org
                  weight: 2
                  enrolledNumbers:
                    - +15555555555
           """);
      DynamicTurnConfiguration turnConfiguration = DynamicConfigurationManager
          .parseConfiguration(config, DynamicConfiguration.class)
          .orElseThrow()
          .getTurnConfiguration();
      assertThat(turnConfiguration.getSecret()).isEqualTo("bloop");
      assertThat(turnConfiguration.getUriConfigs().get(0).getUris()).hasSize(2);
      assertThat(turnConfiguration.getUriConfigs().get(1).getUris()).hasSize(1);
      assertThat(turnConfiguration.getUriConfigs().get(0).getWeight()).isEqualTo(1);
      assertThat(turnConfiguration.getUriConfigs().get(1).getWeight()).isEqualTo(2);
      assertThat(turnConfiguration.getUriConfigs().get(1).getEnrolledNumbers()).containsExactly("+15555555555");

    }
  }

  @Test
  void testMessagePersister() throws JsonProcessingException {
    {
      final String emptyConfigYaml = REQUIRED_CONFIG.concat("test: true");
      final DynamicConfiguration emptyConfig =
          DynamicConfigurationManager.parseConfiguration(emptyConfigYaml, DynamicConfiguration.class).orElseThrow();

      assertTrue(emptyConfig.getMessagePersisterConfiguration().isPersistenceEnabled());
    }

    {
      final String messagePersisterEnabledYaml = REQUIRED_CONFIG.concat("""
          messagePersister:
            persistenceEnabled: true
          """);

      final DynamicConfiguration config =
          DynamicConfigurationManager.parseConfiguration(messagePersisterEnabledYaml, DynamicConfiguration.class)
              .orElseThrow();

      assertTrue(config.getMessagePersisterConfiguration().isPersistenceEnabled());
    }

    {
      final String messagePersisterDisabledYaml = REQUIRED_CONFIG.concat("""
          messagePersister:
            persistenceEnabled: false
          """);

      final DynamicConfiguration config =
          DynamicConfigurationManager.parseConfiguration(messagePersisterDisabledYaml, DynamicConfiguration.class)
              .orElseThrow();

      assertFalse(config.getMessagePersisterConfiguration().isPersistenceEnabled());
    }
  }

}
