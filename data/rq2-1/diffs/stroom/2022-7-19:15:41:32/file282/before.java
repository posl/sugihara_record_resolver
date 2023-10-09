package stroom.db.util;

import stroom.config.common.ConnectionConfig;
import stroom.util.logging.LogUtil;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.DataSource;

public class DbUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbUtil.class);
    private static final long MAX_SLEEP_TIME_MS = 30_000;
    private static final int ACCESS_DENIED_BAD_UNAME_OR_PWORD = 1045;
    private static final int ACCESS_DENIED_BAD_DATABASE = 1044;

    private DbUtil() {
    }

    public static void validate(final ConnectionConfig connectionConfig) {
        Preconditions.checkNotNull(connectionConfig.getClassName(),
                "The JDBC driver class has not been supplied. %s", connectionConfig);
        Preconditions.checkNotNull(connectionConfig.getUrl(),
                "The JDBC URL has not been supplied. %s", connectionConfig);
        Preconditions.checkNotNull(connectionConfig.getUser(),
                "The JDBC username has not been supplied. %s", connectionConfig);
        Preconditions.checkNotNull(connectionConfig.getPassword(),
                "The JDBC password has not been supplied. %s", connectionConfig);

        try {
            Class.forName(connectionConfig.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(LogUtil.message(
                    "Invalid JDBC driver class name {}", connectionConfig.getClassName()), e);
        }
    }

    public static Connection getSingleConnection(final ConnectionConfig connectionConfig) throws SQLException {
        return DriverManager.getConnection(
                connectionConfig.getUrl(),
                connectionConfig.getUser(),
                connectionConfig.getPassword());
    }

    /**
     * Attempts to connect to the database using the passed connection details. If it fails
     * it will log a warning, and keep retrying. The retry interval will steadily increase.
     * <p>
     * If the connection could not be established and the reason for the failure makes a
     * retry pointless, e.g. invalid password, then an exception will be thrown.
     */
    public static void waitForConnection(ConnectionConfig connectionConfig) {
        final String jdbcUrl = connectionConfig.getUrl();
        final String username = connectionConfig.getUser();
        LOGGER.info("Ensuring database connection to [{}] with username [{}] and driver class [{}]",
                jdbcUrl, username, connectionConfig.getClassName());

        long sleepMs = 500;
        Throwable lastThrowable = null;

        while (true) {
            try (Connection connection = getSingleConnection(connectionConfig)) {
                LOGGER.info("Successfully established connection to {} with username {}", jdbcUrl, username);
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == ACCESS_DENIED_BAD_UNAME_OR_PWORD ||
                        e.getErrorCode() == ACCESS_DENIED_BAD_DATABASE ||
                        (e.getMessage() != null && e.getMessage().startsWith("Unsupported"))) {

                    // These errors are not due to the DB not being up, so throw it
                    throw new RuntimeException(LogUtil.message(
                            "Error connecting to {} with username {}", jdbcUrl, username), e);
                }
                final Throwable cause = e.getCause();
                final String errorMsg = cause != null
                        ? cause.getMessage()
                        : e.getMessage();
                final int vendorCode = e.getErrorCode();
                LOGGER.warn("Unable to establish database connection due to error: [{}] " +
                                "and vendorCode [{}], will try again " +
                                "in {}ms, enable debug to see stack trace",
                        errorMsg, vendorCode, sleepMs);
                if (LOGGER.isDebugEnabled()) {
                    if (lastThrowable == null || !e.getMessage().equals(lastThrowable.getMessage())) {
                        // Only log the stack when it changes, else it fills up the log pretty quickly
                        LOGGER.debug("Unable to establish database connection due to error", e);
                    }
                    lastThrowable = e;
                }
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                // Nothing to do here as the
                Thread.currentThread().interrupt();
                break;
            }

            // Gradually increase the sleep time up to a maximum
            sleepMs = (long) (sleepMs * 1.3);
            if (sleepMs >= MAX_SLEEP_TIME_MS) {
                sleepMs = MAX_SLEEP_TIME_MS;
            }
        }
    }

    public static boolean validateConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return true;
        } catch (SQLException e) {
            final Throwable cause = e.getCause();
            final String errorMsg = cause != null
                    ? cause.getMessage()
                    : e.getMessage();
            final int vendorCode = e.getErrorCode();
            throw new RuntimeException(LogUtil.message(
                    "Unable to establish database connection due to error: [{}] and vendorCode [{}].",
                    errorMsg, vendorCode));
        }
    }

    public static int countEntity(final Connection connection, final String tableName) {
        try (final Statement statement = connection.createStatement()) {
            try (final ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM " + tableName)) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return 0;
    }

    public static boolean doesTableExist(final Connection connection, final String tableName) {
        try {
            final DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet tables = meta.getTables(null, null, tableName, null)) {
                return tables.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    LogUtil.message("Error establishing if table {} exists in the database.", tableName), e);
        }
    }

    public static void renameTable(final Connection connection,
                                   final String oldTableName,
                                   final String newTableName) {
        renameTable(connection, oldTableName, newTableName);
    }

    public static void renameTable(final Connection connection,
                                   final String oldTableName,
                                   final String newTableName,
                                   final boolean isIdempotent) {

        if (doesTableExist(connection, oldTableName)) {
            String sql = "RENAME TABLE " + oldTableName + " TO " + newTableName;
            try {
                try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                        LogUtil.message("Error renaming table {} to {}.", oldTableName, newTableName), e);
            }
        } else {
            if (!isIdempotent) {
                throw new RuntimeException(LogUtil.message("Expecting table {} to exist", oldTableName));
            }
        }
    }

    public static Byte getByte(final ResultSet rs, final String columnLabel) throws SQLException {
        final byte val = rs.getByte(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Byte getByte(final ResultSet rs, final int columnIndex) throws SQLException {
        final byte val = rs.getByte(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Short getShort(final ResultSet rs, final String columnLabel) throws SQLException {
        final short val = rs.getShort(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Short getShort(final ResultSet rs, final int columnIndex) throws SQLException {
        final short val = rs.getShort(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Float getFloat(final ResultSet rs, final String columnLabel) throws SQLException {
        final float val = rs.getFloat(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Float getFloat(final ResultSet rs, final int columnIndex) throws SQLException {
        final float val = rs.getFloat(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Double getDouble(final ResultSet rs, final String columnLabel) throws SQLException {
        final double val = rs.getDouble(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Double getDouble(final ResultSet rs, final int columnIndex) throws SQLException {
        final double val = rs.getDouble(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Integer getInteger(final ResultSet rs, final String columnLabel) throws SQLException {
        final int val = rs.getInt(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Integer getInteger(final ResultSet rs, final int columnIndex) throws SQLException {
        final int val = rs.getInt(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Long getLong(final ResultSet rs, final String columnLabel) throws SQLException {
        final long val = rs.getLong(columnLabel);
        return rs.wasNull()
                ? null
                : val;
    }

    public static Long getLong(final ResultSet rs, final int columnIndex) throws SQLException {
        final long val = rs.getLong(columnIndex);
        return rs.wasNull()
                ? null
                : val;
    }

    public static void setByte(final PreparedStatement ps,
                               final int parameterIndex,
                               final Byte val) throws SQLException {
        if (val == null) {
            ps.setNull(parameterIndex, Types.TINYINT);
        } else {
            ps.setByte(parameterIndex, val);
        }
    }

    public static void setInteger(final PreparedStatement ps,
                                  final int parameterIndex,
                                  final Integer val) throws SQLException {
        if (val == null) {
            ps.setNull(parameterIndex, Types.INTEGER);
        } else {
            ps.setInt(parameterIndex, val);
        }
    }

    public static void setLong(final PreparedStatement ps,
                               final int columnIndex,
                               final Long val) throws SQLException {
        if (val == null) {
            ps.setNull(columnIndex, Types.BIGINT);
        } else {
            ps.setLong(columnIndex, val);
        }
    }
}
