package stroom.pipeline.writer;

import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.StandardHeaderArguments;
import stroom.pipeline.destination.Destination;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.MetaDataHolder;
import stroom.util.cert.SSLConfig;
import stroom.util.cert.SSLUtil;
import stroom.util.io.ByteCountOutputStream;
import stroom.util.io.PathCreator;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ModelStringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Handler class that forwards the request to a URL.
 */
@ConfigurableElement(
        type = "HTTPAppender",
        category = Category.DESTINATION,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_DESTINATION,
                PipelineElementType.VISABILITY_STEPPING},
        icon = ElementIcons.STREAM)
public class HTTPAppender extends AbstractAppender {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(HTTPAppender.class);
    private static final Logger SEND_LOG = LoggerFactory.getLogger("send");

    private final MetaDataHolder metaDataHolder;
    private final PathCreator pathCreator;

    private String forwardUrl;
    private Long connectionTimeout;
    private Long readTimeout;
    private Long forwardChunkSize;
    private boolean useCompression = true;
    private Set<String> metaKeySet = getMetaKeySet("guid,feed,system,environment,remotehost,remoteaddress");

    private HttpURLConnection connection;
    private ZipOutputStream zipOutputStream;
    private ByteCountOutputStream byteCountOutputStream;
    private long startTimeMs;
    private long count;

    private boolean useJvmSslConfig = true;
    //    private final SSLConfig sslConfig = new SSLConfig();
    private final SSLConfig.Builder sslConfigBuilder = SSLConfig.builder();

    private String requestMethod = "POST";
    private String contentType = "application/json";

    private boolean httpHeadersIncludeStreamMetaData = true;
    private String httpHeadersUserDefinedHeader1;
    private String httpHeadersUserDefinedHeader2;
    private String httpHeadersUserDefinedHeader3;


    @Inject
    HTTPAppender(final ErrorReceiverProxy errorReceiverProxy,
                 final MetaDataHolder metaDataHolder,
                 final PathCreator pathCreator) {
        super(errorReceiverProxy);
        this.metaDataHolder = metaDataHolder;
        this.pathCreator = pathCreator;
    }

    @Override
    public Destination borrowDestination() throws IOException {
        nextEntry();
        return super.borrowDestination();
    }

    @Override
    public void returnDestination(final Destination destination) throws IOException {
        closeEntry();
        super.returnDestination(destination);
    }

    private void addAttributeIfHeaderDefined(final AttributeMap attributeMap, final String headerText) {
        if (headerText == null || headerText.length() < 3) {
            return;
        }
        if (!headerText.contains(":")) {
            throw new IllegalArgumentException("Additional Headers must be specified as 'Name: Value', but '"
                    + headerText + "' supplied.");
        }

        int delimiterPos = headerText.indexOf(':');
        attributeMap.put(headerText.substring(0, delimiterPos), headerText.substring(delimiterPos + 1));
    }

