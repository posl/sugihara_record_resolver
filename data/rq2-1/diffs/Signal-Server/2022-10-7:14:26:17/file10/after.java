/*
 * Copyright 2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.whispersystems.textsecuregcm.util.AttributeValues.b;
import static org.whispersystems.textsecuregcm.util.AttributeValues.n;

import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.signal.libsignal.zkgroup.receipts.ServerZkReceiptOperations;
import org.whispersystems.textsecuregcm.auth.AuthenticatedAccount;
import org.whispersystems.textsecuregcm.auth.DisabledPermittedAuthenticatedAccount;
import org.whispersystems.textsecuregcm.badges.BadgeTranslator;
import org.whispersystems.textsecuregcm.badges.LevelTranslator;
import org.whispersystems.textsecuregcm.configuration.BoostConfiguration;
import org.whispersystems.textsecuregcm.configuration.GiftConfiguration;
import org.whispersystems.textsecuregcm.configuration.SubscriptionConfiguration;
import org.whispersystems.textsecuregcm.configuration.SubscriptionLevelConfiguration;
import org.whispersystems.textsecuregcm.configuration.SubscriptionPriceConfiguration;
import org.whispersystems.textsecuregcm.controllers.SubscriptionController.GetLevelsResponse;
import org.whispersystems.textsecuregcm.entities.Badge;
import org.whispersystems.textsecuregcm.entities.BadgeSvg;
import org.whispersystems.textsecuregcm.storage.IssuedReceiptsManager;
import org.whispersystems.textsecuregcm.storage.SubscriptionManager;
import org.whispersystems.textsecuregcm.subscriptions.ProcessorCustomer;
import org.whispersystems.textsecuregcm.subscriptions.StripeManager;
import org.whispersystems.textsecuregcm.subscriptions.SubscriptionProcessor;
import org.whispersystems.textsecuregcm.tests.util.AuthHelper;
import org.whispersystems.textsecuregcm.util.SystemMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@ExtendWith(DropwizardExtensionsSupport.class)
class SubscriptionControllerTest {

  private static final Clock CLOCK = mock(Clock.class);
  private static final SubscriptionConfiguration SUBSCRIPTION_CONFIG = mock(SubscriptionConfiguration.class);
  private static final BoostConfiguration BOOST_CONFIG = mock(BoostConfiguration.class);
  private static final GiftConfiguration GIFT_CONFIG = mock(GiftConfiguration.class);
  private static final SubscriptionManager SUBSCRIPTION_MANAGER = mock(SubscriptionManager.class);
  private static final StripeManager STRIPE_MANAGER = mock(StripeManager.class);
  private static final ServerZkReceiptOperations ZK_OPS = mock(ServerZkReceiptOperations.class);
  private static final IssuedReceiptsManager ISSUED_RECEIPTS_MANAGER = mock(IssuedReceiptsManager.class);
  private static final BadgeTranslator BADGE_TRANSLATOR = mock(BadgeTranslator.class);
  private static final LevelTranslator LEVEL_TRANSLATOR = mock(LevelTranslator.class);
  private static final SubscriptionController SUBSCRIPTION_CONTROLLER = new SubscriptionController(
      CLOCK, SUBSCRIPTION_CONFIG, BOOST_CONFIG, GIFT_CONFIG, SUBSCRIPTION_MANAGER, STRIPE_MANAGER, ZK_OPS,
      ISSUED_RECEIPTS_MANAGER, BADGE_TRANSLATOR, LEVEL_TRANSLATOR);
  private static final ResourceExtension RESOURCE_EXTENSION = ResourceExtension.builder()
      .addProperty(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE)
      .addProvider(AuthHelper.getAuthFilter())
      .addProvider(new PolymorphicAuthValueFactoryProvider.Binder<>(Set.of(
          AuthenticatedAccount.class, DisabledPermittedAuthenticatedAccount.class)))
      .setMapper(SystemMapper.getMapper())
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .addResource(SUBSCRIPTION_CONTROLLER)
      .build();

  @BeforeEach
  void setUp() {
    when(STRIPE_MANAGER.getProcessor()).thenReturn(SubscriptionProcessor.STRIPE);
  }

  @AfterEach
  void tearDown() {
    reset(CLOCK, SUBSCRIPTION_CONFIG, SUBSCRIPTION_MANAGER, STRIPE_MANAGER, ZK_OPS, ISSUED_RECEIPTS_MANAGER,
        BADGE_TRANSLATOR, LEVEL_TRANSLATOR);
  }

  @Test
  void createBoostReceiptInvalid() {
    final Response response = RESOURCE_EXTENSION.target("/v1/subscription/boost/receipt_credentials")
        .request()
        // invalid, request body should have receiptCredentialRequest
        .post(Entity.json("{\"paymentIntentId\": \"foo\"}"));
    assertThat(response.getStatus()).isEqualTo(422);
  }

  @Test
  void createBoostReceiptNoRequest() {
    final Response response = RESOURCE_EXTENSION.target("/v1/subscription/boost/receipt_credentials")
        .request()
        .post(Entity.json(""));
    assertThat(response.getStatus()).isEqualTo(422);
  }

  @Test
  void createSubscriber() {
    when(CLOCK.instant()).thenReturn(Instant.now());

    // basic create
    final byte[] subscriberUserAndKey = new byte[32];
    Arrays.fill(subscriberUserAndKey, (byte) 1);
    final String subscriberId = Base64.getEncoder().encodeToString(subscriberUserAndKey);

    when(SUBSCRIPTION_MANAGER.get(any(), any())).thenReturn(CompletableFuture.completedFuture(
        SubscriptionManager.GetResult.NOT_STORED));

    final Map<String, AttributeValue> dynamoItem = Map.of(SubscriptionManager.KEY_PASSWORD, b(new byte[16]),
        SubscriptionManager.KEY_CREATED_AT, n(Instant.now().getEpochSecond()),
        SubscriptionManager.KEY_ACCESSED_AT, n(Instant.now().getEpochSecond())
    );
    final SubscriptionManager.Record record = SubscriptionManager.Record.from(
        Arrays.copyOfRange(subscriberUserAndKey, 0, 16), dynamoItem);
    when(SUBSCRIPTION_MANAGER.create(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(record));

    final Response createResponse = RESOURCE_EXTENSION.target(String.format("/v1/subscription/%s", subscriberId))
        .request()
        .put(Entity.json(""));
    assertThat(createResponse.getStatus()).isEqualTo(200);

    // creating should be idempotent
    when(SUBSCRIPTION_MANAGER.get(any(), any())).thenReturn(CompletableFuture.completedFuture(
        SubscriptionManager.GetResult.found(record)));
    when(SUBSCRIPTION_MANAGER.accessedAt(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    final Response idempotentCreateResponse = RESOURCE_EXTENSION.target(
            String.format("/v1/subscription/%s", subscriberId))
        .request()
        .put(Entity.json(""));
    assertThat(idempotentCreateResponse.getStatus()).isEqualTo(200);

    // when the manager returns `null`, it means there was a password mismatch from the storage layer `create`.
    // this could happen if there is a race between two concurrent `create` requests for the same user ID
    when(SUBSCRIPTION_MANAGER.get(any(), any())).thenReturn(CompletableFuture.completedFuture(
        SubscriptionManager.GetResult.NOT_STORED));
    when(SUBSCRIPTION_MANAGER.create(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

    final Response managerCreateNullResponse = RESOURCE_EXTENSION.target(
            String.format("/v1/subscription/%s", subscriberId))
        .request()
        .put(Entity.json(""));
    assertThat(managerCreateNullResponse.getStatus()).isEqualTo(403);

    final byte[] subscriberUserAndMismatchedKey = new byte[32];
    Arrays.fill(subscriberUserAndMismatchedKey, 0, 16, (byte) 1);
    Arrays.fill(subscriberUserAndMismatchedKey, 16, 32, (byte) 2);
    final String mismatchedSubscriberId = Base64.getEncoder().encodeToString(subscriberUserAndMismatchedKey);

    // a password mismatch for an existing record
    when(SUBSCRIPTION_MANAGER.get(any(), any())).thenReturn(CompletableFuture.completedFuture(
        SubscriptionManager.GetResult.PASSWORD_MISMATCH));

    final Response passwordMismatchResponse = RESOURCE_EXTENSION.target(
            String.format("/v1/subscription/%s", mismatchedSubscriberId))
        .request()
        .put(Entity.json(""));

    assertThat(passwordMismatchResponse.getStatus()).isEqualTo(403);

    // invalid request data is a 404
    final byte[] malformedUserAndKey = new byte[16];
    Arrays.fill(malformedUserAndKey, (byte) 1);
    final String malformedUserId = Base64.getEncoder().encodeToString(malformedUserAndKey);

    final Response malformedUserAndKeyResponse = RESOURCE_EXTENSION.target(
            String.format("/v1/subscription/%s", malformedUserId))
        .request()
        .put(Entity.json(""));

    assertThat(malformedUserAndKeyResponse.getStatus()).isEqualTo(404);
  }

  @Test
  void createPaymentMethod() {
    final byte[] subscriberUserAndKey = new byte[32];
    Arrays.fill(subscriberUserAndKey, (byte) 1);
    final String subscriberId = Base64.getEncoder().encodeToString(subscriberUserAndKey);

    when(CLOCK.instant()).thenReturn(Instant.now());
    when(SUBSCRIPTION_MANAGER.get(any(), any())).thenReturn(CompletableFuture.completedFuture(
        SubscriptionManager.GetResult.NOT_STORED));

    final Map<String, AttributeValue> dynamoItem = Map.of(SubscriptionManager.KEY_PASSWORD, b(new byte[16]),
        SubscriptionManager.KEY_CREATED_AT, n(Instant.now().getEpochSecond()),
        SubscriptionManager.KEY_ACCESSED_AT, n(Instant.now().getEpochSecond())
    );
    final SubscriptionManager.Record record = SubscriptionManager.Record.from(
        Arrays.copyOfRange(subscriberUserAndKey, 0, 16), dynamoItem);
    when(SUBSCRIPTION_MANAGER.create(any(), any(), any(Instant.class)))
        .thenReturn(CompletableFuture.completedFuture(
            record));

    final Response createSubscriberResponse = RESOURCE_EXTENSION
        .target(String.format("/v1/subscription/%s", subscriberId))
        .request()
        .put(Entity.json(""));

    assertThat(createSubscriberResponse.getStatus()).isEqualTo(200);

    when(SUBSCRIPTION_MANAGER.get(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(SubscriptionManager.GetResult.found(record)));

    final String customerId = "some-customer-id";
    final ProcessorCustomer customer = new ProcessorCustomer(
        customerId, SubscriptionProcessor.STRIPE);
    when(STRIPE_MANAGER.createCustomer(any()))
        .thenReturn(CompletableFuture.completedFuture(customer));

    final SubscriptionManager.Record recordWithCustomerId = SubscriptionManager.Record.from(record.user, dynamoItem);
    recordWithCustomerId.customerId = customerId;
    recordWithCustomerId.processorsToCustomerIds.put(SubscriptionProcessor.STRIPE, customerId);

    when(SUBSCRIPTION_MANAGER.updateProcessorAndCustomerId(any(SubscriptionManager.Record.class), any(),
        any(Instant.class)))
        .thenReturn(CompletableFuture.completedFuture(recordWithCustomerId));

    final String clientSecret = "some-client-secret";
    when(STRIPE_MANAGER.createPaymentMethodSetupToken(customerId))
        .thenReturn(CompletableFuture.completedFuture(clientSecret));

    final SubscriptionController.CreatePaymentMethodResponse createPaymentMethodResponse = RESOURCE_EXTENSION
        .target(String.format("/v1/subscription/%s/create_payment_method", subscriberId))
        .request()
        .post(Entity.json(""))
        .readEntity(SubscriptionController.CreatePaymentMethodResponse.class);

    assertThat(createPaymentMethodResponse.processor()).isEqualTo(SubscriptionProcessor.STRIPE);
    assertThat(createPaymentMethodResponse.clientSecret()).isEqualTo(clientSecret);

  }

  @Test
  void getLevels() {
    when(SUBSCRIPTION_CONFIG.getLevels()).thenReturn(Map.of(
        1L, new SubscriptionLevelConfiguration("B1", "P1",
            Map.of("USD", new SubscriptionPriceConfiguration("R1", BigDecimal.valueOf(100)))),
        2L, new SubscriptionLevelConfiguration("B2", "P2",
            Map.of("USD", new SubscriptionPriceConfiguration("R2", BigDecimal.valueOf(200)))),
        3L, new SubscriptionLevelConfiguration("B3", "P3",
            Map.of("USD", new SubscriptionPriceConfiguration("R3", BigDecimal.valueOf(300))))
    ));
    when(BADGE_TRANSLATOR.translate(any(), eq("B1"))).thenReturn(new Badge("B1", "cat1", "name1", "desc1",
        List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
        List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))));
    when(BADGE_TRANSLATOR.translate(any(), eq("B2"))).thenReturn(new Badge("B2", "cat2", "name2", "desc2",
        List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
        List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))));
    when(BADGE_TRANSLATOR.translate(any(), eq("B3"))).thenReturn(new Badge("B3", "cat3", "name3", "desc3",
        List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
        List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))));
    when(LEVEL_TRANSLATOR.translate(any(), eq("B1"))).thenReturn("Z1");
    when(LEVEL_TRANSLATOR.translate(any(), eq("B2"))).thenReturn("Z2");
    when(LEVEL_TRANSLATOR.translate(any(), eq("B3"))).thenReturn("Z3");

    GetLevelsResponse response = RESOURCE_EXTENSION.target("/v1/subscription/levels")
        .request()
        .get(GetLevelsResponse.class);

    assertThat(response.getLevels()).containsKeys(1L, 2L, 3L).satisfies(longLevelMap -> {
      assertThat(longLevelMap).extractingByKey(1L).satisfies(level -> {
        assertThat(level.getName()).isEqualTo("Z1");
        assertThat(level.getBadge().getId()).isEqualTo("B1");
        assertThat(level.getCurrencies()).containsKeys("USD").extractingByKey("USD").satisfies(price -> {
          assertThat(price).isEqualTo("100");
        });
      });
      assertThat(longLevelMap).extractingByKey(2L).satisfies(level -> {
        assertThat(level.getName()).isEqualTo("Z2");
        assertThat(level.getBadge().getId()).isEqualTo("B2");
        assertThat(level.getCurrencies()).containsKeys("USD").extractingByKey("USD").satisfies(price -> {
          assertThat(price).isEqualTo("200");
        });
      });
      assertThat(longLevelMap).extractingByKey(3L).satisfies(level -> {
        assertThat(level.getName()).isEqualTo("Z3");
        assertThat(level.getBadge().getId()).isEqualTo("B3");
        assertThat(level.getCurrencies()).containsKeys("USD").extractingByKey("USD").satisfies(price -> {
          assertThat(price).isEqualTo("300");
        });
      });
    });
  }
}
