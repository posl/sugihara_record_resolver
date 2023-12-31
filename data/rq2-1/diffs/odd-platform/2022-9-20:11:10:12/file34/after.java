package org.opendatadiscovery.oddplatform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendatadiscovery.oddplatform.api.contract.api.IdentityApi;
import org.opendatadiscovery.oddplatform.api.contract.model.AssociatedOwner;
import org.opendatadiscovery.oddplatform.service.IdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IdentityController implements IdentityApi {
    private final IdentityService identityService;

    @Override
    public Mono<ResponseEntity<AssociatedOwner>> whoami(final ServerWebExchange exchange) {
        return identityService.whoami()
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.noContent().build()));
    }
}
