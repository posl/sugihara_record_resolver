/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 04/feb/2015
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.pdf;

import static org.pdfsam.pdf.PdfDescriptorLoadingStatus.ENCRYPTED;
import static org.pdfsam.pdf.PdfDescriptorLoadingStatus.LOADED;
import static org.pdfsam.pdf.PdfDescriptorLoadingStatus.LOADED_WITH_USER_PWD_DECRYPTION;
import static org.pdfsam.pdf.PdfDescriptorLoadingStatus.LOADING;
import static org.pdfsam.pdf.PdfDescriptorLoadingStatus.WITH_ERRORS;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.module.RequiredPdfData;
import org.sejda.io.BufferedSeekableSource;
import org.sejda.io.FileChannelSeekableSource;
import org.sejda.sambox.input.PDFParser;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

/**
 * @author Andrea Vacondio
 *
 */
class SAMBoxPdfLoadService implements PdfLoadService {
    private static final Logger LOG = LoggerFactory.getLogger(SAMBoxPdfLoadService.class);
    private final Map<RequiredPdfData, BiConsumer<PDDocument, PdfDocumentDescriptor>> consumers = new HashMap<>();

    private static final BiConsumer<PDDocument, PdfDocumentDescriptor> STARTER = (r, descriptor) -> {
        // NO OP
    };

    private static final BiConsumer<PDDocument, PdfDocumentDescriptor> FINISHER = (r, descriptor) -> {
        if (descriptor.hasPassword()) {
            fxMoveStatusTo(descriptor, LOADED_WITH_USER_PWD_DECRYPTION);
        } else {
            fxMoveStatusTo(descriptor, LOADED);
        }
    };

    public SAMBoxPdfLoadService(List<PdfLoader<PDDocument>> loaders) {
        loaders.forEach(l -> consumers.put(l.key(), l));
    }

    @Override
    public void load(Collection<? extends PdfDocumentDescriptor> toLoad, RequiredPdfData... requires) {
        LOG.debug(DefaultI18nContext.getInstance().i18n("Loading pdf documents"));
        BiConsumer<PDDocument, PdfDocumentDescriptor> consumer = Arrays.stream(requires).map(consumers::get)
                .reduce(STARTER, (r, d) -> r.andThen(d)).andThen(FINISHER);

        for (PdfDocumentDescriptor current : toLoad) {
            if (current.hasReferences()) {
                LOG.trace("Loading {}", current.getFileName());
                fxMoveStatusTo(current, LOADING);
                try (PDDocument document = PDFParser.parse(
                        new BufferedSeekableSource(new FileChannelSeekableSource(current.getFile())),
                        current.getPassword())) {
                    consumer.accept(document, current);
                } catch (InvalidPasswordException twpe) {
                    fxMoveStatusTo(current, ENCRYPTED);
                    LOG.warn("User password required for '{}'", current.getFileName(), twpe);
                } catch (Exception e) {
                    LOG.error("An error occured loading the document '{}'", current.getFileName(), e);
                    fxMoveStatusTo(current, WITH_ERRORS);
                }
                LOG.info("{} loaded", current.getFileName());
            } else {
                LOG.trace("Skipping invalidated document {}", current.getFileName());
            }
        }
        LOG.debug(DefaultI18nContext.getInstance().i18n("Documents loaded"));
    }

    private static void fxMoveStatusTo(PdfDocumentDescriptor descriptor, PdfDescriptorLoadingStatus status) {
        Platform.runLater(() -> descriptor.moveStatusTo(status));
    }
}
