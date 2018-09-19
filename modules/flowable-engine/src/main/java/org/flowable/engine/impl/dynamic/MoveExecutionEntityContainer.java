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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;

public class MoveExecutionEntityContainer {

    protected List<ExecutionEntity> executions;
    protected List<String> moveToActivityIds;
    protected boolean moveToParentProcess;
    protected boolean moveToSubProcessInstance;
    protected boolean directExecutionMigration;
    protected String callActivityId;
    protected CallActivity callActivity;
    protected ProcessDefinition subProcessDefinition;
    protected BpmnModel subProcessModel;
    protected BpmnModel processModel;
    protected ExecutionEntity superExecution;
    protected Map<String, ExecutionEntity> continueParentExecutionMap = new HashMap<>();
    protected Map<String, FlowElementMoveEntry> moveToFlowElementMap = new HashMap<>();

    public MoveExecutionEntityContainer(List<ExecutionEntity> executions, List<String> moveToActivityIds) {
        this.executions = executions;
        this.moveToActivityIds = moveToActivityIds;
    }

    public List<ExecutionEntity> getExecutions() {
        return executions;
    }

    public List<String> getMoveToActivityIds() {
        return moveToActivityIds;
    }

    public boolean isMoveToParentProcess() {
        return moveToParentProcess;
    }

    public void setMoveToParentProcess(boolean moveToParentProcess) {
        this.moveToParentProcess = moveToParentProcess;
    }

    public boolean isMoveToSubProcessInstance() {
        return moveToSubProcessInstance;
    }

    public void setMoveToSubProcessInstance(boolean moveToSubProcessInstance) {
        this.moveToSubProcessInstance = moveToSubProcessInstance;
    }

    public boolean isDirectExecutionMigration() {
        return directExecutionMigration;
    }

    public void setDirectExecutionMigration(boolean directMigrateUserTask) {
        this.directExecutionMigration = directMigrateUserTask;
    }

    public String getCallActivityId() {
        return callActivityId;
    }

    public void setCallActivityId(String callActivityId) {
        this.callActivityId = callActivityId;
    }

    public CallActivity getCallActivity() {
        return callActivity;
    }

    public void setCallActivity(CallActivity callActivity) {
        this.callActivity = callActivity;
    }

    public ProcessDefinition getSubProcessDefinition() {
        return subProcessDefinition;
    }

    public void setSubProcessDefinition(ProcessDefinition subProcessDefinition) {
        this.subProcessDefinition = subProcessDefinition;
    }

    public BpmnModel getProcessModel() {
        return processModel;
    }

    public void setProcessModel(BpmnModel processModel) {
        this.processModel = processModel;
    }

    public BpmnModel getSubProcessModel() {
        return subProcessModel;
    }

    public void setSubProcessModel(BpmnModel subProcessModel) {
        this.subProcessModel = subProcessModel;
    }

    public ExecutionEntity getSuperExecution() {
        return superExecution;
    }

    public void setSuperExecution(ExecutionEntity superExecution) {
        this.superExecution = superExecution;
    }

    public void addContinueParentExecution(String executionId, ExecutionEntity continueParentExecution) {
        continueParentExecutionMap.put(executionId, continueParentExecution);
    }

    public ExecutionEntity getContinueParentExecution(String executionId) {
        return continueParentExecutionMap.get(executionId);
    }

    public void addMoveToFlowElement(String activityId, FlowElementMoveEntry flowElementMoveEntry) {
        moveToFlowElementMap.put(activityId, flowElementMoveEntry);
    }

    public void addMoveToFlowElement(String activityId, FlowElement originalFlowElement, FlowElement newFlowElement) {
        moveToFlowElementMap.put(activityId, new FlowElementMoveEntry(originalFlowElement, newFlowElement));
    }

    public void addMoveToFlowElement(String activityId, FlowElement originalFlowElement) {
        moveToFlowElementMap.put(activityId, new FlowElementMoveEntry(originalFlowElement, originalFlowElement));
    }

    public FlowElementMoveEntry getMoveToFlowElement(String activityId) {
        return moveToFlowElementMap.get(activityId);
    }

    public Collection<FlowElementMoveEntry> getMoveToFlowElements() {
        return moveToFlowElementMap.values();
    }

    public static class FlowElementMoveEntry {

        protected FlowElement originalFlowElement;
        protected FlowElement newFlowElement;

        public FlowElementMoveEntry(FlowElement originalFlowElement, FlowElement newFlowElement) {
            this.originalFlowElement = originalFlowElement;
            this.newFlowElement = newFlowElement;
        }

        public FlowElement getOriginalFlowElement() {
            return originalFlowElement;
        }

        public FlowElement getNewFlowElement() {
            return newFlowElement;
        }
    }
}
