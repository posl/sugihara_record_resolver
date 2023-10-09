/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.pipeline.refdata.store;

import stroom.pipeline.refdata.store.offheapstore.TypedByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A wrapper for multiple {@link RefDataValueProxy} objects. A lookup (consumeBytes or supplyValue)
 * will perform a lookup against each sub-proxy in turn until a value is found.
 * <p>
 * This is used when we have multiple ref loaders attached to an XSLT. Any one of them may be
 * able to provide the value. This means the order of ref loaders is important for performance.
 * The one most likely to yield results should be at the top.
 */
public class MultiRefDataValueProxy implements RefDataValueProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiRefDataValueProxy.class);

    private final List<SingleRefDataValueProxy> refDataValueProxies;

    public MultiRefDataValueProxy(final List<SingleRefDataValueProxy> refDataValueProxies) {
        this.refDataValueProxies = Objects.requireNonNull(refDataValueProxies);
    }

    @Override
    public Optional<RefDataValue> supplyValue() {
        // try each of our proxies in turn and as soon as one finds a result break out
        Optional<RefDataValue> optResult = Optional.empty();
        for (RefDataValueProxy refDataValueProxy : refDataValueProxies) {

            LOGGER.trace("Attempting to supplyValue with sub-proxy {}", refDataValueProxy);
            optResult = refDataValueProxy.supplyValue();
            if (optResult.isPresent()) {
                LOGGER.trace("Found result with sub-proxy {}", refDataValueProxy);
                break;
            }
        }
        return optResult;
    }

    @Override
    public boolean consumeBytes(final Consumer<TypedByteBuffer> typedByteBufferConsumer) {
        // try each of our proxies in turn and as soon as one finds a result break out
        boolean foundValue = false;
        // We could construct this object with a pipeline scoped object to hold a
        // map of mapName to refDataValueProxy which we could populate when we find a
        // result. Thus for any future lookups we could try that one first before looping over
        // the rest.  For pipelines with a lot of ref loaders this should speed things up.
        // The downside of this is that it would change the behavior in the event that two
        // ref streams can supply a value for the same map/key
        for (SingleRefDataValueProxy refDataValueProxy : refDataValueProxies) {
            LOGGER.trace("Attempting to consumeBytes with sub-proxy {}", refDataValueProxy);
            foundValue = refDataValueProxy.consumeBytes(typedByteBufferConsumer);
            if (foundValue) {
                LOGGER.trace("Found result with sub-proxy {}", refDataValueProxy);
                break;
            }
        }
        if (foundValue) {
            LOGGER.trace("Result found for proxy {}", this);
        } else {
            LOGGER.trace("No result found for proxy {}", this);
        }
        return foundValue;
    }

    @Override
    public boolean consumeValue(final RefDataValueProxyConsumerFactory refDataValueProxyConsumerFactory) {
        // try each of our proxies in turn and as soon as one finds a result break out
        boolean foundValue = false;
        // We could construct this object with a pipeline scoped object to hold a
        // map of mapName to refDataValueProxy which we could populate when we find a
        // result. Thus for any future lookups we could try that one first before looping over
        // the rest.  For pipelines with a lot of ref loaders this should speed things up.
        // The downside of this is that it would change the behavior in the event that two
        // ref streams can supply a value for the same map/key
        for (RefDataValueProxy refDataValueProxy : refDataValueProxies) {
            LOGGER.trace("Attempting to consumeBytes with sub-proxy {}", refDataValueProxy);
            foundValue = refDataValueProxy.consumeValue(refDataValueProxyConsumerFactory);
            if (foundValue) {
                LOGGER.trace("Found result with sub-proxy {}", refDataValueProxy);
                break;
            }
        }
        if (foundValue) {
            LOGGER.trace("Result found for proxy {}", this);
        } else {
            LOGGER.trace("No result found for proxy {}", this);
        }
        return foundValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MultiRefDataValueProxy that = (MultiRefDataValueProxy) o;
        return Objects.equals(refDataValueProxies, that.refDataValueProxies);
    }

    @Override
    public int hashCode() {

        return Objects.hash(refDataValueProxies);
    }

    @Override
    public String toString() {
        return "MultiRefDataValueProxy{" +
                "refDataValueProxies=" + refDataValueProxies +
                '}';
    }
}
