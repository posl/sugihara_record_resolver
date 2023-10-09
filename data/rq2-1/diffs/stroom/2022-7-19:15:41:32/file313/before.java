/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.index.impl;

import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFields;
import stroom.index.shared.IndexFieldsMap;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.index.shared.IndexVolume;
import stroom.search.impl.shard.IndexShardSearcher;
import stroom.util.io.FileUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestIndexShardIO {

    //    private static final IndexShardService INDEX_SHARD_SERVICE = new MockIndexShardService();
    private static final List<IndexField> INDEX_FIELDS = IndexFields.createStreamIndexFields();
    //    private static final IndexShardWriterCache INDEX_SHARD_WRITER_CACHE = new MockIndexShardWriterCache();
//    private static final IndexShardManager INDEX_SHARD_MANAGER = new MockIndexShardManager();
    private static final IndexStructure INDEX_CONFIG;

    static {
        INDEX_FIELDS.add(IndexField.createField("Id"));
        INDEX_FIELDS.add(IndexField.createField("Test"));
        INDEX_FIELDS.add(IndexField.createField("Id2"));

        final IndexDoc index = new IndexDoc();
        index.setName("Test");
        INDEX_CONFIG = new IndexStructure(index, INDEX_FIELDS, new IndexFieldsMap(INDEX_FIELDS));
    }

    public static void main(final String[] args) {
        for (final Object s : System.getProperties().keySet()) {
            System.out.println(s + "=" + System.getProperty((String) s));
        }
    }

    private Document buildDocument(final int id) {
        final Document d = new Document();
        d.add(FieldFactory.create(IndexField.createIdField("Id"), id));
        d.add(FieldFactory.create(IndexField.createField("Test"), "Test"));
        d.add(FieldFactory.create(IndexField.createIdField("Id2"), id));

        return d;
    }

    @Test
    void testOpenCloseManyWrite() throws IOException {
        final IndexVolume volume = new IndexVolume();
        volume.setPath(FileUtil.getCanonicalPath(Files.createTempDirectory("stroom")));
        final IndexDoc index = new IndexDoc();
        index.setUuid(UUID.randomUUID().toString());
        index.setName("Test");

        final IndexShard idx1 = new IndexShard();
        idx1.setId(1L);
        idx1.setIndexUuid(index.getUuid());
        idx1.setPartition("all");
        idx1.setVolume(volume);
        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());

        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);

        // Clean up from previous tests.
        final Path dir = IndexShardUtil.getIndexPath(idx1);
        FileUtil.deleteDir(dir);

        for (int i = 1; i <= 10; i++) {
            final IndexShardWriter writer = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);
            writer.flush();
            writer.addDocument(buildDocument(i));
            writer.flush();
            assertThat(writer.getDocumentCount()).isEqualTo(i);
            writer.close();
        }
    }

    @Test
    void testOpenCloseManyReadWrite() throws IOException {
        final IndexDoc index = new IndexDoc();
        index.setUuid(UUID.randomUUID().toString());
        index.setName("Test");

        final IndexVolume volume = new IndexVolume();
        volume.setPath(FileUtil.getCanonicalPath(Files.createTempDirectory("stroom")));
        final IndexShard idx1 = new IndexShard();
        idx1.setIndexUuid(index.getUuid());
        idx1.setPartition("all");
        idx1.setId(1L);
        idx1.setVolume(volume);
        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());

        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);

        // Clean up from previous tests.
        final Path dir = IndexShardUtil.getIndexPath(idx1);
        FileUtil.deleteDir(dir);

        for (int i = 1; i <= 10; i++) {
            final IndexShardWriter writer = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);
            writer.addDocument(buildDocument(i));
            writer.close();
            assertThat(writer.getDocumentCount()).isEqualTo(i);

            final IndexShardSearcher indexShardSearcher = new IndexShardSearcher(idx1);
            final SearcherManager searcherManager = indexShardSearcher.getSearcherManager();
            final IndexSearcher searcher = searcherManager.acquire();
            try {
                assertThat(searcher.getIndexReader().maxDoc()).isEqualTo(i);
            } finally {
                searcherManager.release(searcher);
            }
            indexShardSearcher.destroy();
        }
    }

    @Test
    void testShardCorruption() {
//        final Executor executor = Executors.newCachedThreadPool();
//
//        final Index index = new Index();
//        index.setName("Test");
//
//        final Volume volume = new Volume();
//        volume.setPath(getCurrentTestDir().getAbsolutePath());
//        final IndexShard idx1 = new IndexShard();
//        idx1.setIndex(index);
//        idx1.setPartition("all");
//        idx1.setId(1L);
//        idx1.setVolume(volume);
//        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());
//
//        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);
//
//        // Clean up from previous tests.
//        final File dir = IndexShardUtil.getIndexDir(idx1);
//        FileUtil.deleteDir(dir);
//
//        final Map<IndexShardKey, IndexShardWriter> map = new ConcurrentHashMap<>();
//
//        final List<CompletableFuture> futures = new ArrayList<>();
//
//        final CompletableFuture writerFuture = CompletableFuture.runAsync(() -> {
//            for (int i = 1; i < 1000000; i++) {
//                try {
//                    final IndexShardWriter writer = map.compute(indexShardKey, (k, v) -> {
//                        if (v != null) {
//                            return v;
//                        }
//
//                        IndexShardWriter result = null;
//                        try {
//                            result = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);
//                        } catch (final IOException e) {
//                            System.err.println(e.getMessage());
//                        }
//                        return result;
//                    });
//
//                    if (writer != null) {
//                        //                        System.out.println("Adding doc " + i);
//                        writer.addDocument(buildDocument(i));
//                        ThreadUtil.sleep(1);
//                    }
//                } catch (final AlreadyClosedException e) {
//                    System.out.println(e.getMessage());
//                } catch (final RuntimeException e) {
//                    System.err.println(e.getMessage());
//                }
//            }
//        }, executor);
//        futures.add(writerFuture);
//
//        final CompletableFuture closerFuture = CompletableFuture.runAsync(() -> {
//            while (!writerFuture.isDone()) {
//                try {
//                    map.compute(indexShardKey, (k, v) -> {
//                        if (v != null) {
//                            ThreadUtil.sleep(10);
//                            v.close();
//                        }
//                        return null;
//                    });
//                    ThreadUtil.sleep(100);
//                } catch (final RuntimeException e) {
//                    System.err.println(e.getMessage());
//                }
//            }
//        }, executor);
//        futures.add(closerFuture);

//        for (int i = 0; i < 100; i++) {
//            final CompletableFuture readerFuture = CompletableFuture.runAsync(() -> {
//                while (!writerFuture.isDone()) {
//                    try {
//                        final IndexShardWriter indexShardWriter = map.get(indexShardKey);
//                        IndexWriter writer = null;
//                        if (indexShardWriter != null) {
//                            writer = indexShardWriter.getWriter();
//                        }
//
//                        if (writer != null) {
//                            final IndexShardSearcher indexShardSearcher = new IndexShardSearcherImpl(idx1, writer);
//                            final SearcherManager searcherManager = indexShardSearcher.getSearcherManager();
//                            final IndexSearcher searcher = searcherManager.acquire();
//                            //                final Query query = NumericRangeQuery.newLongRange(
//                            "Id", 3L, 100L, true, true);
//
//                            final Query query = new TermQuery(new Term("Test", "Test"));
//
//                            final TotalHitCountCollector collector = new TotalHitCountCollector();
//                            searcher.search(query, collector);
////                        System.out.println(collector.getTotalHits());
//                            ThreadUtil.sleep(10);
//                        }
//                    } catch (final SearchException e) {
//                        System.out.println(e.getMessage());
//                    } catch (final RuntimeException e) {
//                        System.err.println(e.getMessage());
//                    }
//                }
//            }, executor);
//            futures.add(readerFuture);
//        }
//
//
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//        final IndexShardSearcher indexShardSearcher = new IndexShardSearcherImpl(idx1, writer1.getWriter());
//        final SearcherManager searcherManager = indexShardSearcher.getSearcherManager();
//        final IndexSearcher searcher = searcherManager.acquire();
////        try {
//            assertThat(searcher.getIndexReader().maxDoc()).isEqualTo(1);
////        } finally {
////            searcherManager.release(searcher);
////        }
//
//        writer1.close();
//
//        final IndexShardWriter writer2 = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);
//        writer2.addDocument(buildDocument(2));
//

//
//            final IndexShardSearcher indexShardSearcher = new IndexShardSearcherImpl(idx1);
//            final SearcherManager searcherManager = indexShardSearcher.getSearcherManager();
//            final IndexSearcher searcher = searcherManager.acquire();
//            try {
//                assertThat(searcher.getIndexReader().maxDoc()).isEqualTo(i);
//            } finally {
//                searcherManager.release(searcher);
//            }
//            indexShardSearcher.destroy();

    }

    @Test
    void testFailToCloseAndReopen() throws IOException {
        final IndexDoc index = new IndexDoc();
        index.setUuid(UUID.randomUUID().toString());
        index.setName("Test");

        final IndexVolume volume = new IndexVolume();
        volume.setPath(FileUtil.getCanonicalPath(Files.createTempDirectory("stroom")));
        final IndexShard idx1 = new IndexShard();
        idx1.setIndexUuid(index.getUuid());
        idx1.setPartition("all");
        idx1.setId(1L);
        idx1.setVolume(volume);
        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());

        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);

        // Clean up from previous tests.
        final Path dir = IndexShardUtil.getIndexPath(idx1);
        FileUtil.deleteDir(dir);

        final IndexShardWriter writer = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);

        for (int i = 1; i <= 10; i++) {
            writer.addDocument(buildDocument(i));
            writer.flush();
            assertThat(writer.getDocumentCount()).isEqualTo(i);
        }

        writer.close();
    }

    @Test
    void testFailToCloseFlushAndReopen() throws IOException {
        final IndexDoc index = new IndexDoc();
        index.setUuid(UUID.randomUUID().toString());
        index.setName("Test");

        final IndexVolume volume = new IndexVolume();
        volume.setPath(FileUtil.getCanonicalPath(Files.createTempDirectory("stroom")));
        final IndexShard idx1 = new IndexShard();
        idx1.setIndexUuid(index.getUuid());
        idx1.setPartition("all");
        idx1.setId(1L);
        idx1.setVolume(volume);
        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());

        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);

        // Clean up from previous tests.
        final Path dir = IndexShardUtil.getIndexPath(idx1);
        FileUtil.deleteDir(dir);

        final IndexShardWriter writer = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);

        for (int i = 1; i <= 10; i++) {
            writer.addDocument(buildDocument(i));
            writer.flush();
            assertThat(writer.getDocumentCount()).as("No docs flushed ").isEqualTo(i);
        }

        writer.close();
    }

    @Test
    void testWriteLoadsNoFlush() throws IOException {
        final IndexDoc index = new IndexDoc();
        index.setUuid(UUID.randomUUID().toString());
        index.setName("Test");

        final IndexVolume volume = new IndexVolume();
        final Path testDir = Files.createTempDirectory("stroom");
        volume.setPath(FileUtil.getCanonicalPath(testDir));
        final IndexShard idx1 = new IndexShard();
        idx1.setIndexUuid(index.getUuid());
        idx1.setPartition("all");
        idx1.setId(1L);
        idx1.setVolume(volume);
        idx1.setIndexVersion(LuceneVersionUtil.getCurrentVersion());

        final IndexShardKey indexShardKey = IndexShardKeyUtil.createTestKey(index);

        final IndexShardWriter writer = new IndexShardWriterImpl(null, INDEX_CONFIG, indexShardKey, idx1);

        Long lastSize = null;

        final HashSet<Integer> flushSet = new HashSet<>();

        for (int i = 1; i <= 100; i++) {
            writer.addDocument(buildDocument(i));
//            writer.sync();
            // System.out.println(writer.getIndexWriter().ramSizeInBytes());

            final Long newSize = idx1.getFileSize();

            if (newSize != null) {
                if (lastSize != null) {
                    if (!lastSize.equals(newSize)) {
                        flushSet.add(i);
                    }
                }
                lastSize = newSize;
            }

        }
        // TODO - TO Fix
        // assertThat(flushSet.size()).as("Some flush happened before we expected it "
        // + flushSet).isEqualTo(0);

        writer.close();
        assertThat(flushSet.isEmpty()).as("Expected not to flush").isTrue();
        // assertThat( // flushSet.toString()).as("Expected to flush every 2048 docs...").isEqualTo("[2048,
        // 6144, 4096, 8192]");
    }
}
