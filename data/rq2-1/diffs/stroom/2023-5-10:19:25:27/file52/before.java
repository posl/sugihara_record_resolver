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
 */

package stroom.dashboard.expression.v1;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.text.ParseException;
import java.util.function.Supplier;

@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = LastIndexOf.NAME,
        commonCategory = FunctionCategory.STRING,
        commonReturnType = ValInteger.class,
        commonReturnDescription = "The last position of subString.",
        signatures = @FunctionSignature(
                description = "Finds the last position (zero based) of subString in inputString or -1 if it " +
                        "cannot be found. Uses a simple literal match.",
                args = {
                        @FunctionArg(
                                name = "inputString",
                                description = "The string to find subString in.",
                                argType = ValString.class),
                        @FunctionArg(
                                name = "subString",
                                description = "The string to find in inputString.",
                                argType = ValString.class)
                }))
class LastIndexOf extends AbstractFunction {

    static final String NAME = "lastIndexOf";
    private Function stringFunction;

    private Generator gen;
    private Function function;
    private boolean hasAggregate;

    public LastIndexOf(final String name) {
        super(name, 2, 2);
    }

    @Override
    public void setParams(final Param[] params) throws ParseException {
        super.setParams(params);

        stringFunction = ParamParseUtil.parseStringFunctionParam(params, 1, name);

        final Param param = params[0];
        if (param instanceof Function) {
            function = (Function) param;
            hasAggregate = function.hasAggregate();

        } else {
            function = new StaticValueFunction((Val) param);

            // Optimise replacement of static input in case user does something stupid.
            if (stringFunction instanceof StaticValueFunction) {
                final String string = stringFunction.createGenerator().eval(null).toString();
                if (string != null) {
                    final String value = param.toString();
                    final int index = value.lastIndexOf(string);
                    if (index < 0) {
                        gen = new StaticValueFunction(ValNull.INSTANCE).createGenerator();
                    } else {
                        gen = new StaticValueFunction(ValInteger.create(index)).createGenerator();
                    }
                } else {
                    gen = new StaticValueFunction(ValNull.INSTANCE).createGenerator();
                }
            }
        }
    }

    @Override
    public Generator createGenerator() {
        if (gen != null) {
            return gen;
        }

        final Generator childGenerator = function.createGenerator();
        return new Gen(childGenerator, stringFunction.createGenerator());
    }

    @Override
    public boolean hasAggregate() {
        return hasAggregate;
    }

    private static final class Gen extends AbstractSingleChildGenerator {


        private final Generator stringGenerator;

        Gen(final Generator childGenerator, final Generator stringGenerator) {
            super(childGenerator);
            this.stringGenerator = stringGenerator;
        }

        @Override
        public void set(final Val[] values) {
            childGenerator.set(values);
            stringGenerator.set(values);
        }

        @Override
        public Val eval(final Supplier<ChildData> childDataSupplier) {
            final Val val = childGenerator.eval(childDataSupplier);
            if (!val.type().isValue()) {
                return ValErr.wrap(val);
            }
            final String value = val.toString();
            final Val strVal = stringGenerator.eval(childDataSupplier);
            if (!strVal.type().isValue()) {
                return ValErr.wrap(strVal);
            }
            final String str = strVal.toString();

            final int index = value.lastIndexOf(str);
            if (index >= 0) {
                return ValInteger.create(index);
            }

            return ValNull.INSTANCE;
        }

        @Override
        public void read(final Input input) {
            super.read(input);
            stringGenerator.read(input);
        }

        @Override
        public void write(final Output output) {
            super.write(output);
            stringGenerator.write(output);
        }
    }
}
