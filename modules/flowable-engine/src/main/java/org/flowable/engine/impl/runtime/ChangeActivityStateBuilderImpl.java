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
package org.flowable.engine.impl.runtime;

import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateBuilderImpl implements ChangeActivityStateBuilder {

    protected RuntimeServiceImpl runtimeService;

    protected String processInstanceId;
    protected String executionId;
    protected String cancelActivityId;
    protected String startActivityId;

    public ChangeActivityStateBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ChangeActivityStateBuilder processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }
    
    @Override
    public ChangeActivityStateBuilder executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    @Override
    public ChangeActivityStateBuilder cancelActivityId(String cancelActivityId) {
        this.cancelActivityId = cancelActivityId;
        return this;
    }

    @Override
    public ChangeActivityStateBuilder startActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
        return this;
    }

    @Override
    public void changeState() {
        runtimeService.changeActivityState(this);
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    public String getExecutionId() {
        return executionId;
    }

    public String getCancelActivityId() {
        return cancelActivityId;
    }

    public String getStartActivityId() {
        return startActivityId;
    }
}
