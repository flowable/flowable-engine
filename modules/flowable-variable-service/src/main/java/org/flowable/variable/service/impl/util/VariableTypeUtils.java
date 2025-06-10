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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class VariableTypeUtils {

    public static void validateMaxAllowedLength(int maxAllowedLength, int length, ValueFields valueFields, VariableType variableType) {
        if (maxAllowedLength < 0) {
            return;
        }

        if (maxAllowedLength > 0 && length > maxAllowedLength) {
            String scopeId;
            String scopeType;
            if (StringUtils.isNotEmpty(valueFields.getTaskId())) {
                scopeId = valueFields.getTaskId();
                scopeType = ScopeTypes.TASK;
            } else if (StringUtils.isNotEmpty(valueFields.getProcessInstanceId())) {
                scopeId = valueFields.getProcessInstanceId();
                scopeType = ScopeTypes.BPMN;
            } else {
                scopeId = valueFields.getScopeId();
                scopeType = valueFields.getScopeType();
            }
            throw new FlowableIllegalArgumentException(
                    "The length of the " + variableType.getTypeName() + " value exceeds the maximum allowed length of " + maxAllowedLength
                            + " characters. Current length: " + length
                            + ", for variable: " + valueFields.getName() + " in scope " + scopeType + " with id " + scopeId);
        }
    }

}
