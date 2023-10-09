import utils.Options;

/**
 * This example shows how to count the number of images in a
 * recursively-defined folder structure using the Java sequential
 * stream framework and the {@code teeing} {@link Collector}.
 */
public class ex42 {
    /**
     * This static main() entry point runs the example.
     */
    public static void main(String[] args) {
        // Initializes the Options singleton.
        Options.instance().parseArgs(args);

        // Create an object that count the images.
        new ImageCounter();
    }
}
