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

package stroom.pipeline.xsltfunctions;

import stroom.docref.DocRef;
import stroom.pipeline.refdata.LookupIdentifier;
import stroom.pipeline.refdata.ReferenceData;
import stroom.pipeline.refdata.ReferenceDataResult;
import stroom.pipeline.refdata.store.GenericRefDataValueProxyConsumer;
import stroom.pipeline.refdata.store.RefDataValueProxy;
import stroom.pipeline.refdata.store.RefDataValueProxyConsumerFactory;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.shared.data.PipelineReference;
import stroom.pipeline.state.MetaHolder;
import stroom.util.NullSafe;
import stroom.util.date.DateUtil;
import stroom.util.logging.LogUtil;
import stroom.util.shared.Severity;
import stroom.util.shared.StoredError;
import stroom.util.shared.StringUtil;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.EmptyAtomicSequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


abstract class AbstractLookup extends StroomExtensionFunctionCall {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLookup.class);

    private final ReferenceData referenceData;
    private final MetaHolder metaHolder;
    private final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory;

    private long defaultMs = -1;

    AbstractLookup(final ReferenceData referenceData,
                   final MetaHolder metaHolder,
                   final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory) {
        this.referenceData = referenceData;
        this.metaHolder = metaHolder;
        this.consumerFactoryFactory = consumerFactoryFactory;
    }

    RefDataValueProxyConsumerFactory.Factory getRefDataValueProxyConsumerFactoryFactory() {
        return consumerFactoryFactory;
    }

