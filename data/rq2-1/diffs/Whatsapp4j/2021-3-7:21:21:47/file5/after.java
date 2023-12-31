package it.auties.whatsapp4j.binary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various flags that can be used when sending a WhatsappNode, encrypted using {@code BinaryEncoder}, to WhatsappWeb's WebSocket
 * Some of these constants are also used to describe a {@code WhatsappContact}'s status in the {@code WhatsappContactStatus} class
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum BinaryFlag {
    AVAILABLE((byte) 160),
    IGNORE((byte) (1 << 7)),
    ACKNOLEDGE((byte) (1 << 6)),
    UNAVAILABLE((byte) (1 << 4)),
    EXPIRES((byte) (1 << 3)),
    COMPOSING((byte) (1 << 2)),
    RECORDING((byte) (1 << 2)),
    PAUSED((byte) (1 << 2));

    @Getter
    private final byte data;
}
