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

package stroom.pipeline.refdata.store.offheapstore.serdes;


import stroom.pipeline.refdata.store.offheapstore.RangeStoreKey;
import stroom.pipeline.refdata.store.offheapstore.UID;
import stroom.util.shared.Range;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class TestRangeStoreKeySerde extends AbstractSerdeTest<RangeStoreKey, RangeStoreKeySerde> {

    @Test
    void testSerialiseDeserialise() {
        final UID uid = UID.of(ByteBuffer.allocateDirect(UID.UID_ARRAY_LENGTH), 0, 1, 2, 3);
        final Range<Long> range = new Range<>(23L, 52L);

        final RangeStoreKey rangeStoreKey = new RangeStoreKey(uid, range);

        doSerialisationDeserialisationTest(rangeStoreKey);
    }

    @Override
    Class<RangeStoreKeySerde> getSerdeType() {
        return RangeStoreKeySerde.class;
    }
}
