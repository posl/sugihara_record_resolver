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

import java.text.ParseException;
import java.util.List;
import java.util.Stack;

class ExpressionValidator {

    public void validate(final List<ExpressionTokeniser.Token> tokens) throws ParseException {
        // Ensure there are no unidentified tokens.
        for (final ExpressionTokeniser.Token token : tokens) {
            if (ExpressionTokeniser.Token.Type.UNIDENTIFIED.equals(token.getType())) {
                throw new ParseException("Unexpected expression content '" + token.toString() + "'", token.getStart());
            }
        }

        // Ensure all opened functions are closed.
        final Stack<ExpressionTokeniser.Token> functionStack = new Stack<>();
        for (final ExpressionTokeniser.Token token : tokens) {
            if (ExpressionTokeniser.Token.Type.FUNCTION_START.equals(token.getType())) {
                functionStack.push(token);
            } else if (ExpressionTokeniser.Token.Type.FUNCTION_END.equals(token.getType())) {
                if (functionStack.size() == 0) {
                    throw new ParseException("Unexpected close bracket found", token.getStart());
                } else {
                    functionStack.pop();
                }
            }
        }

        if (functionStack.size() > 0) {
            final ExpressionTokeniser.Token token = functionStack.pop();
            throw new ParseException(
                    "No close bracket found for function '" + token.toString() + "'", token.getStart());
        }
    }
}
