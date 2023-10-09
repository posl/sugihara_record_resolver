package stroom.query.api.v2;

import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.BooleanField;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.DocRefField;
import stroom.datasource.api.v2.DoubleField;
import stroom.datasource.api.v2.FloatField;
import stroom.datasource.api.v2.IdField;
import stroom.datasource.api.v2.IntegerField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.TextField;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpressionUtil {

    private ExpressionUtil() {
        // Utility class.
    }

    public static ExpressionOperator equals(final String field, final String value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final BooleanField field, final boolean value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final DateField field, final String value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final DocRefField field, final DocRef value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.IS_DOC_REF, value)
                .build();
    }

    public static ExpressionOperator equals(final IdField field, final long value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final IntegerField field, final int value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final LongField field, final long value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final FloatField field, final float value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final DoubleField field, final double value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final TextField field, final String value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static ExpressionOperator equals(final String field, final DocRef value) {
        return ExpressionOperator.builder()
                .addTerm(field, Condition.EQUALS, value)
                .build();
    }

    public static int termCount(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).size();
    }

    public static int termCount(final ExpressionOperator expressionOperator, final AbstractField field) {
        return terms(expressionOperator, Collections.singleton(field)).size();
    }

    public static int termCount(final ExpressionOperator expressionOperator, final Collection<AbstractField> fields) {
        return terms(expressionOperator, fields).size();
    }

    public static List<String> fields(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).stream().map(ExpressionTerm::getField).collect(Collectors.toList());
    }

    public static List<String> fields(final ExpressionOperator expressionOperator, final AbstractField field) {
        return terms(expressionOperator, Collections.singleton(field)).stream().map(ExpressionTerm::getField).collect(
                Collectors.toList());
    }

    public static List<String> fields(final ExpressionOperator expressionOperator,
                                      final Collection<AbstractField> fields) {
        return terms(expressionOperator, fields).stream().map(ExpressionTerm::getField).collect(Collectors.toList());
    }

    public static List<String> values(final ExpressionOperator expressionOperator) {
        return terms(expressionOperator, null).stream().map(ExpressionTerm::getValue).collect(Collectors.toList());
    }

    public static List<String> values(final ExpressionOperator expressionOperator, final AbstractField field) {
        return terms(expressionOperator, Collections.singleton(field)).stream().map(ExpressionTerm::getValue).collect(
                Collectors.toList());
    }

    public static List<String> values(final ExpressionOperator expressionOperator,
                                      final Collection<AbstractField> fields) {
        return terms(expressionOperator, fields).stream().map(ExpressionTerm::getValue).collect(Collectors.toList());
    }

    public static List<ExpressionTerm> terms(final ExpressionOperator expressionOperator,
                                             final Collection<AbstractField> fields) {
        final List<ExpressionTerm> terms = new ArrayList<>();
        addTerms(expressionOperator, fields, terms);
        return terms;
    }

    private static void addTerms(final ExpressionOperator expressionOperator,
                                 final Collection<AbstractField> fields,
                                 final List<ExpressionTerm> terms) {
        if (expressionOperator != null &&
                expressionOperator.enabled() &&
                expressionOperator.getChildren() != null &&
                !Op.NOT.equals(expressionOperator.op())) {
            for (final ExpressionItem item : expressionOperator.getChildren()) {
                if (item.enabled()) {
                    if (item instanceof ExpressionTerm) {
                        final ExpressionTerm expressionTerm = (ExpressionTerm) item;
                        if (fields == null || fields.stream()
                                .anyMatch(field ->
                                        field.getName().equals(expressionTerm.getField()) &&
                                                (Condition.IS_DOC_REF.equals(expressionTerm.getCondition()) &&
                                                        expressionTerm.getDocRef() != null &&
                                                        expressionTerm.getDocRef().getUuid() != null) ||
                                                (expressionTerm.getValue() != null &&
                                                        expressionTerm.getValue().length() > 0))) {
                            terms.add(expressionTerm);
                        }
                    } else if (item instanceof ExpressionOperator) {
                        addTerms((ExpressionOperator) item, fields, terms);
                    }
                }
            }
        }
    }

    public static ExpressionOperator copyOperator(final ExpressionOperator operator) {
        if (operator == null) {
            return null;
        }

        final ExpressionOperator.Builder builder = ExpressionOperator
                .builder()
                .enabled(operator.getEnabled())
                .op(operator.getOp());
        if (operator.getChildren() != null) {
            operator.getChildren().forEach(item -> {
                if (item instanceof ExpressionOperator) {
                    builder.addOperator(copyOperator((ExpressionOperator) item));

                } else if (item instanceof ExpressionTerm) {
                    builder.addTerm(copyTerm((ExpressionTerm) item));
                }
            });
        }
        return builder.build();
    }

    public static ExpressionTerm copyTerm(final ExpressionTerm term) {
        if (term == null) {
            return null;
        }

        return ExpressionTerm
                .builder()
                .enabled(term.getEnabled())
                .field(term.getField())
                .condition(term.getCondition())
                .value(term.getValue())
                .docRef(term.getDocRef())
                .build();
    }

    public static SearchRequest replaceExpressionParameters(final SearchRequest searchRequest) {
        SearchRequest result = searchRequest;
        if (searchRequest != null && searchRequest.getQuery() != null) {
            final Query query = replaceExpressionParameters(searchRequest.getQuery());
            result = searchRequest.copy().query(query).build();
        }
        return result;
    }

    public static Query replaceExpressionParameters(final Query query) {
        Query result = query;
        if (query != null) {
            ExpressionOperator expression = query.getExpression();
            if (query.getParams() != null && expression != null) {
                final Map<String, String> paramMap = ParamUtil.createParamMap(query.getParams());
                expression = replaceExpressionParameters(expression, paramMap);
            }
            result = query.copy().expression(expression).build();
        }
        return result;
    }

    public static ExpressionOperator replaceExpressionParameters(final ExpressionOperator operator,
                                                                 final Map<String, String> paramMap) {
        final ExpressionOperator.Builder builder = ExpressionOperator
                .builder()
                .enabled(operator.getEnabled())
                .op(operator.getOp());
        if (operator.getChildren() != null) {
            for (ExpressionItem child : operator.getChildren()) {
                if (child instanceof ExpressionOperator) {
                    final ExpressionOperator childOperator = (ExpressionOperator) child;
                    builder.addOperator(replaceExpressionParameters(childOperator, paramMap));

                } else if (child instanceof ExpressionTerm) {
                    final ExpressionTerm term = (ExpressionTerm) child;
                    final String value = term.getValue();
                    final String replaced = ParamUtil.replaceParameters(value, paramMap);
                    builder.addTerm(ExpressionTerm.builder()
                            .enabled(term.enabled())
                            .field(term.getField())
                            .condition(term.getCondition())
                            .value(replaced)
                            .docRef(term.getDocRef())
                            .build());
                }
            }
        }
        return builder.build();
    }
}
