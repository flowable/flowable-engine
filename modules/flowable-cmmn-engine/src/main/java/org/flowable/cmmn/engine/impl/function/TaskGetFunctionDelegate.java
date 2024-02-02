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
package org.flowable.cmmn.engine.impl.function;

import java.lang.reflect.Method;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Filip Hrisafov
 */
public class TaskGetFunctionDelegate implements FlowableFunctionDelegate {

    @Override
    public String prefix() {
        return "task";
    }

    @Override
    public String localName() {
        return "get";
    }

    @Override
    public Method functionMethod() {
        try {
            return TaskGetFunctionDelegate.class.getDeclaredMethod("getTask", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find getTask function", e);
        }
    }

    public static TaskInfo getTask(String taskId) {
        TaskEntity task = CommandContextUtil.getTaskService().getTask(taskId);
        if (task != null) {
            return task;
        }
        return CommandContextUtil.getHistoricTaskService().getHistoricTask(taskId);
    }
}
