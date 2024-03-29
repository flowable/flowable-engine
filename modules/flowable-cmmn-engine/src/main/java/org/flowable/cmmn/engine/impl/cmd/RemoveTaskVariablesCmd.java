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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Collection;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class RemoveTaskVariablesCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    private final Collection<String> variableNames;
    private final boolean isLocal;

    public RemoveTaskVariablesCmd(String taskId, Collection<String> variableNames, boolean isLocal) {
        super(taskId);
        this.variableNames = variableNames;
        this.isLocal = isLocal;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {

        if (isLocal) {
            task.removeVariablesLocal(variableNames);
        } else {
            task.removeVariables(variableNames);
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskExceptionPrefix() {
        return "Cannot remove variables from";
    }

}
