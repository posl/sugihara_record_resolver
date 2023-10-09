package stroom.cache.api;

import java.util.Optional;
import java.util.function.Function;

/**
 * A self-loading cache that will load entries on demand.
 */
public interface LoadingStroomCache<K, V> extends StroomCache<K, V> {

    /**
     * Gets the value associated with key from the cache. If key is not found in the cache
     * then the loadFunction will be called to load the entry into the cache.
     *
     * @return The value associated with key or null if the loadFunction returns null.
     */
    @Override
    V get(K key);

    /**
     * Gets the value associated with key from the cache. If key is not found in the cache
     * then the loadFunction will be called to load the entry into the cache.
     *
     * @return The value associated with key or an empty {@link Optional} if the loadFunction
     * returns null.
     */
    Optional<V> getOptional(K key);

    /**
     * Gets the value associated with key from the cache. If key is not found in the cache
     * then valueProvider will be called to load the entry into the cache.
     * valueProvider overrides loadFunction so loadFunction will NOT be called, even if
     * valueProvider returns null.
     *
     * @return The value associated with key or null if the valueProvider returns null.
     */
    @Override
    V get(K key, Function<K, V> valueProvider);

    /**
     * If key exists in the cache returns an {@link Optional} containing the value
     * associated with key, else returns an empty optional.
     * It will NOT call the loadFunction.
     */
    @Override
    Optional<V> getIfPresent(K key);
}
