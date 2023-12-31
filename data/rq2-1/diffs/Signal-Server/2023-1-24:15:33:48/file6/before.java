/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.controllers;

import static org.whispersystems.textsecuregcm.metrics.MetricsUtil.name;

import com.codahale.metrics.annotation.Timed;
import com.google.common.net.HttpHeaders;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.textsecuregcm.auth.BasicAuthorizationHeader;
import org.whispersystems.textsecuregcm.auth.RegistrationLockVerificationManager;
import org.whispersystems.textsecuregcm.entities.AccountIdentityResponse;
import org.whispersystems.textsecuregcm.entities.RegistrationRequest;
import org.whispersystems.textsecuregcm.entities.RegistrationSession;
import org.whispersystems.textsecuregcm.limits.RateLimiters;
import org.whispersystems.textsecuregcm.metrics.UserAgentTagUtil;
import org.whispersystems.textsecuregcm.registration.RegistrationServiceClient;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.AccountsManager;
import org.whispersystems.textsecuregcm.storage.RegistrationRecoveryPasswordsManager;
import org.whispersystems.textsecuregcm.util.HeaderUtils;
import org.whispersystems.textsecuregcm.util.Util;

@Path("/v1/registration")
public class RegistrationController {

  private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

  private static final DistributionSummary REREGISTRATION_IDLE_DAYS_DISTRIBUTION = DistributionSummary
      .builder(name(RegistrationController.class, "reregistrationIdleDays"))
      .publishPercentiles(0.75, 0.95, 0.99, 0.999)
      .distributionStatisticExpiry(Duration.ofHours(2))
      .register(Metrics.globalRegistry);

  private static final String ACCOUNT_CREATED_COUNTER_NAME = name(RegistrationController.class, "accountCreated");
  private static final String COUNTRY_CODE_TAG_NAME = "countryCode";
  private static final String REGION_CODE_TAG_NAME = "regionCode";
  private static final String VERIFICATION_TYPE_TAG_NAME = "verification";

  private static final Duration REGISTRATION_RPC_TIMEOUT = Duration.ofSeconds(15);
  private static final long VERIFICATION_TIMEOUT_SECONDS = REGISTRATION_RPC_TIMEOUT.plusSeconds(1).getSeconds();

  private final AccountsManager accounts;
  private final RegistrationServiceClient registrationServiceClient;
  private final RegistrationLockVerificationManager registrationLockVerificationManager;
  private final RegistrationRecoveryPasswordsManager registrationRecoveryPasswordsManager;
  private final RateLimiters rateLimiters;

  public RegistrationController(final AccountsManager accounts,
      final RegistrationServiceClient registrationServiceClient,
      final RegistrationLockVerificationManager registrationLockVerificationManager,
      final RegistrationRecoveryPasswordsManager registrationRecoveryPasswordsManager,
      final RateLimiters rateLimiters) {
    this.accounts = accounts;
    this.registrationServiceClient = registrationServiceClient;
    this.registrationLockVerificationManager = registrationLockVerificationManager;
    this.registrationRecoveryPasswordsManager = registrationRecoveryPasswordsManager;
    this.rateLimiters = rateLimiters;
  }

  @Timed
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public AccountIdentityResponse register(
      @HeaderParam(HttpHeaders.AUTHORIZATION) @NotNull final BasicAuthorizationHeader authorizationHeader,
      @HeaderParam(HeaderUtils.X_SIGNAL_AGENT) final String signalAgent,
      @HeaderParam(HttpHeaders.USER_AGENT) final String userAgent,
      @NotNull @Valid final RegistrationRequest registrationRequest) throws RateLimitExceededException, InterruptedException {

    rateLimiters.getRegistrationLimiter().validate(registrationRequest.sessionId());

    final String number = authorizationHeader.getUsername();
    final String password = authorizationHeader.getPassword();

    // decide on the method of verification based on the registration request parameters and verify
    final RegistrationRequest.VerificationType verificationType = registrationRequest.verificationType();
    switch (verificationType) {
      case SESSION -> verifyBySessionId(number, registrationRequest.decodeSessionId());
      case RECOVERY_PASSWORD -> verifyByRecoveryPassword(number, registrationRequest.recoveryPassword());
    }

    final Optional<Account> existingAccount = accounts.getByE164(number);

    existingAccount.ifPresent(account -> {
      final Instant accountLastSeen = Instant.ofEpochMilli(account.getLastSeen());
      final Duration timeSinceLastSeen = Duration.between(accountLastSeen, Instant.now());
      REREGISTRATION_IDLE_DAYS_DISTRIBUTION.record(timeSinceLastSeen.toDays());
    });

    if (existingAccount.isPresent()) {
      registrationLockVerificationManager.verifyRegistrationLock(existingAccount.get(),
          registrationRequest.accountAttributes().getRegistrationLock());
    }

    if (!registrationRequest.skipDeviceTransfer() && existingAccount.map(Account::isTransferSupported).orElse(false)) {
      // If a device transfer is possible, clients must explicitly opt out of a transfer (i.e. after prompting the user)
      // before we'll let them create a new account "from scratch"
      throw new WebApplicationException(Response.status(409, "device transfer available").build());
    }

    final Account account = accounts.create(number, password, signalAgent, registrationRequest.accountAttributes(),
        existingAccount.map(Account::getBadges).orElseGet(ArrayList::new));

    Metrics.counter(ACCOUNT_CREATED_COUNTER_NAME, Tags.of(UserAgentTagUtil.getPlatformTag(userAgent),
            Tag.of(COUNTRY_CODE_TAG_NAME, Util.getCountryCode(number)),
            Tag.of(REGION_CODE_TAG_NAME, Util.getRegion(number)),
            Tag.of(VERIFICATION_TYPE_TAG_NAME, verificationType.name())))
        .increment();

    return new AccountIdentityResponse(account.getUuid(),
        account.getNumber(),
        account.getPhoneNumberIdentifier(),
        account.getUsernameHash().orElse(null),
        existingAccount.map(Account::isStorageSupported).orElse(false));
  }

  private void verifyBySessionId(final String number, final byte[] sessionId) throws InterruptedException {
    try {
      final RegistrationSession session = registrationServiceClient
          .getSession(sessionId, REGISTRATION_RPC_TIMEOUT)
          .get(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .orElseThrow(() -> new NotAuthorizedException("session not verified"));

      if (!MessageDigest.isEqual(number.getBytes(), session.number().getBytes())) {
        throw new BadRequestException("number does not match session");
      }
      if (!session.verified()) {
        throw new NotAuthorizedException("session not verified");
      }
    } catch (final CancellationException | ExecutionException | TimeoutException e) {
      logger.error("Registration service failure", e);
      throw new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE);
    }
  }

  private void verifyByRecoveryPassword(final String number, final byte[] recoveryPassword) throws InterruptedException {
    try {
      final boolean verified = registrationRecoveryPasswordsManager.verify(number, recoveryPassword)
          .get(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!verified) {
        throw new ForbiddenException("recoveryPassword couldn't be verified");
      }
    } catch (final ExecutionException | TimeoutException e) {
      throw new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE);
    }
  }
}