    @Override
    protected OutputStream createOutputStream() throws IOException {
        try {
            OutputStream outputStream;

            final AttributeMap sendHeader;
            if (httpHeadersIncludeStreamMetaData) {
                final AttributeMap attributeMap = metaDataHolder.getMetaData();


                startTimeMs = System.currentTimeMillis();
                attributeMap.computeIfAbsent(StandardHeaderArguments.GUID, k -> UUID.randomUUID().toString());

                sendHeader = AttributeMapUtil.cloneAllowable(attributeMap);
            } else {
                sendHeader = new AttributeMap();
            }

            addAttributeIfHeaderDefined(sendHeader, httpHeadersUserDefinedHeader1);
            addAttributeIfHeaderDefined(sendHeader, httpHeadersUserDefinedHeader2);
            addAttributeIfHeaderDefined(sendHeader, httpHeadersUserDefinedHeader3);

            LOGGER.info(() -> "createOutputStream() - " + forwardUrl + " Sending request " + sendHeader);

            URL url = new URL(forwardUrl);
            connection = (HttpURLConnection) url.openConnection();

            if (connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
                final SSLConfig sslConfig = sslConfigBuilder.build();
                if (!useJvmSslConfig) {
                    LOGGER.info(() -> "Configuring SSLSocketFactory for destination " + forwardUrl);
                    final SSLSocketFactory sslSocketFactory = SSLUtil.createSslSocketFactory(
                            sslConfig, pathCreator);
                    SSLUtil.applySSLConfiguration(connection, sslSocketFactory, sslConfig);
                } else if (!sslConfig.isHostnameVerificationEnabled()) {
                    SSLUtil.disableHostnameVerification(httpsURLConnection);
                }
            }

            if (connectionTimeout != null) {
                connection.setConnectTimeout(connectionTimeout.intValue());
            }
            if (readTimeout != null) {
                connection.setReadTimeout(readTimeout.intValue());
            } else {
                connection.setReadTimeout(0);
            }

            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            if (useCompression) {
                connection.addRequestProperty(StandardHeaderArguments.COMPRESSION,
                        StandardHeaderArguments.COMPRESSION_ZIP);
            }


            for (Entry<String, String> entry : sendHeader.entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            if (forwardChunkSize != null) {
                LOGGER.debug(() -> "handleHeader() - setting ChunkedStreamingMode = " + forwardChunkSize);
                connection.setChunkedStreamingMode(forwardChunkSize.intValue());
            }
            connection.connect();

            if (useCompression) {
                zipOutputStream = new ZipOutputStream(connection.getOutputStream());
                outputStream = zipOutputStream;
                nextEntry();
            } else {
                outputStream = connection.getOutputStream();
            }

            byteCountOutputStream = new ByteCountOutputStream(outputStream) {
                @Override
                public void close() throws IOException {
                    super.close();
                    closeConnection();
                }
            };

            return byteCountOutputStream;

        } catch (final IOException | RuntimeException e) {
            LOGGER.debug(e::getMessage, e);
            closeConnection();
            throw e;
        }
    }

    @Override
    long getCurrentOutputSize() {
        if (byteCountOutputStream == null) {
            return 0;
        }
        return byteCountOutputStream.getCount();
    }

    private void nextEntry() throws IOException {
        if (zipOutputStream != null) {
            count++;
            final AttributeMap attributeMap = metaDataHolder.getMetaData();
            String fileName = attributeMap.get("fileName");
            if (fileName == null) {
                fileName = ModelStringUtil.zeroPad(3, String.valueOf(count)) + ".dat";
            }

            zipOutputStream.putNextEntry(new ZipEntry(fileName));
        }
    }

    private void closeEntry() throws IOException {
        if (zipOutputStream != null) {
            zipOutputStream.closeEntry();
        }
    }

    private void closeConnection() throws IOException {
        int responseCode = -1;

        if (connection != null) {
            LOGGER.debug(() -> "closeConnection() - header fields " + connection.getHeaderFields());
            try {
                responseCode = StroomStreamException.checkConnectionResponse(connection);
            } catch (final RuntimeException e) {
                LOGGER.debug(e::getMessage, e);
                throw e;
            } finally {
                long bytes = 0;
                if (byteCountOutputStream != null) {
                    bytes = byteCountOutputStream.getCount();
                }

                final long duration = System.currentTimeMillis() - startTimeMs;
                final AttributeMap attributeMap = metaDataHolder.getMetaData();
                log(SEND_LOG, attributeMap, "SEND", forwardUrl, responseCode, bytes, duration);

                connection.disconnect();
                connection = null;
            }
        }

    }

    public void log(final Logger logger,
                    final AttributeMap attributeMap,
                    final String type,
                    final String url,
                    final int responseCode,
                    final long bytes,
                    final long duration) {
        if (logger.isInfoEnabled() && metaKeySet.size() > 0) {
            final Map<String, String> filteredMap = attributeMap.entrySet().stream()
                    .filter(entry -> metaKeySet.contains(entry.getKey().toLowerCase()))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            final String kvPairs = CSVFormatter.format(filteredMap);
            final String message = CSVFormatter.escape(type) +
                    "," +
                    CSVFormatter.escape(url) +
                    "," +
                    responseCode +
                    "," +
                    bytes +
                    "," +
                    duration +
                    "," +
                    kvPairs;
            logger.info(message);
        }
    }

    private Set<String> getMetaKeySet(final String csv) {
        if (csv == null || csv.length() == 0) {
            return Collections.emptySet();
        }

        return Arrays.stream(csv.toLowerCase().split(",")).collect(Collectors.toSet());
    }

    @Override
    @PipelineProperty(description = "When the current output exceeds this size it will be closed and a " +
            "new one created.",
            displayPriority = 2)
    public void setRollSize(final String size) {
        super.setRollSize(size);
    }

    @Override
    @PipelineProperty(description = "Choose if you want to split aggregated streams into separate output.",
            defaultValue = "false",
            displayPriority = 3)
    public void setSplitAggregatedStreams(final boolean splitAggregatedStreams) {
        super.setSplitAggregatedStreams(splitAggregatedStreams);
    }

    @Override
    @PipelineProperty(description = "Choose if you want to split individual records into separate output.",
            defaultValue = "false",
            displayPriority = 4)
    public void setSplitRecords(final boolean splitRecords) {
        super.setSplitRecords(splitRecords);
    }

    @PipelineProperty(description = "The URL to send data to",
            displayPriority = 5)
    public void setForwardUrl(final String forwardUrl) {
        this.forwardUrl = forwardUrl;
    }

    @PipelineProperty(description = "How long to wait before we abort sending data due to connection timeout",
            displayPriority = 6)
    public void setConnectionTimeout(final String string) {
        connectionTimeout = null;
        if (string != null && !string.isEmpty()) {
            connectionTimeout = ModelStringUtil.parseDurationString(string);
        }
    }

    @PipelineProperty(description = "How long to wait for data to be available before closing the connection",
            displayPriority = 7)
    public void setReadTimeout(final String string) {
        readTimeout = null;
        if (string != null && !string.isEmpty()) {
            readTimeout = ModelStringUtil.parseDurationString(string);
        }
    }

    @PipelineProperty(description = "Should data be sent in chunks and if so how big should the chunks be",
            displayPriority = 8)
    public void setForwardChunkSize(final String string) {
        this.forwardChunkSize = ModelStringUtil.parseIECByteSizeString(string);
    }

    @PipelineProperty(description = "Should data be compressed when sending", defaultValue = "true",
            displayPriority = 9)
    public void setUseCompression(final boolean useCompression) {
        this.useCompression = useCompression;
    }

    @PipelineProperty(
            description = "Which meta data values will be logged in the send log",
            defaultValue = "guid,feed,system,environment,remotehost,remoteaddress",
            displayPriority = 10)
    public void setLogMetaKeys(final String string) {
        metaKeySet = getMetaKeySet(string);
    }

    @PipelineProperty(description = "Use JVM SSL config",
            defaultValue = "true",
            displayPriority = 11)
    public void setUseJvmSslConfig(final boolean useJvmSslConfig) {
        this.useJvmSslConfig = useJvmSslConfig;
    }

    @PipelineProperty(description = "The key store file path on the server",
            displayPriority = 12)
    public void setKeyStorePath(final String keyStorePath) {
        sslConfigBuilder.withKeyStorePath(keyStorePath);
    }

    @PipelineProperty(description = "The key store type",
            defaultValue = "JKS",
            displayPriority = 13)
    public void setKeyStoreType(final String keyStoreType) {
        sslConfigBuilder.withKeyStoreType(keyStoreType);
    }

    @PipelineProperty(description = "The key store password",
            displayPriority = 14)
    public void setKeyStorePassword(final String keyStorePassword) {
        sslConfigBuilder.withKeyStorePassword(keyStorePassword);
    }

    @PipelineProperty(description = "The trust store file path on the server",
            displayPriority = 15)
    public void setTrustStorePath(final String trustStorePath) {
        sslConfigBuilder.withTrustStorePath(trustStorePath);
    }

    @PipelineProperty(description = "The trust store type",
            defaultValue = "JKS",
            displayPriority = 16)
    public void setTrustStoreType(final String trustStoreType) {
        sslConfigBuilder.withTrustStoreType(trustStoreType);
    }

    @PipelineProperty(description = "The trust store password",
            displayPriority = 17)
    public void setTrustStorePassword(final String trustStorePassword) {
        sslConfigBuilder.withTrustStorePassword(trustStorePassword);
    }

    @PipelineProperty(description = "Verify host names",
            defaultValue = "true",
            displayPriority = 18)
    public void setHostnameVerificationEnabled(final boolean hostnameVerificationEnabled) {
        sslConfigBuilder.withHostnameVerificationEnabled(hostnameVerificationEnabled);
    }

    @PipelineProperty(description = "The SSL protocol to use",
            defaultValue = "TLSv1.2",
            displayPriority = 19)
    public void setSslProtocol(final String sslProtocol) {
        sslConfigBuilder.withSslProtocol(sslProtocol);
    }

    @PipelineProperty(description = "The request method, e.g. POST",
            defaultValue = "POST",
            displayPriority = 20)
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    @PipelineProperty(description = "The content type",
            defaultValue = "application/json",
            displayPriority = 21)
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @PipelineProperty(description = "Provide stream metadata as HTTP headers",
            defaultValue = "true",
            displayPriority = 22)
    public void setHttpHeadersIncludeStreamMetaData(final boolean newValue) {
        this.httpHeadersIncludeStreamMetaData = newValue;
    }

    @PipelineProperty(description = "Additional HTTP Header 1, format is 'HeaderName: HeaderValue'",
            displayPriority = 23)
    public void setHttpHeadersUserDefinedHeader1(final String headerText) {
        this.httpHeadersUserDefinedHeader1 = headerText;
    }

    @PipelineProperty(description = "Additional HTTP Header 2, format is 'HeaderName: HeaderValue'",
            displayPriority = 24)
    public void setHttpHeadersUserDefinedHeader2(final String headerText) {
        this.httpHeadersUserDefinedHeader2 = headerText;
    }

    @PipelineProperty(description = "Additional HTTP Header 3, format is 'HeaderName: HeaderValue'",
            displayPriority = 25)
    public void setHttpHeadersUserDefinedHeader3(final String headerText) {
        this.httpHeadersUserDefinedHeader3 = headerText;
    }
}
