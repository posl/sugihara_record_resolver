package org.whispersystems.textsecuregcm.limits;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.whispersystems.textsecuregcm.abuse.RateLimitChallengeListener;
import org.whispersystems.textsecuregcm.controllers.RateLimitExceededException;
import org.whispersystems.textsecuregcm.recaptcha.RecaptchaClient;
import org.whispersystems.textsecuregcm.storage.Account;

class RateLimitChallengeManagerTest {

  private PushChallengeManager pushChallengeManager;
  private RecaptchaClient recaptchaClient;
  private DynamicRateLimiters rateLimiters;
  private RateLimitChallengeListener rateLimitChallengeListener;

  private RateLimitChallengeManager rateLimitChallengeManager;

  @BeforeEach
  void setUp() {
    pushChallengeManager = mock(PushChallengeManager.class);
    recaptchaClient = mock(RecaptchaClient.class);
    rateLimiters = mock(DynamicRateLimiters.class);
    rateLimitChallengeListener = mock(RateLimitChallengeListener.class);

    rateLimitChallengeManager = new RateLimitChallengeManager(
        pushChallengeManager,
        recaptchaClient,
        rateLimiters);

    rateLimitChallengeManager.addListener(rateLimitChallengeListener);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void answerPushChallenge(final boolean successfulChallenge) throws RateLimitExceededException {
    final Account account = mock(Account.class);
    when(account.getUuid()).thenReturn(UUID.randomUUID());

    when(pushChallengeManager.answerChallenge(eq(account), any())).thenReturn(successfulChallenge);

    when(rateLimiters.getPushChallengeAttemptLimiter()).thenReturn(mock(RateLimiter.class));
    when(rateLimiters.getPushChallengeSuccessLimiter()).thenReturn(mock(RateLimiter.class));
    when(rateLimiters.getRateLimitResetLimiter()).thenReturn(mock(RateLimiter.class));

    rateLimitChallengeManager.answerPushChallenge(account, "challenge");

    if (successfulChallenge) {
      verify(rateLimitChallengeListener).handleRateLimitChallengeAnswered(account);
    } else {
      verifyNoInteractions(rateLimitChallengeListener);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void answerRecaptchaChallenge(final boolean successfulChallenge) throws RateLimitExceededException {
    final Account account = mock(Account.class);
    when(account.getNumber()).thenReturn("+18005551234");
    when(account.getUuid()).thenReturn(UUID.randomUUID());

    when(recaptchaClient.verify(any(), any())).thenReturn(successfulChallenge);

    when(rateLimiters.getRecaptchaChallengeAttemptLimiter()).thenReturn(mock(RateLimiter.class));
    when(rateLimiters.getRecaptchaChallengeSuccessLimiter()).thenReturn(mock(RateLimiter.class));
    when(rateLimiters.getRateLimitResetLimiter()).thenReturn(mock(RateLimiter.class));

    rateLimitChallengeManager.answerRecaptchaChallenge(account, "captcha", "10.0.0.1", "Test User-Agent");

    if (successfulChallenge) {
      verify(rateLimitChallengeListener).handleRateLimitChallengeAnswered(account);
    } else {
      verifyNoInteractions(rateLimitChallengeListener);
    }
  }
}
