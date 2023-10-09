package org.apereo.cas;

import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test concurrency of PersonDirectoryPrincipalResolver.
 * Use CachingPersonAttributeDao -> MergingPersonAttributeDao -> with multiple PersonAttributeDao implementations.
 *
 * @since 6.2
 */
@SpringBootTest(classes = BasePrincipalAttributeRepositoryTests.SharedTestConfiguration.class, properties = {
    "cas.authn.attribute-repository.stub.attributes.uid=cas",
    "cas.authn.attribute-repository.stub.attributes.givenName=apereo-cas",
    "cas.authn.attribute-repository.stub.attributes.phone=123456789",

    "cas.authn.attribute-repository.json[0].location=classpath:/json-attribute-repository.json",
    "cas.authn.attribute-repository.json[0].order=1",

    "cas.authn.attribute-repository.groovy[0].location=classpath:/GroovyAttributeDao.groovy",
    "cas.authn.attribute-repository.groovy[0].order=2",

    "cas.authn.attribute-repository.core.aggregation=MERGE",
    "cas.authn.attribute-repository.core.merger=MULTIVALUED"
})
@Tag("Attributes")
public class PersonDirectoryPrincipalResolverConcurrencyTests {

    private static final int NUM_USERS = 100;

    private static final int EXECUTIONS_PER_USER = 1000;

    @Autowired
    @Qualifier(PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY)
    private IPersonAttributeDao attributeRepository;

    @Autowired
    private CasConfigurationProperties casProperties;

    private PrincipalResolver personDirectoryResolver;

    /**
     * Assert a list of runnables can run in parallel without any concurrency related exceptions.
     * Use CountDownLatch to start all threads at same time and wait for them to finish.
     *
     * @param message           error message
     * @param runnables         list of runnables
     * @param maxTimeoutSeconds timeout for test completion
     * @throws InterruptedException interruption
     */
    private static void assertConcurrent(final String message, final List<? extends Runnable> runnables,
                                         final int maxTimeoutSeconds) throws InterruptedException {
        val numThreads = runnables.size();
        val exceptions = Collections.synchronizedList(new ArrayList<>());
        val threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            val allExecutorThreadsReady = new CountDownLatch(numThreads);
            val afterInitBlocker = new CountDownLatch(1);
            val allDone = new CountDownLatch(numThreads);
            for (val submittedTestRunnable : runnables) {
                threadPool.execute(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }
            assertTrue(allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS),
                "Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent");
            afterInitBlocker.countDown();
            assertTrue(allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS), () -> message + " timeout! More than " + maxTimeoutSeconds + " seconds");
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(exceptions.isEmpty(), () -> message + " failed with exception(s)" + exceptions);
    }

    @BeforeEach
    protected void setUp() {
        this.personDirectoryResolver = CoreAuthenticationUtils.newPersonDirectoryPrincipalResolver(
            PrincipalFactoryUtils.newPrincipalFactory(),
            attributeRepository,
            CoreAuthenticationUtils.getAttributeMerger(casProperties.getAuthn().getAttributeRepository().getCore().getMerger()),
            casProperties.getPersonDirectory()
        );
    }

    /**
     * Create a PersonAttrGetter for each user and run them in parallel
     *
     * @throws Exception concurrency assertion failed
     */
    @Test
    public void validatePersonDirConcurrency() throws Exception {
        val userList = IntStream.range(0, NUM_USERS).mapToObj(i -> "user_" + i)
            .collect(Collectors.toCollection(ArrayList::new));

        val runnables = userList.stream().map(user -> new PersonAttrGetter(personDirectoryResolver, user))
            .collect(Collectors.toCollection(() -> new ArrayList<Runnable>()));
        assertConcurrent("Getting persons", runnables, 600);
    }

    @Slf4j
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    private record PersonAttrGetter(PrincipalResolver personDirectoryResolver, String username) implements Runnable {

        @Override
        public void run() {
            val upc = new UsernamePasswordCredential(username, "password");
            for (var i = 0; i < EXECUTIONS_PER_USER; i++) {
                try {
                    val person = this.personDirectoryResolver.resolve(upc);
                    val attributes = person.getAttributes();
                    assertEquals(username, person.getId());
                    LOGGER.debug("Fetched person: [{}] [{}], last-name [{}]", attributes.get("uid"),
                        attributes.get("lastName"), attributes.get("nickname"));
                } catch (final Exception e) {
                    LOGGER.warn("Error getting person: [{}]", e.getMessage(), e);
                    throw e;
                }
            }
        }
    }
}
