/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.data.inmemory.util;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.service.impl.QueryOperator;
import org.flowable.variable.service.impl.QueryVariableValue;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * Common utilities used In-Memory data managers.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class QueryUtil {

    /**
     * Checks if requested and value are equal. Takes nulls into consideration
     * on both the requested and the value side. If both values are null they
     * are considered equal.
     *
     * <p>
     * Take care when using: if a check for the value should not be made at all
     * when 'requested' is null this method will yield incorrect results!
     *
     * @param requested
     *            The requested value
     * @param value
     *            The actual value
     * @return true if requested equals actual
     */
    public static boolean nullSafeEquals(Object requested, Object value) {
        if (requested == null) {
            if (value != null) {
                return false;
            }
            // both null
            return true;
        }
        return requested.equals(value);
    }

    /**
     * @param match
     *            if a query matched
     * @param orQuery
     *            if the query was an or query
     * @return Boolean.FALSE or Boolean.TRUE if the match should return a value,
     *         or null if the match should not return a value
     */
    public static Boolean matchReturn(Boolean match, boolean orQuery) {
        if (match == null) {
            return null;
        }
        if (!match && !orQuery) {
            return Boolean.FALSE;
        }
        if (match && orQuery) {
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * Remove SQL 'like' (%) from start and end of a string
     *
     * @param like
     *            String to remove from
     * @return resulting string
     */
    public static String stripLike(String like) {
        if (like == null) {
            return null;
        }
        if (like.charAt(0) == '%') {
            like = like.substring(1);
        }
        if (like.isEmpty()) {
            return like;
        }
        if (like.charAt(like.length() - 1) == '%') {
            like = like.substring(0, like.length() - 1);
        }
        return like;
    }

    /**
     * Perform a '%like%' query against given value.
     * 
     * @param like
     *            The query to match against value
     * @param target
     *            The value to match against
     * @return true if target contains the given 'like' value
     */
    public static Boolean queryLike(String like, String target) {
        return queryLike(like, target, false);
    }

    /**
     * Perform a '%like%' query against given value in a case insensitive
     * manner.
     * 
     * @param like
     *            The query to match against value
     * @param target
     *            The value to match against
     * @return true if target contains the given 'like' value
     */
    public static boolean queryLikeCaseInsensitive(String like, String target) {
        return queryLike(like, target, true);
    }

    private static boolean queryLike(String like, String target, boolean caseInsensitive) {
        if (caseInsensitive) {
            if (like != null) {
                like = like.toLowerCase();
            }
            if (target != null) {
                target = target.toLowerCase();
            }
        }
        return (target != null && target.contains(stripLike(like)));
    }

    /**
     * Apply a query to a list of variables.
     *
     * @param queryVariableValues
     *            The variable query to apply
     * @param localVariables
     *            Execution local variables
     * @param scopeVariables
     *            Execution scope variables
     * @param processVariables
     *            Execution process variables
     * @boolean isOrQuery Is the filtering happening inside an 'OR' query
     * @return Boolean.FALSE or Boolean.TRUE based on how the filter matched and
     *         if executing inside an or query, null if the filter did not match
     *         at all.
     */
    public static Boolean filterVariables(List<QueryVariableValue> queryVariableValues, List<VariableInstanceEntity> localVariables,
                    List<VariableInstanceEntity> scopeVariables, List<VariableInstanceEntity> processVariables, boolean isOrQuery) {
        Boolean retVal = null;

        for (QueryVariableValue queryVariableValue : queryVariableValues) {
            List<VariableInstanceEntity> variables;
            if (queryVariableValue.isLocal()) {
                variables = localVariables;
            } else if (queryVariableValue.getScopeType() == null || queryVariableValue.getScopeType().equals(ScopeTypes.BPMN)) {
                variables = processVariables;
            } else {
                variables = scopeVariables.stream().filter(v -> v.getScopeType() != null && v.getScopeType().equals(queryVariableValue.getScopeType()))
                                .collect(Collectors.toList());
            }
            switch (queryVariableValue.getOperator()) {
            case "EXISTS":
                retVal = matchReturn(variables.stream().anyMatch(var -> nullSafeEquals(queryVariableValue.getName(), var.getName())), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
                break;

            case "NOT_EXISTS":
                retVal = matchReturn(!variables.stream().anyMatch(var -> nullSafeEquals(queryVariableValue.getName(), var.getName())), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
                break;

            default:
                retVal = matchReturn(matchAnyVariable(variables, queryVariableValue), isOrQuery);
                if (retVal != null) {
                    return retVal;
                }
            }
        }

        return retVal;
    }

    /**
     * Check if the query matches the given variable
     * 
     * @param query
     *            Query to match with
     * @param value
     *            Variable to match against
     * @return true if query matches variable
     */
    public static boolean variableMatches(QueryVariableValue query, VariableInstanceEntity value) {
        switch (query.getOperator()) {
        case "EXISTS":
            return nullSafeEquals(query.getName(), value.getName());
        case "NOT_EXISTS":
            return !nullSafeEquals(query.getName(), value.getName());
        default:
            return matchVariable(value, query);
        }
    }

    private static boolean matchAnyVariable(List<VariableInstanceEntity> variables, QueryVariableValue queryVariableValue) {
        return variables.stream().anyMatch(var -> {
            return matchVariable(var, queryVariableValue);
        });
    }

    private static boolean matchVariable(VariableInstanceEntity var, QueryVariableValue queryVariableValue) {
        if (queryVariableValue.getName() != null && !nullSafeEquals(queryVariableValue.getName(), var.getName())) {
            return false;
        }

        if (queryVariableValue.needsTypeCheck() && !nullSafeEquals(queryVariableValue.getType(), var.getType() == null ? null : var.getType().getTypeName())) {
            return false;
        }

        QueryOperator op = QueryOperator.valueOf(queryVariableValue.getOperator());

        if (queryVariableValue.getTextValue() != null && queryVariableValue.getLongValue() == null && queryVariableValue.getDoubleValue() == null
                        && !matchTextVariable(op, queryVariableValue.getTextValue(), var.getTextValue())) {
            return false;
        }

        if (queryVariableValue.getTextValue2() != null && !matchTextVariable(op, queryVariableValue.getTextValue2(), var.getTextValue2())) {
            return false;
        }

        if (queryVariableValue.getLongValue() != null && !matchLongVariable(op, queryVariableValue.getLongValue(), var.getLongValue())) {
            return false;
        }

        if (queryVariableValue.getDoubleValue() != null && !matchDoubleVariable(op, queryVariableValue.getDoubleValue(), var.getDoubleValue())) {
            return false;
        }

        if (queryVariableValue.getTextValue() == null && queryVariableValue.getTextValue2() == null && queryVariableValue.getLongValue() == null
                        && queryVariableValue.getDoubleValue() == null) {
            // Null variable type
            if (op == QueryOperator.NOT_EQUALS) {
                // if all fields are set to null -> no match
                if (var.getTextValue() == null && var.getTextValue2() == null && var.getLongValue() == null && var.getDoubleValue() == null) {
                    return false;
                }
            } else {
                // if any field is set to non-null -> no match
                if (var.getTextValue() != null || var.getTextValue2() != null || var.getLongValue() != null || var.getDoubleValue() != null) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean matchTextVariable(QueryOperator op, String requested, String actual) {
        if (op == QueryOperator.EQUALS_IGNORE_CASE || op == QueryOperator.NOT_EQUALS_IGNORE_CASE || op == QueryOperator.LIKE_IGNORE_CASE) {
            requested = requested.toLowerCase();
            actual = actual == null ? null : actual.toLowerCase();
        }

        switch (op) {
        case EQUALS:
        case EQUALS_IGNORE_CASE:
            return nullSafeEquals(requested, actual);
        case EXISTS:
            return actual != null;
        case LIKE:
        case LIKE_IGNORE_CASE:
            return requested.contains(stripLike(actual));
        case NOT_EQUALS:
        case NOT_EQUALS_IGNORE_CASE:
            return !nullSafeEquals(requested, actual);
        case NOT_EXISTS:
            return actual == null;
        case GREATER_THAN:
        case GREATER_THAN_OR_EQUAL:
        case LESS_THAN:
        case LESS_THAN_OR_EQUAL:
            return false;
        default:
            throw new IllegalStateException("Attempt to filter Text variable with unknown operator " + op);
        }
    }

    private static boolean matchLongVariable(QueryOperator op, Long requested, Long actual) {
        switch (op) {
        case EQUALS:
        case EQUALS_IGNORE_CASE:
            if (actual == null) {
                return false;
            }
            return actual.equals(requested);
        case EXISTS:
            return actual != null;
        case GREATER_THAN:
            if (actual == null) {
                return false;
            }
            return actual > requested;
        case GREATER_THAN_OR_EQUAL:
            if (actual == null) {
                return false;
            }
            return actual >= requested;
        case LESS_THAN:
            if (actual == null) {
                return false;
            }
            return actual < requested;
        case LESS_THAN_OR_EQUAL:
            if (actual == null) {
                return false;
            }
            return actual <= requested;
        case LIKE:
        case LIKE_IGNORE_CASE:
            return false;
        case NOT_EQUALS:
        case NOT_EQUALS_IGNORE_CASE:
            if (actual == null) {
                return true;
            }
            return !actual.equals(requested);
        case NOT_EXISTS:
            return actual == null;
        default:
            throw new IllegalStateException("Attempt to filter Long variable with unknown operator " + op);
        }
    }

    private static boolean matchDoubleVariable(QueryOperator op, Double requested, Double actual) {
        switch (op) {
        case EQUALS:
        case EQUALS_IGNORE_CASE:
            if (actual == null) {
                return false;
            }
            return actual.equals(requested);
        case EXISTS:
            return actual != null;
        case GREATER_THAN:
            if (actual == null) {
                return false;
            }
            return actual > requested;
        case GREATER_THAN_OR_EQUAL:
            if (actual == null) {
                return false;
            }
            return actual >= requested;
        case LESS_THAN:
            if (actual == null) {
                return false;
            }
            return actual < requested;
        case LESS_THAN_OR_EQUAL:
            if (actual == null) {
                return false;
            }
            return actual <= requested;
        case LIKE:
        case LIKE_IGNORE_CASE:
            return false;
        case NOT_EQUALS:
        case NOT_EQUALS_IGNORE_CASE:
            if (actual == null) {
                return true;
            }
            return !actual.equals(requested);
        case NOT_EXISTS:
            return actual == null;
        default:
            throw new IllegalStateException("Attempt to filter Double variable with unknown operator " + op);
        }
    }
}
