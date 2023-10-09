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

import java.util.function.Supplier;

abstract class AbstractAggregateFunction extends AbstractManyChildFunction implements AggregateFunction {

    private final Calculator calculator;

    AbstractAggregateFunction(final String name, final Calculator calculator) {
        super(name, 1, Integer.MAX_VALUE);
        this.calculator = calculator;
    }

    @Override
    public Generator createGenerator() {
        // If we only have a single param then we are operating in aggregate
        // mode.
        if (isAggregate()) {
            final Generator childGenerator = functions[0].createGenerator();
            return new AggregateGen(childGenerator, calculator);
        }

        return super.createGenerator();
    }

    @Override
    protected Generator createGenerator(final Generator[] childGenerators) {
        return new Gen(childGenerators, calculator);
    }

    @Override
    public boolean isAggregate() {
        return functions.length == 1;
    }

    private static final class AggregateGen extends AbstractSingleChildGenerator {

        private final Calculator calculator;

        private Val current = ValNull.INSTANCE;

        AggregateGen(final Generator childGenerator, final Calculator calculator) {
            super(childGenerator);
            this.calculator = calculator;
        }

        @Override
        public void set(final Values values) {
            childGenerator.set(values);
            current = calculator.calc(current, childGenerator.eval(null));
        }

        @Override
        public Val eval(final Supplier<ChildData> childDataSupplier) {
            return current;
        }

        @Override
        public void merge(final Generator generator) {
            final AggregateGen aggregateGen = (AggregateGen) generator;
            current = calculator.calc(current, aggregateGen.current);
            super.merge(generator);
        }

        @Override
        public void read(final Input input) {
            super.read(input);
            current = ValSerialiser.read(input);
        }

        @Override
        public void write(final Output output) {
            super.write(output);
            ValSerialiser.write(output, current);
        }
    }

    private static final class Gen extends AbstractManyChildGenerator {

        private final Calculator calculator;

        Gen(final Generator[] childGenerators, final Calculator calculator) {
            super(childGenerators);
            this.calculator = calculator;
        }

        @Override
        public void set(final Values values) {
            for (final Generator gen : childGenerators) {
                gen.set(values);
            }
        }

        @Override
        public Val eval(final Supplier<ChildData> childDataSupplier) {
            Val value = ValNull.INSTANCE;
            for (final Generator gen : childGenerators) {
                final Val val = gen.eval(childDataSupplier);
                if (!val.type().isValue()) {
                    return val;
                }
                value = calculator.calc(value, val);
            }
            return value;
        }
    }
}
