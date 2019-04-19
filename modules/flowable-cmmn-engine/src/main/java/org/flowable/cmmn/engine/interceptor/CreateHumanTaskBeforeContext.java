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
import org.flowable.cmmn.model.HumanTask;


public class CreateHumanTaskBeforeContext {

    protected HumanTask humanTask;
    protected PlanItemInstanceEntity planItemInstanceEntity;
    protected String name;
    protected String description;
    protected String dueDate;
    protected String priority;
    protected String category;
    protected String formKey;
    protected String assignee;
    protected String owner;
    protected List<String> candidateUsers;
    protected List<String> candidateGroups;
    
    public CreateHumanTaskBeforeContext() {
        
    }

    public CreateHumanTaskBeforeContext(HumanTask humanTask, PlanItemInstanceEntity planItemInstanceEntity, String name, String description, String dueDate, 
                    String priority, String category, String formKey, String assignee, String owner, 
                    List<String> candidateUsers, List<String> candidateGroups) {

        this.humanTask = humanTask;
        this.planItemInstanceEntity = planItemInstanceEntity;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.formKey = formKey;
        this.assignee = assignee;
        this.owner = owner;
        this.candidateUsers = candidateUsers;
        this.candidateGroups = candidateGroups;
    }

    public HumanTask getHumanTask() {
        return humanTask;
    }

    public void setHumanTask(HumanTask humanTask) {
        this.humanTask = humanTask;
    }

    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public void setPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity) {
        this.planItemInstanceEntity = planItemInstanceEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
