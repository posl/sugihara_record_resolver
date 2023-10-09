package folders.folder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * This super class defines the common capabilities provided by a
 * directory entry and is inherited by the Folder and Document
 * subclasses.
 *
 * The {@code JsonTypeInfo }annotation below instructs JACKSON to
 * handle the inheritance relationship between the {@link Dirent}
 * superclass and its {@link Folder} and {@link Document} subclasses.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS,
              include=JsonTypeInfo.As.PROPERTY,
              property="@class")
public class Dirent
       implements Iterable<Dirent> {
    /**
     * Path of the document.
     */
    private File mPath;

    /**
     * The total number of entries in this recursively structured
     * folder.
     */
    private long mSize;

    /**
     * Default constructor.
     */
    public Dirent() {
    }

    /**
     * Constructor initializes the fields.
     */
    public Dirent(File path, long size) {
        mPath = path;
        mSize = size;
    }

    /**
     * @return A {@link Spliterator} for this {@link Dirent}
     */
    public Spliterator<Dirent> spliterator() {
        return Spliterators
            .spliteratorUnknownSize(iterator(), 0);
    }

    /**
     * @param parallel True if a parallel stream should be created,
     *                 false if a sequential stream should be created
     * @return A stream of {@link Dirent} objects
     */
    public Stream<Dirent> stream(boolean parallel) {
        return StreamSupport
            .stream(spliterator(), parallel);
    }

    /**
     * Factory method that returns an iterator.
     */
    public Iterator<Dirent> iterator() {
        // Initialize the breadth-first search iterator.
        return new BFSIterator(this);
    }

    /**
     * @return The list of subfolders in this folder
     */
    public List<Dirent> getSubFolders() { 
        return null;
    }

    /**
     * Set the subfolders field.
     */
    public void setSubFolders(List<Dirent> subFolders) {
    }

    /**
     * @return The list of documents in this folder
     */
    public List<Dirent> getDocuments() {
        return null;
    }

    /**
     * Set the documents field.
     */
    public void setDocuments(List<Dirent> documents) {
    }

    /**
     * @return The contents of this document
     */
    public CharSequence getContents() {
        // This is a no-op.
        return null;
    };

    /**
     * @return Name of the dirent
     */
    public String getName() {
        return mPath.getName();
    }

    /**
     * @return The path of the dirent.
     */
    public String toString() {
        return getPath().toString();
    }

    /**
     * Set path of the dirent.
     */
    public void setPath(File path) {
        mPath = path;
    }

    /**
     * @return Path of the dirent
     */
    public File getPath() {
        return mPath;
    }

    /**
     * @return The total number of entries in this recursively
     * structured dirent.
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Set the total number of entries in this recursively
     * structured dirent.
     */
    public void setSize(long size) {
        mSize = size;
    }

    /**
     * Add {@code size} to the current size.
     */
    public void addToSize(long size) {
        mSize += size;
    }

    /**
     * This iterator traverses each element in the folder using
     * (reverse) breadth-first search.
     */
    private static class BFSIterator
        implements Iterator<Dirent> {
        /**
         * The current entry to process.
         */
        private Dirent mCurrentEntry;

        /**
         * The list of (sub)folders to process.
         */
        private final List<Dirent> mFoldersList;

        /**
         * The list of documents to process.
         */
        private final List<Dirent> mDocsList;

        /**
         * Constructor initializes the fields.
         */
        BFSIterator(Dirent rootFolder) {
            // Make the rootFolder the current entry. 
            mCurrentEntry = rootFolder;

            // Add the subfolders (if any) in the rootFolder.
            mFoldersList = new ArrayList<>(rootFolder.getSubFolders());

            // Add the documents (if any) in the rootFolder.
            mDocsList = new ArrayList<>(rootFolder.getDocuments());
        }

        /**
         * @return True if the iterator can continue, false if it's at
         * the end
         */
        public boolean hasNext() {
            // See if we need to refresh the current entry.
            if (mCurrentEntry == null) {
                // See if there are any subfolders left to process.
                if (mFoldersList.size() > 0) {
                    // If there are subfolders left then pop the one
                    // at the end and make it the current entry.
                    mCurrentEntry =
                        mFoldersList.remove(mFoldersList.size() - 1);

                    // Add any/all subfolders from the new current
                    // entry to the end of the subfolders list.
                    mFoldersList.addAll(mCurrentEntry.getSubFolders());

                    // Add any/all documents from the new current
                    // entry to the end of the documents list.
                    mDocsList.addAll(mCurrentEntry.getDocuments());
                }
                // See if there are any documents left to process.
                else if (mDocsList.size() > 0) {
                    // Pop the document at the end of the list off and
                    // make it the current entry.
                    mCurrentEntry =
                        mDocsList.remove(mDocsList.size() - 1);
                }
            }

            // Return false if there are no more entries, else true.
            return mCurrentEntry != null;
        }

        /**
         * @return The next unseen entry in the folder
         */
        public Dirent next() {
            // Store the current entry.
            Dirent nextDirent = mCurrentEntry;

            // Reset current entry to null.
            mCurrentEntry = null;
            
            // Return the current entry.
            return nextDirent;
        }
    }
}
