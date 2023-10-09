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

package stroom.importexport.impl;

import stroom.importexport.shared.ImportSettings;
import stroom.security.api.SecurityContext;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

@SuppressWarnings("unused")
public class ContentPackImport {

    static final String FAILED_DIR = "failed";
    static final String IMPORTED_DIR = "imported";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPackImport.class);

    private final ImportExportService importExportService;
    private final ContentPackImportConfig config;
    private final SecurityContext securityContext;
    private final PathCreator pathCreator;

    @SuppressWarnings("unused")
    @Inject
    ContentPackImport(final ImportExportService importExportService,
                      final ContentPackImportConfig config,
                      final SecurityContext securityContext,
                      final PathCreator pathCreator) {
        this.importExportService = importExportService;
        this.config = config;
        this.securityContext = securityContext;
        this.pathCreator = pathCreator;
    }

    public void startup() {
        final boolean isEnabled = config.isEnabled();

        if (isEnabled) {
            LOGGER.info("Configured import dir is '" + config.getImportDirectory() + "'");
            if (config.getImportDirectory() != null) {
                final Path resolvedPath = pathCreator.toAppPath(config.getImportDirectory());
                LOGGER.info("Importing from resolved dir '" + resolvedPath + "'");
                securityContext.asAdminUser(() -> doImport(resolvedPath));
            } else {
                LOGGER.warn("Content pack import is enabled but the configured directory is null");
            }
        } else {
            LOGGER.info("Content pack import currently disabled via property");
        }
    }

    // pkg private for testing
//    void startup(final List<Path> contentPacksDirs) {
//        final boolean isEnabled = config.isEnabled();
//
//        if (isEnabled) {
//            final UserIdentity admin = securityContext.createIdentity(User.ADMIN_USER_NAME);
//            securityContext.asUser(admin, () ->
//                    doImport(contentPacksDirs));
//        } else {
//            LOGGER.info("Content pack import currently disabled via property");
//        }
//    }

    private void doImport(final Path contentPacksDir) {

        final AtomicInteger successCounter = new AtomicInteger();
        final AtomicInteger failedCounter = new AtomicInteger();

        LOGGER.info("ContentPackImport started, checking the following directory for content packs to import,\n{}",
                contentPacksDir.toAbsolutePath().normalize());

        if (!Files.isDirectory(contentPacksDir)) {
            LOGGER.warn("Content pack import directory {} doesn't exist", FileUtil.getCanonicalPath(contentPacksDir));
        } else {
            LOGGER.info("Processing content packs in directory {}", FileUtil.getCanonicalPath(contentPacksDir));

            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(contentPacksDir, "*.zip")) {
                stream.forEach(file -> {
                    try {
                        boolean result = importContentPack(contentPacksDir, file);
                        if (result) {
                            successCounter.incrementAndGet();
                        } else {
                            failedCounter.incrementAndGet();
                        }
                    } catch (final RuntimeException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
            } catch (final IOException e) {
                LOGGER.error("Unable to read content pack files from {}",
                        FileUtil.getCanonicalPath(contentPacksDir),
                        e);
            }
        }
        LOGGER.info("Content pack import counts - success: {}, failed: {}",
                successCounter.get(),
                failedCounter.get());

        LOGGER.info("ContentPackImport finished");
    }

    private boolean importContentPack(Path parentPath, Path contentPack) {
        LOGGER.info("Starting import of content pack {}", FileUtil.getCanonicalPath(contentPack));

        try {
            //It is possible to import a content pack (or packs) with missing dependencies
            //so the onus is on the person putting the file in the import directory to
            //ensure the packs they import are complete
            importExportService.importConfig(contentPack,
                    ImportSettings.auto(),
                    new ArrayList<>());
            moveFile(contentPack, contentPack.getParent().resolve(IMPORTED_DIR));

            LOGGER.info("Completed import of content pack {}", FileUtil.getCanonicalPath(contentPack));

        } catch (final RuntimeException e) {
            LOGGER.error("Error importing content pack {}", FileUtil.getCanonicalPath(contentPack), e);
            moveFile(contentPack, contentPack.getParent().resolve(FAILED_DIR));
            return false;
        }
        return true;
    }

    private void moveFile(Path contentPack, Path destDir) {
        Path destPath = destDir.resolve(contentPack.getFileName());
        try {
            //make sure the directory exists
            Files.createDirectories(destDir);
            Files.move(contentPack, destPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new UncheckedIOException(String.format("Error moving file from %s to %s",
                    FileUtil.getCanonicalPath(contentPack), FileUtil.getCanonicalPath(destPath)), e);
        }
    }
}
