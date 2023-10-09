/*
 * Copyright 2013-2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.tests.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.whispersystems.textsecuregcm.auth.AuthenticatedAccount;
import org.whispersystems.textsecuregcm.auth.DisabledPermittedAuthenticatedAccount;
import org.whispersystems.textsecuregcm.controllers.AttachmentControllerV2;
import org.whispersystems.textsecuregcm.controllers.AttachmentControllerV3;
import org.whispersystems.textsecuregcm.entities.AttachmentDescriptorV2;
import org.whispersystems.textsecuregcm.entities.AttachmentDescriptorV3;
import org.whispersystems.textsecuregcm.limits.RateLimiter;
import org.whispersystems.textsecuregcm.limits.RateLimiters;
import org.whispersystems.textsecuregcm.tests.util.AuthHelper;
import org.whispersystems.textsecuregcm.util.SystemMapper;

@ExtendWith(DropwizardExtensionsSupport.class)
class AttachmentControllerTest {

  private static RateLimiters             rateLimiters  = mock(RateLimiters.class            );
  private static RateLimiter              rateLimiter   = mock(RateLimiter.class             );

  static {
    when(rateLimiters.getAttachmentLimiter()).thenReturn(rateLimiter);
  }

  public static final String RSA_PRIVATE_KEY_PEM;

  static {
    try {
      final KeyPairGenerator  keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(1024);
      final KeyPair           keyPair          = keyPairGenerator.generateKeyPair();

      RSA_PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
          Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded()) + "\n" +
          "-----END PRIVATE KEY-----";
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static final ResourceExtension resources;

  static {
    try {
      resources = ResourceExtension.builder()
          .addProvider(AuthHelper.getAuthFilter())
          .addProvider(new PolymorphicAuthValueFactoryProvider.Binder<>(
              ImmutableSet.of(AuthenticatedAccount.class, DisabledPermittedAuthenticatedAccount.class)))
              .setMapper(SystemMapper.getMapper())
              .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
              .addResource(new AttachmentControllerV2(rateLimiters, "accessKey", "accessSecret", "us-east-1", "attachmentv2-bucket"))
              .addResource(new AttachmentControllerV3(rateLimiters, "some-cdn.signal.org", "signal@example.com", 1000, "/attach-here", RSA_PRIVATE_KEY_PEM))
              .build();
    } catch (IOException | InvalidKeyException | InvalidKeySpecException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void testV3Form() {
    AttachmentDescriptorV3 descriptor = resources.getJerseyTest()
            .target("/v3/attachments/form/upload")
            .request()
            .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
            .get(AttachmentDescriptorV3.class);

    assertThat(descriptor.getKey()).isNotBlank();
    assertThat(descriptor.getCdn()).isEqualTo(2);
    assertThat(descriptor.getHeaders()).hasSize(3);
    assertThat(descriptor.getHeaders()).extractingByKey("host").isEqualTo("some-cdn.signal.org");
    assertThat(descriptor.getHeaders()).extractingByKey("x-goog-resumable").isEqualTo("start");
    assertThat(descriptor.getHeaders()).extractingByKey("x-goog-content-length-range").isEqualTo("1,1000");
    assertThat(descriptor.getSignedUploadLocation()).isNotEmpty();
    assertThat(descriptor.getSignedUploadLocation()).contains("X-Goog-Signature");
    assertThat(descriptor.getSignedUploadLocation()).is(new Condition<>(x -> {
      try {
        new URL(x);
      } catch (MalformedURLException e) {
        return false;
      }
      return true;
    }, "convertible to a URL", (Object[]) null));

    final URL signedUploadLocation;
    try {
      signedUploadLocation = new URL(descriptor.getSignedUploadLocation());
    } catch (MalformedURLException e) {
      throw new AssertionError(e);
    }
    assertThat(signedUploadLocation.getHost()).isEqualTo("some-cdn.signal.org");
    assertThat(signedUploadLocation.getPath()).startsWith("/attach-here/");
    final Map<String, String> queryParamMap = new HashMap<>();
    final String[] queryTerms = signedUploadLocation.getQuery().split("&");
    for (final String queryTerm : queryTerms) {
      final String[] keyValueArray = queryTerm.split("=", 2);
      queryParamMap.put(
              URLDecoder.decode(keyValueArray[0], StandardCharsets.UTF_8),
              URLDecoder.decode(keyValueArray[1], StandardCharsets.UTF_8));
    }

    assertThat(queryParamMap).extractingByKey("X-Goog-Algorithm").isEqualTo("GOOG4-RSA-SHA256");
    assertThat(queryParamMap).extractingByKey("X-Goog-Expires").isEqualTo("90000");
    assertThat(queryParamMap).extractingByKey("X-Goog-SignedHeaders").isEqualTo("host;x-goog-content-length-range;x-goog-resumable");
    assertThat(queryParamMap).extractingByKey("X-Goog-Date", Assertions.as(InstanceOfAssertFactories.STRING)).isNotEmpty();

    final String credential = queryParamMap.get("X-Goog-Credential");
    String[] credentialParts = credential.split("/");
    assertThat(credentialParts).hasSize(5);
    assertThat(credentialParts[0]).isEqualTo("signal@example.com");
    assertThat(credentialParts[2]).isEqualTo("auto");
    assertThat(credentialParts[3]).isEqualTo("storage");
    assertThat(credentialParts[4]).isEqualTo("goog4_request");
  }

  @Test
  void testV3FormDisabled() {
    Response response = resources.getJerseyTest()
            .target("/v3/attachments/form/upload")
            .request()
            .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.DISABLED_UUID, AuthHelper.DISABLED_PASSWORD))
            .get();

    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void testV2Form() throws IOException {
    AttachmentDescriptorV2 descriptor = resources.getJerseyTest()
                                                 .target("/v2/attachments/form/upload")
                                                 .request()
                                                 .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
                                                 .get(AttachmentDescriptorV2.class);

    assertThat(descriptor.getKey()).isEqualTo(descriptor.getAttachmentIdString());
    assertThat(descriptor.getAcl()).isEqualTo("private");
    assertThat(descriptor.getAlgorithm()).isEqualTo("AWS4-HMAC-SHA256");
    assertThat(descriptor.getAttachmentId()).isGreaterThan(0);
    assertThat(String.valueOf(descriptor.getAttachmentId())).isEqualTo(descriptor.getAttachmentIdString());

    String[] credentialParts = descriptor.getCredential().split("/");

    assertThat(credentialParts[0]).isEqualTo("accessKey");
    assertThat(credentialParts[2]).isEqualTo("us-east-1");
    assertThat(credentialParts[3]).isEqualTo("s3");
    assertThat(credentialParts[4]).isEqualTo("aws4_request");

    assertThat(descriptor.getDate()).isNotBlank();
    assertThat(descriptor.getPolicy()).isNotBlank();
    assertThat(descriptor.getSignature()).isNotBlank();

    assertThat(new String(Base64.getDecoder().decode(descriptor.getPolicy()))).contains("[\"content-length-range\", 1, 104857600]");
  }

  @Test
  void testV2FormDisabled() {
    Response response = resources.getJerseyTest()
                                 .target("/v2/attachments/form/upload")
                                 .request()
                                 .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.DISABLED_UUID, AuthHelper.DISABLED_PASSWORD))
                                 .get();

    assertThat(response.getStatus()).isEqualTo(401);
  }

}
