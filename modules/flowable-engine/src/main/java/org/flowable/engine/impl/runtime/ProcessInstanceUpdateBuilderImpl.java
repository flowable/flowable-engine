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

import java.util.Date;

import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.runtime.ProcessInstanceUpdateBuilder;

/**
 * @author Tijs Rademakers
 */
public class ProcessInstanceUpdateBuilderImpl implements ProcessInstanceUpdateBuilder {

    protected RuntimeServiceImpl runtimeService;
    protected String processInstanceId;
    protected String businessKey;
    protected boolean businessKeySet;
    protected String businessStatus;
    protected boolean businessStatusSet;
    protected String name;
    protected boolean nameSet;
    protected Date dueDate;
    protected boolean dueDateSet;

    public ProcessInstanceUpdateBuilderImpl(RuntimeServiceImpl runtimeService, String processInstanceId) {
        this.runtimeService = runtimeService;
        this.processInstanceId = processInstanceId;
    }

    @Override
    public ProcessInstanceUpdateBuilder businessKey(String businessKey) {
        this.businessKey = businessKey;
        this.businessKeySet = true;
        return this;
    }

    @Override
    public ProcessInstanceUpdateBuilder businessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
        this.businessStatusSet = true;
        return this;
    }

    @Override
    public ProcessInstanceUpdateBuilder name(String name) {
        this.name = name;
        this.nameSet = true;
        return this;
    }

    @Override
    public ProcessInstanceUpdateBuilder dueDate(Date dueDate) {
        this.dueDate = dueDate;
        this.dueDateSet = true;
        return this;
    }

    @Override
    public void update() {
        runtimeService.updateProcessInstance(this);
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public boolean isBusinessKeySet() {
        return businessKeySet;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public boolean isBusinessStatusSet() {
        return businessStatusSet;
    }

    public String getName() {
        return name;
    }

    public boolean isNameSet() {
        return nameSet;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isDueDateSet() {
        return dueDateSet;
    }
}
