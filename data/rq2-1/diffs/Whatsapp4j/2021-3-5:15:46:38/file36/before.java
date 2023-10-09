package it.auties.whatsapp4j.request;


import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LogOutRequest extends WhatsappRequest {
    public LogOutRequest(@NotNull WhatsappKeysManager keysManager, @NotNull WhatsappConfiguration options) {
        super(keysManager, options);
    }

    @Override
    public @NotNull List<Object> buildBody() {
        return List.of("admin", "Conn", "disconnect");
    }

    @Override
    public @NotNull String tag() {
        return "goodbye,";
    }
}
