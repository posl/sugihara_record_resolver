/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.info.PluginsAndModules;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.test.ESTestCase;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class PluginDescriptorTests extends ESTestCase {

    public void testReadFromProperties() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin",
            "modulename",
            "org.mymodule"
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertEquals("my_plugin", info.getName());
        assertEquals("fake desc", info.getDescription());
        assertEquals("1.0", info.getVersion());
        assertEquals("FakePlugin", info.getClassname());
        assertEquals("org.mymodule", info.getModuleName().orElseThrow());
        assertThat(info.getExtendedPlugins(), empty());
    }

    public void testReadFromPropertiesNameMissing() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(pluginDir);
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("property [name] is missing in"));

        PluginTestUtil.writePluginProperties(pluginDir, "name", "");
        e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("property [name] is missing in"));
    }

    public void testReadFromPropertiesDescriptionMissing() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(pluginDir, "name", "fake-plugin");
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[description] is missing"));
    }

    public void testReadFromPropertiesVersionMissing() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(pluginDir, "description", "fake desc", "name", "fake-plugin");
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[version] is missing"));
    }

    public void testReadFromPropertiesElasticsearchVersionMissing() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(pluginDir, "description", "fake desc", "name", "my_plugin", "version", "1.0");
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[elasticsearch.version] is missing"));
    }

    public void testReadFromPropertiesElasticsearchVersionEmpty() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            "  "
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[elasticsearch.version] is missing"));
    }

    public void testReadFromPropertiesJavaVersionMissing() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "version",
            "1.0"
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[java.version] is missing"));
    }

    public void testReadFromPropertiesBadJavaVersionFormat() throws Exception {
        String pluginName = "fake-plugin";
        Path pluginDir = createTempDir().resolve(pluginName);
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            pluginName,
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            "1.7.0_80",
            "classname",
            "FakePlugin",
            "version",
            "1.0"
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), equalTo("Invalid version string: '1.7.0_80'"));
    }

    public void testReadFromPropertiesBogusElasticsearchVersion() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "version",
            "1.0",
            "name",
            "my_plugin",
            "elasticsearch.version",
            "bogus"
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("version needs to contain major, minor, and revision"));
    }

    public void testReadFromPropertiesJvmMissingClassname() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version")
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("property [classname] is missing"));
    }

    public void testReadFromPropertiesModulenameFallback() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin"
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(info.getModuleName().isPresent(), is(false));
        assertThat(info.getExtendedPlugins(), empty());
    }

    public void testReadFromPropertiesModulenameEmpty() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin",
            "modulename",
            " "
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(info.getModuleName().isPresent(), is(false));
        assertThat(info.getExtendedPlugins(), empty());
    }

    public void testExtendedPluginsSingleExtension() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin",
            "extended.plugins",
            "foo"
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(info.getExtendedPlugins(), contains("foo"));
    }

    public void testExtendedPluginsMultipleExtensions() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin",
            "extended.plugins",
            "foo,bar,baz"
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(info.getExtendedPlugins(), contains("foo", "bar", "baz"));
    }

    public void testExtendedPluginsEmpty() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "classname",
            "FakePlugin",
            "extended.plugins",
            ""
        );
        PluginDescriptor info = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(info.getExtendedPlugins(), empty());
    }

    public void testSerialize() throws Exception {
        PluginDescriptor info = new PluginDescriptor(
            "c",
            "foo",
            "dummy",
            Version.CURRENT,
            "1.8",
            "dummyclass",
            null,
            Collections.singletonList("foo"),
            randomBoolean(),
            PluginType.ISOLATED,
            "-Dfoo=bar",
            randomBoolean()
        );
        BytesStreamOutput output = new BytesStreamOutput();
        info.writeTo(output);
        ByteBuffer buffer = ByteBuffer.wrap(output.bytes().toBytesRef().bytes);
        ByteBufferStreamInput input = new ByteBufferStreamInput(buffer);
        PluginDescriptor info2 = new PluginDescriptor(input);
        assertThat(info2.toString(), equalTo(info.toString()));
    }

    public void testSerializeWithModuleName() throws Exception {
        PluginDescriptor info = new PluginDescriptor(
            "c",
            "foo",
            "dummy",
            Version.CURRENT,
            "1.8",
            "dummyclass",
            "some.module",
            Collections.singletonList("foo"),
            randomBoolean(),
            PluginType.ISOLATED,
            "-Dfoo=bar",
            randomBoolean()
        );
        BytesStreamOutput output = new BytesStreamOutput();
        info.writeTo(output);
        ByteBuffer buffer = ByteBuffer.wrap(output.bytes().toBytesRef().bytes);
        ByteBufferStreamInput input = new ByteBufferStreamInput(buffer);
        PluginDescriptor info2 = new PluginDescriptor(input);
        assertThat(info2.toString(), equalTo(info.toString()));
    }

    public void testPluginListSorted() {
        List<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(
            new PluginDescriptor(
                "c",
                "foo",
                "dummy",
                Version.CURRENT,
                "1.8",
                "dummyclass",
                null,
                Collections.emptyList(),
                randomBoolean(),
                PluginType.ISOLATED,
                "-Da",
                randomBoolean()
            )
        );
        plugins.add(
            new PluginDescriptor(
                "b",
                "foo",
                "dummy",
                Version.CURRENT,
                "1.8",
                "dummyclass",
                null,
                Collections.emptyList(),
                randomBoolean(),
                PluginType.BOOTSTRAP,
                "-Db",
                randomBoolean()
            )
        );
        plugins.add(
            new PluginDescriptor(
                "e",
                "foo",
                "dummy",
                Version.CURRENT,
                "1.8",
                "dummyclass",
                null,
                Collections.emptyList(),
                randomBoolean(),
                PluginType.ISOLATED,
                "-Dc",
                randomBoolean()
            )
        );
        plugins.add(
            new PluginDescriptor(
                "a",
                "foo",
                "dummy",
                Version.CURRENT,
                "1.8",
                "dummyclass",
                null,
                Collections.emptyList(),
                randomBoolean(),
                PluginType.BOOTSTRAP,
                "-Dd",
                randomBoolean()
            )
        );
        plugins.add(
            new PluginDescriptor(
                "d",
                "foo",
                "dummy",
                Version.CURRENT,
                "1.8",
                "dummyclass",
                null,
                Collections.emptyList(),
                randomBoolean(),
                PluginType.ISOLATED,
                "-De",
                randomBoolean()
            )
        );
        PluginsAndModules pluginsInfo = new PluginsAndModules(plugins, Collections.emptyList());

        final List<PluginDescriptor> infos = pluginsInfo.getPluginInfos();
        List<String> names = infos.stream().map(PluginDescriptor::getName).toList();
        assertThat(names, contains("a", "b", "c", "d", "e"));
    }

    public void testUnknownProperties() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "extra",
            "property",
            "unknown",
            "property",
            "description",
            "fake desc",
            "classname",
            "Foo",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version")
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("Unknown properties for plugin [my_plugin] in plugin descriptor"));
    }

    public void testMissingType() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "classname",
            "Foo",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version")
        );

        final PluginDescriptor pluginDescriptor = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(pluginDescriptor.getType(), equalTo(PluginType.ISOLATED));
    }

    public void testInvalidType() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "classname",
            "Foo",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "type",
            "invalid"
        );

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[type] must be unspecified or one of [isolated, bootstrap] but found [invalid]"));
    }

    public void testJavaOptsAreAcceptedWithBootstrapPlugin() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "type",
            "bootstrap",
            "java.opts",
            "-Dfoo=bar"
        );

        final PluginDescriptor pluginDescriptor = PluginDescriptor.readFromProperties(pluginDir);
        assertThat(pluginDescriptor.getType(), equalTo(PluginType.BOOTSTRAP));
        assertThat(pluginDescriptor.getJavaOpts(), equalTo("-Dfoo=bar"));
    }

    public void testJavaOptsAreRejectedWithNonBootstrapPlugin() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "classname",
            "Foo",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "type",
            "isolated",
            "java.opts",
            "-Dfoo=bar"
        );

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[java.opts] can only have a value when [type] is set to [bootstrap]"));
    }

    public void testClassnameIsRejectedWithBootstrapPlugin() throws Exception {
        Path pluginDir = createTempDir().resolve("fake-plugin");
        PluginTestUtil.writePluginProperties(
            pluginDir,
            "description",
            "fake desc",
            "classname",
            "Foo",
            "name",
            "my_plugin",
            "version",
            "1.0",
            "elasticsearch.version",
            Version.CURRENT.toString(),
            "java.version",
            System.getProperty("java.specification.version"),
            "type",
            "bootstrap"
        );

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> PluginDescriptor.readFromProperties(pluginDir));
        assertThat(e.getMessage(), containsString("[classname] can only have a value when [type] is set to [bootstrap]"));
    }

    /**
     * This is important because {@link PluginsUtils#getPluginBundles(Path)} will
     * use the hashcode to catch duplicate names
     */
    public void testPluginEqualityAndHash() {
        PluginDescriptor descriptor1 = new PluginDescriptor(
            "c",
            "foo",
            "dummy",
            Version.CURRENT,
            "1.8",
            "dummyclass",
            null,
            Collections.singletonList("foo"),
            randomBoolean(),
            PluginType.ISOLATED,
            "-Dfoo=bar",
            randomBoolean()
        );
        // everything but name is different from descriptor1
        PluginDescriptor descriptor2 = new PluginDescriptor(
            descriptor1.getName(),
            randomValueOtherThan(descriptor1.getDescription(), () -> randomAlphaOfLengthBetween(4, 12)),
            randomValueOtherThan(descriptor1.getVersion(), () -> randomAlphaOfLengthBetween(4, 12)),
            descriptor1.getElasticsearchVersion().previousMajor(),
            randomValueOtherThan(descriptor1.getJavaVersion(), () -> randomAlphaOfLengthBetween(4, 12)),
            randomValueOtherThan(descriptor1.getClassname(), () -> randomAlphaOfLengthBetween(4, 12)),
            randomAlphaOfLength(6),
            Collections.singletonList(
                randomValueOtherThanMany(v -> descriptor1.getExtendedPlugins().contains(v), () -> randomAlphaOfLengthBetween(4, 12))
            ),
            descriptor1.hasNativeController() == false,
            randomValueOtherThan(descriptor1.getType(), () -> randomFrom(PluginType.values())),
            randomValueOtherThan(descriptor1.getJavaOpts(), () -> randomAlphaOfLengthBetween(4, 12)),
            descriptor1.isLicensed() == false
        );
        // only name is different from descriptor1
        PluginDescriptor descriptor3 = new PluginDescriptor(
            randomValueOtherThan(descriptor1.getName(), () -> randomAlphaOfLengthBetween(4, 12)),
            descriptor1.getDescription(),
            descriptor1.getVersion(),
            descriptor1.getElasticsearchVersion(),
            descriptor1.getJavaVersion(),
            descriptor1.getClassname(),
            descriptor1.getModuleName().orElse(null),
            descriptor1.getExtendedPlugins(),
            descriptor1.hasNativeController(),
            descriptor1.getType(),
            descriptor1.getJavaOpts(),
            descriptor1.isLicensed()
        );

        assertThat(descriptor1, equalTo(descriptor2));
        assertThat(descriptor1.hashCode(), equalTo(descriptor2.hashCode()));

        assertThat(descriptor1, not(equalTo(descriptor3)));
        assertThat(descriptor1.hashCode(), not(equalTo(descriptor3.hashCode())));
    }

}
