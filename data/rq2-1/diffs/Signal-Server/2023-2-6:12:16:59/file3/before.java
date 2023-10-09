/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.whispersystems.textsecuregcm.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class DirectoryClientConfiguration {

  @NotEmpty
  @JsonProperty
  private String userAuthenticationTokenSharedSecret;

  @NotEmpty
  @JsonProperty
  private String userAuthenticationTokenUserIdSecret;

  public byte[] getUserAuthenticationTokenSharedSecret() throws DecoderException {
    return Hex.decodeHex(userAuthenticationTokenSharedSecret.toCharArray());
  }

  public byte[] getUserAuthenticationTokenUserIdSecret() throws DecoderException {
    return Hex.decodeHex(userAuthenticationTokenUserIdSecret.toCharArray());
  }

}
