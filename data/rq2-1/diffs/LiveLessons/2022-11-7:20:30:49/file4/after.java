package utils;

/**
 * This class implements the Singleton pattern to handle
 * command-line option processing.
 */
public final class Options {
    /** The singleton @a Options instance. */
    private static Options mUniqueInstance = null;

    /**
     * Controls whether debugging output will be generated (defaults
     * to false).
     */
    private boolean mDiagnosticsEnabled = false;

    /**
     * Method to return the one and only singleton uniqueInstance.
     */
    public static Options instance() {
        if (mUniqueInstance == null)
            mUniqueInstance = new Options();

        return mUniqueInstance;
    }

    /**
     * Returns whether debugging output is generated.
     */
    public boolean getDiagnosticsEnabled() {
        return mDiagnosticsEnabled;
    }

    /**
     * Sets whether debugging output is generated.
     */
    public boolean setDiagnosticsEnabled(boolean enable) {
        return mDiagnosticsEnabled = enable;
    }

    /**
     * Parse command-line arguments and set the appropriate values.
     */
    public void parseArgs(String[] argv) {
        if (argv != null) {
            for (int argc = 0; argc < argv.length; argc++)
                switch (argv[argc]) {
                case "-d":
                    mDiagnosticsEnabled = argv[++argc].equals("true");
                    break;
                default:
                    printUsage(argv[argc]);
                    return;
                }
        }
    }

    /**
     * Display the {@code output} if debugging is enabled.

     * @param output The {@code output} to display
     */
    public static void display(String output) {
        if (Options.instance().getDiagnosticsEnabled())
            System.out.println("["
                               + Thread.currentThread().getId()
                               + "] "
                               + output);
    }

    /**
     * Print out usage and default values.
     */
    public void printUsage(String arg) {
        System.out.println(arg + " is an invalid argument\n" + "Usage: ");
        System.out.println("-d [true|false]");
    }

    /**
     * Make the constructor private for a singleton.
     */
    private Options() {
    }
}
