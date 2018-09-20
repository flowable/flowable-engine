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
package org.flowable.engine.impl.dynamic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Dennis
 */
//Auxiliary class to kick off a changeActivityState / processMigration and store its state
public class ProcessInstanceChangeState {

    protected String processInstanceId;
    protected ProcessDefinition processDefinitionToMigrateTo;
    protected Map<String, Object> processVariables = new HashMap<>();
    protected Map<String, Map<String, Object>> localVariables = new HashMap<>();
    protected Map<String, List<ExecutionEntity>> processInstanceActiveEmbeddedExecutions;
    protected List<MoveExecutionEntityContainer> moveExecutionEntityContainers;
    protected HashMap<String, ExecutionEntity> createdEmbeddedSubProcess = new HashMap<>();

    public ProcessInstanceChangeState() {
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceChangeState setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public Optional<ProcessDefinition> getProcessDefinitionToMigrateTo() {
        return Optional.ofNullable(processDefinitionToMigrateTo);
    }

    public ProcessInstanceChangeState setProcessDefinitionToMigrateTo(ProcessDefinition processDefinitionToMigrateTo) {
        this.processDefinitionToMigrateTo = processDefinitionToMigrateTo;
        return this;
    }

    public boolean isMigrateToProcessDefinition() {
        return getProcessDefinitionToMigrateTo().isPresent();
    }

    public Map<String, Object> getProcessInstanceVariables() {
        return processVariables;
    }

    public ProcessInstanceChangeState setProcessInstanceVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
        return this;
    }

    public Map<String, Map<String, Object>> getLocalVariables() {
        return localVariables;
    }

    public ProcessInstanceChangeState setLocalVariables(Map<String, Map<String, Object>> localVariables) {
        this.localVariables = localVariables;
        return this;
    }

    public List<MoveExecutionEntityContainer> getMoveExecutionEntityContainers() {
        return moveExecutionEntityContainers;
    }

    public ProcessInstanceChangeState setMoveExecutionEntityContainers(List<MoveExecutionEntityContainer> moveExecutionEntityContainers) {
        this.moveExecutionEntityContainers = moveExecutionEntityContainers;
        return this;
    }

    public HashMap<String, ExecutionEntity> getCreatedEmbeddedSubProcesses() {
        return createdEmbeddedSubProcess;
    }

    public Optional<ExecutionEntity> getCreatedEmbeddedSubProcessByKey(String key) {
        return Optional.ofNullable(createdEmbeddedSubProcess.get(key));
    }

    public void addCreatedEmbeddedSubProcess(String key, ExecutionEntity executionEntity) {
        this.createdEmbeddedSubProcess.put(key, executionEntity);
    }

    public Map<String, List<ExecutionEntity>> getProcessInstanceActiveEmbeddedExecutions() {
        return processInstanceActiveEmbeddedExecutions;
    }

    public ProcessInstanceChangeState setProcessInstanceActiveEmbeddedExecutions(Map<String, List<ExecutionEntity>> processInstanceActiveEmbeddedExecutions) {
        this.processInstanceActiveEmbeddedExecutions = processInstanceActiveEmbeddedExecutions;
        return this;
    }

}
