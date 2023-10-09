/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.dictionary.impl;

import stroom.dictionary.api.WordListProvider;
import stroom.dictionary.shared.DictionaryDoc;
import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docstore.api.AuditFieldFilter;
import stroom.docstore.api.DependencyRemapper;
import stroom.docstore.api.Store;
import stroom.docstore.api.StoreFactory;
import stroom.docstore.api.UniqueNameUtil;
import stroom.explorer.shared.DocumentType;
import stroom.explorer.shared.DocumentTypeGroup;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.util.shared.Message;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class DictionaryStoreImpl implements DictionaryStore, WordListProvider {

    private final Store<DictionaryDoc> store;

    @Inject
    DictionaryStoreImpl(final StoreFactory storeFactory,
                        final DictionarySerialiser serialiser) {
        this.store = storeFactory.createStore(serialiser, DictionaryDoc.DOCUMENT_TYPE, DictionaryDoc.class);
    }

    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public DocRef createDocument(final String name) {
        return store.createDocument(name);
    }

    @Override
    public DocRef copyDocument(final DocRef docRef, final Set<String> existingNames) {
        final String newName = UniqueNameUtil.getCopyName(docRef.getName(), existingNames);
        return store.copyDocument(docRef.getUuid(), newName);
    }

    @Override
    public DocRef moveDocument(final String uuid) {
        return store.moveDocument(uuid);
    }

    @Override
    public DocRef renameDocument(final String uuid, final String name) {
        return store.renameDocument(uuid, name);
    }

    @Override
    public void deleteDocument(final String uuid) {
        store.deleteDocument(uuid);
    }

    @Override
    public DocRefInfo info(String uuid) {
        return store.info(uuid);
    }

    @Override
    public DocumentType getDocumentType() {
        return new DocumentType(DocumentTypeGroup.CONFIGURATION, DictionaryDoc.DOCUMENT_TYPE,
                DictionaryDoc.DOCUMENT_TYPE);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        return store.getDependencies(createMapper());
    }

    @Override
    public Set<DocRef> getDependencies(final DocRef docRef) {
        return store.getDependencies(docRef, createMapper());
    }

    @Override
    public void remapDependencies(final DocRef docRef,
                                  final Map<DocRef, DocRef> remappings) {
        store.remapDependencies(docRef, remappings, createMapper());
    }

    private BiConsumer<DictionaryDoc, DependencyRemapper> createMapper() {
        return (doc, dependencyRemapper) -> {
            if (doc.getImports() != null) {
                final List<DocRef> replacedDocRefImports = doc
                        .getImports()
                        .stream()
                        .map(dependencyRemapper::remap)
                        .collect(Collectors.toList());
                doc.setImports(replacedDocRefImports);
            }
        };
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public DictionaryDoc readDocument(final DocRef docRef) {
        return store.readDocument(docRef);
    }

    @Override
    public DictionaryDoc writeDocument(final DictionaryDoc document) {
        return store.writeDocument(document);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Set<DocRef> listDocuments() {
        return store.listDocuments();
    }

    @Override
    public ImpexDetails importDocument(final DocRef docRef,
                                       final Map<String, byte[]> dataMap,
                                       final ImportState importState,
                                       final ImportMode importMode) {
        return store.importDocument(docRef, dataMap, importState, importMode);
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef,
                                              final boolean omitAuditFields,
                                              final List<Message> messageList) {
        if (omitAuditFields) {
            return store.exportDocument(docRef, messageList, new AuditFieldFilter<>());
        }
        return store.exportDocument(docRef, messageList, d -> d);
    }

    @Override
    public String getType() {
        return DictionaryDoc.DOCUMENT_TYPE;
    }

    @Override
    public Set<DocRef> findAssociatedNonExplorerDocRefs(DocRef docRef) {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public List<DocRef> findByName(final String name) {
        return store.findByName(name);
    }

    @Override
    public List<DocRef> list() {
        return store.list();
    }

    @Override
    public String getCombinedData(final DocRef docRef) {
        return doGetCombinedData(docRef, new HashSet<>());
    }

    @Override
    public String[] getWords(final DocRef dictionaryRef) {
        final String words = getCombinedData(dictionaryRef);
        if (words != null) {
            // Split by line break (`LF` or `CRLF`) and trim whitespace from each resulting line
            return Arrays.stream(words.split("\r?\n"))
                    .map(String::trim)
                    .toArray(String[]::new);
        }

        return null;
    }

    private String doGetCombinedData(final DocRef docRef, final Set<DocRef> visited) {
        final DictionaryDoc doc = readDocument(docRef);
        if (doc != null && !visited.contains(docRef)) {
            // Prevent circular dependencies.
            visited.add(docRef);

            final StringBuilder sb = new StringBuilder();
            if (doc.getImports() != null) {
                for (final DocRef ref : doc.getImports()) {
                    final String data = doGetCombinedData(ref, visited);
                    if (data != null && !data.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(data);
                    }
                }
            }
            if (doc.getData() != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(doc.getData());
            }
            return sb.toString();
        }
        return null;
    }
}
