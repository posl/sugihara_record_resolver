package org.opendatadiscovery.oddplatform.repository.reactive;

import org.opendatadiscovery.oddplatform.dto.OwnershipDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.OwnershipPojo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveOwnershipRepository {
    Mono<OwnershipDto> get(final long id);

    Mono<OwnershipPojo> create(final OwnershipPojo pojo);

    Mono<OwnershipPojo> delete(final long ownershipId);

    Mono<OwnershipPojo> updateTitle(final long ownershipId, final long titleId);

    Mono<Boolean> existsByOwner(final long ownerId);

    Flux<OwnershipPojo> deleteByDataEntityId(final long dataEntityId);

    Flux<OwnershipDto> getOwnershipsByDataEntityId(final long dataEntityId);
}
