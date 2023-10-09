/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.entities;

import java.util.Map;

public record AttachmentDescriptorV3(int cdn, String key, Map<String, String> headers, String signedUploadLocation) {
}
