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
package org.flowable.ui.task.model.runtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.AbstractRepresentation;
import org.flowable.ui.common.model.UserRepresentation;

/**
 * REST representation of a case instance.
 *
 * @author Tijs Rademakers
 */
public class CaseInstanceRepresentation extends AbstractRepresentation {

    protected String id;
    protected String name;
    protected String businessKey;
    protected String caseDefinitionId;
    protected String tenantId;
    protected Date started;
    protected Date ended;
    protected UserRepresentation startedBy;
    protected String caseDefinitionName;
    protected String caseDefinitionDescription;
    protected String caseDefinitionKey;
    protected String caseDefinitionCategory;
    protected int caseDefinitionVersion;
    protected String caseDefinitionDeploymentId;
    protected boolean graphicalNotationDefined;
    protected boolean startFormDefined;

    protected List<RestVariable> variables = new ArrayList<>();

    public CaseInstanceRepresentation(CaseInstance caseInstance, CaseDefinition caseDefinition, boolean graphicalNotation, User startedBy) {
        this(caseInstance, graphicalNotation, startedBy);
        mapCaseDefinition(caseDefinition);
    }

    public CaseInstanceRepresentation(CaseInstance caseInstance, boolean graphicalNotation, User startedBy) {
        this.id = caseInstance.getId();
        this.name = caseInstance.getName();
        this.businessKey = caseInstance.getBusinessKey();
        this.caseDefinitionId = caseInstance.getCaseDefinitionId();
        this.tenantId = caseInstance.getTenantId();
        this.graphicalNotationDefined = graphicalNotation;
        this.startedBy = startedBy != null ? new UserRepresentation(startedBy) : null;
    }

    public CaseInstanceRepresentation(HistoricCaseInstance caseInstance, CaseDefinition caseDefinition, boolean graphicalNotation, User startedBy) {
        this(caseInstance, graphicalNotation, startedBy);
        mapCaseDefinition(caseDefinition);
    }

    public CaseInstanceRepresentation(HistoricCaseInstance caseInstance, boolean graphicalNotation, User startedBy) {
        this.id = caseInstance.getId();
        this.name = caseInstance.getName();
        this.businessKey = caseInstance.getBusinessKey();
        this.caseDefinitionId = caseInstance.getCaseDefinitionId();
        this.tenantId = caseInstance.getTenantId();
        this.started = caseInstance.getStartTime();
        this.ended = caseInstance.getEndTime();
        this.graphicalNotationDefined = graphicalNotation;
        this.startedBy = startedBy != null ? new UserRepresentation(startedBy) : null;
    }

    protected void mapCaseDefinition(CaseDefinition caseDefinition) {
        if (caseDefinition != null) {
            this.caseDefinitionName = caseDefinition.getName();
            this.caseDefinitionDescription = caseDefinition.getDescription();
            this.caseDefinitionKey = caseDefinition.getKey();
            this.caseDefinitionCategory = caseDefinition.getCategory();
            this.caseDefinitionVersion = caseDefinition.getVersion();
            this.caseDefinitionDeploymentId = caseDefinition.getDeploymentId();
            this.startFormDefined = caseDefinition.hasStartFormKey();
        }
    }

    public CaseInstanceRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public UserRepresentation getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(UserRepresentation startedBy) {
        this.startedBy = startedBy;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getEnded() {
        return ended;
    }

    public void setEnded(Date ended) {
        this.ended = ended;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
    }

    public String getCaseDefinitionDescription() {
        return caseDefinitionDescription;
    }

    public void setCaseDefinitionDescription(String caseDefinitionDescription) {
        this.caseDefinitionDescription = caseDefinitionDescription;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }

    public void setCaseDefinitionCategory(String caseDefinitionCategory) {
        this.caseDefinitionCategory = caseDefinitionCategory;
    }

    public int getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public void setCaseDefinitionVersion(int caseDefinitionVersion) {
        this.caseDefinitionVersion = caseDefinitionVersion;
    }

    public String getCaseDefinitionDeploymentId() {
        return caseDefinitionDeploymentId;
    }

    public void setCaseDefinitionDeploymentId(String caseDefinitionDeploymentId) {
        this.caseDefinitionDeploymentId = caseDefinitionDeploymentId;
    }

    public List<RestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(RestVariable variable) {
        variables.add(variable);
    }

    public boolean isGraphicalNotationDefined() {
        return graphicalNotationDefined;
    }

    public void setGraphicalNotationDefined(boolean graphicalNotationDefined) {
        this.graphicalNotationDefined = graphicalNotationDefined;
    }

    public boolean isStartFormDefined() {
        return startFormDefined;
    }

    public void setStartFormDefined(boolean startFormDefined) {
        this.startFormDefined = startFormDefined;
    }
}
