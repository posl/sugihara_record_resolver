import utils.Options;

/**
 * This example shows how to concurrently count the number of images
 * in a recursively-defined folder structure using a range of RxJava
 * features.
 */
public class ex4 {
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
