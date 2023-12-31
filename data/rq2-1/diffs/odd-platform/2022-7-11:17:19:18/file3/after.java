package org.opendatadiscovery.oddplatform.notification;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Table;
import org.opendatadiscovery.oddplatform.model.Tables;
import org.opendatadiscovery.oddplatform.notification.NotificationsProperties.WalProperties;
import org.opendatadiscovery.oddplatform.notification.processor.PostgresWALMessageProcessor;
import org.opendatadiscovery.oddplatform.notification.wal.PostgresWALMessageDecoder;
import org.postgresql.PGConnection;
import org.postgresql.replication.PGReplicationStream;
import org.postgresql.replication.fluent.logical.ChainedLogicalStreamBuilder;

@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber extends Thread {
    private static final String PG_REPLICATION_OUTPUT_PLUGIN = "pgoutput";

    private final WalProperties walProperties;

    private final PGConnectionFactory connectionFactory;
    private final PostgresWALMessageDecoder messageDecoder;
    private final PostgresWALMessageProcessor messageProcessor;

    @Override
    public void run() {
        final Properties replicationSlotOptions = new Properties();
        replicationSlotOptions.putAll(Map.of(
            "proto_version", "1",
            "publication_names", walProperties.getPublicationName()
        ));

        while (true) {
            final Connection replicationConnection = connectionFactory.getConnection(true);

            try {
                replicationConnection.createStatement()
                    .execute("SELECT pg_advisory_lock(%d)".formatted(walProperties.getAdvisoryLockId()));

                log.debug("Acquired advisory lock on id = {}", walProperties.getAdvisoryLockId());

                final PGConnection pgReplicationConnection = replicationConnection.unwrap(PGConnection.class);

                registerReplicationSlot(replicationConnection, pgReplicationConnection);
                registerPublication(replicationConnection, Tables.ALERT);

                final ChainedLogicalStreamBuilder streamBuilder =
                    pgReplicationConnection.getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(walProperties.getReplicationSlotName())
                        .withSlotOptions(replicationSlotOptions);

                try (final PGReplicationStream stream = streamBuilder.start()) {
                    while (true) {
                        final ByteBuffer buffer = stream.readPending();

                        if (buffer == null) {
                            TimeUnit.MILLISECONDS.sleep(10L);
                            continue;
                        }

                        log.debug("processing LSN: {}", stream.getLastReceiveLSN());

                        messageDecoder.decode(buffer)
                            .ifPresent(messageProcessor::process);

                        stream.setAppliedLSN(stream.getLastReceiveLSN());
                        stream.setFlushedLSN(stream.getLastReceiveLSN());
                    }
                }
            } catch (final Exception e) {
                log.error("Error occurred while subscribing: {}", e);
            } finally {
                try {
                    replicationConnection.close();
                } catch (final SQLException e) {
                    log.error("Error while trying to close JDBC replication connection", e);
                }
            }

            log.debug("Released a lock, waiting 10 seconds for next iteration");
            try {
                TimeUnit.SECONDS.sleep(10L);
            } catch (final InterruptedException e) {
                log.error("Error while sleeping", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void registerReplicationSlot(final Connection connection,
                                         final PGConnection replicationConnection) throws SQLException {
        final String existsQuery = "SELECT EXISTS (SELECT slot_name FROM pg_replication_slots WHERE slot_name = ?)";

        final PreparedStatement statement = connection.prepareStatement(existsQuery);
        statement.setString(1, walProperties.getReplicationSlotName());

        try (final ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            if (!resultSet.getBoolean(1)) {
                log.debug("Creating replication slot with name {}", walProperties.getReplicationSlotName());
                replicationConnection.getReplicationAPI()
                    .createReplicationSlot()
                    .logical()
                    .withSlotName(walProperties.getReplicationSlotName())
                    .withOutputPlugin(PG_REPLICATION_OUTPUT_PLUGIN)
                    .make();
            }
        }

        log.debug("Replication slot {} registed", walProperties.getReplicationSlotName());
    }

    private void registerPublication(final Connection connection, final Table<?> targetTable) throws SQLException {
        if (targetTable == null) {
            throw new IllegalArgumentException("targetTable cannot be null");
        }

        final String existsQuery = "SELECT EXISTS (SELECT oid FROM pg_publication WHERE pubname = ?)";

        final PreparedStatement existsStatement = connection.prepareStatement(existsQuery);
        existsStatement.setString(1, walProperties.getPublicationName());

        try (final ResultSet resultSet = existsStatement.executeQuery()) {
            resultSet.next();
            if (!resultSet.getBoolean(1)) {
                // PostgreSQL tables are always in a schema, so we don't need to check targetTable.getSchema() for null
                final String tableName =
                    String.format("%s.%s", targetTable.getSchema().getName(), targetTable.getName());

                log.debug("Creating publication with name {} for table {}",
                    walProperties.getPublicationName(), tableName);

                connection.createStatement().execute(
                    "CREATE PUBLICATION %s FOR TABLE %s".formatted(walProperties.getPublicationName(), tableName));
            }
        }

        log.debug("Publication {} registered", walProperties.getPublicationName());
    }
}
