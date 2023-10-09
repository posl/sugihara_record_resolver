/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package org.whispersystems.textsecuregcm.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.textsecuregcm.controllers.RateLimitExceededException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RateLimitExceededExceptionMapper implements ExceptionMapper<RateLimitExceededException> {
  private static final Logger logger = LoggerFactory.getLogger(RateLimitExceededExceptionMapper.class);

  /**
   * Convert a RateLimitExceededException to a 413 response with a
   * Retry-After header.
   *
   * @param e A RateLimitExceededException potentially containing a recommended retry duration
   * @return the response
   */
  @Override
  public Response toResponse(RateLimitExceededException e) {
    return e.getRetryDuration()
        .filter(d -> {
          if (d.isNegative()) {
            logger.warn("Encountered a negative retry duration: {}, will not include a Retry-After header in response", d);
          }
          // only include non-negative durations in retry headers
          return !d.isNegative();
        })
        .map(d -> Response.status(413).header("Retry-After", d.toSeconds()))
        .orElseGet(() -> Response.status(413)).build();
  }
}
