package com.redis.om.spring.annotations.document.fixtures;

import com.redis.om.spring.repository.RedisDocumentRepository;

@SuppressWarnings("unused") public interface ExpiringPersonWithDefaultRepository extends RedisDocumentRepository<ExpiringPersonWithDefault, String> {
}