//    OffHeapRefDataValueProxyConsumer.Factory getConsumerFactory() {
//        return consumerFactory;
//    }

    @Override
    protected Sequence call(final String functionName, final XPathContext context, final Sequence[] arguments) {
        LOGGER.trace("call({}, {}, {}", functionName, context, arguments);
        Sequence result = EmptyAtomicSequence.getInstance();

        try {
            // Setup the defaultMs to be the received time of the stream.
            if (defaultMs == -1) {
                if (metaHolder.getMeta() != null) {
                    defaultMs = metaHolder.getMeta().getCreateMs();
                } else {
                    defaultMs = System.currentTimeMillis();
                }
            }

            // Get the map and key names.
            final String map = getSafeString(functionName, context, arguments, 0);
            final String key = getSafeString(functionName, context, arguments, 1);

            // Find out if we are going to ignore warnings.
            boolean ignoreWarnings = false;
            if (arguments.length > 3) {
                final Boolean ignore = getSafeBoolean(functionName, context, arguments, 3);
                if (ignore != null) {
                    ignoreWarnings = ignore;
                }
            }

            // Find out if we are going to trace the lookup.
            boolean traceLookup = false;
            if (arguments.length > 4) {
                final Boolean trace = getSafeBoolean(functionName, context, arguments, 4);
                if (trace != null) {
                    traceLookup = trace;
                }
            }

            // Make sure we can get the date ok.
            long ms = defaultMs;
            if (arguments.length > 2) {
                final String time = getSafeString(functionName, context, arguments, 2);
                try {
                    ms = DateUtil.parseNormalDateTimeString(time);
                } catch (final RuntimeException e) {
                    if (!ignoreWarnings) {
                        if (time == null) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Lookup failed to parse empty date");
                            outputWarning(context, sb, null);
                        } else {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Lookup failed to parse date: ");
                            sb.append(time);
                            outputWarning(context, sb, e);
                        }
                    }

                    return result;
                }
            }

            // Create a lookup identifier if we are going to output debug.
            final LookupIdentifier lookupIdentifier;
            try {
                lookupIdentifier = new LookupIdentifier(map, key, ms);

                // If we have got the date then continue to do the lookup.
                try {
                    LOGGER.debug("doLookup for {}", lookupIdentifier);
                    result = doLookup(context, ignoreWarnings, traceLookup, lookupIdentifier);
                } catch (final RuntimeException e) {
                    createLookupFailError(context, lookupIdentifier, e);
                }
            } catch (RuntimeException e) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Identifier must have a map and a key (map: ");
                sb.append(map);
                sb.append(", key: ");
                sb.append(key);
                sb.append(", lookup time: ");
                sb.append(ms);
                log(context, Severity.ERROR, e.getMessage(), e);
            }

        } catch (final XPathException | RuntimeException e) {
            log(context, Severity.ERROR, e.getMessage(), e);
        }

        return result;
    }

    abstract Sequence doLookup(final XPathContext context,
                               final boolean ignoreWarnings,
                               final boolean trace,
                               final LookupIdentifier lookupIdentifier)
            throws XPathException;


    ReferenceDataResult getReferenceData(final LookupIdentifier lookupIdentifier,
                                         final boolean isTraceEnabled,
                                         final boolean isIgnoreWarnings) {
        LOGGER.trace("getReferenceData({})", lookupIdentifier);
        final ReferenceDataResult result = new ReferenceDataResult(
                lookupIdentifier, isTraceEnabled, isIgnoreWarnings);

        result.logLazyTemplate(
                Severity.INFO,
                "Lookup - " +
                        "key: '{}', map: '{}', lookup time: {} (primary map: '{}', secondary map: '{}', " +
                        "nested lookup: {})",
                () -> Arrays.asList(lookupIdentifier.getKey(),
                        lookupIdentifier.getMap(),
                        Instant.ofEpochMilli(lookupIdentifier.getEventTime()),
                        lookupIdentifier.getPrimaryMapName(),
                        Objects.requireNonNullElse(lookupIdentifier.getSecondaryMapName(), ""),
                        lookupIdentifier.isMapNested()));

        final List<PipelineReference> pipelineReferences = getPipelineReferences();
        if (pipelineReferences == null || pipelineReferences.size() == 0) {
            result.logSimpleTemplate(
                    Severity.ERROR,
                    "No pipeline references have been added to this XSLT step to perform a lookup");
        } else {
            referenceData.ensureReferenceDataAvailability(pipelineReferences, lookupIdentifier, result);
        }
        return result;
    }

    private void createLookupFailError(final XPathContext context,
                                       final LookupIdentifier lookupIdentifier,
                                       final Throwable e) {
        // Create the message.
        final StringBuilder sb = new StringBuilder();
        sb.append("Lookup errored ");
        lookupIdentifier.append(sb);

        outputError(context, sb, e);
    }

    private void createLookupFailWarning(final XPathContext context,
                                         final LookupIdentifier lookupIdentifier,
                                         final Throwable e) {
        // Create the message.
        final StringBuilder sb = new StringBuilder();
        sb.append("Lookup failed ");
        lookupIdentifier.append(sb);

        outputWarning(context, sb, e);
    }

    void outputInfo(final Severity severity,
                    final String msg,
                    final LookupIdentifier lookupIdentifier,
                    final boolean trace,
                    final boolean ignoreWarnings,
                    final ReferenceDataResult result,
                    final XPathContext context) {

        final StringBuilder sb = new StringBuilder();
        sb.append(msg);
        if (!msg.endsWith(" ")) {
            sb.append(" ");
        }
        lookupIdentifier.append(sb);

        result.getMessages()
                .stream()
                .filter(lazyMessage ->
                        trace ||
                                (!ignoreWarnings && lazyMessage.getSeverity().greaterThanOrEqual(Severity.WARNING)))
                .forEach(lazyMessage -> {
                    sb.append("\n");
                    sb.append(StoredError.MESSAGE_CAUSE_DELIMITER);
                    sb.append(lazyMessage.getSeverity().getDisplayValue());
                    sb.append(": ");
                    sb.append(lazyMessage.getMessage());
                });

        final String message = sb.toString();
        LOGGER.debug(message);
        log(context, severity, message, null);
    }

    String getEffectiveStreamIds(final ReferenceDataResult result) {
        Objects.requireNonNull(result);
        return result.getEffectiveStreams()
                .stream()
                .map(RefStreamDefinition::getStreamId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    String getQualifyingStreamIds(final ReferenceDataResult result) {
        Objects.requireNonNull(result);
        return result.getQualifyingStreams()
                .stream()
                .map(RefStreamDefinition::getStreamId)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    void logMapLocations(final ReferenceDataResult result,
                         final RefDataValueProxy refDataValueProxy) {
        result.logLazyTemplate(Severity.INFO,
                "Executing lookup of key: '{}' in map '{}' in {} out of {} effective stream{}: [{}]",
                () -> {
                    final String streamsStr = getQualifyingStreamIds(result);
                    final int qualifyingStreamCount = result.getQualifyingStreams().size();
                    final int effectiveStreamCount = result.getEffectiveStreams().size();
                    final String pluralSuffix = StringUtil.pluralSuffix(effectiveStreamCount);

                    return Arrays.asList(
                            refDataValueProxy.getKey(),
                            refDataValueProxy.getMapName(),
                            qualifyingStreamCount,
                            effectiveStreamCount,
                            pluralSuffix,
                            streamsStr);
                });
    }

    void logFailureReason(final ReferenceDataResult result,
                          final XPathContext context,
                          final boolean ignoreWarnings,
                          final boolean trace) {

        if (!ignoreWarnings && result.getEffectiveStreams().isEmpty()) {
            // No effective streams were found to lookup from
            final List<PipelineReference> pipelineReferences = NullSafe.list(getPipelineReferences());

            if (pipelineReferences.size() > 1) {
                final String feeds = pipelineReferences.stream()
                        .map(pipeRef -> NullSafe.get(pipeRef, PipelineReference::getFeed, DocRef::getName))
                        .filter(Objects::nonNull)
                        .map(name -> "'" + name + "'")
                        .collect(Collectors.joining(", "));

                outputInfo(
                        Severity.WARNING,
                        LogUtil.message(
                                "No effective streams found in any of the reference loaders (feeds: [{}]). " +
                                        "Do reference data streams exist for the lookup time?",
                                feeds),
                        result.getCurrentLookupIdentifier(),
                        trace,
                        ignoreWarnings,
                        result,
                        context);
            }
        } else if (!ignoreWarnings && result.getQualifyingStreams().isEmpty()) {
            // None of the effective streams contains the map
            outputInfo(
                    Severity.WARNING,
                    "Map not found in effective streams [" + getEffectiveStreamIds(result) + "] ",
                    result.getCurrentLookupIdentifier(),
                    trace,
                    ignoreWarnings,
                    result,
                    context);
        }
    }

    void logLookupValue(final boolean wasValueFound,
                        final ReferenceDataResult result,
                        final XPathContext context,
                        final boolean ignoreWarnings,
                        final boolean trace) {

        if (wasValueFound && trace) {
            outputInfo(
                    Severity.INFO,
                    "Success ",
                    result.getCurrentLookupIdentifier(),
                    trace,
                    ignoreWarnings,
                    result,
                    context);
        } else if (!wasValueFound && !ignoreWarnings) {
            outputInfo(
                    Severity.WARNING,
                    "Key not found ",
                    result.getCurrentLookupIdentifier(),
                    trace,
                    ignoreWarnings,
                    result,
                    context);
        }
    }

// --------------------------------------------------------------------------------


    static class SequenceMaker {

        private final XPathContext context;
        private final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory;
        private Builder builder;
        private RefDataValueProxyConsumerFactory consumerFactory;
        private GenericRefDataValueProxyConsumer consumer;

        SequenceMaker(final XPathContext context,
                      final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory) {
            this.context = context;
            this.consumerFactoryFactory = consumerFactoryFactory;
        }

        void open() throws XPathException {
            LOGGER.trace("open()");
            // Make sure we have made a consumer.
            ensureConsumer();
            consumer.startDocument();
        }

        void close() throws XPathException {
            LOGGER.trace("close()");
            consumer.endDocument();
        }

        boolean consume(final RefDataValueProxy refDataValueProxy) throws XPathException {
            return consumer.consume(refDataValueProxy);
        }

        private void ensureConsumer() {
            LOGGER.trace("ensureConsumer()");
            if (consumer == null) {
                // We have some reference data so build a tiny tree.
                final Configuration configuration = context.getConfiguration();

                final PipelineConfiguration pipelineConfiguration = configuration.makePipelineConfiguration();

                builder = new TinyBuilder(pipelineConfiguration);

                // At this point we don't know if we are dealing with heap object values or off-heap bytebuffer values.
                // We also don't know if the value is a string or a fastinfoset.
                consumer = new GenericRefDataValueProxyConsumer(
                        builder,
                        pipelineConfiguration,
                        consumerFactoryFactory.create(builder, pipelineConfiguration));

//                consumer = consumerFactory.create(builder, pipelineConfiguration);
            }
        }

        Sequence toSequence() {
            LOGGER.trace("toSequence()");
            if (builder == null) {
                return EmptyAtomicSequence.getInstance();
            }

            final Sequence sequence = builder.getCurrentRoot();

            // Reset the builder, detaching it from the constructed document.
            builder.reset();

            return sequence;
        }
    }
}
