package org.asamk.signal.manager.api;

public class LastGroupAdminException extends Exception {

    public LastGroupAdminException(GroupId groupId, String groupName) {
        super("User is last admin in group: " + groupName + " (" + groupId.toBase64() + ")");
    }
}
