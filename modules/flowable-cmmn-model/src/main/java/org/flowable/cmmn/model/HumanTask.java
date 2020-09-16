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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tijs Rademakers
 */
public class HumanTask extends Task {

    protected String assignee;
    protected String owner;
    protected String priority;
    protected String formKey;
    protected boolean sameDeployment = true;
    protected String validateFormFields;
    protected String dueDate;
    protected String category;
    protected String taskIdVariableName;
    protected List<String> candidateUsers = new ArrayList<>();
    protected List<String> candidateGroups = new ArrayList<>();
    protected List<FlowableListener> taskListeners = new ArrayList<>();

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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
    }

    public String getValidateFormFields() {
        return validateFormFields;
    }

    public void setValidateFormFields(String validateFormFields) {
        this.validateFormFields = validateFormFields;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTaskIdVariableName() {
        return taskIdVariableName;
    }

    public void setTaskIdVariableName(String taskIdVariableName) {
        this.taskIdVariableName = taskIdVariableName;
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

    public List<FlowableListener> getTaskListeners() {
        return taskListeners;
    }

    public void setTaskListeners(List<FlowableListener> taskListeners) {
        this.taskListeners = taskListeners;
    }

    @Override
    public HumanTask clone() {
        HumanTask clone = new HumanTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(HumanTask otherElement) {
        super.setValues(otherElement);
        setAssignee(otherElement.getAssignee());
        setOwner(otherElement.getOwner());
        setFormKey(otherElement.getFormKey());
        setSameDeployment(otherElement.isSameDeployment());
        setValidateFormFields(otherElement.getValidateFormFields());
        setDueDate(otherElement.getDueDate());
        setPriority(otherElement.getPriority());
        setCategory(otherElement.getCategory());
        setTaskIdVariableName(otherElement.getTaskIdVariableName());

        setCandidateGroups(new ArrayList<>(otherElement.getCandidateGroups()));
        setCandidateUsers(new ArrayList<>(otherElement.getCandidateUsers()));
    }
}
