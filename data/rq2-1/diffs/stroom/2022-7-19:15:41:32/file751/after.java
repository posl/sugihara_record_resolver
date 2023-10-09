/*
 * Copyright 2016 Crown Copyright
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

import stroom.dashboard.expression.v1.ExpressionTokeniser.Token;

import java.text.ParseException;

public class ParamFactory {

    public Param create(final FieldIndex fieldIndexMap, final Token token) throws ParseException {
        final String value = token.toString();

        // Token should be string or number or field.
        switch (token.getType()) {
            case STRING:
                return ValString.create(StringUtil.unescape(value));

            case NUMBER:
                return ValDouble.create(Double.valueOf(value));

            case FIELD:
                final String fieldName = value.substring(2, value.length() - 1);
                final int fieldIndex = fieldIndexMap.create(fieldName);
                return new Ref(value, fieldIndex);

            default:
                throw new ParseException("Unexpected token type '" + token.getType() + "'", token.getStart());
        }
    }
}
