package it.auties.whatsapp4j.response.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A json model that contains information about the groups in common with a WhatsappContact
 *
 */
public record CommonGroupsResponse(int status, List<WhatsappChat> groups) implements JsonResponseModel {
    @JsonCreator
    public CommonGroupsResponse(@JsonProperty("groups") List<String> groups, @JsonProperty("status") int status) {
        this(status, groups.stream().map(WhatsappDataManager.singletonInstance()::findChatByJid).map(Optional::orElseThrow).toList());
    }
}
