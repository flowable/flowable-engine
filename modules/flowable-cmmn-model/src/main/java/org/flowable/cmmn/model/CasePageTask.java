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

public class CasePageTask extends Task {

    public static final String TYPE = "casePage";
    
    protected String type;
    protected String formKey;
    protected boolean sameDeployment = true;
    protected String label;
    protected String icon;
    protected String assignee;
    protected String owner;
    protected List<String> candidateUsers = new ArrayList<>();
    protected List<String> candidateGroups = new ArrayList<>();

    public CasePageTask() {
        type = TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    @Override
    public CasePageTask clone() {
        CasePageTask clone = new CasePageTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(CasePageTask otherElement) {
        super.setValues(otherElement);
        
        setType(otherElement.getType());
        setFormKey(otherElement.getFormKey());
        setSameDeployment(otherElement.isSameDeployment());
        setLabel(otherElement.getLabel());
        setIcon(otherElement.getIcon());
        setAssignee(otherElement.getAssignee());
        setOwner(otherElement.getOwner());
        
        setCandidateGroups(new ArrayList<>(otherElement.getCandidateGroups()));
        setCandidateUsers(new ArrayList<>(otherElement.getCandidateUsers()));
    }

}
