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

import java.io.Serializable;

@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = IsBoolean.NAME,
        commonCategory = FunctionCategory.TYPE_CHECKING,
        commonReturnType = ValBoolean.class,
        commonReturnDescription = "True if value is a boolean.",
        signatures = @FunctionSignature(
                description = "Checks if the passed value has a boolean data type.",
                args = {
                        @FunctionArg(
                                name = "value",
                                description = "Field, the result of another function or a constant.",
                                argType = Val.class)
                }))
class IsBoolean extends AbstractIsFunction implements Serializable {

    static final String NAME = "isBoolean";
    private static final long serialVersionUID = -305145496413936297L;
    private static final BooleanTest TEST = new BooleanTest();

    public IsBoolean(final String name) {
        super(name);
    }

    @Override
    Test getTest() {
        return TEST;
    }

    private static class BooleanTest implements Test {

        @Override
        public Val test(final Val val) {
            return ValBoolean.create(val instanceof ValBoolean);
        }
    }
}
