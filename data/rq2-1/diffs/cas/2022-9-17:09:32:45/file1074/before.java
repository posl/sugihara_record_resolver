package org.apereo.cas;

import org.apereo.cas.config.JdbcCloudConfigBootstrapConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.jpa.AbstractJpaProperties;
import org.apereo.cas.configuration.support.JpaBeans;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link JdbcCloudConfigBootstrapConfigurationTests}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    JdbcCloudConfigBootstrapConfiguration.class
})
@Tag("JDBC")
public class JdbcCloudConfigBootstrapConfigurationTests {
    private static final String STATIC_AUTHN_USERS = "casuser::WHATEVER";

    @Autowired
    private CasConfigurationProperties casProperties;

    @BeforeAll
    @SneakyThrows
    public static void initialize() {
        val jpa = new Jpa();
        val ds = JpaBeans.newDataSource(jpa);
        try (val c = ds.getConnection()) {
            try (val s = c.createStatement()) {
                c.setAutoCommit(true);
                s.execute("create table CAS_SETTINGS_TABLE (id VARCHAR(255), name VARCHAR(255), value VARCHAR(255));");
                s.execute("insert into CAS_SETTINGS_TABLE (id, name, value) values('1', 'cas.authn.accept.users', '" + STATIC_AUTHN_USERS + "');");
            }
        }
    }

    @Test
    public void verifyOperation() {
        assertEquals(STATIC_AUTHN_USERS, casProperties.getAuthn().getAccept().getUsers());
    }

    public static class Jpa extends AbstractJpaProperties {
        private static final long serialVersionUID = 1210163210567513705L;
    }
}
