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
package org.flowable.engine.impl.cmd;

import java.util.Collection;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.api.Task;

/**
 * @author Christopher Welsch
 */
public class BulkSaveTasksCmd implements Command<Void> {

    protected Collection<Task> taskEntities;

    public BulkSaveTasksCmd(Collection<Task> taskList) {
        this.taskEntities = taskList;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (taskEntities == null) {
            throw new FlowableIllegalArgumentException("tasks are null");
        }
        for (Task task : taskEntities) {
            SaveTaskCmd command = new SaveTaskCmd(task);
            command.execute(commandContext);
        }
        return null;
    }

}
