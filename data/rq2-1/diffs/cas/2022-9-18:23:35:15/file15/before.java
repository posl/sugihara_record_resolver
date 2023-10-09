package org.apereo.cas.configuration.support;

import org.apereo.cas.configuration.model.core.authentication.AttributeRepositoryStates;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.ConnectionPoolingProperties;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.support.NamedStubPersonAttributeDao;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


/**
 * A re-usable collection of utility methods for object instantiations and configurations used cross various
 * {@code @Bean} creation methods throughout CAS server.
 *
 * @author Dmitriy Kopylenko
 * @since 5.0.0
 */
@UtilityClass
public class Beans {

    /**
     * New thread pool executor factory bean.
     *
     * @param config the config
     * @return the thread pool executor factory bean
     */
    public static FactoryBean<ExecutorService> newThreadPoolExecutorFactoryBean(final ConnectionPoolingProperties config) {
        val bean = new ThreadPoolExecutorFactoryBean();
        bean.setMaxPoolSize(config.getMaxSize());
        bean.setCorePoolSize(config.getMinSize());
        bean.afterPropertiesSet();
        return bean;
    }

    /**
     * New attribute repository person attribute dao.
     *
     * @param p the properties
     * @return the person attribute dao
     */
    public static IPersonAttributeDao newStubAttributeRepository(final PrincipalAttributesProperties p) {
        val dao = new NamedStubPersonAttributeDao();
        val backingMap = new LinkedHashMap<String, List<Object>>();
        val stub = p.getStub();
        stub.getAttributes().forEach((key, value) -> {
            val vals = StringUtils.commaDelimitedListToStringArray(value);
            backingMap.put(key, Arrays.stream(vals)
                .map(v -> {
                    val bool = BooleanUtils.toBooleanObject(v);
                    if (bool != null) {
                        return bool;
                    }
                    return v;
                })
                .collect(Collectors.toList()));
        });
        dao.setBackingMap(backingMap);
        dao.setOrder(stub.getOrder());
        dao.setEnabled(stub.getState() != AttributeRepositoryStates.DISABLED);
        dao.putTag("state", stub.getState() == AttributeRepositoryStates.ACTIVE);
        if (StringUtils.hasText(stub.getId())) {
            dao.setId(stub.getId());
        }
        return dao;
    }

    /**
     * New duration. If the provided length is duration,
     * it will be parsed accordingly, or if it's a numeric value
     * it will be pared as a duration assuming it's provided as seconds.
     *
     * @param value the length in seconds.
     * @return the duration
     */
    public static Duration newDuration(final String value) {
        if (isNeverDurable(value)) {
            return Duration.ZERO;
        }
        if (isInfinitelyDurable(value)) {
            return Duration.ofDays(Integer.MAX_VALUE);
        }
        if (NumberUtils.isCreatable(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        return Duration.parse(value);
    }

    /**
     * Is infinitely durable?
     *
     * @param value the value
     * @return true/false
     */
    public static boolean isInfinitelyDurable(final String value) {
        return "-1".equalsIgnoreCase(value) || !StringUtils.hasText(value) || "INFINITE".equalsIgnoreCase(value);
    }

    /**
     * Is never durable?
     *
     * @param value the value
     * @return true/false
     */
    public static boolean isNeverDurable(final String value) {
        return "0".equalsIgnoreCase(value) || "NEVER".equalsIgnoreCase(value) || !StringUtils.hasText(value);
    }

    /**
     * Gets temp file path.
     *
     * @param prefix the prefix
     * @param suffix the suffix
     * @return the temp file path
     */
    public static String getTempFilePath(final String prefix, final String suffix) {
        return Unchecked.supplier(() -> File.createTempFile(prefix, suffix).getCanonicalPath()).get();
    }
}
