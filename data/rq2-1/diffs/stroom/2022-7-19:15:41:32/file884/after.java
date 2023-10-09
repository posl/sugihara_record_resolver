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
 */

package stroom.search.solr.indexing;

import stroom.docref.DocRef;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.ErrorStatistics;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.factory.PipelinePropertyDocRef;
import stroom.pipeline.filter.AbstractXMLFilter;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.search.solr.CachedSolrIndex;
import stroom.search.solr.SolrIndexCache;
import stroom.search.solr.SolrIndexClientCache;
import stroom.search.solr.shared.SolrIndexDoc;
import stroom.search.solr.shared.SolrIndexField;
import stroom.search.solr.shared.SolrIndexFieldType;
import stroom.util.date.DateUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Severity;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.inject.Inject;

/**
 * The Solr index filter... takes the index XML and sends documents to Solr for indexing.
 */
@ConfigurableElement(
        type = "SolrIndexingFilter",
        description = """
                Delivers source data to the specified index in an external Solr instance/cluster.
                """,
        category = Category.FILTER,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_HAS_TARGETS,
                PipelineElementType.VISABILITY_SIMPLE},
        icon = ElementIcons.SOLR)
class SolrIndexingFilter extends AbstractXMLFilter {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SolrIndexingFilter.class);

    private static final String RECORD = "record";
    private static final String DATA = "data";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private final LocationFactoryProxy locationFactory;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final SolrIndexCache solrIndexCache;
    private final SolrIndexClientCache solrIndexClientCache;
    private Map<String, SolrIndexField> fieldsMap;
    private CachedSolrIndex indexConfig;
    private DocRef indexRef;
    private Collection<SolrInputDocument> currentDocuments = new ArrayList<>();
    private SolrInputDocument document = new SolrInputDocument();

    private int batchSize = 1000;
    private int commitWithinMs = -1;
    private boolean softCommit = true;
    private int fieldsIndexed;
    private long docsIndexed;

    private Locator locator;

    @Inject
    SolrIndexingFilter(final LocationFactoryProxy locationFactory,
                       final ErrorReceiverProxy errorReceiverProxy,
                       final SolrIndexCache solrIndexCache,
                       final SolrIndexClientCache solrIndexClientCache) {
        this.locationFactory = locationFactory;
        this.errorReceiverProxy = errorReceiverProxy;
        this.solrIndexCache = solrIndexCache;
        this.solrIndexClientCache = solrIndexClientCache;
    }

    /**
     * Initialise
     */
    @Override
    public void startProcessing() {
        try {
            if (indexRef == null) {
                log(Severity.FATAL_ERROR, "Index has not been set", null);
                throw new LoggedException("Index has not been set");
            }

            // Get the index and index fields from the cache.
            indexConfig = solrIndexCache.get(indexRef);
            if (indexConfig == null) {
                log(Severity.FATAL_ERROR, "Unable to load index", null);
                throw new LoggedException("Unable to load index");
            }

            fieldsMap = indexConfig.getFieldsMap();

            // Create a solr client.
            solrIndexClientCache.context(indexConfig.getIndex().getSolrConnectionConfig(), solrClient -> {
                try {
                    final SolrPingResponse response = solrClient.ping();
                    LOGGER.debug(() ->
                            "Ping to Solr with status " + response.getStatus() + " in " +
                                    response.getElapsedTime() + "ms");
                } catch (final IOException | SolrServerException e) {
                    log(Severity.FATAL_ERROR, e.getMessage(), e);
                    // Terminate processing as this is a fatal error.
                    throw new LoggedException(e.getMessage(), e);
                }
            });

        } finally {
            super.startProcessing();
        }
    }

    @Override
    public void endProcessing() {
        try {
            // Send last docs.
            addDocuments(currentDocuments, true);
            currentDocuments = null;
        } finally {
            super.endProcessing();
        }
    }

    /**
     * Sets the locator to use when reporting errors.
     *
     * @param locator The locator to use.
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
        if (DATA.equals(localName) && document != null) {
            String name = atts.getValue(NAME);
            String value = atts.getValue(VALUE);
            if (name != null && value != null) {
                name = name.trim();
                value = value.trim();

                if (name.length() > 0 && value.length() > 0) {
                    // See if we can get this field.
                    final SolrIndexField indexField = fieldsMap.get(name);
                    if (indexField != null) {
                        // Index the current content if we are to store or index
                        // this field.
                        if (indexField.isIndexed() || indexField.isStored()) {
                            processIndexContent(indexField, value);
                        }
                    } else {
                        log(Severity.WARNING, "Attempt to index unknown field: " + name, null);
                    }
                }
            }
        } else if (RECORD.equals(localName)) {
            // Create a document to store fields in.
            document = new SolrInputDocument();
        }

        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (RECORD.equals(localName)) {
            processDocument();
            document = null;

            // Reset the count of how many fields we have indexed for the
            // current event.
            fieldsIndexed = 0;

            if (errorReceiverProxy.getErrorReceiver() != null
                    && errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
            }
        }

        super.endElement(uri, localName, qName);
    }

    private void processDocument() {
        // Write the document if we have dropped out of the record element and
        // have indexed some fields.
        if (fieldsIndexed > 0) {
            docsIndexed++;
            currentDocuments.add(document);
            document = new SolrInputDocument();

            if (currentDocuments.size() > batchSize) {
                addDocuments(currentDocuments, false);
                currentDocuments = new ArrayList<>();
            }
        }
    }

    private void addDocuments(final Collection<SolrInputDocument> documents, final boolean commit) {
        if (docsIndexed > 0) {
            final String collection = indexConfig.getIndex().getCollection();
            solrIndexClientCache.context(indexConfig.getIndex().getSolrConnectionConfig(), solrClient -> {
                try {
                    if (documents.size() > 0) {
                        solrClient.add(collection, documents, commitWithinMs);
                    }

                    if (commit) {
                        solrClient.commit(collection);
                    } else if (softCommit) {
                        solrClient.commit(collection, false, false, true);
                    }
                } catch (final RuntimeException | SolrServerException | IOException e) {
                    log(Severity.FATAL_ERROR, e.getMessage(), e);
                    // Terminate processing as this is a fatal error.
                    throw new LoggedException(e.getMessage(), e);
                }
            });
        }
    }

    private void processIndexContent(final SolrIndexField indexField, final String value) {
        try {
            Object val = null;

            if (indexField.getFieldUse().isNumeric()) {
                val = Long.parseLong(value);

            } else if (SolrIndexFieldType.DATE_FIELD.equals(indexField.getFieldUse())) {
                try {
                    val = DateUtil.parseUnknownString(value);
                } catch (final Exception e) {
                    LOGGER.trace(e::getMessage, e);
                }
            } else {
                val = value;
            }

            // Add the current field to the document if it is not null.
            if (val != null) {
                // Output some debug.
                LOGGER.debug(() -> "processIndexContent() - Adding to index indexName=" +
                        indexRef.getName() +
                        " name=" +
                        indexField.getFieldName() +
                        " value=" +
                        value);

                fieldsIndexed++;
                document.addField(indexField.getFieldName(), val);
            }
        } catch (final RuntimeException e) {
            log(Severity.ERROR, e.getMessage(), e);
        }
    }

    @PipelineProperty(
            description = "The index to send records to.",
            displayPriority = 1)
    @PipelinePropertyDocRef(types = SolrIndexDoc.DOCUMENT_TYPE)
    public void setIndex(final DocRef indexRef) {
        this.indexRef = indexRef;
    }

    @PipelineProperty(
            description = "How many documents to send to the index in a single post.",
            defaultValue = "1000",
            displayPriority = 2)
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    @PipelineProperty(
            description = "Commit indexed documents within the specified number of milliseconds.",
            defaultValue = "-1",
            displayPriority = 3)
    public void setCommitWithinMs(final int commitWithinMs) {
        if (commitWithinMs < 0) {
            this.commitWithinMs = -1;
        } else {
            this.commitWithinMs = commitWithinMs;
        }
    }

    @PipelineProperty(
            description = "Perform a soft commit after every batch so that docs are available for searching " +
                    "immediately (if using NRT replicas).",
            defaultValue = "true",
            displayPriority = 4)
    public void setSoftCommit(final boolean softCommit) {
        this.softCommit = softCommit;
    }

    private void log(final Severity severity, final String message, final Exception e) {
        errorReceiverProxy.log(severity, locationFactory.create(locator), getElementId(), message, e);
    }
}
