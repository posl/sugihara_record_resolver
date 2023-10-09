package it.auties.whatsapp.model.info;

import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that holds the information related to a native flow.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class NativeFlowInfo
    implements Info {

  /**
   * The name of the flow
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String name;

  /**
   * The params of the flow, encoded as json
   */
  @ProtobufProperty(index = 2, type = STRING)
  private String parameters;
}