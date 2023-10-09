/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.dashboard.expression.v1;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = CountGroups.NAME,
        commonCategory = FunctionCategory.AGGREGATE,
        commonDescription = "This is used to count the number of unique group keys where there are multiple " +
                "group levels. For example if records are grouped by Name then Type then for each Name, " +
                "countGroups will give you the number of unique Type values for that Name.",
        commonReturnType = ValLong.class,
        commonReturnDescription = "Number of unique child group keys within the current group.",
        signatures = @FunctionSignature(
                args = {}))
class CountGroups extends AbstractFunction {

    static final String NAME = "countGroups";

    public CountGroups(final String name) {
        super(name, 0, 0);
    }

    @Override
    public Generator createGenerator() {
        return new Gen();
    }

    @Override
    public boolean isAggregate() {
        return true;
    }

    @Override
    public boolean hasAggregate() {
        return isAggregate();
    }

    private static final class Gen extends AbstractNoChildGenerator {

        private static final long serialVersionUID = -9130548669643582369L;

        private final Set<GroupKey> childGroups = new HashSet<>();
        private long nonGroupedChildCount;

        @Override
        public Val eval() {
            final long count = nonGroupedChildCount + childGroups.size();
            if (count == 0) {
                return ValNull.INSTANCE;
            }

            return ValLong.create(count);
        }

        @Override
        public void addChildKey(final GroupKey key) {
            if (key == null) {
                nonGroupedChildCount++;
            } else {
                childGroups.add(key);
            }
        }

        @Override
        public void merge(final Generator generator) {
            final Gen countGen = (Gen) generator;
            nonGroupedChildCount += countGen.nonGroupedChildCount;
            childGroups.addAll(countGen.childGroups);
            super.merge(generator);
        }

        @Override
        public void read(final Input input) {
            childGroups.clear();
            final int length = input.readInt(true);
            for (int i = 0; i < length; i++) {
                childGroups.add(GroupKeySerialiser.read(input));
            }
            nonGroupedChildCount = input.readLong(true);
        }

        @Override
        public void write(final Output output) {
            output.writeInt(childGroups.size(), true);
            for (final GroupKey key : childGroups) {
                GroupKeySerialiser.write(output, key);
            }
            output.writeLong(nonGroupedChildCount, true);
        }
    }
}
