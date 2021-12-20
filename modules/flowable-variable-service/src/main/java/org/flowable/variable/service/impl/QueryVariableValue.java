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

package org.flowable.variable.service.impl;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.ByteArrayType;
import org.flowable.variable.service.impl.types.JPAEntityListVariableType;
import org.flowable.variable.service.impl.types.JPAEntityVariableType;
import org.flowable.variable.service.impl.types.NullType;

/**
 * Represents a variable value used in queries.
 * 
 * @author Frederik Heremans
 */
public class QueryVariableValue implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Object value;
    private QueryOperator operator;

    private VariableInstanceEntity variableInstanceEntity;
    private boolean local;

    private String scopeType;

    public QueryVariableValue(String name, Object value, QueryOperator operator, boolean local) {
        this.name = name;
        this.value = value;
        this.operator = operator;
        this.local = local;
    }

    public QueryVariableValue(String name, Object value, QueryOperator operator, boolean local, String scopeType) {
        this(name, value, operator, local);
        this.scopeType = scopeType;
    }

    public void initialize(VariableServiceConfiguration variableServiceConfiguration) {
        if (variableInstanceEntity == null) {
            VariableType type = variableServiceConfiguration.getVariableTypes().findVariableType(value);
            if (type instanceof ByteArrayType) {
                throw new FlowableIllegalArgumentException("Variables of type ByteArray cannot be used to query");
            } else if (type instanceof JPAEntityVariableType && operator != QueryOperator.EQUALS) {
                throw new FlowableIllegalArgumentException("JPA entity variables can only be used in 'variableValueEquals'");
            } else if (type instanceof JPAEntityListVariableType) {
                throw new FlowableIllegalArgumentException("Variables containing a list of JPA entities cannot be used to query");
            } else {
                // Type implementation determines which fields are set on the entity
                variableInstanceEntity = variableServiceConfiguration.getVariableInstanceEntityManager().create(name, type, value);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getOperator() {
        if (operator != null) {
            return operator.toString();
        }
        return QueryOperator.EQUALS.toString();
    }

    public String getTextValue() {
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getTextValue();
        }
        return null;
    }

    public Long getLongValue() {
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getLongValue();
        }
        return null;
    }

    public Double getDoubleValue() {
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getDoubleValue();
        }
        return null;
    }

    public String getTextValue2() {
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getTextValue2();
        }
        return null;
    }

    public String getType() {
        if (variableInstanceEntity != null) {
            return variableInstanceEntity.getType().getTypeName();
        }
        return null;
    }

    public boolean needsTypeCheck() {
        // When operator is not-equals or type of value is null, type doesn't matter!
        if (operator == QueryOperator.NOT_EQUALS || operator == QueryOperator.NOT_EQUALS_IGNORE_CASE) {
            return false;
        }

        if (variableInstanceEntity != null) {
            return !NullType.TYPE_NAME.equals(variableInstanceEntity.getType().getTypeName());
        }

        return false;
    }

    public boolean isLocal() {
        return local;
    }

    public String getScopeType() {
        return scopeType;
    }
}