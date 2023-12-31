package it.auties.whatsapp4j.whatsapp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import it.auties.whatsapp4j.binary.BinaryArray;
import lombok.NonNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A configuration class used to specify the behaviour of {@link it.auties.whatsapp4j.whatsapp.WhatsappAPI}.
 * Each field is immutable, this means that once this class has been initialized, cannot be changed.
 * If reflection is used, it is not guaranteed that the settings will effectively change.
 * This class should be configured using its builder, accessible using {@link WhatsappConfiguration#builder()}.
 * An all arguments constructor is also available if considered more suitable for the coding style of the project.
 */
@Builder
@Data
@Accessors(fluent = true)
public class WhatsappConfiguration {
    /**
     * The url of WhatsappWeb's WebSocket
     * This may change based on the region this API is being used in
     */
    @Default
    private final @NonNull String whatsappUrl = "wss://web.whatsapp.com/ws";

    /**
     * The tag used to send messages to WhatsappWeb's WebSocket
     * The tag used to send binary requests to WhatsappWeb's WebSocket after the authentication process has succeeded is built using {@link it.auties.whatsapp4j.utils.WhatsappUtils#buildRequestTag(WhatsappConfiguration)}
     * It is important to use a pseudo random string as using the same tag two times in a binary request, even in different sessions, will make the request fail
     */
    @Default
    private final @NonNull String requestTag = BinaryArray.random(12).toHex();

    /**
     * The description provided to Whatsapp during the authentication process
     * This should be, for example, the name of your service
     */
    @Default
    private final @NonNull String description = "Whatsapp4j";

    /**
     * The short description provided to Whatsapp during the authentication process
     * This should be, for example, an acronym for your service
     */
    @Default
    private final @NonNull String shortDescription = "W4J";

    /**
     * When someone logs into WhatsappWeb from another location, this function is used to determine if the connection should be reclaimed
     * If the connection should be reclaimed this function should return true
     * The first and only parameter of this function is a String describing the reason the connection was terminated
     * By default, WhatsappWeb4j will reclaim connection
     */
    @Default
    private final @NonNull Function<String, Boolean> reconnectWhenDisconnected = (reason) -> true;

    /**
     * When a connection is established with WhatsappWeb's WebSocket with new secrets, a QR code is generated.
     * This function is used to consume said QR code.
     * By default, the QR code is printed on the terminal.
     */
    @Default
    private final @NonNull Consumer<BitMatrix> qrCodeHandler = (qr) -> System.out.println(qr.toString("\033[40m  \033[0m", "\033[47m  \033[0m"));

    /**
     * This property determines whether the requests sent to WhatsappWeb's WebSocket should be sent asynchronously or not
     * It is recommended to set this field to true as it helps with performance while not using necessarily more resources
     */
    @Default
    private final boolean async = true;

    /**
     * Constructs a new instance of WhatsappConfiguration with default options
     *
     * @return a new instance of WhatsappConfiguration with the above characteristics
     */
    public static @NonNull WhatsappConfiguration defaultOptions() {
        return WhatsappConfiguration.builder().build();
    }
}
