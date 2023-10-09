package it.auties.whatsapp.model.product;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents the header of a product list
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("ListMessage.ProductListHeaderImage")
public class ProductListHeaderImage implements ProtobufMessage {
    /**
     * The id of the product
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String id;

    /**
     * The thumbnail of the product
     */
    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] thumbnail;
}