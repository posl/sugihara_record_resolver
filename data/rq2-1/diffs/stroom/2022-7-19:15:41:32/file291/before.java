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

package stroom.docstore.impl;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docstore.api.AuditFieldFilter;
import stroom.docstore.api.DependencyRemapper;
import stroom.docstore.api.DocumentSerialiser2;
import stroom.docstore.api.Store;
import stroom.docstore.shared.Doc;
import stroom.importexport.api.ImportConverter;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.importexport.shared.ImportState.State;
import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.AuditUtil;
import stroom.util.entityevent.EntityAction;
import stroom.util.entityevent.EntityEvent;
import stroom.util.entityevent.EntityEventBus;
import stroom.util.logging.LogUtil;
import stroom.util.shared.Message;
import stroom.util.shared.PermissionException;
import stroom.util.shared.Severity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class StoreImpl<D extends Doc> implements Store<D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreImpl.class);

    private final Persistence persistence;
    private final EntityEventBus entityEventBus;
    private final ImportConverter importConverter;
    private final SecurityContext securityContext;

    private final DocumentSerialiser2<D> serialiser;
    private final String type;
    private final Class<D> clazz;

    private final AtomicBoolean dirty = new AtomicBoolean();

    @Inject
    StoreImpl(final Persistence persistence,
              final EntityEventBus entityEventBus,
              final ImportConverter importConverter,
              final SecurityContext securityContext,
              final DocumentSerialiser2<D> serialiser,
              final String type,
              final Class<D> clazz) {
        this.persistence = persistence;
        this.entityEventBus = entityEventBus;
        this.importConverter = importConverter;
        this.securityContext = securityContext;
        this.serialiser = serialiser;
        this.type = type;
        this.clazz = clazz;
    }

    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public final DocRef createDocument(final String name) {
        Objects.requireNonNull(name);

        final D document = create(type, UUID.randomUUID().toString(), name);
        document.setVersion(UUID.randomUUID().toString());

        // Add audit data.
        stampAuditData(document);

        final D created = create(document);
        return createDocRef(created);
    }

    @Override
    public final DocRef createDocument(final String name, final DocumentCreator<D> documentCreator) {
        Objects.requireNonNull(name);
        final long now = System.currentTimeMillis();
        final String userId = securityContext.getUserId();

        final D document = documentCreator.create(
                type,
                UUID.randomUUID().toString(),
                name,
                UUID.randomUUID().toString(),
                now,
                now,
                userId,
                userId);

        final D created = create(document);
        return createDocRef(created);
    }

    @Override
    public DocRef copyDocument(final String originalUuid,
                               final String newName) {
        Objects.requireNonNull(originalUuid);
        Objects.requireNonNull(newName);

        final D document = read(originalUuid);
        document.setType(type);
        document.setUuid(UUID.randomUUID().toString());
        document.setName(newName);
        document.setVersion(UUID.randomUUID().toString());

        // Add audit data.
        stampAuditData(document);

        final D created = create(document);
        return createDocRef(created);
    }

    @Override
    public final DocRef moveDocument(final String uuid) {
        Objects.requireNonNull(uuid);
        final D document = read(uuid);

//        // If we are moving folder then make sure we are allowed to create items in the target folder.
//        final String permissionName = DocumentPermissionNames.getDocumentCreatePermission(type);
//        if (!securityContext.hasDocumentPermission(FOLDER, parentFolderUUID, permissionName)) {
//            throw new PermissionException(
//            securityContext.getUserId(), "You are not authorised to create items in this folder");
//        }

        // No need to save as the document has not been changed only moved.
        return createDocRef(document);
    }

    @Override
    public DocRef renameDocument(final String uuid, final String name) {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        final D document = read(uuid);

        // Only update the document if the name has actually changed.
        if (!Objects.equals(document.getName(), name)) {
            document.setName(name);
            final D updated = update(document);
            return createDocRef(updated);
        }

        return createDocRef(document);
    }

    @Override
    public final void deleteDocument(final String uuid) {
        Objects.requireNonNull(uuid);
        // Check that the user has permission to delete this item.
        if (!securityContext.hasDocumentPermission(uuid, DocumentPermissionNames.DELETE)) {
            throw new PermissionException(securityContext.getUserId(), "You are not authorised to delete this item");
        }

        persistence.getLockFactory().lock(uuid, () -> {
            final DocRef docRef = new DocRef(type, uuid);
            persistence.delete(docRef);
            EntityEvent.fire(entityEventBus, docRef, EntityAction.DELETE);
            dirty.set(true);
        });
    }

    @Override
    public DocRefInfo info(final String uuid) {
        Objects.requireNonNull(uuid);
        final D document = read(uuid);
        return DocRefInfo
                .builder()
                .docRef(DocRef.builder()
                        .type(document.getType())
                        .uuid(document.getUuid())
                        .name(document.getName())
                        .build())
                .createTime(document.getCreateTimeMs())
                .createUser(document.getCreateUser())
                .updateTime(document.getUpdateTimeMs())
                .updateUser(document.getUpdateUser())
                .build();
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies(final BiConsumer<D, DependencyRemapper> mapper) {
        return list()
                .stream()
                .filter(this::canRead)
                .collect(Collectors.toMap(docRef -> docRef, docRef ->
                        getDependencies(docRef, mapper)));
    }

    @Override
    public Set<DocRef> getDependencies(final DocRef docRef,
                                       final BiConsumer<D, DependencyRemapper> mapper) {
        if (mapper != null) {
            try {
                final D doc = readDocument(docRef);
                if (doc != null) {
                    final DependencyRemapper dependencyRemapper = new DependencyRemapper();
                    mapper.accept(doc, dependencyRemapper);
                    return dependencyRemapper.getDependencies();
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public void remapDependencies(final DocRef docRef,
                                  final Map<DocRef, DocRef> remappings,
                                  final BiConsumer<D, DependencyRemapper> mapper) {
        if (mapper != null) {
            try {
                final D doc = readDocument(docRef);
                if (doc != null) {
                    final DependencyRemapper dependencyRemapper = new DependencyRemapper(remappings);
                    mapper.accept(doc, dependencyRemapper);
                    if (dependencyRemapper.isChanged()) {
                        writeDocument(doc);
                    }
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public D readDocument(final DocRef docRef) {
        Objects.requireNonNull(docRef);
        return read(docRef.getUuid());
    }

    @Override
    public D writeDocument(final D document) {
        Objects.requireNonNull(document);
        return update(document);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////


    @Override
    public boolean exists(final DocRef docRef) {
        Objects.requireNonNull(docRef);
        return persistence.exists(docRef);
    }

    @Override
    public Set<DocRef> listDocuments() {
        final List<DocRef> list = list();
        return list.stream()
                .filter(this::canRead)
                .collect(Collectors.toSet());
    }

    private boolean canRead(final DocRef docRef) {
        Objects.requireNonNull(docRef);
        return securityContext.hasDocumentPermission(docRef.getUuid(), DocumentPermissionNames.READ);
    }

    @Override
    public ImportExportActionHandler.ImpexDetails importDocument(DocRef docRef,
                                                                 final Map<String, byte[]> dataMap,
                                                                 final ImportState importState,
                                                                 final ImportMode importMode) {
        // Convert legacy import format to the new format if necessary.
        final Map<String, byte[]> convertedDataMap = importConverter.convert(
                docRef,
                dataMap,
                importState,
                importMode,
                securityContext.getUserId());

        if (convertedDataMap != null) {
            Objects.requireNonNull(docRef);
            final String uuid = docRef.getUuid();
            try {
                // See if this document already exists and try and read it.
                final D existingDocument = getExistingDocument(docRef);

                if (ImportMode.CREATE_CONFIRMATION.equals(importMode)) {
                    // See if the new document is the same as the old one.
                    if (existingDocument == null) {
                        importState.setState(State.NEW);

                    } else {
                        docRef = docRef.copy().name(existingDocument.getName()).build();
                        if (!securityContext.hasDocumentPermission(uuid, DocumentPermissionNames.UPDATE)) {
                            throw new PermissionException(
                                    securityContext.getUserId(),
                                    "You are not authorised to update this document " + docRef);
                        }

                        final List<String> updatedFields = importState.getUpdatedFieldList();
                        checkForUpdatedFields(
                                existingDocument,
                                convertedDataMap,
                                new AuditFieldFilter<>(),
                                updatedFields);
                        if (updatedFields.size() == 0) {
                            importState.setState(State.EQUAL);
                        }
                    }

                } else if (importState.ok(importMode)) {
                    if (existingDocument != null) {
                        docRef = docRef.copy().name(existingDocument.getName()).build();
                        if (!securityContext.hasDocumentPermission(uuid, DocumentPermissionNames.UPDATE)) {
                            throw new PermissionException(
                                    securityContext.getUserId(),
                                    "You are not authorised to update this document " + docRef);
                        }
                    }

                    importDocument(docRef, existingDocument, uuid, convertedDataMap);
                }

            } catch (final RuntimeException e) {
                importState.addMessage(Severity.ERROR, e.getMessage());
            }
        }

        return new ImportExportActionHandler.ImpexDetails(docRef);
    }

    private void importDocument(final DocRef docRef,
                                final D existingDocument,
                                final String uuid,
                                final Map<String, byte[]> convertedDataMap) {
        persistence.getLockFactory().lock(uuid, () -> {
            try {
                // Turn the data map into a document.
                final D newDocument = serialiser.read(convertedDataMap);
                // Copy create time and user from the existing document.
                if (existingDocument != null) {
                    newDocument.setName(existingDocument.getName());
                    newDocument.setCreateTimeMs(existingDocument.getCreateTimeMs());
                    newDocument.setCreateUser(existingDocument.getCreateUser());
                }
                // Stamp audit data on the imported document.
                stampAuditData(newDocument);
                // Convert the document back into a data map.
                final Map<String, byte[]> finalData = serialiser.write(newDocument);
                // Write the data.
                persistence.write(docRef, existingDocument != null, finalData);

                // Fire an entity event to alert other services of the change.
                if (existingDocument != null) {
                    EntityEvent.fire(entityEventBus, docRef, EntityAction.UPDATE);
                } else {
                    EntityEvent.fire(entityEventBus, docRef, EntityAction.CREATE);
                }

                dirty.set(true);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private D getExistingDocument(final DocRef docRef) {
        try {
            if (!exists(docRef)) {
                return null;
            } else {
                return readDocument(docRef);
            }
        } catch (final PermissionException e) {
            throw new PermissionException(
                    securityContext.getUserId(),
                    "The document being imported exists but you are not authorised to read this document " + docRef);
        } catch (final RuntimeException e) {
            // Ignore.
            LOGGER.debug(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef,
                                              final List<Message> messageList,
                                              final Function<D, D> filter) {
        Map<String, byte[]> data = Collections.emptyMap();

        final String uuid = docRef.getUuid();

        try {
            // Check that the user has permission to read this item.
            if (!canRead(docRef)) {
                throw new PermissionException(
                        securityContext.getUserId(), "You are not authorised to read this document " + docRef);
            } else {
                D document = read(uuid);
                if (document == null) {
                    throw new IOException("Unable to read " + docRef);
                }
                document = filter.apply(document);
                data = serialiser.write(document);
            }
        } catch (final IOException e) {
            messageList.add(new Message(Severity.ERROR, e.getMessage()));
        }

        return data;
    }

    private void checkForUpdatedFields(final D existingDoc,
                                       final Map<String, byte[]> dataMap,
                                       final Function<D, D> filter,
                                       final List<String> updatedFieldList) {
        try {
            final D newDoc = serialiser.read(dataMap);
            final D existingDocument = filter.apply(existingDoc);
            final D newDocument = filter.apply(newDoc);

            try {
                final Method[] methods = existingDocument.getClass().getMethods();
                for (final Method method : methods) {
                    String field = method.getName();
                    if (field.length() > 4 && field.startsWith("get") && method.getParameterTypes().length == 0) {
                        final Object existingObject = method.invoke(existingDocument);
                        final Object newObject = method.invoke(newDocument);
                        if (!Objects.equals(existingObject, newObject)) {
                            field = field.substring(3);
                            field = field.substring(0, 1).toLowerCase() + field.substring(1);

                            updatedFieldList.add(field);
                        }
                    }
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }

        } catch (final RuntimeException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    private DocRef createDocRef(final D document) {
        if (document == null) {
            return null;
        }

        return new DocRef(type, document.getUuid(), document.getName());
    }

    private D create(final D document) {
        try {
            final DocRef docRef = createDocRef(document);
            final Map<String, byte[]> data = serialiser.write(document);
            persistence.getLockFactory().lock(document.getUuid(), () -> {
                try {
                    persistence.write(docRef, false, data);
                    EntityEvent.fire(entityEventBus, docRef, EntityAction.CREATE);
                    dirty.set(true);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            LOGGER.error("Error serialising {}", document.getType(), e);
            throw new UncheckedIOException(e);
        }

        return document;
    }

    private D create(final String type, final String uuid, final String name) {
        try {
            final D document = clazz.getDeclaredConstructor(new Class[0]).newInstance();
            document.setType(type);
            document.setUuid(uuid);
            document.setName(name);
            return document;
        } catch (final InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private D read(final String uuid) {
        // Check that the user has permission to read this item.
        if (!securityContext.hasDocumentPermission(uuid, DocumentPermissionNames.READ)) {
            throw new PermissionException(
                    securityContext.getUserId(),
                    LogUtil.message("You are not authorised to read document with UUID {}", uuid));
        }

        final Map<String, byte[]> data = persistence.getLockFactory().lockResult(uuid, () -> {
            try {
                return persistence.read(new DocRef(type, uuid));
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new UncheckedIOException(
                        LogUtil.message("Error reading doc {} from store {}, {}",
                                uuid, persistence.getClass().getSimpleName(), e.getMessage()), e);
            }
        });

        if (data != null) {
            try {
                return serialiser.read(data);
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new UncheckedIOException(
                        LogUtil.message("Error deserialising doc {} from store {}, {}",
                                uuid, persistence.getClass().getSimpleName(), e.getMessage()), e);
            }
        } else {
            throw new RuntimeException(LogUtil.message("No document found for UUID {} in store {}",
                    uuid, persistence.getClass().getSimpleName()));
        }
    }

    private D update(final D document) {
        final DocRef docRef = createDocRef(document);

        // Check that the user has permission to update this item.
        if (!securityContext.hasDocumentPermission(document.getUuid(), DocumentPermissionNames.UPDATE)) {
            throw new PermissionException(
                    securityContext.getUserId(), "You are not authorised to update " + document.getType() +
                    (((document.getName() != null) && document.getName().length() > 0)
                            ? " " + document.getName()
                            : "")
                    + " (" + document.getUuid() + ")");
        }

        try {
            // Get the current document version to make sure the document hasn't been changed by
            // somebody else since we last read it.
            final String currentVersion = document.getVersion();
            document.setVersion(UUID.randomUUID().toString());

            // Add audit data.
            stampAuditData(document);

            final Map<String, byte[]> newData = serialiser.write(document);

            persistence.getLockFactory().lock(document.getUuid(), () -> {
                try {
                    // Read existing data for this document.
                    final Map<String, byte[]> data = persistence.read(docRef);

                    // Perform version check to ensure the item hasn't been updated by somebody
                    // else before we try to update it.
                    if (data == null) {
                        throw new RuntimeException("Document does not exist " + docRef);
                    }

                    final D existingDocument = serialiser.read(data);

                    // Perform version check to ensure the item hasn't been updated by somebody
                    // else before we try to update it.
                    if (!existingDocument.getVersion().equals(currentVersion)) {
                        throw new RuntimeException("Document has already been updated " + docRef);
                    }

                    persistence.write(docRef, true, newData);
                    EntityEvent.fire(entityEventBus, docRef, EntityAction.UPDATE);
                    dirty.set(true);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return document;
    }

    @Override
    public List<DocRef> list() {
        return persistence
                .list(type)
                .stream()
                .filter(this::canRead)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocRef> findByName(final String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return list()
                .stream()
                .filter(docRef -> name.equals(docRef.getName()))
                .collect(Collectors.toList());
    }

    private void stampAuditData(final D document) {
        final String userId = securityContext.getUserId();
        AuditUtil.stamp(userId, document);
    }
}
