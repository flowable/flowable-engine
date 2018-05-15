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
package org.activiti.engine.impl.persistence.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.form.StartFormHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.runtime.InterpretableExecution;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.IdentityLinkType;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class ProcessDefinitionEntity extends ProcessDefinitionImpl implements ProcessDefinition, PersistentObject, HasRevision {

    private static final long serialVersionUID = 1L;

    protected String key;
    protected int revision = 1;
    protected int version;
    protected String category;
    protected String deploymentId;
    protected String resourceName;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected Integer historyLevel;
    protected StartFormHandler startFormHandler;
    protected String diagramResourceName;
    protected boolean isGraphicalNotationDefined;
    protected Map<String, TaskDefinition> taskDefinitions;
    protected Map<String, Object> variables;
    protected boolean hasStartFormKey;
    protected int suspensionState = SuspensionState.ACTIVE.getStateCode();
    protected boolean isIdentityLinksInitialized;
    protected List<IdentityLinkEntity> definitionIdentityLinkEntities = new ArrayList<>();
    protected Set<Expression> candidateStarterUserIdExpressions = new HashSet<>();
    protected Set<Expression> candidateStarterGroupIdExpressions = new HashSet<>();

    // Backwards compatibility
    protected String engineVersion;

    public ProcessDefinitionEntity() {
        super(null);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public ExecutionEntity createProcessInstance(String businessKey, ActivityImpl initial) {
        ExecutionEntity processInstance = null;

        if (initial == null) {
            processInstance = (ExecutionEntity) super.createProcessInstance();
        } else {
            processInstance = (ExecutionEntity) super.createProcessInstanceForInitial(initial);
        }

        processInstance.setExecutions(new ArrayList<>());
        processInstance.setProcessDefinition(processDefinition);
        // Do not initialize variable map (let it happen lazily)

        // Set business key (if any)
        if (businessKey != null) {
            processInstance.setBusinessKey(businessKey);
        }

        // Inherit tenant id (if any)
        if (getTenantId() != null) {
            processInstance.setTenantId(getTenantId());
        }

        // Reset the process instance in order to have the db-generated process instance id available
        processInstance.setProcessInstance(processInstance);

        // initialize the template-defined data objects as variables first
        Map<String, Object> dataObjectVars = getVariables();
        if (dataObjectVars != null) {
            processInstance.setVariables(dataObjectVars);
        }

        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        String initiatorVariableName = (String) getProperty(BpmnParse.PROPERTYNAME_INITIATOR_VARIABLE_NAME);
        if (initiatorVariableName != null) {
            processInstance.setVariable(initiatorVariableName, authenticatedUserId);
        }
        if (authenticatedUserId != null) {
            processInstance.addIdentityLink(authenticatedUserId, null, IdentityLinkType.STARTER);
        }

        Context.getCommandContext().getHistoryManager()
                .recordProcessInstanceStart(processInstance);

        if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processInstance));
        }

        return processInstance;
    }

    public ExecutionEntity createProcessInstance(String businessKey) {
        return createProcessInstance(businessKey, null);
    }

    @Override
    public ExecutionEntity createProcessInstance() {
        return createProcessInstance(null);
    }

    @Override
    protected InterpretableExecution newProcessInstance(ActivityImpl activityImpl) {
        ExecutionEntity processInstance = new ExecutionEntity(activityImpl);
        processInstance.insert();
        return processInstance;
    }

    public IdentityLinkEntity addIdentityLink(String userId, String groupId) {
        IdentityLinkEntity identityLinkEntity = new IdentityLinkEntity();
        getIdentityLinks().add(identityLinkEntity);
        identityLinkEntity.setProcessDef(this);
        identityLinkEntity.setUserId(userId);
        identityLinkEntity.setGroupId(groupId);
        identityLinkEntity.setType(IdentityLinkType.CANDIDATE);
        identityLinkEntity.insert();
        return identityLinkEntity;
    }

    public void deleteIdentityLink(String userId, String groupId) {
        List<IdentityLinkEntity> identityLinks = Context
                .getCommandContext()
                .getIdentityLinkEntityManager()
                .findIdentityLinkByProcessDefinitionUserAndGroup(id, userId, groupId);

        for (IdentityLinkEntity identityLink : identityLinks) {
            Context
                    .getCommandContext()
                    .getIdentityLinkEntityManager()
                    .deleteIdentityLink(identityLink, false);
        }
    }

    public List<IdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            definitionIdentityLinkEntities = Context
                    .getCommandContext()
                    .getIdentityLinkEntityManager()
                    .findIdentityLinksByProcessDefinitionId(id);
            isIdentityLinksInitialized = true;
        }

        return definitionIdentityLinkEntities;
    }

    @Override
    public String toString() {
        return "ProcessDefinitionEntity[" + id + "]";
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("suspensionState", this.suspensionState);
        persistentState.put("category", this.category);
        return persistentState;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    
    // only valid in Flowable 6
    @Override
    public String getDerivedFrom() {
        return null;
    }

    @Override
    public String getDerivedFromRoot() {
        return null;
    }

    @Override
    public int getDerivedVersion() {
        return 0;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getHistoryLevel() {
        return historyLevel;
    }

    public void setHistoryLevel(Integer historyLevel) {
        this.historyLevel = historyLevel;
    }

    public StartFormHandler getStartFormHandler() {
        return startFormHandler;
    }

    public void setStartFormHandler(StartFormHandler startFormHandler) {
        this.startFormHandler = startFormHandler;
    }

    public Map<String, TaskDefinition> getTaskDefinitions() {
        return taskDefinitions;
    }

    public void setTaskDefinitions(Map<String, TaskDefinition> taskDefinitions) {
        this.taskDefinitions = taskDefinitions;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getDiagramResourceName() {
        return diagramResourceName;
    }

    public void setDiagramResourceName(String diagramResourceName) {
        this.diagramResourceName = diagramResourceName;
    }

    @Override
    public boolean hasStartFormKey() {
        return hasStartFormKey;
    }

    public boolean getHasStartFormKey() {
        return hasStartFormKey;
    }

    public void setStartFormKey(boolean hasStartFormKey) {
        this.hasStartFormKey = hasStartFormKey;
    }

    public void setHasStartFormKey(boolean hasStartFormKey) {
        this.hasStartFormKey = hasStartFormKey;
    }

    public boolean isGraphicalNotationDefined() {
        return isGraphicalNotationDefined;
    }

    @Override
    public boolean hasGraphicalNotation() {
        return isGraphicalNotationDefined;
    }

    public void setGraphicalNotationDefined(boolean isGraphicalNotationDefined) {
        this.isGraphicalNotationDefined = isGraphicalNotationDefined;
    }

    @Override
    public int getRevision() {
        return revision;
    }

    @Override
    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Override
    public int getRevisionNext() {
        return revision + 1;
    }

    public int getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    @Override
    public boolean isSuspended() {
        return suspensionState == SuspensionState.SUSPENDED.getStateCode();
    }

    public Set<Expression> getCandidateStarterUserIdExpressions() {
        return candidateStarterUserIdExpressions;
    }

    public void addCandidateStarterUserIdExpression(Expression userId) {
        candidateStarterUserIdExpressions.add(userId);
    }

    public Set<Expression> getCandidateStarterGroupIdExpressions() {
        return candidateStarterGroupIdExpressions;
    }

    public void addCandidateStarterGroupIdExpression(Expression groupId) {
        candidateStarterGroupIdExpressions.add(groupId);
    }

    @Override
    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }
}
