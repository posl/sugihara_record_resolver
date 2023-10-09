package stroom.docstore.api;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docstore.shared.Doc;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.importexport.shared.ImportState;
import stroom.util.shared.Message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface Store<D extends Doc> extends DocumentActionHandler<D> {
    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    DocRef createDocument(String name);

    DocRef copyDocument(String originalUuid,
                        String newName);

    DocRef moveDocument(String uuid);

    DocRef renameDocument(String uuid, String name);

    void deleteDocument(String uuid);

    DocRefInfo info(String uuid);

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    Map<DocRef, Set<DocRef>> getDependencies(BiConsumer<D, DependencyRemapper> mapper);

    Set<DocRef> getDependencies(DocRef docRef, BiConsumer<D, DependencyRemapper> mapper);

    void remapDependencies(DocRef docRef, Map<DocRef, DocRef> remappings, BiConsumer<D, DependencyRemapper> mapper);

    ////////////////////////////////////////////////////////////////////////
    // END OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    /**
     * Creates the named document, using the supplied {@link DocumentCreator} to
     * provide the initial document skeleton. This allows doc store implementors
     * to provide custom skeleton content.
     */
    DocRef createDocument(final String name, final DocumentCreator<D> documentCreator);

    boolean exists(DocRef docRef);

    Set<DocRef> listDocuments();

    ImportExportActionHandler.ImpexDetails importDocument(
            DocRef docRef,
            Map<String, byte[]> dataMap,
            ImportState importState,
            ImportState.ImportMode importMode);

    Map<String, byte[]> exportDocument(DocRef docRef,
                                       List<Message> messageList,
                                       Function<D, D> filter);

    List<DocRef> list();

    List<DocRef> findByName(String name);

    interface DocumentCreator<D extends Doc> {

        D create(final String type,
                 final String uuid,
                 final String name,
                 final String version,
                 final Long createTime,
                 final Long updateTime,
                 final String createUser,
                 final String updateUser);
    }
}
