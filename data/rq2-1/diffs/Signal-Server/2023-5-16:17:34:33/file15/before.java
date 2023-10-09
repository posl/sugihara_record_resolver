/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;

public class SignedPreKey extends PreKey {

  @JsonProperty
  @NotEmpty
  private String signature;

  public SignedPreKey() {}

  public SignedPreKey(long keyId, String publicKey, String signature) {
    super(keyId, publicKey);
    this.signature = signature;
  }

  public String getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || !(object instanceof SignedPreKey)) return false;
    SignedPreKey that = (SignedPreKey) object;

    if (signature == null) {
      return super.equals(object) && that.signature == null;
    } else {
      return super.equals(object) && this.signature.equals(that.signature);
    }
  }

  @Override
  public int hashCode() {
    if (signature == null) {
      return super.hashCode();
    } else {
      return super.hashCode() ^ signature.hashCode();
    }
  }

}
