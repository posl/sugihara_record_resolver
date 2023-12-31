/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import sun.management.jmxremote.ConnectorBootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jdk.internal.agent.AgentConfigurationError;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;

/*
 * @test
 * @bug     4932854
 * @key     intermittent
 * @summary Test RMI Bootstrap with SSL and no keystore.
 *
 * @library /test/lib
 *
 * @run main/timeout=300 RmiSslNoKeyStoreTest .*_ssltest.*.in
 * */

/**
 * <p>This class implements unit test for RMI Bootstrap.
 * When called with no arguments main() looks in the directory indicated
 * by the "test.src" system property for files called management*ok.properties
 * or management*ko.properties. The *ok.properties files are assumed to be
 * valid Java M&M config files for which the bootstrap should succeed.
 * The *ko.properties files are assumed to be configurations for which the
 * bootstrap & connection test will fail.</p>
 *
 * <p>The rmi port number can be specified with the "rmi.port" system property.
 * If not, this test will use 12424</p>
 *
 * <p>When called with some argument, the main() will interprete its args to
 * be Java M&M configuration file names. The filenames are expected to end
 * with ok.properties or ko.properties - and are interpreted as above.</p>
 *
 * <p>Note that a limitation of the RMI registry (bug 4267864) prevent
 * this test from succeeding if more than 1 configuration is used.
 * As long as 4267864 isn't fix, this test must be called as many times
 * as needed but with a single argument (no arguments, or several arguments
 * will fail).</p>
 *
 * <p>Debug traces are logged in "sun.management.test"</p>
 **/
public class RmiSslNoKeyStoreTest extends RmiTestBase {

    static TestLogger log = new TestLogger("RmiSslNoKeyStoreTest");
    /**
     * When launching several registries, we increment the port number
     * to avoid falling into "port number already in use" problems.
     **/
    static int testPort = 0;
    final String DEFAULT_KEY_STORE = defaultStoreNamePrefix + DefaultValues.KEYSTORE;
    final String KEY_STORE = System.getProperty(PropertyNames.KEYSTORE, DEFAULT_KEY_STORE);

    private static void checkKeystore(Properties props)
            throws IOException, GeneralSecurityException {
        if (log.isDebugOn()) {
            log.debug("checkKeystore", "Checking Keystore configuration");
        }

        final String keyStore = System.getProperty(PropertyNames.KEYSTORE);
        if (keyStore == null) {
            throw new IllegalArgumentException("System property " + PropertyNames.KEYSTORE + " not specified");
        }

        final String keyStorePass = System.getProperty(PropertyNames.KEYSTORE_PASSWD);
        if (keyStorePass == null) {
            // We don't have the password, we can only check whether the
            // file exists...
            //
            final File ksf = new File(keyStore);
            if (!ksf.canRead()) {
                throw new IOException(keyStore + ": not readable");
            }

            if (log.isDebugOn()) {
                log.debug("checkSSL", "No password.");
            }
            throw new IllegalArgumentException("System property " + PropertyNames.KEYSTORE_PASSWD + " not specified");
        }

        // Now we're going to load the keyStore - just to check it's
        // correct.
        //
        final String keyStoreType = System.getProperty(PropertyNames.KEYSTORE_TYPE, KeyStore.getDefaultType());
        final KeyStore ks = KeyStore.getInstance(keyStoreType);
        final FileInputStream fin = new FileInputStream(keyStore);
        final char keypassword[] = keyStorePass.toCharArray();

        try {
            ks.load(fin, keypassword);
        } finally {
            Arrays.fill(keypassword, ' ');
            fin.close();
        }

        if (log.isDebugOn()) {
            log.debug("checkSSL", "SSL configuration successfully checked");
        }
    }

