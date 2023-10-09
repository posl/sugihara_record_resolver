/*
 * Copyright 2018 Crown Copyright
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

package stroom.pipeline.refdata.store.offheapstore;

import stroom.bytebuffer.ByteBufferPool;
import stroom.lmdb.PutOutcome;
import stroom.pipeline.refdata.ReferenceDataConfig;
import stroom.pipeline.refdata.ReferenceDataLmdbConfig;
import stroom.pipeline.refdata.store.MapDefinition;
import stroom.pipeline.refdata.store.ProcessingState;
import stroom.pipeline.refdata.store.RefDataLoader;
import stroom.pipeline.refdata.store.RefDataProcessingInfo;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataStoreFactory;
import stroom.pipeline.refdata.store.RefDataStoreModule;
import stroom.pipeline.refdata.store.RefDataValue;
import stroom.pipeline.refdata.store.RefDataValueProxy;
import stroom.pipeline.refdata.store.RefStoreEntry;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.refdata.store.StringValue;
import stroom.pipeline.refdata.store.offheapstore.databases.KeyValueStoreDb;
import stroom.pipeline.refdata.store.offheapstore.databases.MapUidForwardDb;
import stroom.pipeline.refdata.store.offheapstore.databases.MapUidReverseDb;
import stroom.pipeline.refdata.store.offheapstore.databases.ProcessingInfoDb;
import stroom.pipeline.refdata.store.offheapstore.databases.RangeStoreDb;
import stroom.pipeline.refdata.store.offheapstore.databases.ValueStoreDb;
import stroom.task.mock.MockTaskModule;
import stroom.test.common.util.test.StroomUnitTest;
import stroom.util.concurrent.ThreadUtil;
import stroom.util.io.HomeDirProvider;
import stroom.util.io.PathCreator;
import stroom.util.io.SimplePathCreator;
import stroom.util.io.TempDirProvider;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.pipeline.scope.PipelineScopeModule;
import stroom.util.shared.Range;
import stroom.util.sysinfo.SystemInfoResult;
import stroom.util.time.StroomDuration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class TestRefDataOffHeapStore extends StroomUnitTest {

    public static final String FIXED_PIPELINE_UUID = UUID.randomUUID().toString();
    public static final String FIXED_PIPELINE_VERSION = UUID.randomUUID().toString();
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TestRefDataOffHeapStore.class);
    private static final String KV_TYPE = "KV";
    private static final String RANGE_TYPE = "Range";
    private static final String PADDING = IntStream.rangeClosed(1, 300)
            .boxed()
            .map(i -> "-")
            .collect(Collectors.joining());

    private static final int REF_STREAM_DEF_COUNT = 2;
    private static final int ENTRIES_PER_MAP_DEF = 20;
    private static final int MAPS_PER_REF_STREAM_DEF = 2;

    @Inject
    private RefDataStoreFactory refDataStoreFactory;
    @Inject
    private ByteBufferPool byteBufferPool;

    private ReferenceDataConfig referenceDataConfig = new ReferenceDataConfig();
    private Injector injector;
    private RefDataStore refDataStore;
    private Path dbDir = null;

//    void setDbMaxSizeProperty() {
//        setDbMaxSizeProperty(ByteSize.ofMebibytes(5_000));
//    }
//
//    void setDbMaxSizeProperty(final ByteSize size) {
//        referenceDataConfig.setMaxStoreSize(size);
//    }

    @BeforeEach
    void setup() throws IOException {
        dbDir = Files.createTempDirectory("stroom");
        LOGGER.debug("Creating LMDB environment in dbDir {}", dbDir.toAbsolutePath().toString());

        // This should ensure batching is exercised, including partial batches
        final int batchSize = Math.max(1, ENTRIES_PER_MAP_DEF / 2) - 1;
        LOGGER.debug("Using batchSize {}", batchSize);
        referenceDataConfig = new ReferenceDataConfig()
                .withLmdbConfig(new ReferenceDataLmdbConfig()
                        .withLocalDir(dbDir.toAbsolutePath().toString())
                        .withReaderBlockedByWriter(false))
                .withMaxPutsBeforeCommit(batchSize)
                .withMaxPurgeDeletesBeforeCommit(batchSize);

        injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ReferenceDataConfig.class).toProvider(() -> getReferenceDataConfig());
                        bind(HomeDirProvider.class).toInstance(() -> getCurrentTestDir());
                        bind(TempDirProvider.class).toInstance(() -> getCurrentTestDir());
                        bind(PathCreator.class).to(SimplePathCreator.class);
                        install(new RefDataStoreModule());
                        install(new MockTaskModule());
                        install(new PipelineScopeModule());
                    }
                });

        injector.injectMembers(this);
        refDataStore = refDataStoreFactory.getOffHeapStore();
    }

    @Test
    void getProcessingInfo() {
    }

    @Test
    void isDataLoaded_true() {
    }

    @Test
    void isDataLoaded_false() {
        final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();

        boolean isLoaded = refDataStore.isDataLoaded(refStreamDefinition);

        assertThat(isLoaded)
                .isFalse();
    }

    @Test
    void testNoReadAhead() {
        referenceDataConfig = referenceDataConfig.withLmdbConfig(referenceDataConfig.getLmdbConfig()
                .withReadAheadEnabled(false));

        // ensure loading and reading works with the NOREADAHEAD flag set
        bulkLoadAndAssert(true, 100);
    }

    @Test
    void testConsumeEntryStream() {

        bulkLoadAndAssert(true, 100);

        final List<RefStoreEntry> entries = refDataStore.consumeEntryStream(stream ->
                stream
                        .filter(refStoreEntry ->
                                refStoreEntry.getKey().equals("key38")
                                        || refStoreEntry.getKey().equals("key2"))
                        .collect(Collectors.toList()));

        // 2 because we have filtered on two unique entries
        Assertions.assertThat(entries)
                .hasSize(2);
    }

    @Test
    void testOverwrite_doOverwrite_keyValueStore() throws Exception {
        StringValue value1 = StringValue.of("myValue1");
        StringValue value2 = StringValue.of("myValue2");

        // overwriting so value changes to value2
        StringValue expectedFinalValue = value2;

        doKeyValueOverwriteTest(true, value1, value2, expectedFinalValue);
    }

    @Test
    void testOverwrite_doOverwrite_rangeValueStore() throws Exception {
        StringValue value1 = StringValue.of("myValue1");
        StringValue value2 = StringValue.of("myValue2");

        // overwriting so value changes to value2
        StringValue expectedFinalValue = value2;

        doKeyRangeValueOverwriteTest(true, value1, value2, expectedFinalValue);
    }

    @Test
    void testOverwrite_doNotOverwrite_keyValueStore() throws Exception {
        StringValue value1 = StringValue.of("myValue1");
        StringValue value2 = StringValue.of("myValue2");

        // no overwriting so value stays as value1
        StringValue expectedFinalValue = value1;

        doKeyValueOverwriteTest(false, value1, value2, expectedFinalValue);
    }

    @Test
    void testOverwrite_doNotOverwrite_rangeValueStore() throws Exception {
        StringValue value1 = StringValue.of("myValue1");
        StringValue value2 = StringValue.of("myValue2");

        // no overwriting so value stays as value1
        StringValue expectedFinalValue = value1;

        doKeyRangeValueOverwriteTest(false, value1, value2, expectedFinalValue);
    }

    private void doKeyValueOverwriteTest(final boolean overwriteExisting,
                                         final StringValue value1,
                                         final StringValue value2,
                                         final StringValue expectedFinalValue) throws Exception {

        final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();
        long effectiveTimeMs = System.currentTimeMillis();
        MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, "map1");
        String key = "myKey";

        assertThat(refDataStore.getKeyValueEntryCount())
                .isEqualTo(0);

        AtomicBoolean didPutSucceed = new AtomicBoolean(false);
        refDataStore.doWithLoaderUnlessComplete(refStreamDefinition, effectiveTimeMs, loader -> {
            loader.initialise(overwriteExisting);

            PutOutcome putOutcome;

            putOutcome = loader.put(mapDefinition, key, value1);

            assertThat(putOutcome.isSuccess())
                    .isTrue();
            assertThat(putOutcome.isDuplicate())
                    .hasValue(false);

            putOutcome = loader.put(mapDefinition, key, value2);

            assertThat(putOutcome.isSuccess())
                    .isEqualTo(overwriteExisting);
            assertThat(putOutcome.isDuplicate())
                    .hasValue(true);

            loader.completeProcessing();
        });
        refDataStore.logAllContents();

        assertThat((StringValue) refDataStore.getValue(mapDefinition, key).get())
                .isEqualTo(expectedFinalValue);

        assertThat(refDataStore.getKeyValueEntryCount())
                .isEqualTo(1);
    }

    private void doKeyRangeValueOverwriteTest(final boolean overwriteExisting,
                                              final StringValue value1,
                                              final StringValue value2,
                                              final StringValue expectedFinalValue) throws Exception {

        final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();
        long effectiveTimeMs = System.currentTimeMillis();
        MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, "map1");
        Range<Long> range = new Range<>(1L, 100L);
        final String key = "50";

        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isEqualTo(0);

        refDataStore.doWithLoaderUnlessComplete(refStreamDefinition, effectiveTimeMs, loader -> {
            loader.initialise(overwriteExisting);

            PutOutcome putOutcome;

            putOutcome = loader.put(mapDefinition, range, value1);

            assertThat(putOutcome.isSuccess())
                    .isTrue();
            assertThat(putOutcome.isDuplicate())
                    .hasValue(false);

            // second put for same key, should only succeed if overwriteExisting is enabled
            putOutcome = loader.put(mapDefinition, range, value2);

            assertThat(putOutcome.isSuccess())
                    .isEqualTo(overwriteExisting);
            assertThat(putOutcome.isDuplicate())
                    .hasValue(true);

            loader.completeProcessing();
        });

        refDataStore.logAllContents();
        assertThat((StringValue) refDataStore.getValue(mapDefinition, key).get())
                .isEqualTo(expectedFinalValue);

        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isEqualTo(1);
    }

    @Test
    void loader_noOverwriteBigCommitInterval() throws Exception {
        boolean overwriteExisting = false;
        int commitInterval = Integer.MAX_VALUE;

        bulkLoadAndAssert(overwriteExisting, commitInterval);
    }

    @Test
    void loader_noOverwriteSmallCommitInterval() throws Exception {
        boolean overwriteExisting = false;
        int commitInterval = 2;

        bulkLoadAndAssert(overwriteExisting, commitInterval);
    }

    @Test
    void loader_noOverwriteWithDuplicateData() throws Exception {
        int commitInterval = Integer.MAX_VALUE;

        RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();

        // same refStreamDefinition twice to imitate a re-load
        List<RefStreamDefinition> refStreamDefinitions = Arrays.asList(
                refStreamDefinition, refStreamDefinition);

        bulkLoadAndAssert(refStreamDefinitions, false, commitInterval);
    }

    @Test
    void loader_overwriteWithDuplicateData() throws Exception {
        int commitInterval = Integer.MAX_VALUE;

        RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();

        // same refStreamDefinition twice to imitate a re-load
        List<RefStreamDefinition> refStreamDefinitions = Arrays.asList(
                refStreamDefinition, refStreamDefinition);

        bulkLoadAndAssert(refStreamDefinitions, true, commitInterval);
    }


    @Test
    void testLoaderConcurrency() {

        final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();
        final long effectiveTimeMs = System.currentTimeMillis();

        final MapDefinition mapDefinitionKey = new MapDefinition(refStreamDefinition, "MyKeyMap");
        final MapDefinition mapDefinitionRange = new MapDefinition(refStreamDefinition, "MyRangeMap");
        final int recCount = 1_000;

        Runnable loadTask = () -> {
            LOGGER.debug("Running loadTask on thread {}", Thread.currentThread().getName());
            try {
                refDataStore.doWithLoaderUnlessComplete(refStreamDefinition, effectiveTimeMs, refDataLoader -> {
                    refDataLoader.setCommitInterval(200);
                    refDataLoader.initialise(false);

                    long rangeStartInc = 0;
                    long rangeEndExc;
                    for (int i = 0; i < recCount; i++) {
                        refDataLoader.put(mapDefinitionKey, "key" + i, StringValue.of("Value" + i));

                        rangeEndExc = rangeStartInc + 10;
                        Range<Long> range = new Range<>(rangeStartInc, rangeEndExc);
                        rangeStartInc = rangeEndExc;
                        refDataLoader.put(mapDefinitionRange, range, StringValue.of("Value" + i));
                        //                        ThreadUtil.sleepAtLeastIgnoreInterrupts(50);
                    }
                    refDataLoader.completeProcessing();
                    LOGGER.debug("Finished loading data");

                });

                LOGGER.debug("Getting values");
                LOGGER.logDurationIfDebugEnabled(() -> {
                    IntStream.range(0, recCount)
                            .boxed()
                            .sorted(Comparator.reverseOrder())
                            .forEach(i -> {
                                Optional<RefDataValue> optValue = refDataStore.getValue(mapDefinitionKey, "key" + i);
                                assertThat(optValue.isPresent());

                                optValue = refDataStore.getValue(mapDefinitionKey, Integer.toString((i * 10) + 5));
                                assertThat(optValue.isPresent());
                            });
                }, () -> LogUtil.message("Getting {} entries, twice", recCount));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LOGGER.debug("Finished running loadTask on thread {}", Thread.currentThread().getName());
        };

        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        final List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, 10)
                .boxed()
                .map(i -> {
                    LOGGER.debug("Running async task on thread {}", Thread.currentThread().getName());
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(loadTask, executorService);
                    LOGGER.debug("Got future");
                    return future;
                })
                .collect(Collectors.toList());

        futures.forEach(voidCompletableFuture -> {
            try {
                voidCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        LOGGER.debug("Finished all");
    }

    /**
     * Test to make sure that multiple threads trying to load and purge the same data do not clash
     */
    @Test
    void testConcurrentLoadAndPurge() {
        final int refStreamDefCount = 5;
        final long effectiveTimeMs = System.currentTimeMillis();
        final int recCount = 1_000;
        final int iterationCount = 200;
        final int maxTaskSleepMs = 50;

        final List<RefStreamDefinition> refStreamDefinitions = new ArrayList<>(refStreamDefCount);
        final List<MapDefinition> mapDefinitions = new ArrayList<>(refStreamDefCount);

        for (int strmId = 0; strmId < refStreamDefCount; strmId++) {
            final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition(strmId);
            refStreamDefinitions.add(refStreamDefinition);
            mapDefinitions.add(new MapDefinition(refStreamDefinition, "MyMap"));
        }
        final BiConsumer<RefStreamDefinition, MapDefinition> loadTask = (refStreamDefinition, mapDefinitionKey) -> {
            LOGGER.debug("Running loadTask on thread {}", Thread.currentThread().getName());
            try {
                refDataStore.doWithLoaderUnlessComplete(refStreamDefinition, effectiveTimeMs, refDataLoader -> {
                    // Add a cheeky sleep to make the task take a bit longer
                    ThreadUtil.sleep(ThreadLocalRandom.current().nextInt(maxTaskSleepMs));
                    try {
                        refDataLoader.setCommitInterval(200);
                        final PutOutcome putOutcome = refDataLoader.initialise(true);
                        assertThat(putOutcome.isSuccess())
                                .isTrue();

                        for (int i = 0; i < recCount; i++) {
                            refDataLoader.put(mapDefinitionKey, "key" + i, StringValue.of("Value" + i));
                        }
                        refDataLoader.completeProcessing();
                        LOGGER.debug("Finished loading data");
                    } catch (Exception e) {
                        Assertions.fail("Error: " + e.getMessage());
                    }

                    LOGGER.debug("Getting values under lock");
                    LOGGER.logDurationIfDebugEnabled(() -> {
                        IntStream.range(0, recCount)
                                .boxed()
                                .sorted(Comparator.reverseOrder())
                                .forEach(i -> {
                                    Optional<RefDataValue> optValue = refDataStore.getValue(
                                            mapDefinitionKey, "key" + i);
                                    assertThat(optValue.isPresent())
                                            .isTrue();
                                });
                    }, () -> LogUtil.message("Getting {} entries, twice", recCount));
                });


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LOGGER.debug("Finished running loadTask on thread {}", Thread.currentThread().getName());
        };

        final Consumer<RefStreamDefinition> purgeTask = (refStreamDefinition) -> {
            ThreadUtil.sleep(ThreadLocalRandom.current().nextInt(maxTaskSleepMs));
            LOGGER.debug("Running purgeTask on thread {}", Thread.currentThread().getName());
            refDataStore.purge(refStreamDefinition.getStreamId());
            LOGGER.debug("Finished running purgeTask on thread {}", Thread.currentThread().getName());
        };

        final ExecutorService executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());

        final Random random = new Random();
        final List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, iterationCount)
                .boxed()
                .flatMap(i -> {
                    final int randomInt = random.nextInt(refStreamDefCount);
                    final RefStreamDefinition refStreamDefinition = refStreamDefinitions.get(randomInt);
                    final MapDefinition mapDefinition = mapDefinitions.get(randomInt);

                    final CompletableFuture<Void> loadFuture = CompletableFuture.runAsync(
                            () -> loadTask.accept(refStreamDefinition, mapDefinition),
                            executorService);
                    final CompletableFuture<Void> purgeFuture = CompletableFuture.runAsync(
                            () -> purgeTask.accept(refStreamDefinition),
                            executorService);

                    return Stream.of(loadFuture, purgeFuture);
                })
                .collect(Collectors.toList());

        futures.forEach(voidCompletableFuture -> {
            try {
                voidCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        LOGGER.debug("Finished all");
    }

    /**
     * Each task is trying to load data for a different {@link RefStreamDefinition} so they will all be fighting over
     * the write txn
     */
    @Test
    void testLoaderConcurrency_multipleStreamDefs() {

        final long effectiveTimeMs = System.currentTimeMillis();
        final int recCount = 1_000;

        Consumer<RefStreamDefinition> loadTask = refStreamDefinition -> {
            final MapDefinition mapDefinitionKey = new MapDefinition(refStreamDefinition, "MyKeyMap");
            final MapDefinition mapDefinitionRange = new MapDefinition(refStreamDefinition, "MyRangeMap");
            LOGGER.debug("Running loadTask on thread {}", Thread.currentThread().getName());
            try {
                refDataStore.doWithLoaderUnlessComplete(refStreamDefinition, effectiveTimeMs, refDataLoader -> {
                    refDataLoader.setCommitInterval(200);
                    refDataLoader.initialise(false);

                    long rangeStartInc = 0;
                    long rangeEndExc;
                    for (int i = 0; i < recCount; i++) {
                        refDataLoader.put(mapDefinitionKey, "key" + i, StringValue.of("Value" + i));

                        rangeEndExc = rangeStartInc + 10;
                        Range<Long> range = new Range<>(rangeStartInc, rangeEndExc);
                        rangeStartInc = rangeEndExc;
                        refDataLoader.put(mapDefinitionRange, range, StringValue.of("Value" + i));
                        //                        ThreadUtil.sleepAtLeastIgnoreInterrupts(50);
                    }
                    refDataLoader.completeProcessing();
                    LOGGER.debug("Finished loading data");

                });

                LOGGER.debug("Getting values");
                LOGGER.logDurationIfDebugEnabled(() -> {
                    IntStream.range(0, recCount)
                            .boxed()
                            .sorted(Comparator.reverseOrder())
                            .forEach(i -> {
                                Optional<RefDataValue> optValue =
                                        refDataStore.getValue(mapDefinitionKey, "key" + i);
                                assertThat(optValue.isPresent());

                                optValue = refDataStore.getValue(mapDefinitionKey, Integer.toString((i * 10) + 5));
                                assertThat(optValue.isPresent());
                            });
                }, () -> LogUtil.message("Getting {} entries, twice", recCount));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LOGGER.debug("Finished running loadTask on thread {}", Thread.currentThread().getName());
        };

        ExecutorService executorService = Executors.newFixedThreadPool(6);
        List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, 10)
                .boxed()
                .map(i -> {
                    LOGGER.debug("Running async task on thread {}", Thread.currentThread().getName());
                    RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(
                            () ->
                                    loadTask.accept(refStreamDefinition),
                            executorService);
                    LOGGER.debug("Got future");
                    return future;
                })
                .collect(Collectors.toList());

        futures.forEach(voidCompletableFuture -> {
            try {
                voidCompletableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        LOGGER.debug("Finished all");
    }


    @Test
    void testDoWithRefStreamDefinitionLock() {

        final RefStreamDefinition refStreamDefinition = buildUniqueRefStreamDefinition();

        // ensure reentrance works
        ((RefDataOffHeapStore) refDataStore).doWithRefStreamDefinitionLock(refStreamDefinition, () -> {
            LOGGER.debug("Got lock");
            ((RefDataOffHeapStore) refDataStore).doWithRefStreamDefinitionLock(refStreamDefinition, () -> {
                LOGGER.debug("Got inner lock");
            });
        });
    }

    @Test
    void testPurgeOldData_all() {

        // two different ref stream definitions
        final List<RefStreamDefinition> refStreamDefinitions = IntStream.rangeClosed(1, REF_STREAM_DEF_COUNT)
                .boxed()
                .map(this::buildUniqueRefStreamDefinition)
                .collect(Collectors.toList());

        bulkLoadAndAssert(refStreamDefinitions, false, 0);

        referenceDataConfig = referenceDataConfig.withPurgeAge(StroomDuration.ZERO);

        assertThat(refDataStore.getProcessingInfoEntryCount())
                .isEqualTo(REF_STREAM_DEF_COUNT);
        assertThat(refDataStore.getKeyValueEntryCount())
                .isGreaterThan(0);
        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isGreaterThan(0);

        refDataStore.logAllContents();

        LOGGER.info("------------------------purge-starts-here--------------------------------------");
        refDataStore.purgeOldData();

        assertThat(refDataStore.getProcessingInfoEntryCount())
                .isEqualTo(0);
        assertThat(refDataStore.getKeyValueEntryCount())
                .isEqualTo(0);
        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isEqualTo(0);
    }


    @Test
    void testPurgeOldData_partial() {

        setPurgeAgeProperty(StroomDuration.ofDays(1));
        int refStreamDefCount = 4;
        int keyValueMapCount = 2;
        int rangeValueMapCount = 2;
        int entryCount = 2;
        int totalMapEntries = (refStreamDefCount * keyValueMapCount) + (refStreamDefCount * rangeValueMapCount);
        int totalKeyValueEntryCount = refStreamDefCount * keyValueMapCount * entryCount;
        int totalRangeValueEntryCount = refStreamDefCount * rangeValueMapCount * entryCount;
        int totalValueEntryCount = totalKeyValueEntryCount + totalRangeValueEntryCount;

        List<RefStreamDefinition> refStreamDefs = loadBulkData(
                refStreamDefCount, keyValueMapCount, rangeValueMapCount, entryCount);

        refDataStore.logAllContents();

        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

        long twoDaysAgoMs = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();

        // set two of the refStreamDefs to be two days old so they should get purged
        setLastAccessedTime(refStreamDefs.get(1), twoDaysAgoMs);
        setLastAccessedTime(refStreamDefs.get(3), twoDaysAgoMs);

        LOGGER.info("------------------------purge-starts-here--------------------------------------");

        // do the purge
        refDataStore.purgeOldData();

        refDataStore.logAllContents();

        int expectedRefStreamDefCount = 2;
        assertDbCounts(
                expectedRefStreamDefCount,
                (expectedRefStreamDefCount * keyValueMapCount) + (expectedRefStreamDefCount * rangeValueMapCount),
                expectedRefStreamDefCount * keyValueMapCount * entryCount,
                expectedRefStreamDefCount * rangeValueMapCount * entryCount,
                (expectedRefStreamDefCount * rangeValueMapCount * entryCount) +
                        (expectedRefStreamDefCount * rangeValueMapCount * entryCount));
    }

    @Test
    void testPurgeOldData_partial_2() {

        setPurgeAgeProperty(StroomDuration.ofDays(1));
        int refStreamDefCount = 8;
        int keyValueMapCount = 2;
        int rangeValueMapCount = 2;
        int entryCount = 2;
        int totalMapEntries = (refStreamDefCount * keyValueMapCount) + (refStreamDefCount * rangeValueMapCount);
        int totalKeyValueEntryCount = refStreamDefCount * keyValueMapCount * entryCount;
        int totalRangeValueEntryCount = refStreamDefCount * rangeValueMapCount * entryCount;
        int totalValueEntryCount = totalKeyValueEntryCount + totalRangeValueEntryCount;

        final List<RefStreamDefinition> refStreamDefs = loadBulkData(
                refStreamDefCount, keyValueMapCount, rangeValueMapCount, entryCount);

        refDataStore.logAllContents();

        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

        refDataStore.purgeOldData();

        // do the purge - nothing is old/partial so no change expected
        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

        int expectedRefStreamDefCount = refStreamDefCount;

        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(ProcessingInfoDb.DB_NAME))
                .isEqualTo(expectedRefStreamDefCount);

        // Now change the states

        // These three should be purged
        setProcessingState(refStreamDefs.get(1), ProcessingState.LOAD_IN_PROGRESS);
        setProcessingState(refStreamDefs.get(3), ProcessingState.PURGE_IN_PROGRESS);
        setProcessingState(refStreamDefs.get(5), ProcessingState.TERMINATED);

        // These two won't be purged
        setProcessingState(refStreamDefs.get(6), ProcessingState.FAILED);
        setProcessingState(refStreamDefs.get(7), ProcessingState.PURGE_FAILED);

        LOGGER.info("------------------------purge-starts-here--------------------------------------");

        // do the purge
        refDataStore.purgeOldData();

        refDataStore.logAllContents();

        expectedRefStreamDefCount = refStreamDefCount - 3;
        assertDbCounts(
                expectedRefStreamDefCount,
                (expectedRefStreamDefCount * keyValueMapCount) + (expectedRefStreamDefCount * rangeValueMapCount),
                expectedRefStreamDefCount * keyValueMapCount * entryCount,
                expectedRefStreamDefCount * rangeValueMapCount * entryCount,
                (expectedRefStreamDefCount * rangeValueMapCount * entryCount) +
                        (expectedRefStreamDefCount * rangeValueMapCount * entryCount));
    }

    @Test
    void testPurgeOldData_nothingToPurge() {

        setPurgeAgeProperty(StroomDuration.ofDays(1));
        int refStreamDefCount = 4;
        int keyValueMapCount = 2;
        int rangeValueMapCount = 2;
        int entryCount = 2;
        int totalMapEntries = (refStreamDefCount * keyValueMapCount) + (refStreamDefCount * rangeValueMapCount);
        int totalKeyValueEntryCount = refStreamDefCount * keyValueMapCount * entryCount;
        int totalRangeValueEntryCount = refStreamDefCount * rangeValueMapCount * entryCount;
        int totalValueEntryCount = totalKeyValueEntryCount + totalRangeValueEntryCount;

        List<RefStreamDefinition> refStreamDefs = loadBulkData(
                refStreamDefCount, keyValueMapCount, rangeValueMapCount, entryCount);

        refDataStore.logAllContents();

        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

        LOGGER.info("------------------------purge-starts-here--------------------------------------");

        // do the purge
        refDataStore.purgeOldData();

        refDataStore.logAllContents();

        // same as above as nothing is old enough for a purge
        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

    }

    @Test
    void testPurgeOldData_deReferenceValues() {

        setPurgeAgeProperty(StroomDuration.ofDays(1));
        int refStreamDefCount = 1;
        int keyValueMapCount = 1;
        int rangeValueMapCount = 1;
        int entryCount = 1;
        int totalMapEntries = (refStreamDefCount * keyValueMapCount) + (refStreamDefCount * rangeValueMapCount);
        int totalKeyValueEntryCount = refStreamDefCount * keyValueMapCount * entryCount;
        int totalRangeValueEntryCount = refStreamDefCount * rangeValueMapCount * entryCount;
        int totalValueEntryCount = totalKeyValueEntryCount + totalRangeValueEntryCount;

        final List<RefStreamDefinition> refStreamDefs = loadBulkData(
                refStreamDefCount,
                keyValueMapCount,
                rangeValueMapCount,
                entryCount,
                0,
                this::buildMapNameWithoutRefStreamDef);

        refDataStore.logAllContents();

        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);

        LOGGER.info("---------------------second-load-starts-here----------------------------------");

        loadBulkData(
                refStreamDefCount,
                keyValueMapCount,
                rangeValueMapCount,
                entryCount,
                4,
                this::buildMapNameWithoutRefStreamDef);

        refDataStore.logAllContents();

        // as we have run again with different refStreamDefs we should get 2x of everything
        // except the value entries as those will be the same (except with high reference counts)
        assertDbCounts(
                refStreamDefCount * 2,
                totalMapEntries * 2,
                totalKeyValueEntryCount * 2,
                totalRangeValueEntryCount * 2,
                totalValueEntryCount);


        long twoDaysAgoMs = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();

        // set two of the refStreamDefs to be two days old so they should get purged
        setLastAccessedTime(refStreamDefs.get(0), twoDaysAgoMs);

        LOGGER.info("------------------------purge-starts-here--------------------------------------");
        // do the purge
        refDataStore.purgeOldData();

        refDataStore.logAllContents();

        // back to how it was after first load with no change to value entry count as they have just been de-referenced
        assertDbCounts(
                refStreamDefCount,
                totalMapEntries,
                totalKeyValueEntryCount,
                totalRangeValueEntryCount,
                totalValueEntryCount);
    }

    @Test
    void testPurgeRefStream() {

        // two different ref stream definitions
        List<RefStreamDefinition> refStreamDefinitions = Arrays.asList(
                buildUniqueRefStreamDefinition(1),
                buildUniqueRefStreamDefinition(2),
                buildUniqueRefStreamDefinition(3),
                buildUniqueRefStreamDefinition(4));

        bulkLoadAndAssert(refStreamDefinitions, false, 1000);

        referenceDataConfig = referenceDataConfig.withPurgeAge(StroomDuration.ZERO);

        int entriesPerRefStream = MAPS_PER_REF_STREAM_DEF * ENTRIES_PER_MAP_DEF;

        assertThat(refDataStore.getProcessingInfoEntryCount())
                .isEqualTo(4);
        assertThat(refDataStore.getKeyValueEntryCount())
                .isEqualTo((long) refStreamDefinitions.size() * entriesPerRefStream);
        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isEqualTo((long) refStreamDefinitions.size() * entriesPerRefStream);

        refDataStore.logAllContents();

        LOGGER.info("------------------------purge-starts-here--------------------------------------");
        refDataStore.purge(2, 0);

        // We have purged one ref stream so there is now one less
        final int postPurgeRefStreamCount = refStreamDefinitions.size() - 1;

        assertThat(refDataStore.getProcessingInfoEntryCount())
                .isEqualTo(postPurgeRefStreamCount);
        assertThat(refDataStore.getKeyValueEntryCount())
                .isEqualTo((long) postPurgeRefStreamCount * entriesPerRefStream);
        assertThat(refDataStore.getKeyRangeValueEntryCount())
                .isEqualTo((long) postPurgeRefStreamCount * entriesPerRefStream);
    }

    /**
     * Make entryCount very big for manual performance testing or profiling
     * 50_000 takes about 4mins and makes a 250Mb db file.
     */
    @Test
    void testBigLoadForPerfTesting() {

        // Wait for visualvm to spin up
        try {
            Thread.sleep(0_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        doBigLoadGetAndPurgeForPerfTesting(5, true, false, false, true);
    }

    @Test
    void testConcurrentBigLoadAndGet() {
        // Wait for visualvm to spin up
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int entryCount = 5;
        int threads = 6;

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        final List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, threads)
                .boxed()
                .map(i -> {
                    LOGGER.info("Creating future {}", i);
                    return CompletableFuture.runAsync(() -> {
                        LOGGER.info("Running load {} on thread {}", i, Thread.currentThread().getName());
                        doBigLoadGetAndPurgeForPerfTesting(
                                entryCount, true, true, false, false);
                    }, executorService);
                })
                .collect(Collectors.toList());

        futures.forEach(cf -> {
            try {
                cf.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        final SystemInfoResult systemInfo = byteBufferPool.getSystemInfo();

        LOGGER.info(systemInfo.toString());
    }

    @Test
    void testLoadAndConcurrentGets() {
        // Wait for visualvm to spin up
        try {
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int entryCount = 5;
        int threads = 6;

        // Load the data
        doBigLoadGetAndPurgeForPerfTesting(entryCount, true, false, false, true);

        final ExecutorService executorService = Executors.newFixedThreadPool(threads);

        final List<CompletableFuture<Void>> futures = IntStream.rangeClosed(1, threads)
                .boxed()
                .map(i -> {
                    LOGGER.info("Creating future {}", i);
                    return CompletableFuture.runAsync(() -> {
                        LOGGER.info("Running load {} on thread {}", i, Thread.currentThread().getName());
                        // Now query the data
                        doBigLoadGetAndPurgeForPerfTesting(
                                entryCount, false, true, false, true);
                    }, executorService);
                })
                .collect(Collectors.toList());

        futures
                .forEach(cf -> {
                    try {
                        cf.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });

        final SystemInfoResult systemInfo = byteBufferPool.getSystemInfo();

        LOGGER.info(systemInfo.toString());
    }

    /**
     * Make entryCount very big for manual performance testing or profiling
     */
    @Test
    void testBigLoadAndGetForPerfTesting() {
        // Wait for visualvm to spin up
        try {
            Thread.sleep(00_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        doBigLoadGetAndPurgeForPerfTesting(5, true, true, false, true);

    }

    /**
     * Make entryCount very big for manual performance testing or profiling
     */
    @Test
    void testBigLoadGetAndPurgeForPerfTesting() {
        doBigLoadGetAndPurgeForPerfTesting(5, true, true, true, true);
    }


    /**
     * Make entryCount very big for manual performance testing or profiling
     */
    private void doBigLoadGetAndPurgeForPerfTesting(final int entryCount,
                                                    final boolean doLoad,
                                                    final boolean doGets,
                                                    final boolean doPurges,
                                                    final boolean doAsserts) {

        final Instant fullTestStartTime = Instant.now();

        MapNamFunc mapNamFunc = this::buildMapNameWithoutRefStreamDef;

        setPurgeAgeProperty(StroomDuration.ofDays(1));
        int refStreamDefCount = 5;
        int keyValueMapCount = 2;
        int rangeValueMapCount = 2;
        int totalMapEntries = (refStreamDefCount * keyValueMapCount) + (refStreamDefCount * rangeValueMapCount);

        int totalKeyValueEntryCount = refStreamDefCount * keyValueMapCount * entryCount;
        int totalRangeValueEntryCount = refStreamDefCount * rangeValueMapCount * entryCount;
        int totalValueEntryCount = (totalKeyValueEntryCount + totalRangeValueEntryCount) / refStreamDefCount;

        List<RefStreamDefinition> refStreamDefs1 = null;
        List<RefStreamDefinition> refStreamDefs2 = null;

        final Instant startInstant = Instant.now();

        if (doLoad) {
            LOGGER.info("-------------------------load starts here--------------------------------------");
            refStreamDefs1 = loadBulkData(
                    refStreamDefCount,
                    keyValueMapCount,
                    rangeValueMapCount,
                    entryCount,
                    0,
                    mapNamFunc);

            if (doAsserts) {
                assertDbCounts(
                        refStreamDefCount,
                        totalMapEntries,
                        totalKeyValueEntryCount,
                        totalRangeValueEntryCount,
                        totalValueEntryCount);
            }

            // here to aid debugging problems at low volumes
            if (entryCount < 10) {
                refDataStore.logAllContents(LOGGER::info);
            }


            LOGGER.info("-----------------------second-load starts here----------------------------------");

            refStreamDefs2 = loadBulkData(
                    refStreamDefCount, keyValueMapCount, rangeValueMapCount, entryCount, refStreamDefCount, mapNamFunc);

            LOGGER.info("Completed both loads in {}",
                    Duration.between(startInstant, Instant.now()).toString());
        }

        if (doAsserts) {
            assertDbCounts(
                    refStreamDefCount * 2,
                    totalMapEntries * 2,
                    totalKeyValueEntryCount * 2,
                    totalRangeValueEntryCount * 2,
                    totalValueEntryCount);
        }

        if (doGets) {
            LOGGER.info("-------------------------gets start here---------------------------------------");

            // In case the load was done elsewhere
            if (refStreamDefs1 == null) {
                refStreamDefs1 = buildRefStreamDefs(refStreamDefCount, 0);
            }
            if (refStreamDefs2 == null) {
                refStreamDefs2 = buildRefStreamDefs(refStreamDefCount, refStreamDefCount);
            }

            Random random = new Random();
            // for each ref stream def & map def, have N goes at picking a random key and getting the value for it
            Stream.concat(refStreamDefs1.stream(), refStreamDefs2.stream()).forEach(refStreamDef -> {
                Instant startTime = Instant.now();
                Stream.of(KV_TYPE, RANGE_TYPE).forEach(valueType -> {
                    for (int i = 0; i < entryCount; i++) {

                        String mapName = mapNamFunc.buildMapName(refStreamDef,
                                valueType,
                                random.nextInt(keyValueMapCount));
                        MapDefinition mapDefinition = new MapDefinition(refStreamDef, mapName);
                        int entryIdx = random.nextInt(entryCount);

                        String queryKey;
                        String expectedValue;
                        if (valueType.equals(KV_TYPE)) {
                            queryKey = buildKey(entryIdx);
                            expectedValue = buildKeyStoreValue(mapName, entryIdx, queryKey);
                        } else {
                            Range<Long> range = buildRangeKey(entryIdx);
                            // in the DB teh keys are ranges so we need to pick a value in that range
                            queryKey = Long.toString(random.nextInt(range.size().intValue()) + range.getFrom());
                            expectedValue = buildRangeStoreValue(mapName, entryIdx, range);
                        }

                        // get the proxy then get the value
                        RefDataValueProxy valueProxy = refDataStore.getValueProxy(mapDefinition, queryKey);
                        Optional<RefDataValue> optRefDataValue = valueProxy.supplyValue();

                        assertThat(optRefDataValue).isNotEmpty();
                        String value = ((StringValue) (optRefDataValue.get())).getValue();
                        assertThat(value)
                                .isEqualTo(expectedValue);

                        //now do it in one hit
                        optRefDataValue = refDataStore.getValue(mapDefinition, queryKey);
                        assertThat(optRefDataValue).isNotEmpty();
                        value = ((StringValue) (optRefDataValue.get())).getValue();
                        assertThat(value)
                                .isEqualTo(expectedValue);
                    }
                });
                LOGGER.info("Done {} queries in {} for {}",
                        entryCount * 2, Duration.between(startTime, Instant.now()).toString(), refStreamDef);
            });
        }

        if (doPurges) {

            LOGGER.info("------------------------purge-starts-here--------------------------------------");

            long twoDaysAgoMs = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli();

            refStreamDefs1.forEach(refStreamDefinition -> setLastAccessedTime(refStreamDefinition, twoDaysAgoMs));

            // do the purge
            refDataStore.purgeOldData();

            if (doAsserts) {
                assertDbCounts(
                        refStreamDefCount,
                        totalMapEntries,
                        totalKeyValueEntryCount,
                        totalRangeValueEntryCount,
                        totalValueEntryCount);
            }

            LOGGER.info("--------------------second-purge-starts-here------------------------------------");

            refStreamDefs2.forEach(refStreamDefinition -> setLastAccessedTime(refStreamDefinition, twoDaysAgoMs));

            // do the purge
            refDataStore.purgeOldData();

            if (doAsserts) {
                assertDbCounts(
                        0,
                        0,
                        0,
                        0,
                        0);
            }
        }

        final SystemInfoResult systemInfo = byteBufferPool.getSystemInfo();

        LOGGER.info(systemInfo.toString());

        LOGGER.info("Full test time {}", Duration.between(fullTestStartTime, Instant.now()));
    }

    private void assertDbCounts(final int refStreamDefCount,
                                final int totalMapEntries,
                                final int totalKeyValueEntryCount,
                                final int totalRangeValueEntryCount,
                                final int totalValueEntryCount) {

        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(ProcessingInfoDb.DB_NAME))
                .isEqualTo(refStreamDefCount);
        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(MapUidForwardDb.DB_NAME))
                .isEqualTo(totalMapEntries);
        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(MapUidReverseDb.DB_NAME))
                .isEqualTo(totalMapEntries);
        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(KeyValueStoreDb.DB_NAME))
                .isEqualTo(totalKeyValueEntryCount);
        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(RangeStoreDb.DB_NAME))
                .isEqualTo(totalRangeValueEntryCount);
        assertThat(((RefDataOffHeapStore) refDataStore).getEntryCount(ValueStoreDb.DB_NAME))
                .isEqualTo(totalValueEntryCount);
    }

    private void setLastAccessedTime(final RefStreamDefinition refStreamDef, final long newLastAccessedTimeMs) {
        ((RefDataOffHeapStore) refDataStore).setLastAccessedTime(refStreamDef, newLastAccessedTimeMs);
    }

    private void setProcessingState(final RefStreamDefinition refStreamDef, final ProcessingState processingState) {
        ((RefDataOffHeapStore) refDataStore).setProcessingState(refStreamDef, processingState);
    }

    private RefStreamDefinition buildUniqueRefStreamDefinition(final long streamId) {
        return new RefStreamDefinition(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                streamId);
    }

    private RefStreamDefinition buildUniqueRefStreamDefinition() {
        return new RefStreamDefinition(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                123456L);
    }

    private void bulkLoadAndAssert(final boolean overwriteExisting,
                                   final int commitInterval) {
        List<RefStreamDefinition> refStreamDefinitions = IntStream.rangeClosed(1, REF_STREAM_DEF_COUNT)
                .boxed()
                .map(i -> buildUniqueRefStreamDefinition())
                .collect(Collectors.toList());

        bulkLoadAndAssert(refStreamDefinitions, overwriteExisting, commitInterval);
    }

    private List<RefStreamDefinition> loadBulkData(
            final int refStreamDefinitionCount,
            final int keyValueMapCount,
            final int rangeValueMapCount,
            final int entryCount) {
        return loadBulkData(
                refStreamDefinitionCount,
                keyValueMapCount,
                rangeValueMapCount,
                entryCount,
                0,
                this::buildMapNameWithRefStreamDef);
    }

    private List<RefStreamDefinition> buildRefStreamDefs(final int count, final int offset) {

        return IntStream.rangeClosed(1, count)
                .boxed()
                .map(i -> buildRefStreamDefinition(i + offset))
                .collect(Collectors.toList());
    }

    /**
     * @param refStreamDefinitionCount  Number of {@link RefStreamDefinition}s to create.
     * @param keyValueMapCount          Number of KeyValue type maps to create per {@link RefStreamDefinition}
     * @param rangeValueMapCount        Number of RangeValue type maps to create per {@link RefStreamDefinition}
     * @param entryCount                Number of map entries to create per map
     * @param refStreamDefinitionOffset The offset from zero for the refStreamDefinition partIndex
     * @return The created {@link RefStreamDefinition} objects
     */
    private List<RefStreamDefinition> loadBulkData(
            final int refStreamDefinitionCount,
            final int keyValueMapCount,
            final int rangeValueMapCount,
            final int entryCount,
            final int refStreamDefinitionOffset,
            final MapNamFunc mapNamFunc) {

        assertThat(refStreamDefinitionCount)
                .isGreaterThan(0);
        assertThat(keyValueMapCount)
                .isGreaterThanOrEqualTo(0);
        assertThat(rangeValueMapCount)
                .isGreaterThanOrEqualTo(0);
        assertThat(entryCount)
                .isGreaterThan(0);

        List<RefStreamDefinition> refStreamDefinitions = new ArrayList<>();

        final Instant startInstant = Instant.now();

        buildRefStreamDefs(refStreamDefinitionCount, refStreamDefinitionOffset)
                .forEach(refStreamDefinition -> {
                    refStreamDefinitions.add(refStreamDefinition);

                    refDataStore.doWithLoaderUnlessComplete(
                            refStreamDefinition,
                            System.currentTimeMillis(),
                            loader -> {
                                loader.initialise(false);
                                loader.setCommitInterval(32_000);

                                loadKeyValueData(
                                        keyValueMapCount,
                                        entryCount,
                                        refStreamDefinition,
                                        loader,
                                        mapNamFunc);

                                loadRangeValueData(keyValueMapCount,
                                        entryCount,
                                        refStreamDefinition,
                                        loader,
                                        mapNamFunc);

                                loader.completeProcessing();
                            });
                });

        LOGGER.info("Loaded {} ref stream definitions in {}",
                refStreamDefinitionCount, Duration.between(startInstant, Instant.now()).toString());

        LOGGER.info("Counts:, KeyValue: {}, KeyRangeValue: {}, ProcInfo: {}",
                refDataStore.getKeyValueEntryCount(),
                refDataStore.getKeyRangeValueEntryCount(),
                refDataStore.getProcessingInfoEntryCount());

        return refStreamDefinitions;
    }

    private RefStreamDefinition buildRefStreamDefinition(final long i) {
        return new RefStreamDefinition(
                FIXED_PIPELINE_UUID,
                FIXED_PIPELINE_VERSION,
                i);
    }

    private void loadRangeValueData(final int keyValueMapCount,
                                    final int entryCount,
                                    final RefStreamDefinition refStreamDefinition,
                                    final RefDataLoader loader) {
        loadRangeValueData(keyValueMapCount,
                entryCount,
                refStreamDefinition,
                loader,
                this::buildMapNameWithRefStreamDef);
    }

    private void loadRangeValueData(final int keyValueMapCount,
                                    final int entryCount,
                                    final RefStreamDefinition refStreamDefinition,
                                    final RefDataLoader loader,
                                    final MapNamFunc mapNamFunc) {
        // load the range/value data
        for (int j = 0; j < keyValueMapCount; j++) {
            String mapName = mapNamFunc.buildMapName(refStreamDefinition, RANGE_TYPE, j);
            MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);

            for (int k = 0; k < entryCount; k++) {
                Range<Long> range = buildRangeKey(k);
                String value = buildRangeStoreValue(mapName, k, range);
                loader.put(mapDefinition, range, StringValue.of(value));
            }
        }
    }

    private Range<Long> buildRangeKey(final int k) {
        return Range.of((long) k * 10, (long) (k * 10) + 10);
    }


    private void loadKeyValueData(final int keyValueMapCount,
                                  final int entryCount,
                                  final RefStreamDefinition refStreamDefinition,
                                  final RefDataLoader loader) {
        loadKeyValueData(keyValueMapCount, entryCount, refStreamDefinition, loader, this::buildMapNameWithRefStreamDef);
    }

    private void loadKeyValueData(final int keyValueMapCount,
                                  final int entryCount,
                                  final RefStreamDefinition refStreamDefinition,
                                  final RefDataLoader loader,
                                  final MapNamFunc mapNamFunc) {
        // load the key/value data
        for (int j = 0; j < keyValueMapCount; j++) {
            String mapName = mapNamFunc.buildMapName(refStreamDefinition, KV_TYPE, j);
            MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);

            for (int k = 0; k < entryCount; k++) {
                String key = buildKey(k);
                String value = buildKeyStoreValue(mapName, k, key);
                loader.put(mapDefinition, key, StringValue.of(value));
            }
        }
    }

    private String buildRangeStoreValue(final String mapName, final int i, final Range<Long> range) {
        // pad the values out to make them more realistic in length to see impact on writes
        return LogUtil.message("{}-{}-{}-value{}{}",
                mapName, range.getFrom(), range.getTo(), i, PADDING);
    }

    private String buildMapNameWithRefStreamDef(
            final RefStreamDefinition refStreamDefinition,
            final String type,
            final int i) {
        return LogUtil.message("refStreamDef{}-{}map{}",
                refStreamDefinition.getStreamId(), type, i);
    }

    private String buildMapNameWithoutRefStreamDef(
            final RefStreamDefinition refStreamDefinition,
            final String type,
            final int i) {
        return LogUtil.message("{}map{}",
                type, i);
    }

    private String buildKeyStoreValue(final String mapName,
                                      final int i,
                                      final String key) {
        // pad the values out to make them more realistic in length to see impact on writes
        return LogUtil.message("{}-{}-value{}{}", mapName, key, i, PADDING);
    }

    private String buildKey(final int k) {
        return "key" + k;
    }


    private void bulkLoadAndAssert(final List<RefStreamDefinition> refStreamDefinitions,
                                   final boolean overwriteExisting,
                                   final int commitInterval) {


        long effectiveTimeMs = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger();

        final List<String> mapNames = IntStream.rangeClosed(1, MAPS_PER_REF_STREAM_DEF)
                .boxed()
                .map(i -> "Map" + i)
                .collect(Collectors.toList());

        final List<Tuple3<MapDefinition, String, StringValue>> keyValueLoadedData = new ArrayList<>();
        final List<Tuple3<MapDefinition, Range<Long>, StringValue>> keyRangeValueLoadedData = new ArrayList<>();

        final AtomicReference<RefStreamDefinition> lastRefStreamDefinition = new AtomicReference<>(null);
        final AtomicInteger lastCounterStartVal = new AtomicInteger();

        refStreamDefinitions.forEach(refStreamDefinition -> {
            try {
                final Tuple2<Long, Long> startEntryCounts = Tuple.of(
                        refDataStore.getKeyValueEntryCount(),
                        refDataStore.getKeyRangeValueEntryCount());

                boolean isLoadExpectedToHappen = true;
                //Same stream def as last time so
                if (refStreamDefinition.equals(lastRefStreamDefinition.get())) {
                    counter.set(lastCounterStartVal.get());
                    isLoadExpectedToHappen = false;
                }
                lastCounterStartVal.set(counter.get());

                int putAttempts = loadData(refStreamDefinition,
                        effectiveTimeMs,
                        commitInterval,
                        mapNames,
                        overwriteExisting,
                        counter,
                        keyValueLoadedData,
                        keyRangeValueLoadedData,
                        isLoadExpectedToHappen);


                final Tuple2<Long, Long> endEntryCounts = Tuple.of(
                        refDataStore.getKeyValueEntryCount(),
                        refDataStore.getKeyRangeValueEntryCount());

                int expectedNewEntries;
                if (refStreamDefinition.equals(lastRefStreamDefinition.get())) {
                    expectedNewEntries = 0;
                } else {
                    expectedNewEntries = putAttempts;
                }

                assertThat(endEntryCounts._1)
                        .isEqualTo(startEntryCounts._1 + expectedNewEntries);
                assertThat(endEntryCounts._2)
                        .isEqualTo(startEntryCounts._2 + expectedNewEntries);

                lastRefStreamDefinition.set(refStreamDefinition);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

//            refDataStore.logAllContents();

            RefDataProcessingInfo refDataProcessingInfo = refDataStore.getAndTouchProcessingInfo(refStreamDefinition)
                    .get();

            assertThat(refDataProcessingInfo.getProcessingState())
                    .isEqualTo(ProcessingState.COMPLETE);

        });

        if (ENTRIES_PER_MAP_DEF < 50) {
            refDataStore.logAllContents(LOGGER::info);
        }

        LOGGER.info("Starting key value asserts");
        assertLoadedKeyValueData(keyValueLoadedData);
        LOGGER.info("Starting range value asserts");
        assertLoadedKeyRangeValueData(keyRangeValueLoadedData);
        LOGGER.info("Completed asserts");
    }

    private void assertLoadedKeyValueData(final List<Tuple3<MapDefinition, String, StringValue>> keyValueLoadedData) {

        LOGGER.info("Starting key value asserts on {} entries", keyValueLoadedData.size());

        final Instant startTime = Instant.now();
        final AtomicInteger counter = new AtomicInteger();
        final int tenPercent = keyValueLoadedData.size() / 10;

        // query all values from the key/value store
        keyValueLoadedData.forEach(tuple3 -> {
            final MapDefinition mapDefinition = tuple3._1;
            final String key = tuple3._2;
            final StringValue expectedValue = tuple3._3;

            if (counter.incrementAndGet() % tenPercent == 0) {
                final Duration duration = Duration.between(startTime, Instant.now());
                LOGGER.info("Count so far: {}, in {}, gets/ms: {}",
                        counter.get(),
                        duration,
                        counter.get() / (double) duration.toMillis());
            }
            // get the proxy object
            final RefDataValueProxy valueProxy = refDataStore.getValueProxy(mapDefinition, key);

            // Trigger the lookup
            final RefDataValue refDataValue = valueProxy.supplyValue().get();

            assertThat(refDataValue).isInstanceOf(StringValue.class);
            assertThat((StringValue) refDataValue)
                    .isEqualTo(expectedValue);

            // now consume the proxied value in a txn
            valueProxy.consumeBytes(typedByteBuffer -> {
                assertThat(typedByteBuffer.getTypeId())
                        .isEqualTo(StringValue.TYPE_ID);
                String foundStrVal = StandardCharsets.UTF_8.decode(typedByteBuffer.getByteBuffer()).toString();
                assertThat(foundStrVal)
                        .isEqualTo(expectedValue.getValue());
            });
        });
    }

    private void assertLoadedKeyRangeValueData(
            final List<Tuple3<MapDefinition, Range<Long>, StringValue>> keyRangeValueLoadedData) {

        LOGGER.info("Starting range value asserts on {} entries", keyRangeValueLoadedData.size());

        final Instant startTime = Instant.now();
        final AtomicInteger counter = new AtomicInteger();
        final int tenPercent = keyRangeValueLoadedData.size() / 10;

        keyRangeValueLoadedData.forEach(tuple3 -> {
            final MapDefinition mapDefinition = tuple3._1;
            final Range<Long> keyRange = tuple3._2;
            final StringValue expectedValue = tuple3._3;

            if (counter.incrementAndGet() % tenPercent == 0) {
                final Duration duration = Duration.between(startTime, Instant.now());
                // *5 because we do 5 different gets for each tuple3 entry
                LOGGER.info("Count so far: {}, in {}, gets/ms: {}",
                        counter.get(),
                        duration,
                        (counter.get() * 5L) / (double) duration.toMillis());
            }

            // build a variety of keys from the supplied range
            final String keyAtStartOfRange = keyRange.getFrom().toString();
            final String keyAtEndOfRange = Long.toString(keyRange.getTo() - 1);
            final String keyInsideRange = Long.toString(keyRange.getFrom() + 5);
            final String keyBelowRange = Long.toString(keyRange.getFrom() - 1);
            final String keyAboveRange = Long.toString(keyRange.getTo() + 1);

            // define the expected result for each key
            final List<Tuple2<String, Boolean>> keysAndExpectedResults = Arrays.asList(
                    Tuple.of(keyAtStartOfRange, true),
                    Tuple.of(keyAtEndOfRange, true),
                    Tuple.of(keyInsideRange, true),
                    Tuple.of(keyBelowRange, false),
                    Tuple.of(keyAboveRange, false));

            keysAndExpectedResults.forEach(tuple2 -> {
                final String key = tuple2._1;
                final boolean isValueExpected = tuple2._2;

                LOGGER.debug(() -> LogUtil.message("range {}, key {}, expected {}",
                        keyRange, key, isValueExpected));

                // get the proxy object
                final RefDataValueProxy valueProxy = refDataStore.getValueProxy(mapDefinition, key);

                // Trigger the lookup
                final Optional<RefDataValue> optRefDataValue = valueProxy.supplyValue();

                assertThat(optRefDataValue.isPresent())
                        .isEqualTo(isValueExpected);

                optRefDataValue.ifPresent(refDataValue -> {

                    assertThat(refDataValue).isInstanceOf(StringValue.class);
                    assertThat((StringValue) refDataValue)
                            .isEqualTo(expectedValue);

                    valueProxy.consumeBytes(typedByteBuffer -> {
                        assertThat(typedByteBuffer.getTypeId())
                                .isEqualTo(StringValue.TYPE_ID);
                        final String foundStrVal = StandardCharsets.UTF_8.decode(typedByteBuffer.getByteBuffer())
                                .toString();
                        assertThat(foundStrVal)
                                .isEqualTo(expectedValue.getValue());
                    });
                });
            });
        });
    }

    private int loadData(
            final RefStreamDefinition refStreamDefinition,
            final long effectiveTimeMs,
            final int commitInterval,
            final List<String> mapNames,
            final boolean overwriteExisting,
            final AtomicInteger counter,
            final List<Tuple3<MapDefinition, String, StringValue>> keyValueLoadedData,
            final List<Tuple3<MapDefinition, Range<Long>, StringValue>> keyRangeValueLoadedData,
            final boolean isLoadExpectedToHappen) throws Exception {


        boolean didLoadHappen = refDataStore.doWithLoaderUnlessComplete(
                refStreamDefinition,
                effectiveTimeMs,
                loader -> {
                    loader.initialise(overwriteExisting);
                    loader.setCommitInterval(commitInterval);

                    for (int i = 0; i < ENTRIES_PER_MAP_DEF; i++) {
                        // put key/values into each mapDef
                        mapNames.stream()
                                .map(name -> new MapDefinition(refStreamDefinition, name))
                                .forEach(mapDefinition -> {
                                    int cnt = counter.incrementAndGet();
                                    String key = buildKey(cnt);
                                    StringValue value = StringValue.of("value" + cnt);
                                    LOGGER.debug("Putting cnt {}, key {}, value {}", cnt, key, value);
                                    loader.put(mapDefinition, key, value);

                                    keyValueLoadedData.add(Tuple.of(mapDefinition, key, value));
                                });

                        // put keyrange/values into each mapDef
                        mapNames.stream()
                                .map(name -> new MapDefinition(refStreamDefinition, name))
                                .forEach(mapDefinition -> {
                                    int cnt = counter.incrementAndGet();
                                    Range<Long> keyRange = new Range<>((long) (cnt * 10), (long) ((cnt * 10) + 10));
                                    StringValue value = StringValue.of("value" + cnt);
                                    LOGGER.debug("Putting cnt {}, key-range {}, value {}", cnt, keyRange, value);
                                    loader.put(mapDefinition, keyRange, value);
                                    keyRangeValueLoadedData.add(Tuple.of(mapDefinition, keyRange, value));
                                });
                    }

                    loader.completeProcessing();
                });

        assertThat(didLoadHappen)
                .isEqualTo(isLoadExpectedToHappen);

        RefDataProcessingInfo processingInfo = refDataStore.getAndTouchProcessingInfo(refStreamDefinition).get();

        assertThat(processingInfo.getProcessingState())
                .isEqualTo(ProcessingState.COMPLETE);

        boolean isDataLoaded = refDataStore.isDataLoaded(refStreamDefinition);
        assertThat(isDataLoaded)
                .isTrue();

        return ENTRIES_PER_MAP_DEF * mapNames.size();
    }

    private interface MapNamFunc {

        String buildMapName(final RefStreamDefinition refStreamDefinition, final String type, final int i);

    }

    protected ReferenceDataConfig getReferenceDataConfig() {
        return referenceDataConfig;
    }

    protected void setPurgeAgeProperty(final StroomDuration purgeAge) {
        referenceDataConfig = referenceDataConfig.withPurgeAge(purgeAge);
    }
}
