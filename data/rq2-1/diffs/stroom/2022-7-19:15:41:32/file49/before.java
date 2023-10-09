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

package stroom.proxy.repo;

import stroom.data.zip.StroomZipEntry;
import stroom.data.zip.StroomZipFile;
import stroom.data.zip.StroomZipFileType;
import stroom.data.zip.StroomZipOutputStream;
import stroom.feed.api.FeedProperties;
import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.StandardHeaderArguments;
import stroom.util.date.DateUtil;
import stroom.util.io.AbstractFileVisitor;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

public class ProxyRepositoryCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRepositoryCreator.class);

    private static final String INPUT_EXTENSION = ".in";
    private static final String ZIP_EXTENSION = ".zip";

    private final FeedProperties feedProperties;
    private final ProxyRepo repository;

    public ProxyRepositoryCreator(final FeedProperties feedProperties, final ProxyRepo repository) {
        this.feedProperties = feedProperties;
        this.repository = repository;
    }

    public void read(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        readDir(dir, mandateEffectiveDate, effectiveMs);
    }

    private void readDir(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        try {
            Files.walkFileTree(dir,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    new AbstractFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                            try {
                                final String fileName = file.getFileName().toString().toLowerCase();
                                if (fileName.endsWith(INPUT_EXTENSION)) {
                                    loadInput(file, mandateEffectiveDate, effectiveMs);

                                } else if (fileName.endsWith(ZIP_EXTENSION)) {
                                    loadZip(file, mandateEffectiveDate, effectiveMs);
                                }
                            } catch (final RuntimeException e) {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadInput(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        // Get the feed.
        final String feedName = getFeedName(file);

        try {
            if (feedProperties.isReference(feedName) == mandateEffectiveDate) {
                LOGGER.info("Loading data: " + FileUtil.getCanonicalPath(file));

                final AttributeMap attributeMap = createMap(feedName, effectiveMs);
                try (final StroomZipOutputStream zipOutputStream = repository.getStroomZipOutputStream(attributeMap)) {
                    int i = 0;
                    i++;
                    String newName = Integer.toString(i);
                    newName = Strings.padStart(newName, 3, '0');

                    // Add meta data.
                    try (final OutputStream zipPart = zipOutputStream.addEntry(StroomZipEntry.create(
                            newName,
                            StroomZipFileType.META).getFullName())) {
                        AttributeMapUtil.write(attributeMap, zipPart);
                    }

                    // Add data.
                    try (final InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                            final OutputStream zipPart = zipOutputStream.addEntry(StroomZipEntry.create(
                                    newName,
                                    StroomZipFileType.DATA).getFullName())) {
                        StreamUtil.streamToStream(inputStream, zipPart);
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadZip(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        // Get the feed.
        final String feedName = getFeedName(file);

        if (feedProperties.isReference(feedName) == mandateEffectiveDate) {
            LOGGER.info("Loading data: " + FileUtil.getCanonicalPath(file));

            final AttributeMap attributeMap = createMap(feedName, effectiveMs);
            try (final StroomZipOutputStream zipOutputStream = repository.getStroomZipOutputStream(attributeMap)) {
                final StroomZipFile stroomZipFile = new StroomZipFile(file);

                int i = 0;
                for (String baseName : stroomZipFile.getStroomZipNameSet().getBaseNameSet()) {
                    i++;
                    String newName = Integer.toString(i);
                    newName = Strings.padStart(newName, 3, '0');

                    // Add meta data.
                    try (final InputStream inputStream = stroomZipFile.getInputStream(baseName,
                            StroomZipFileType.META)) {
                        if (inputStream != null) {
                            AttributeMapUtil.read(inputStream, attributeMap);
                        }
                    }
                    try (final OutputStream outputStream = zipOutputStream
                            .addEntry(StroomZipEntry.create(newName, StroomZipFileType.META).getFullName())) {
                        AttributeMapUtil.write(attributeMap, outputStream);
                    }

                    // Add context data.
                    try (InputStream inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.CONTEXT)) {
                        if (inputStream != null) {
                            try (final OutputStream outputStream = zipOutputStream
                                    .addEntry(StroomZipEntry.create(
                                            newName,
                                            StroomZipFileType.CONTEXT).getFullName())) {
                                StreamUtil.streamToStream(inputStream, outputStream);
                            }
                        }
                    }

                    // Add data.
                    try (InputStream inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.DATA)) {
                        if (inputStream != null) {
                            try (final OutputStream outputStream = zipOutputStream
                                    .addEntry(StroomZipEntry.create(
                                            newName,
                                            StroomZipFileType.DATA).getFullName())) {
                                StreamUtil.streamToStream(inputStream, outputStream);
                            }
                        }
                    }
                }

                stroomZipFile.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private AttributeMap createMap(final String feedName, final Long effectiveMs) {
        final String dateTime = DateUtil.createNormalDateTimeString(effectiveMs);

        final AttributeMap map = new AttributeMap();
        map.put(StandardHeaderArguments.FEED, feedName);
        map.put(StandardHeaderArguments.RECEIVED_TIME, dateTime);
        map.put(StandardHeaderArguments.EFFECTIVE_TIME, dateTime);
        map.put("TestData", "Loaded By SetupSampleData");

        return map;
    }

    private String getFeedName(final Path file) {
        // Get the stem of the file name.
        String stem = file.getFileName().toString();
        int index = stem.indexOf('.');
        if (index != -1) {
            stem = stem.substring(0, index);
        }

        return stem;
    }
}
