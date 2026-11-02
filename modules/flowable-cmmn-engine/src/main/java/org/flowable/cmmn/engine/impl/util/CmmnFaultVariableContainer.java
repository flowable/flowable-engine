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
package org.flowable.cmmn.engine.impl.util;

import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * A dedicated {@link VariableContainer} for fault data, analogous to BPMN's {@code BpmnErrorVariableContainer}.
 *
 * This container provides fault-specific variables (faultCode, faultMessage, error) for expression
 * resolution during sentry if-part evaluation, without polluting the actual variable scope of the
 * plan item instance.
 *
 * @author Joram Barrez
 */
public class CmmnFaultVariableContainer implements VariableContainer {

    public static final String FAULT_CODE_VARIABLE_NAME = "faultCode";
    public static final String FAULT_MESSAGE_VARIABLE_NAME = "faultMessage";
    public static final String ERROR_VARIABLE_NAME = "error";

    protected final BusinessError error;
    protected final VariableContainer delegateContainer;

    /**
     * @param error the business error
     * @param delegateContainer the plan item instance variable container to delegate to for non-fault variables
     */
    public CmmnFaultVariableContainer(BusinessError error, VariableContainer delegateContainer) {
        this.error = error;
        this.delegateContainer = delegateContainer;
    }

    @Override
    public boolean hasVariable(String variableName) {
        if (FAULT_CODE_VARIABLE_NAME.equals(variableName)
                || FAULT_MESSAGE_VARIABLE_NAME.equals(variableName)
                || ERROR_VARIABLE_NAME.equals(variableName)) {
            return true;
        }
        if (error.getAdditionalDataContainer() != null && error.getAdditionalDataContainer().hasVariable(variableName)) {
            return true;
        }
        return delegateContainer.hasVariable(variableName);
    }

    @Override
    public Object getVariable(String variableName) {
        if (FAULT_CODE_VARIABLE_NAME.equals(variableName)) {
            return error.getErrorCode();
        } else if (FAULT_MESSAGE_VARIABLE_NAME.equals(variableName)) {
            return error.getMessage();
        } else if (ERROR_VARIABLE_NAME.equals(variableName)) {
            return error;
        } else if (error.getAdditionalDataContainer() != null && error.getAdditionalDataContainer().hasVariable(variableName)) {
            return error.getAdditionalDataContainer().getVariable(variableName);
        }
        return delegateContainer.getVariable(variableName);
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {
        delegateContainer.setVariable(variableName, variableValue);
    }

    @Override
    public void setTransientVariable(String variableName, Object variableValue) {
        delegateContainer.setTransientVariable(variableName, variableValue);
    }

    @Override
    public String getTenantId() {
        return delegateContainer.getTenantId();
    }

    @Override
    public Set<String> getVariableNames() {
        Set<String> names = new HashSet<>(delegateContainer.getVariableNames());
        names.add(FAULT_CODE_VARIABLE_NAME);
        names.add(FAULT_MESSAGE_VARIABLE_NAME);
        names.add(ERROR_VARIABLE_NAME);
        if (error.getAdditionalDataContainer() != null) {
            names.addAll(error.getAdditionalDataContainer().getVariableNames());
        }
        return names;
    }

    public BusinessError getError() {
        return error;
    }
}
