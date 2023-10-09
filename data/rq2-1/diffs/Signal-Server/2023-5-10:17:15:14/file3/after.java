/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.dynamic;

public record DynamicScheduledApnNotificationSendingConfiguration(boolean enabledForServer,
                                                                  boolean enabledForDedicatedProcess) {

}
