package folder;

import utils.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * In conjunction with {@code StreamSupport.stream()} and {@code
 * Spliterators.spliterator()} this class creates a sequential or
 * parallel stream of {@link Dirent} objects from a
 * recursively-structured directory folder.  Since it processes the
 * contents of subfolders in parallel whenever possible it works best
 * if the contents of the recursively-structured directory folder are
 * relatively balanced.
 */
public class RecursiveFolderSpliterator
       extends Spliterators.AbstractSpliterator<Dirent> {
    /**
     * The current folder that's being processed.
     */
    private Dirent mCurrentFolder;

    /**
     * The list of (sub)folders to process.
     */
    private List<Dirent> mFoldersList;

    /**
     * The list of documents to process.
     */
    private List<Dirent> mDocsList;

    /**
     * Constructor initializes the fields and super class.
     */
    RecursiveFolderSpliterator(Folder folder) {
        super(folder.getSize(), NONNULL + IMMUTABLE);

        mDocsList = new ArrayList<>();
        mFoldersList = new ArrayList<>();
        mFoldersList.add(folder);
    }

    /**
     * Internal constructor that's used during splits.
     */
    private RecursiveFolderSpliterator(List<Dirent> leftHandFolders,
                                       List<Dirent> leftHandDocs) {
        super(Integer.MAX_VALUE, NONNULL + IMMUTABLE);

        mFoldersList = new ArrayList<>(leftHandFolders);
        mDocsList = new ArrayList<>(leftHandDocs);
    }

    /**
     * Attempt to advance the {@link Spliterator} by one {@link
     * Dirent}.  This method is used extensively by sequential streams
     * and at certain points for parallel streams.
     *
     * @return True if the streams framework should continue the
     *         traversal process, else false
     */
    public boolean tryAdvance(Consumer<? super Dirent> action) {
        Dirent currentEntry;

        // If there's an available document remove it and use it as
        // the current entry.
        if (mDocsList.size() > 0)
            // Use the last element in the list as the current entry.
            currentEntry = mDocsList.remove(mDocsList.size() - 1);

        // If folder is available remove it, use it as the current
        // entry, and add all its directory entries to the lists.
        else if (mFoldersList.size() > 0) {
            // Use the last element in the list as the current entry.
            currentEntry = mFoldersList.remove(mFoldersList.size() - 1);

            // Add any/all folders in the current entry into the folders list.
            mFoldersList.addAll(currentEntry.getSubFolders());

            // Add any/all documents in the current entry into the folders list.
            mDocsList.addAll(currentEntry.getDocuments());
        }
        // Bail out and inform the streams framework we're done.
        else
            return false;

        // Pass the current entry back to the streams framework.
        action.accept(currentEntry);
        return true;
    }

    /**
     * This method attempts to partition the folder contents
     * relatively evenly for parallel streams.
     *
     * @return A {@link Spliterator} covering {@link Dirent} objects
     *         in the current folder, that will, upon return from this
     *         method, not be covered by this spliterator (if this
     *         spliterator can be partitioned at all)
     */
    public Spliterator<Dirent> trySplit() {
        // Determine the current size of the folders list.
        int size = mFoldersList.size();

        if (size >= 2)
            // Split the folders.
            return splitMultipleFolders(size / 2);
        else if (size == 1)
            // Handle a single folder.
            return splitSingleFolder();
        else
            // Handle the documents and the current folder.
            return splitCurrentFolderAndDocs();
    }

    /**
     * Split the contents of folder that has at least two subfolders.
     *
     * @param splitPos The index into mFoldersList where the split
     *                 occurs
     * @return A {@link Spliterator} that's null if there's only one
     *         entry in the folder, else one that contains the
     *         "left-hand" dirents of the split.
     */
    private Spliterator<Dirent> splitMultipleFolders(int splitPos) {
        // If there are 2 or more subfolders then split them in half
        // and process them in parallel.  Create a sublist containing
        // the left-hand subfolders.
        List<Dirent> leftHandFolders =
            mFoldersList.subList(0,
                                 splitPos);

        // Create a sublist containing the right-hand subfolders
        // and update "this" object accordingly.
        mFoldersList =
            mFoldersList.subList(splitPos,
                                 mFoldersList.size());

        // Create and return a new RecursiveFolderSpliterator
        // containing the left-hand dirents.
        return new RecursiveFolderSpliterator(leftHandFolders,
                                              new ArrayList<>());
    }

    /**
     * @return A {@link Spliterator} either covering documents in the
     *         current folder or subfolders associated with the
     *         current folder
     */
    private Spliterator<Dirent> splitSingleFolder() {
        // Remove the one and only folder in the list and make it the
        // current folder.
        mCurrentFolder =
            mFoldersList.remove(0);

        // Initialize the documents list to the documents (if any) in
        // the current folder.
        mDocsList =
            new ArrayList<>(mCurrentFolder.getDocuments());

        // Store the subfolders (if any) in the current folder.
        List<Dirent> subFolders =
            mCurrentFolder.getSubFolders();

        // If there are no subfolders then split the current folder
        // and any documents it contains.
        if (subFolders.size() == 0) 
            return splitCurrentFolderAndDocs();

        // Otherwise split subfolders contained in the current folder.
        else
            return new RecursiveFolderSpliterator
                (subFolders, new ArrayList<>());
    }

    /**
     * @return null if there's no current folder or if there's a
     *         current folder then a {@link Spliterator} containing
     *         all the documents (if any) plus the current folder
     */
    private Spliterator<Dirent> splitCurrentFolderAndDocs() {
        // See if there's no more to be done.
        if (mCurrentFolder == null)
            // Bail out and process any documents sequentially
            // via tryAdvance().
            return null;
        else {
            // Add the current folder to the documents list so it's
            // processed by the spliterator created below.
            mDocsList.add(mCurrentFolder);

            // Convert the documents list (plus the current folder) to
            // an array and create a spliterator to process it.
            Spliterator<Dirent> spliterator = Spliterators
                .spliterator(mDocsList.toArray(),
                             0,
                             mDocsList.size(),
                             0);

            // Null out the current folder.
            mCurrentFolder = null;

            // Clear out the documents list.
            mDocsList.clear();

            // Return the spliterator.
            return spliterator;
        }
    }

    /**
     * Print the {@link String} when the verbose option is enabled.
     */
    void debug(String string) {
        if (Options.getInstance().getVerbose())
            System.out.println(string);
    }
}
