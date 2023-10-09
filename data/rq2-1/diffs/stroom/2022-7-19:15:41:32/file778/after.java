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


@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = ToBoolean.NAME,
        commonCategory = FunctionCategory.CAST,
        commonReturnType = ValBoolean.class,
        commonReturnDescription = "The value as the boolean type.",
        signatures = @FunctionSignature(
                description = "Converts the supplied value to a boolean (if it can be). For example if the value is " +
                        "numeric then any non-zero value is considered true. For a text value, any value (ignoring " +
                        "case) equal to \"true\" is considered true.",
                args = @FunctionArg(
                        name = "value",
                        description = "Field, the result of another function or a constant.",
                        argType = Val.class)))
class ToBoolean extends AbstractCast {

    static final String NAME = "toBoolean";
    private static final ValErr ERROR = ValErr.create("Unable to cast to a boolean");
    private static final Cast CAST = new Cast();

    public ToBoolean(final String name) {
        super(name);
    }

    @Override
    AbstractCaster getCaster() {
        return CAST;
    }

    private static class Cast extends AbstractCaster {

        @Override
        Val cast(final Val val) {
            if (!val.type().isValue()) {
                return val;
            }

            final Boolean value = val.toBoolean();
            if (value != null) {
                return ValBoolean.create(value);
            }
            return ERROR;
        }
    }
}
