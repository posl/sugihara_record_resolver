package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A json model that contains information about all the blocked contacts for a session
 *
 * @param id          an unsigned unique identifier for this request
 * @param blockedJids a non null List of blocked contacts for a session, might be empty
 */
public record BlocklistResponse(int id,
                                      @JsonProperty("blocklist") @NotNull List<String> blockedJids) implements JsonResponseModel {
}
