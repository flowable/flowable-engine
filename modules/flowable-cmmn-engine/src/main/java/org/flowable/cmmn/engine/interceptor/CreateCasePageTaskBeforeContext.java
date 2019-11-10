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
package org.flowable.cmmn.engine.interceptor;

import java.util.List;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.CasePageTask;


public class CreateCasePageTaskBeforeContext {

    protected CasePageTask casePageTask;
    protected PlanItemInstanceEntity planItemInstanceEntity;
    protected String formKey;
    protected String assignee;
    protected String owner;
    protected List<String> candidateUsers;
    protected List<String> candidateGroups;
    
    public CreateCasePageTaskBeforeContext() {
        
    }

    public CreateCasePageTaskBeforeContext(CasePageTask casePageTask, PlanItemInstanceEntity planItemInstanceEntity, 
                    String formKey, String assignee, String owner, List<String> candidateUsers, List<String> candidateGroups) {

        this.casePageTask = casePageTask;
        this.planItemInstanceEntity = planItemInstanceEntity;
        this.formKey = formKey;
        this.assignee = assignee;
        this.owner = owner;
        this.candidateUsers = candidateUsers;
        this.candidateGroups = candidateGroups;
    }

    public CasePageTask getCasePageTask() {
        return casePageTask;
    }

    public void setCasePageTask(CasePageTask casePageTask) {
        this.casePageTask = casePageTask;
    }

    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public void setPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity) {
        this.planItemInstanceEntity = planItemInstanceEntity;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }
}