    /**
     * Calls run(args[]).
     * exit(1) if the test fails.
     **/
    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Argument is required for this" + " test");
        }

        final List<Path> credentialFiles = prepareTestFiles(args[0]);

        RmiSslNoKeyStoreTest manager = new RmiSslNoKeyStoreTest();
        try {
            manager.run(args);
        } catch (RuntimeException r) {
            System.err.println("Test Failed: " + r.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            System.err.println("Test Failed: " + t);
            t.printStackTrace();
            System.exit(2);
        }
        System.out.println("**** Test RmiSslNoKeyStoreTest Passed ****");

        grantFilesAccess(credentialFiles, AccessControl.EVERYONE);
    }

    private void checkSslConfiguration() throws Exception {
        final String defaultConf = defaultFileNamePrefix + DefaultValues.CONFIG_FILE_NAME;
        final String confname = System.getProperty(PropertyNames.CONFIG_FILE_NAME, defaultConf);

        final Properties props = new Properties();
        final File conf = new File(confname);
        if (conf.exists()) {
            FileInputStream fin = new FileInputStream(conf);
            try {
                props.load(fin);
            } finally {
                fin.close();
            }
        }

        // Do we use SSL?
        final String useSslStr = props.getProperty(PropertyNames.USE_SSL, DefaultValues.USE_SSL);
        final boolean useSsl = Boolean.valueOf(useSslStr).booleanValue();

        log.debug("checkSslConfiguration", PropertyNames.USE_SSL + "=" + useSsl);
        if (useSsl == false) {
            final String msg = PropertyNames.USE_SSL + "=" + useSsl + ", can't run test";
            throw new IllegalArgumentException(msg);
        }

        try {
            checkKeystore(props);
        } catch (Exception x) {
            // Ok!
            log.debug("checkSslConfiguration", "Test configuration OK: " + x);
            return;
        }

        final String msg = "KeyStore properly configured, can't run test";
        throw new IllegalArgumentException(msg);
    }

    /**
     * Test the configuration indicated by `file'.
     * Sets the appropriate System properties for config file and
     * port and then calls ConnectorBootstrap.initialize().
     * eventually cleans up by calling ConnectorBootstrap.terminate().
     *
     * @return null if the test succeeds, an error message otherwise.
     **/
    private String testConfiguration(File file, int port) {

        final String path = (file == null) ? null : file.getAbsolutePath();
        final String config = (path == null) ? "Default config file" : path;

        try {
            System.out.println("***");
            System.out.println("*** Testing configuration (port=" + port + "): " + path);
            System.out.println("***");

            System.setProperty("com.sun.management.jmxremote.port", Integer.toString(port));
            if (path != null) {
                System.setProperty("com.sun.management.config.file", path);
            } else {
                System.getProperties().remove("com.sun.management.config.file");
            }

            log.trace("testConfiguration", "com.sun.management.jmxremote.port=" + port);
            if (path != null && log.isDebugOn()) {
                log.trace("testConfiguration", "com.sun.management.config.file=" + path);
            }

            checkSslConfiguration();

            final JMXConnectorServer cs;
            try {
                cs = ConnectorBootstrap.initialize();
            } catch (AgentConfigurationError x) {
                final String err = "Failed to initialize connector:" + "\n\tcom.sun.management.jmxremote.port=" + port +
                        ((path != null) ? "\n\tcom.sun.management.config.file=" + path : "\n\t" + config) +
                        "\n\tError is: " + x;

                log.trace("testConfiguration", "Expected failure: " + err);
                log.debug("testConfiguration", x);
                System.out.println("Got expected failure: " + x);
                return null;
            } catch (Exception x) {
                log.debug("testConfiguration", x);
                return x.toString();
            }
            try {
                JMXConnector cc = JMXConnectorFactory.connect(cs.getAddress(), null);
                cc.close();
            } catch (IOException x) {
                final String err = "Failed to initialize connector:" + "\n\tcom.sun.management.jmxremote.port=" + port +
                        ((path != null) ? "\n\tcom.sun.management.config.file=" + path : "\n\t" + config) +
                        "\n\tError is: " + x;

                log.trace("testConfiguration", "Expected failure: " + err);
                log.debug("testConfiguration", x);
                System.out.println("Got expected failure: " + x);
                return null;
            } catch (Exception x) {
                log.debug("testConfiguration", x);
                return x.toString();
            }
            try {
                cs.stop();
            } catch (Exception x) {
                final String err = "Failed to terminate: " + x;
                log.trace("testConfiguration", err);
                log.debug("testConfiguration", x);
            }
            final String err = "Bootstrap should have failed:" + "\n\tcom.sun.management.jmxremote.port=" + port +
                    ((path != null) ? "\n\tcom.sun.management.config.file=" + path : "\n\t" + config);
            log.trace("testConfiguration", err);
            return err;
        } catch (Exception x) {
            final String err = "Failed to test bootstrap for:" + "\n\tcom.sun.management.jmxremote.port=" + port +
                    ((path != null) ? "\n\tcom.sun.management.config.file=" + path : "\n\t" + config) +
                    "\n\tError is: " + x;

            log.trace("testConfiguration", err);
            log.debug("testConfiguration", x);
            return err;
        }
    }

    /**
     * Test a configuration file. Determines whether the bootstrap
     * should succeed or fail depending on the file name:
     * *ok.properties: bootstrap should succeed.
     * *ko.properties: bootstrap or connection should fail.
     *
     * @return null if the test succeeds, an error message otherwise.
     **/
    private String testConfigurationFile(String fileName) {
        File file = new File(fileName);
        final String portStr = System.getProperty("rmi.port", "12424");
        final int port = Integer.parseInt(portStr);

        return testConfiguration(file, port + testPort++);
    }

    /**
     * Test a configuration file.
     **/
    private void runConfigurationFile(String fileName) {
        String errStr = testConfigurationFile(fileName);
        if (errStr != null) {
            throw new RuntimeException(errStr);
        }

        if ((System.getProperty(PropertyNames.KEYSTORE) == null) &&
                (System.getProperty(PropertyNames.KEYSTORE_PASSWD) == null)) {
            try {

                // Specify the keystore, but don't specify the
                // password.
                //
                System.setProperty(PropertyNames.KEYSTORE, KEY_STORE);
                log.trace("run", PropertyNames.KEYSTORE + "=" + KEY_STORE);

                errStr = testConfigurationFile(fileName);
                if (errStr != null) {
                    throw new RuntimeException(errStr);
                }
            } finally {
                System.getProperties().remove(PropertyNames.KEYSTORE);
            }
        }
    }

    /**
     * Finds all configuration files (*ok.properties and *ko.properties)
     * and tests them.
     *
     * @throws RuntimeException if the test fails.
     **/
    public void run(boolean useSsl) throws IOException {
        final File[] conf = findAllConfigurationFiles(useSsl);
        if ((conf == null) || (conf.length == 0)) {
            throw new RuntimeException("No configuration found");
        }

        for (int i = 0; i < conf.length; i++) {
            runConfigurationFile(conf[i].toPath().toString());
        }
    }

    /**
     * Tests the specified configuration files.
     * If args[] is not empty, each element in args[] is expected to be
     * a filename ending either by ok.properties or ko.properties.
     * Otherwise, the configuration files will be automatically determined
     * by looking at all *.properties files located in the directory
     * indicated by the System property "test.src".
     *
     * @throws RuntimeException if the test fails.
     **/
    public void run(String args[]) throws IOException {
        if (args.length == 1) {
            run(args[0].contains("ssl"));
        } else {
            for (int i = 1; i < args.length; i++) {
                runConfigurationFile(args[i]);
            }
        }
    }
}
