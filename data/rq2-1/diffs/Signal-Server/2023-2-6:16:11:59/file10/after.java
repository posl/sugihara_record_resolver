/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.auth;

public enum RegistrationLockError {
  MISMATCH(RegistrationLockVerificationManager.FAILURE_HTTP_STATUS),
  RATE_LIMITED(413) // This will be changed to 429 in a future revision
  ;

  private final int expectedStatus;

  RegistrationLockError(final int expectedStatus) {
    this.expectedStatus = expectedStatus;
  }

  public int getExpectedStatus() {
    return expectedStatus;
  }
}
