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

package stroom.pipeline.refdata;

import stroom.cache.api.CacheManager;
import stroom.cache.impl.CacheManagerImpl;
import stroom.data.shared.StreamTypeNames;
import stroom.docref.DocRef;
import stroom.feed.api.FeedStore;
import stroom.feed.shared.FeedDoc;
import stroom.pipeline.PipelineSerialiser;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.cache.DocumentPermissionCache;
import stroom.pipeline.refdata.store.MapDefinition;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataStoreFactory;
import stroom.pipeline.refdata.store.RefDataValueProxy;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.refdata.store.StringValue;
import stroom.pipeline.shared.data.PipelineReference;
import stroom.pipeline.state.FeedHolder;
import stroom.security.mock.MockSecurityContext;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.util.date.DateUtil;
import stroom.util.io.ByteSize;
import stroom.util.io.FileUtil;
import stroom.util.pipeline.scope.PipelineScopeRunnable;
import stroom.util.shared.Range;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lmdbjava.Env;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestReferenceData extends AbstractCoreIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestReferenceData.class);

    private static final ByteSize DB_MAX_SIZE = ByteSize.ofMebibytes(5);

    private static final String USER_1 = "user1";
    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";
    private static final String VALUE_3 = "value3";
    private static final String VALUE_4 = "value4";
    private static final String SID_TO_PF_1 = "SID_TO_PF_1";
    private static final String SID_TO_PF_2 = "SID_TO_PF_2";
    private static final String SID_TO_PF_3 = "SID_TO_PF_3";
    private static final String SID_TO_PF_4 = "SID_TO_PF_4";
    private static final String IP_TO_LOC_MAP_NAME = "IP_TO_LOC_MAP_NAME";

    private Env<ByteBuffer> lmdbEnv = null;
    private Path dbDir = null;

    @Mock
    private DocumentPermissionCache mockDocumentPermissionCache;
    @Mock
    private ReferenceDataLoader mockReferenceDataLoader;

    @SuppressWarnings("unused")
    @Inject
    private RefDataStoreFactory refDataStoreFactory;

    @SuppressWarnings("unused")
    @Inject
    private PipelineScopeRunnable pipelineScopeRunnable;
    @Inject
    private PipelineSerialiser pipelineSerialiser;

    private ReferenceDataConfig referenceDataConfig = new ReferenceDataConfig();
    private RefDataStore refDataStore;

    @SuppressWarnings("unused")
    @Inject
    private FeedStore feedStore;

    @SuppressWarnings("unused")
    @Inject
    private PipelineStore pipelineStore;

    @Inject
    private Provider<RefDataStoreHolder> refDataStoreHolderProvider;

    @BeforeEach
    void setup() throws IOException {

        dbDir = Files.createTempDirectory("stroom");
        LOGGER.debug("Creating LMDB environment with maxSize: {}, dbDir {}",
                getMaxSizeBytes(), dbDir.toAbsolutePath().toString());

        lmdbEnv = Env.create()
                .setMapSize(getMaxSizeBytes().getBytes())
                .setMaxDbs(10)
                .open(dbDir.toFile());

        LOGGER.debug("Creating LMDB environment in dbDir {}", getDbDir().toAbsolutePath().toString());

        referenceDataConfig.getLmdbConfig().setLocalDir(getDbDir().toAbsolutePath().toString());

        setDbMaxSizeProperty(DB_MAX_SIZE);
        refDataStore = refDataStoreFactory.getOffHeapStore();

        Mockito.when(mockDocumentPermissionCache.canUseDocument(Mockito.anyString()))
                .thenReturn(true);
    }

    @AfterEach
    void teardown() {
        if (lmdbEnv != null) {
            lmdbEnv.close();
        }
        lmdbEnv = null;
        if (Files.isDirectory(dbDir)) {
            FileUtil.deleteDir(dbDir);
        }
    }

    @Test
    void testSimple() {
        pipelineScopeRunnable.scopeRunnable(() -> {
            final DocRef feed1Ref = feedStore.createDocument("TEST_FEED_1");
            final DocRef feed2Ref = feedStore.createDocument("TEST_FEED_2");
            final DocRef pipeline1Ref = pipelineStore.createDocument("TEST_PIPELINE_1");
            final DocRef pipeline2Ref = pipelineStore.createDocument("TEST_PIPELINE_2");

            final List<PipelineReference> pipelineReferences = new ArrayList<>();
            pipelineReferences.add(new PipelineReference(pipeline1Ref, feed1Ref, StreamTypeNames.REFERENCE));
            pipelineReferences.add(new PipelineReference(pipeline2Ref, feed2Ref, StreamTypeNames.REFERENCE));

            // Set up the effective streams to be used for each
            final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
            streamSet.add(new EffectiveStream(
                    1, DateUtil.parseNormalDateTimeString("2008-01-01T09:47:00.000Z")));
            streamSet.add(new EffectiveStream(
                    2, DateUtil.parseNormalDateTimeString("2009-01-01T09:47:00.000Z")));
            streamSet.add(new EffectiveStream(
                    3, DateUtil.parseNormalDateTimeString("2010-01-01T09:47:00.000Z")));

            try (CacheManager cacheManager = new CacheManagerImpl()) {
                final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(
                        cacheManager, null, null, null, ReferenceDataConfig::new) {
                    @Override
                    protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                        return streamSet;
                    }
                };

                final ReferenceData referenceData = new ReferenceData(
                        effectiveStreamCache,
                        new FeedHolder(),
                        null,
                        null,
                        mockDocumentPermissionCache,
                        mockReferenceDataLoader,
                        refDataStoreHolderProvider.get(),
                        new RefDataLoaderHolder(),
                        pipelineStore,
                        new MockSecurityContext());

                Map<RefStreamDefinition, Runnable> mockLoaderActionsMap = new HashMap<>();

                // Add multiple reference data items to prove that looping over maps works.
                addUserDataToMockReferenceDataLoader(
                        pipeline1Ref,
                        streamSet,
                        Arrays.asList(SID_TO_PF_1, SID_TO_PF_2),
                        mockLoaderActionsMap);

                addUserDataToMockReferenceDataLoader(
                        pipeline2Ref,
                        streamSet,
                        Arrays.asList(SID_TO_PF_3, SID_TO_PF_4),
                        mockLoaderActionsMap);

                // set up the mock loader to load the appropriate data when triggered by a lookup call
                Mockito.doAnswer(invocation -> {
                    RefStreamDefinition refStreamDefinition = invocation.getArgument(0);

                    Runnable action = mockLoaderActionsMap.get(refStreamDefinition);
                    action.run();
                    return null;
                }).when(mockReferenceDataLoader).load(Mockito.any(RefStreamDefinition.class));

                // perform lookups (which will trigger a load if required) and assert the result
                checkData(referenceData, pipelineReferences, SID_TO_PF_1);
                checkData(referenceData, pipelineReferences, SID_TO_PF_2);
                checkData(referenceData, pipelineReferences, SID_TO_PF_3);
                checkData(referenceData, pipelineReferences, SID_TO_PF_4);

            } catch (final RuntimeException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * Make sure that it copes with a map not being in an early stream but is in a later one.
     */
    @Test
    void testMissingMaps() {
        pipelineScopeRunnable.scopeRunnable(() -> {
            final DocRef feed1Ref = feedStore.createDocument("TEST_FEED_1");
            final DocRef pipeline1Ref = pipelineStore.createDocument("TEST_PIPELINE_1");

            final List<PipelineReference> pipelineReferences = new ArrayList<>();
            pipelineReferences.add(new PipelineReference(pipeline1Ref, feed1Ref, StreamTypeNames.REFERENCE));

            // Set up the effective streams to be used for each
            final EffectiveStream stream1 = new EffectiveStream(
                    1, DateUtil.parseNormalDateTimeString("2008-01-01T09:47:00.000Z"));
            final EffectiveStream stream2 = new EffectiveStream(
                    2, DateUtil.parseNormalDateTimeString("2009-01-01T09:47:00.000Z"));
            final EffectiveStream stream3 = new EffectiveStream(
                    3, DateUtil.parseNormalDateTimeString("2010-01-01T09:47:00.000Z"));

            final TreeSet<EffectiveStream> streamSet1 = new TreeSet<>();
            streamSet1.add(stream1);
            final TreeSet<EffectiveStream> streamSet2and3 = new TreeSet<>();
            streamSet2and3.add(stream2);
            streamSet2and3.add(stream3);
            final TreeSet<EffectiveStream> streamSetAll = new TreeSet<>();
            streamSetAll.addAll(streamSet1);
            streamSetAll.addAll(streamSet2and3);

            try (CacheManager cacheManager = new CacheManagerImpl()) {
                final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(
                        cacheManager, null, null, null, ReferenceDataConfig::new) {
                    @Override
                    protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                        return streamSetAll;
                    }
                };

                final ReferenceData referenceData = new ReferenceData(
                        effectiveStreamCache,
                        new FeedHolder(),
                        null,
                        null,
                        mockDocumentPermissionCache,
                        mockReferenceDataLoader,
                        refDataStoreHolderProvider.get(),
                        new RefDataLoaderHolder(),
                        pipelineStore,
                        new MockSecurityContext());

                Map<RefStreamDefinition, Runnable> mockLoaderActionsMap = new HashMap<>();

                // Add multiple reference data items to prove that looping over maps works.
                addUserDataToMockReferenceDataLoader(
                        pipeline1Ref,
                        streamSet1,
                        List.of(SID_TO_PF_1),
                        mockLoaderActionsMap);
                addUserDataToMockReferenceDataLoader(
                        pipeline1Ref,
                        streamSet2and3,
                        List.of(SID_TO_PF_1, SID_TO_PF_2),
                        mockLoaderActionsMap);

                // set up the mock loader to load the appropriate data when triggered by a lookup call
                Mockito.doAnswer(
                                invocation -> {
                                    RefStreamDefinition refStreamDefinition = invocation.getArgument(0);

                                    Runnable action = mockLoaderActionsMap.get(refStreamDefinition);
                                    action.run();
                                    return null;
                                }).when(mockReferenceDataLoader)
                        .load(Mockito.any(RefStreamDefinition.class));

                // perform lookups (which will trigger a load if required) and assert the result
                final List<Tuple3<String, String, Boolean>> cases = List.of(
                        Tuple.of("2008-01-01T09:47:00.000Z", SID_TO_PF_2, false), // map not in this stream
                        Tuple.of("2009-01-01T09:47:00.111Z", SID_TO_PF_2, true), // Map found in this stream
                        Tuple.of("2010-01-01T09:47:00.111Z", SID_TO_PF_2, true),

                        Tuple.of("2008-01-01T09:47:00.000Z", SID_TO_PF_1, true),
                        Tuple.of("2009-01-01T09:47:00.111Z", SID_TO_PF_1, true),
                        Tuple.of("2010-01-01T09:47:00.111Z", SID_TO_PF_1, true));

                for (final Tuple3<String, String, Boolean> testCase : cases) {
                    final Optional<String> optFoundValue = lookup(
                            referenceData,
                            pipelineReferences,
                            testCase._1,
                            testCase._2, // Map is NOT in the stream
                            USER_1);
                    Assertions.assertThat(optFoundValue.isPresent())
                            .isEqualTo(testCase._3);
                }
            } catch (final RuntimeException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private void addUserDataToMockReferenceDataLoader(final DocRef pipelineRef,
                                                      final TreeSet<EffectiveStream> effectiveStreams,
                                                      final List<String> mapNames,
                                                      final Map<RefStreamDefinition, Runnable> mockLoaderActions) {

        for (EffectiveStream effectiveStream : effectiveStreams) {

            RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                    pipelineRef, pipelineStore.readDocument(pipelineRef).getVersion(), effectiveStream.getStreamId());


            mockLoaderActions.put(refStreamDefinition, () -> {
                refDataStore.doWithLoaderUnlessComplete(
                        refStreamDefinition, effectiveStream.getEffectiveMs(), refDataLoader -> {

                            refDataLoader.initialise(false);
                            for (String mapName : mapNames) {
                                MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);
                                refDataLoader.put(
                                        mapDefinition,
                                        USER_1,
                                        buildValue(mapDefinition, VALUE_1));
                                refDataLoader.put(
                                        mapDefinition,
                                        "user2",
                                        buildValue(mapDefinition, VALUE_2));
                            }
                            refDataLoader.completeProcessing();
                        });
            });
        }
    }

    private void addKeyValueDataToMockReferenceDataLoader(final DocRef pipelineRef,
                                                          final TreeSet<EffectiveStream> effectiveStreams,
                                                          final List<Tuple3<String, String, String>> mapKeyValueTuples,
                                                          final Map<RefStreamDefinition, Runnable> mockLoaderActions) {

        for (EffectiveStream effectiveStream : effectiveStreams) {

            RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                    pipelineRef, pipelineStore.readDocument(pipelineRef).getVersion(), effectiveStream.getStreamId());


            mockLoaderActions.put(refStreamDefinition, () -> {
                refDataStore.doWithLoaderUnlessComplete(
                        refStreamDefinition, effectiveStream.getEffectiveMs(), refDataLoader -> {

                            refDataLoader.initialise(false);
                            for (Tuple3<String, String, String> mapKeyValueTuple : mapKeyValueTuples) {
                                String mapName = mapKeyValueTuple._1();
                                String key = mapKeyValueTuple._2();
                                String value = mapKeyValueTuple._3();
                                MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);
                                refDataLoader.put(mapDefinition, key, StringValue.of(value));
                            }
                            refDataLoader.completeProcessing();
                        });
            });
        }
    }

    private void addRangeValueDataToMockReferenceDataLoader(
            final DocRef pipelineRef,
            final TreeSet<EffectiveStream> effectiveStreams,
            final List<Tuple3<String, Range<Long>, String>> mapRangeValueTuples,
            final Map<RefStreamDefinition, Runnable> mockLoaderActions) {

        for (EffectiveStream effectiveStream : effectiveStreams) {

            RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                    pipelineRef, pipelineStore.readDocument(pipelineRef).getVersion(), effectiveStream.getStreamId());


            mockLoaderActions.put(refStreamDefinition, () -> {
                refDataStore.doWithLoaderUnlessComplete(
                        refStreamDefinition, effectiveStream.getEffectiveMs(), refDataLoader -> {

                            refDataLoader.initialise(false);
                            for (Tuple3<String, Range<Long>, String> mapKeyValueTuple : mapRangeValueTuples) {
                                String mapName = mapKeyValueTuple._1();
                                Range<Long> range = mapKeyValueTuple._2();
                                String value = mapKeyValueTuple._3();
                                MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);
                                refDataLoader.put(mapDefinition, range, StringValue.of(value));
                            }
                            refDataLoader.completeProcessing();
                        });
            });
        }
    }

    private StringValue buildValue(MapDefinition mapDefinition, String value) {
        return StringValue.of(
                mapDefinition.getRefStreamDefinition().getPipelineDocRef().getUuid() + "|" +
                        mapDefinition.getRefStreamDefinition().getStreamId() + "|" +
                        mapDefinition.getMapName() + "|" +
                        value
        );
    }

    private void checkData(final ReferenceData data,
                           final List<PipelineReference> pipelineReferences,
                           final String mapName) {
        String expectedValuePart = VALUE_1;

        Optional<String> optFoundValue;

        optFoundValue = lookup(data, pipelineReferences, "2010-01-01T09:47:00.111Z", mapName, USER_1);
        doValueAsserts(optFoundValue, 3, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2015-01-01T09:47:00.000Z", mapName, USER_1);
        doValueAsserts(optFoundValue, 3, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2009-10-01T09:47:00.000Z", mapName, USER_1);
        doValueAsserts(optFoundValue, 2, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", mapName, USER_1);
        doValueAsserts(optFoundValue, 2, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2008-01-01T09:47:00.000Z", mapName, USER_1);
        doValueAsserts(optFoundValue, 1, mapName, expectedValuePart);

        optFoundValue = lookup(data, pipelineReferences, "2006-01-01T09:47:00.000Z", mapName, USER_1);
        assertThat(optFoundValue).isEmpty();

        optFoundValue = lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", mapName, "user1_X");
        assertThat(optFoundValue).isEmpty();

        optFoundValue = lookup(data, pipelineReferences, "2009-01-01T09:47:00.000Z", "SID_TO_PF_X", USER_1);
        assertThat(optFoundValue).isEmpty();
    }

    private void doValueAsserts(final Optional<String> optFoundValue,
                                final long expectedStreamId,
                                final String expectedMapName,
                                final String expectedValuePart) {
        assertThat(optFoundValue).isNotEmpty();
        String[] parts = optFoundValue.get().split("\\|");
        assertThat(parts).hasSize(4);
        assertThat(Long.parseLong(parts[1])).isEqualTo(expectedStreamId);
        assertThat(parts[2]).isEqualTo(expectedMapName);
        assertThat(parts[3]).isEqualTo(expectedValuePart);
    }

    @Test
    void testNestedMaps() {
        pipelineScopeRunnable.scopeRunnable(() -> {
            final DocRef feed1Ref = feedStore.createDocument("TEST_FEED_V1");
            final FeedDoc feedDoc = feedStore.readDocument(feed1Ref);
            feedDoc.setReference(true);
            feedStore.writeDocument(feedDoc);

            final DocRef pipelineRef = pipelineStore.createDocument("12345");
            final PipelineReference pipelineReference = new PipelineReference(pipelineRef,
                    feed1Ref,
                    StreamTypeNames.REFERENCE);
            final List<PipelineReference> pipelineReferences = Collections.singletonList(pipelineReference);

            final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
            streamSet.add(new EffectiveStream(0, 0L));
            try (CacheManager cacheManager = new CacheManagerImpl()) {
                final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(cacheManager,
                        null,
                        null,
                        null,
                        ReferenceDataConfig::new) {
                    @Override
                    protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                        return streamSet;
                    }
                };

                final ReferenceData referenceData = new ReferenceData(
                        effectiveStreamCache,
                        new FeedHolder(),
                        null,
                        null,
                        mockDocumentPermissionCache,
                        mockReferenceDataLoader,
                        refDataStoreHolderProvider.get(),
                        new RefDataLoaderHolder(),
                        pipelineStore,
                        new MockSecurityContext());

                Map<RefStreamDefinition, Runnable> mockLoaderActionsMap = new HashMap<>();

                // Add multiple reference data items to prove that looping over maps works.
                addKeyValueDataToMockReferenceDataLoader(
                        pipelineRef,
                        streamSet,
                        Arrays.asList(
                                Tuple.of("CARD_NUMBER_TO_PF_NUMBER", "011111", "091111"),
                                Tuple.of("NUMBER_TO_SID", "091111", USER_1)
                        ),
                        mockLoaderActionsMap);

                Mockito.doAnswer(invocation -> {
                    RefStreamDefinition refStreamDefinition = invocation.getArgument(0);

                    Runnable action = mockLoaderActionsMap.get(refStreamDefinition);
                    action.run();
                    return null;
                }).when(mockReferenceDataLoader).load(Mockito.any(RefStreamDefinition.class));

                Optional<String> optFoundValue;
                optFoundValue = lookup(
                        referenceData, pipelineReferences, 0, "CARD_NUMBER_TO_PF_NUMBER", "011111");
                assertThat(optFoundValue).contains("091111");

                optFoundValue = lookup(
                        referenceData, pipelineReferences, 0, "NUMBER_TO_SID", "091111");
                assertThat(optFoundValue).contains(USER_1);

                optFoundValue = lookup(referenceData, pipelineReferences, 0,
                        "CARD_NUMBER_TO_PF_NUMBER/NUMBER_TO_SID", "011111");
                assertThat(optFoundValue).contains(USER_1);

            } catch (final Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    @Test
    void testRange() {
        pipelineScopeRunnable.scopeRunnable(() -> {
            final DocRef feed1Ref = feedStore.createDocument("TEST_FEED_V1");
            final FeedDoc feedDoc = feedStore.readDocument(feed1Ref);
            feedDoc.setReference(true);
            feedStore.writeDocument(feedDoc);

            final DocRef pipelineRef = pipelineStore.createDocument("12345");
            final PipelineReference pipelineReference = new PipelineReference(pipelineRef,
                    feed1Ref,
                    StreamTypeNames.REFERENCE);
            final List<PipelineReference> pipelineReferences = Collections.singletonList(pipelineReference);

            final TreeSet<EffectiveStream> streamSet = new TreeSet<>();
            streamSet.add(new EffectiveStream(0, 0L));
            try (CacheManager cacheManager = new CacheManagerImpl()) {
                final EffectiveStreamCache effectiveStreamCache = new EffectiveStreamCache(cacheManager,
                        null,
                        null,
                        null,
                        ReferenceDataConfig::new) {
                    @Override
                    protected TreeSet<EffectiveStream> create(final EffectiveStreamKey key) {
                        return streamSet;
                    }
                };

                final ReferenceData referenceData = new ReferenceData(
                        effectiveStreamCache,
                        new FeedHolder(),
                        null,
                        null,
                        mockDocumentPermissionCache,
                        mockReferenceDataLoader,
                        refDataStoreHolderProvider.get(),
                        new RefDataLoaderHolder(),
                        pipelineStore,
                        new MockSecurityContext());

                Map<RefStreamDefinition, Runnable> mockLoaderActionsMap = new HashMap<>();

                // Add multiple reference data items to prove that looping over maps works.
                addRangeValueDataToMockReferenceDataLoader(
                        pipelineRef,
                        streamSet,
                        Arrays.asList(
                                Tuple.of(IP_TO_LOC_MAP_NAME, Range.of(2L, 30L), VALUE_1),
                                Tuple.of(IP_TO_LOC_MAP_NAME, Range.of(40L, 41L), VALUE_2),
                                Tuple.of(IP_TO_LOC_MAP_NAME, Range.of(500L, 2000L), VALUE_3),
                                Tuple.of(IP_TO_LOC_MAP_NAME, Range.of(3000L, 3001L), VALUE_4)),
                        mockLoaderActionsMap);

                Mockito.doAnswer(invocation -> {
                    RefStreamDefinition refStreamDefinition = invocation.getArgument(0);

                    Runnable action = mockLoaderActionsMap.get(refStreamDefinition);
                    action.run();
                    return null;
                }).when(mockReferenceDataLoader).load(Mockito.any(RefStreamDefinition.class));

                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "1"))
                        .isEmpty();
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "2"))
                        .contains(VALUE_1);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "10"))
                        .contains(VALUE_1);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "29"))
                        .contains(VALUE_1);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "30"))
                        .isEmpty();
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "40"))
                        .contains(VALUE_2);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "500"))
                        .contains(VALUE_3);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "1000"))
                        .contains(VALUE_3);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "1999"))
                        .contains(VALUE_3);
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "2000"))
                        .isEmpty();
                assertThat(lookup(referenceData, pipelineReferences, 0, "IP_TO_LOC_MAP_NAME", "3000"))
                        .contains(VALUE_4);
            } catch (final Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }


    private Optional<String> lookup(final ReferenceData referenceData,
                                    final List<PipelineReference> pipelineReferences,
                                    final String time,
                                    final String mapName,
                                    final String key) {
        LOGGER.debug("Looking up {}, {}, {}", time, mapName, key);
        Optional<String> optValue = lookup(referenceData,
                pipelineReferences,
                DateUtil.parseNormalDateTimeString(time),
                mapName,
                key);
        LOGGER.debug("Found {}", optValue.orElse("EMPTY"));
        return optValue;
    }

    private Optional<String> lookup(final ReferenceData referenceData,
                                    final List<PipelineReference> pipelineReferences,
                                    final long time,
                                    final String mapName,
                                    final String key) {
        final LookupIdentifier lookupIdentifier = LookupIdentifier.of(mapName, key, time);
        final ReferenceDataResult result = new ReferenceDataResult(lookupIdentifier);

        referenceData.ensureReferenceDataAvailability(pipelineReferences,
                lookupIdentifier,
                result);

        if (result.getRefDataValueProxy() != null) {
            return result.getRefDataValueProxy()
                    .flatMap(RefDataValueProxy::supplyValue)
                    .flatMap(val -> Optional.of(((StringValue) val).getValue()));
        } else {
            return Optional.empty();
        }
    }

    private void setDbMaxSizeProperty(final ByteSize size) {
        referenceDataConfig = referenceDataConfig.withLmdbConfig(referenceDataConfig.getLmdbConfig()
                .withMaxStoreSize(size));
    }

    private Path getDbDir() {
        return dbDir;
    }

    private ByteSize getMaxSizeBytes() {
        return DB_MAX_SIZE;
    }
}
