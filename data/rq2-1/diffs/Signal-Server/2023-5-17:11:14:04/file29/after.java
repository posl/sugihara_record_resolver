/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.secrets;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForCollection;

public class SecretStringList extends Secret<List<String>> {

  @SuppressWarnings("rawtypes")
  public static class ValidatorNotEmpty extends BaseSecretValidator<NotEmpty, Collection, SecretStringList> {
    public ValidatorNotEmpty() {
      super(new NotEmptyValidatorForCollection());
    }
  }

  public SecretStringList(final List<String> value) {
    super(ImmutableList.copyOf(value));
  }
}
