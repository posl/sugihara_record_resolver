/*
 * Copyright 2017 Crown Copyright
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

import java.io.Serializable;

@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = Include.NAME,
        commonCategory = FunctionCategory.STRING,
        commonReturnType = ValString.class,
        commonReturnDescription = "The input value if one of the patterns matches it, otherwise null.",
        signatures = @FunctionSignature(
                description = "If the supplied input string matches one of the supplied regex patterns then return " +
                        "the input string, otherwise return null.",
                args = {
                        @FunctionArg(
                                name = "input",
                                description = "The input value to test against.",
                                argType = ValString.class),
                        @FunctionArg(
                                name = "pattern",
                                description = "The regex pattern to test against the input string.",
                                argType = ValString.class,
                                isVarargs = true,
                                minVarargsCount = 1)}))
class Include extends AbstractIncludeExclude implements Serializable {

    static final String NAME = "include";
    private static final long serialVersionUID = -305845496003936297L;

    public Include(final String name) {
        super(name);
    }

    @Override
    boolean inverse() {
        return false;
    }

    @Override
    protected Generator createGenerator(final Generator[] childGenerators) {
        return new Gen(childGenerators);
    }

    private static final class Gen extends AbstractGen {

        Gen(final Generator[] childGenerators) {
            super(childGenerators);
        }

        @Override
        boolean inverse() {
            return false;
        }
    }
}
