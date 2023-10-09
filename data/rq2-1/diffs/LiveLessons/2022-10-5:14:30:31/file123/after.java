import java.io.File;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract super class for the various {@code FileCounter*} subclasses.
 */
public abstract class AbstractFileCounter {
    /**
     * The current file (i.e., document or folder) that's being analyzed.
     */
    protected final File mFile;

    /**
     * Keeps track of the total number of documents encountered.
     */
    protected final AtomicLong mDocumentCount;

    /**
     * Keeps track of the total number of folders encountered.
     */
    protected final AtomicLong mFolderCount;

    /**
     * Constructor initializes the fields.
     */
    AbstractFileCounter(File file) {
        mFile = file;
        mDocumentCount = new AtomicLong(0);
        mFolderCount = new AtomicLong(0);
    }

    /**
     * Constructor initializes the fields.
     */
    AbstractFileCounter(File file,
                        AtomicLong documentCount,
                        AtomicLong folderCount) {
        mFile = file;
        mDocumentCount = documentCount;
        mFolderCount = folderCount;
    }

    /**
     * @return The number of documents counted during the recursive
     * traversal
     */
    public long documentCount() {
        return mDocumentCount.get();
    }

    /**
     * @return The number of folders counted during the recursive
     * traversal
     */
    public long folderCount() {
        return mFolderCount.get();
    }

    /**
     * @return The size in bytes of the root file, as well as all the
     * files in folders reachable from this file
     */
    protected abstract long compute();
}

