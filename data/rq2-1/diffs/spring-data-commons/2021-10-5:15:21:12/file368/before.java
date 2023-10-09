/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core.support;

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Dummy implementation for {@link RepositoryFactorySupport} that is equipped with mocks to simulate behavior for test
 * cases.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class DummyRepositoryFactory extends RepositoryFactorySupport {

	public final MyRepositoryQuery queryOne = mock(MyRepositoryQuery.class);
	public final RepositoryQuery queryTwo = mock(RepositoryQuery.class);
	public final QueryLookupStrategy strategy = mock(QueryLookupStrategy.class);

	private final ApplicationStartup applicationStartup;

	@SuppressWarnings("unchecked") private final QuerydslPredicateExecutor<Object> querydsl = mock(
			QuerydslPredicateExecutor.class);
	private final Object repository;

	public DummyRepositoryFactory(Object repository) {

		this.repository = repository;

		when(strategy.resolveQuery(Mockito.any(Method.class), Mockito.any(RepositoryMetadata.class),
				Mockito.any(ProjectionFactory.class), Mockito.any(NamedQueries.class))).thenReturn(queryOne);

		this.applicationStartup = mock(ApplicationStartup.class);
		StartupStep startupStep = mock(StartupStep.class);
		when(applicationStartup.start(anyString())).thenReturn(startupStep);
		when(startupStep.tag(anyString(), anyString())).thenReturn(startupStep);
		when(startupStep.tag(anyString(), ArgumentMatchers.<Supplier<String>> any())).thenReturn(startupStep);

		BeanFactory beanFactory = Mockito.mock(BeanFactory.class);
		when(beanFactory.getBean(ApplicationStartup.class)).thenReturn(applicationStartup);
		setBeanFactory(beanFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return mock(EntityInformation.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		return repository;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return repository.getClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(strategy);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryFragments(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		RepositoryFragments fragments = super.getRepositoryFragments(metadata);

		return QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface()) //
				? fragments.append(RepositoryFragments.just(querydsl)) //
				: fragments;
	}

	ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * @author Mark Paluch
	 */
	public interface MyRepositoryQuery extends RepositoryQuery {

	}

}
