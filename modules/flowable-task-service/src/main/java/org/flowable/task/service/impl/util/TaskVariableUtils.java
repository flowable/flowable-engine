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
package org.flowable.task.service.impl.util;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.api.types.ValueFields;

public class TaskVariableUtils {

    public static boolean isCaseRelated(ValueFields valueField) {
        return isCaseRelated(valueField.getScopeId(), valueField.getScopeType());
    }

    public static boolean isCaseRelated(TaskInfo task) {
        return isCaseRelated(task.getScopeId(), task.getScopeType());
    }

    private static boolean isCaseRelated(String scopeId, String scopeType) {
        return scopeId != null && ScopeTypes.CMMN.equals(scopeType);
    }

    public static boolean isProcessRelated(TaskInfo task) {
        return task.getProcessInstanceId() != null;
    }

    public static boolean doesVariableBelongToTask(ValueFields valueFields, TaskInfo taskInfo) {
        if (taskInfo.getProcessInstanceId() != null) {
            return taskInfo.getProcessInstanceId()
                    .equals(valueFields.getProcessInstanceId());
        }

        if (taskInfo.getScopeType() != null && valueFields.getScopeId() != null) {
            return taskInfo.getScopeType()
                    .equals(valueFields.getScopeType()) && valueFields.getScopeId()
                    .equals(taskInfo.getScopeId());
        }

        return false;
    }
}
