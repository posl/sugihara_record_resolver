/*
 * Copyright 2022 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration;

import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import org.whispersystems.textsecuregcm.util.ExactlySize;

/**
 * One-time donation configuration for a given currency
 *
 * @param minimum the minimum amount permitted to be charged in this currency
 * @param gift    the suggested gift donation amount
 * @param boosts  the list of suggested one-time donation amounts
 */
public record OneTimeDonationCurrencyConfiguration(
    @NotNull @DecimalMin("0.01") BigDecimal minimum,
    @NotNull @DecimalMin("0.01") BigDecimal gift,
    @Valid
    @ExactlySize(6)
    @NotNull
    List<@NotNull @DecimalMin("0.01") BigDecimal> boosts) {

}
