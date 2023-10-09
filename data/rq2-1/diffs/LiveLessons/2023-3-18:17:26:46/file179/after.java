package edu.vandy.lockmanager;

/**
 * Constants shared by the client and server components.
 */
public class Constants {
    public static final String SERVER_BASE_URL = "http://localhost:8080";

    public static class Endpoints {
        public static final String CREATE = "create";
        public static final String ACQUIRE_LOCK = "acquireLock";
        public static final String RELEASE_LOCK = "releaseLock";
    }
}
