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

package stroom.index.shared;

import stroom.docref.HasDisplayValue;

public enum IndexFieldType implements HasDisplayValue {
    ID("Id", true),
    BOOLEAN_FIELD("Boolean", false),
    INTEGER_FIELD("Integer", true),
    LONG_FIELD("Long", true),
    FLOAT_FIELD("Float", true),
    DOUBLE_FIELD("Double", true),
    DATE_FIELD("Date", false),
    FIELD("Text", false),
    NUMERIC_FIELD("Number", true); // Alias for LONG_FIELD.

    private final String displayValue;
    private final boolean numeric;

    IndexFieldType(final String displayValue, final boolean numeric) {
        this.displayValue = displayValue;
        this.numeric = numeric;
    }

    public boolean isNumeric() {
        return numeric;
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }
}
