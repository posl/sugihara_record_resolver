package org.asamk.signal.manager.api;

public class NotRegisteredException extends Exception {

    public NotRegisteredException() {
        super("User is not registered.");
    }
}
