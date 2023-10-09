/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Strings;
import io.dropwizard.validation.ValidationMethod;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

public class GcpAttachmentsConfiguration {

  @NotEmpty
  @JsonProperty
  private String domain;

  @NotEmpty
  @JsonProperty
  private String email;

  @JsonProperty
  @Min(1)
  private int maxSizeInBytes;

  @JsonProperty
  private String pathPrefix;

  @NotEmpty
  @JsonProperty
  private String rsaSigningKey;

  public String getDomain() {
    return domain;
  }

  public String getEmail() {
    return email;
  }

  public int getMaxSizeInBytes() {
    return maxSizeInBytes;
  }

  public String getPathPrefix() {
    return pathPrefix;
  }

  public String getRsaSigningKey() {
    return rsaSigningKey;
  }

  @SuppressWarnings("unused")
  @ValidationMethod(message = "pathPrefix must be empty or start with /")
  public boolean isPathPrefixValid() {
    return Strings.isNullOrEmpty(pathPrefix) || pathPrefix.startsWith("/");
  }
}
