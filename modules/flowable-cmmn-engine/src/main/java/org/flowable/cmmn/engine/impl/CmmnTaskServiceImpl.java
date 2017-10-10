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
package org.flowable.cmmn.engine.impl;

import java.util.Map;

import org.flowable.cmmn.engine.CmmnTaskService;
import org.flowable.cmmn.engine.impl.cmd.CompleteTaskCmd;
import org.flowable.task.service.TaskQuery;
import org.flowable.task.service.impl.TaskQueryImpl;

/**
 * @author Joram Barrez
 */
public class CmmnTaskServiceImpl extends ServiceImpl implements CmmnTaskService {

    @Override
    public void complete(String taskId) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, null, null));
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, variables, null));        
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, variables, transientVariables));        
    }
    
    @Override
    public TaskQuery createTaskQuery() {
        return new TaskQueryImpl(commandExecutor);
    }

}
