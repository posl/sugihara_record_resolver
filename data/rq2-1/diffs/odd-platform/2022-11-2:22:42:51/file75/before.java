package org.opendatadiscovery.oddplatform.repository.reactive;

import org.opendatadiscovery.oddplatform.model.tables.pojos.OwnerPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.UserOwnerMappingPojo;
import reactor.core.publisher.Mono;

public interface ReactiveUserOwnerMappingRepository {
    Mono<UserOwnerMappingPojo> createRelation(final String oidcUsername, final Long ownerId);

    Mono<UserOwnerMappingPojo> deleteRelation(final String oidcUsername);

    Mono<OwnerPojo> getAssociatedOwner(final String oidcUsername);
}
