/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.controllers;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jetbrains.annotations.TestOnly;
import org.whispersystems.textsecuregcm.auth.AuthenticatedAccount;
import org.whispersystems.textsecuregcm.auth.ExternalServiceCredentials;
import org.whispersystems.textsecuregcm.auth.ExternalServiceCredentialsGenerator;
import org.whispersystems.textsecuregcm.auth.ExternalServiceCredentialsSelector;
import org.whispersystems.textsecuregcm.configuration.SecureValueRecovery2Configuration;
import org.whispersystems.textsecuregcm.entities.AuthCheckRequest;
import org.whispersystems.textsecuregcm.entities.AuthCheckResponse;
import org.whispersystems.textsecuregcm.limits.RateLimitedByIp;
import org.whispersystems.textsecuregcm.limits.RateLimiters;
import org.whispersystems.textsecuregcm.storage.Account;
import org.whispersystems.textsecuregcm.storage.AccountsManager;

@Path("/v2/backup")
@Tag(name = "Secure Value Recovery")
public class SecureValueRecovery2Controller {

  private static final long MAX_AGE_SECONDS = TimeUnit.DAYS.toSeconds(30);

  public static ExternalServiceCredentialsGenerator credentialsGenerator(final SecureValueRecovery2Configuration cfg) {
    return credentialsGenerator(cfg, Clock.systemUTC());
  }

  @TestOnly
  public static ExternalServiceCredentialsGenerator credentialsGenerator(final SecureValueRecovery2Configuration cfg, final Clock clock) {
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .withUserDerivationKey(cfg.userIdTokenSharedSecret().value())
        .prependUsername(false)
        .withDerivedUsernameTruncateLength(16)
        .withClock(clock)
        .build();
  }

  private final ExternalServiceCredentialsGenerator backupServiceCredentialGenerator;
  private final AccountsManager accountsManager;

  public SecureValueRecovery2Controller(final ExternalServiceCredentialsGenerator backupServiceCredentialGenerator,
      final AccountsManager accountsManager) {
    this.backupServiceCredentialGenerator = backupServiceCredentialGenerator;
    this.accountsManager = accountsManager;
  }

  @Timed
  @GET
  @Path("/auth")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Generate credentials for SVR2",
      description = """
          Generate SVR2 service credentials. Generated credentials have an expiration time of 30 days 
          (however, the TTL is fully controlled by the server side and may change even for already generated credentials). 
          """
  )
  @ApiResponse(responseCode = "200", description = "`JSON` with generated credentials.", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "401", description = "Account authentication check failed.")
  public ExternalServiceCredentials getAuth(@Auth final AuthenticatedAccount auth) {
    return backupServiceCredentialGenerator.generateFor(auth.getAccount().getUuid().toString());
  }


  @Timed
  @POST
  @Path("/auth/check")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RateLimitedByIp(RateLimiters.For.BACKUP_AUTH_CHECK)
  @Operation(
      summary = "Check SVR2 credentials",
      description = """
          Over time, clients may wind up with multiple sets of SVR2 authentication credentials in cloud storage. 
          To determine which set is most current and should be used to communicate with SVR2 to retrieve a master key
          (from which a registration recovery password can be derived), clients should call this endpoint 
          with a list of stored credentials. The response will identify which (if any) set of credentials are appropriate for communicating with SVR2.
          """
  )
  @ApiResponse(responseCode = "200", description = "`JSON` with the check results.", useReturnTypeSchema = true)
  @ApiResponse(responseCode = "422", description = "Provided list of SVR2 credentials could not be parsed")
  @ApiResponse(responseCode = "400", description = "`POST` request body is not a valid `JSON`")
  public AuthCheckResponse authCheck(@NotNull @Valid final AuthCheckRequest request) {
    final List<ExternalServiceCredentialsSelector.CredentialInfo> credentials = ExternalServiceCredentialsSelector.check(
        request.passwords(),
        backupServiceCredentialGenerator,
        MAX_AGE_SECONDS);

    // the username associated with the provided number
    final Optional<String> matchingUsername = accountsManager
        .getByE164(request.number())
        .map(Account::getUuid)
        .map(backupServiceCredentialGenerator::generateForUuid)
        .map(ExternalServiceCredentials::username);

    return new AuthCheckResponse(credentials.stream().collect(Collectors.toMap(
        ExternalServiceCredentialsSelector.CredentialInfo::token,
        info -> {
          if (!info.valid()) {
            return AuthCheckResponse.Result.INVALID;
          }
          final String username = info.credentials().username();
          // does this credential match the account id for the e164 provided in the request?
          boolean match = matchingUsername.filter(username::equals).isPresent();
          return match ? AuthCheckResponse.Result.MATCH : AuthCheckResponse.Result.NO_MATCH;
        }
    )));
  }
}
