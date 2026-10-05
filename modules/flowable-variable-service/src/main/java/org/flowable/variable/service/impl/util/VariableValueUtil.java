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
package org.flowable.variable.service.impl.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HasUnresolvableValue;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves variable values leniently: a variable whose value cannot be resolved (e.g. its serialized value can no longer be read) must
 * not fail reading the other variables. Such a variable is kept with a {@code null} value and is marked as having an unresolvable value
 * (see {@link HasUnresolvableValue}), so reading its value again returns {@code null} instead of failing again.
 */
public class VariableValueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableValueUtil.class);

    /**
     * Returns the values of the passed variables by name. A variable whose value cannot be resolved, or a name without a variable
     * (a {@code null} variable), gets a {@code null} value.
     */
    public static Map<String, Object> resolveValues(Map<String, VariableInstance> variableInstances) {
        Map<String, Object> variables = new HashMap<>(variableInstances.size());
        for (Map.Entry<String, VariableInstance> variableEntry : variableInstances.entrySet()) {
            variables.put(variableEntry.getKey(), resolveValue(variableEntry.getValue()));
        }
        return variables;
    }

    /**
     * Returns the value of the passed variable, or {@code null} when there is no variable or when its value cannot be resolved.
     */
    public static Object resolveValue(VariableInstance variableInstance) {
        if (variableInstance == null) {
            return null;
        }

        try {
            return variableInstance.getValue();
        } catch (RuntimeException e) {
            if (variableInstance instanceof HasUnresolvableValue variable) {
                variable.setValueUnresolvable(true);
            }
            logUnresolvableValue(variableInstance.getName(), variableInstance.getId(), variableInstance.getTypeName(), e);
            return null;
        }
    }

    /**
     * Resolves the value of the passed variable entity, the variable is marked when its value cannot be resolved.
     */
    public static void initializeValue(VariableInstanceEntity variableInstance) {
        resolveValue(variableInstance);
    }

    /**
     * Resolves the value of the passed historic variable entity, the variable is marked when its value cannot be resolved.
     */
    public static void initializeValue(HistoricVariableInstanceEntity historicVariableInstance) {
        try {
            historicVariableInstance.getValue();
        } catch (RuntimeException e) {
            historicVariableInstance.setValueUnresolvable(true);
            logUnresolvableValue(historicVariableInstance.getName(), historicVariableInstance.getId(), historicVariableInstance.getVariableTypeName(), e);
        }
    }

    /**
     * Returns whether the value of the passed variable could not be resolved when it was read leniently, its value is null in that case.
     */
    public static boolean isValueUnresolvable(Object variable) {
        return variable instanceof HasUnresolvableValue hasUnresolvableValue && hasUnresolvableValue.isValueUnresolvable();
    }

    /**
     * Returns the names of the passed variables whose value could not be resolved.
     */
    public static Set<String> getUnresolvableVariableNames(Collection<?> variables) {
        Set<String> unresolvableVariableNames = new HashSet<>();
        if (variables != null) {
            for (Object variable : variables) {
                if (isValueUnresolvable(variable)) {
                    unresolvableVariableNames.add(((HasUnresolvableValue) variable).getName());
                }
            }
        }
        return unresolvableVariableNames;
    }

    protected static void logUnresolvableValue(String name, String id, String typeName, RuntimeException exception) {
        LOGGER.warn("Could not resolve the value of variable '{}' ({}) of type '{}', the variable is returned with a null value",
                name, id, typeName, exception);
    }
}
