package it.auties.whatsapp4j.request.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.BinaryFlag;
import it.auties.whatsapp4j.binary.BinaryMetric;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.common.ResponseModel;
import lombok.NonNull;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletableFuture;

/**
 * An abstract model class that represents a binary request made from the client to the server
 *
 * @param <M> the type of the model
 */
@Accessors(fluent = true, chain = true)
public abstract non-sealed class BinaryRequest<M extends ResponseModel> extends Request<Node, M>{
    private final @NonNull @Getter
    Node node;
    private final @NonNull @Getter BinaryFlag flag;
    private final @NonNull @Getter BinaryMetric[] tags;

    /**
     * Constructs a new instance of a BinaryRequest using a custom non null request tag
     *
     * @param tag the custom non null tag to assign to this request
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull String tag, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
        super(tag, configuration);
        this.node = node;
        this.flag = flag;
        this.tags = tags;
    }

    /**
     * Constructs a new instance of a BinaryRequest using the default request tag built using {@code configuration}
     *
     * @param configuration the configuration used for {@link WhatsappAPI}
     * @param flag the flag of this request
     * @param tags the tags for this request
     */
    protected BinaryRequest(@NonNull WhatsappConfiguration configuration, @NonNull Node node, @NonNull BinaryFlag flag, @NonNull BinaryMetric... tags) {
        super(configuration);
        this.node = node;
        this.flag = flag;
        this.tags = tags;
    }

    /**
     * Returns the body of this request
     *
     * @return an object to send to WhatsappWeb's WebSocket
     */
    @Override
    public @NonNull Node buildBody() {
        return node;
    }

    /**
     * Sends a binary request to the WebSocket linked to {@code session}.
     *
     * @param session the WhatsappWeb's WebSocket session
     * @return this request
     */
    @SneakyThrows
    public CompletableFuture<M> send(@NonNull Session session) {
        if (configuration.async()) {
            session.getAsyncRemote().sendObject(this, __ -> MANAGER.pendingRequests().add(this));
            if(noResponse()) future.complete(null);
            return future;
        }

        session.getBasicRemote().sendObject(this);
        MANAGER.pendingRequests().add(this);
        if(noResponse()) future.complete(null);
        return future;
    }
}