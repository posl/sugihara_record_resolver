/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.core.SuppressForbidden;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Arguments for running Elasticsearch.
 *
 * @param daemonize {@code true} if Elasticsearch should run as a daemon process, or {@code false} otherwise
 * @param quiet {@code false} if Elasticsearch should print log output to the console, {@code true} otherwise
 * @param pidFile a path to a file Elasticsearch should write its process id to, or {@code null} if no pid file should be written
 * @param keystorePassword the password for the Elasticsearch keystore
 * @param nodeSettings the node settings read from {@code elasticsearch.yml}, the cli and the process environment
 * @param configDir the directory where {@code elasticsearch.yml} and other config exists
 */
public record ServerArgs(
    boolean daemonize,
    boolean quiet,
    Path pidFile,
    SecureString keystorePassword,
    Settings nodeSettings,
    Path configDir
) implements Writeable {

    /**
     * Alternate constructor to read the args from a binary stream.
     */
    public ServerArgs(StreamInput in) throws IOException {
        this(
            in.readBoolean(),
            in.readBoolean(),
            readPidFile(in),
            in.readSecureString(),
            Settings.readSettingsFromStream(in),
            resolvePath(in.readString())
        );
    }

    private static Path readPidFile(StreamInput in) throws IOException {
        String pidFile = in.readOptionalString();
        return pidFile == null ? null : resolvePath(pidFile);
    }

    @SuppressForbidden(reason = "reading local path from stream")
    private static Path resolvePath(String path) {
        return PathUtils.get(path);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(daemonize);
        out.writeBoolean(quiet);
        out.writeOptionalString(pidFile == null ? null : pidFile.toString());
        out.writeSecureString(keystorePassword);
        Settings.writeSettingsToStream(nodeSettings, out);
        out.writeString(configDir.toString());
    }
}
