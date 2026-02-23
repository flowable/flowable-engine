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
package org.flowable.cmmn.api.migration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivatePlanItemDefinitionMapping extends PlanItemDefinitionMapping {

    protected String newName;
    protected String newAssignee;
    protected String newOwner;
    protected String newFormKey;
    protected String newDueDate;
    protected String newPriority;
    protected String newCategory;
    protected List<String> newCandidateUsers;
    protected List<String> newCandidateGroups;
    protected Map<String, Object> withLocalVariables = new LinkedHashMap<>();

    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId, Map<String, Object> withLocalVariables) {
        super(planItemDefinitionId);
        this.withLocalVariables = withLocalVariables;
    }
    
    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId) {
        super(planItemDefinitionId);
    }

    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId, String condition) {
        super(planItemDefinitionId, condition);
    }

    public ActivatePlanItemDefinitionMapping(String planItemDefinitionId, String condition, Map<String, Object> withLocalVariables) {
        super(planItemDefinitionId, condition);
        this.withLocalVariables = withLocalVariables;
    }

    public String getNewName() {
        return newName;
    }

    public ActivatePlanItemDefinitionMapping withNewName(String newName) {
        this.newName = newName;
        return this;
    }

    public String getNewAssignee() {
        return newAssignee;
    }

    public ActivatePlanItemDefinitionMapping withNewAssignee(String newAssignee) {
        this.newAssignee = newAssignee;
        return this;
    }

    public String getNewOwner() {
        return newOwner;
    }

    public ActivatePlanItemDefinitionMapping withNewOwner(String newOwner) {
        this.newOwner = newOwner;
        return this;
    }

    public String getNewFormKey() {
        return newFormKey;
    }

    public ActivatePlanItemDefinitionMapping withNewFormKey(String newFormKey) {
        this.newFormKey = newFormKey;
        return this;
    }

    public String getNewDueDate() {
        return newDueDate;
    }

    public ActivatePlanItemDefinitionMapping withNewDueDate(String newDueDate) {
        this.newDueDate = newDueDate;
        return this;
    }

    public String getNewPriority() {
        return newPriority;
    }

    public ActivatePlanItemDefinitionMapping withNewPriority(String newPriority) {
        this.newPriority = newPriority;
        return this;
    }

    public String getNewCategory() {
        return newCategory;
    }

    public ActivatePlanItemDefinitionMapping withNewCategory(String newCategory) {
        this.newCategory = newCategory;
        return this;
    }

    public List<String> getNewCandidateUsers() {
        return newCandidateUsers;
    }

    public ActivatePlanItemDefinitionMapping withNewCandidateUsers(List<String> newCandidateUsers) {
        this.newCandidateUsers = newCandidateUsers;
        return this;
    }

    public List<String> getNewCandidateGroups() {
        return newCandidateGroups;
    }

    public ActivatePlanItemDefinitionMapping withNewCandidateGroups(List<String> newCandidateGroups) {
        this.newCandidateGroups = newCandidateGroups;
        return this;
    }

    public Map<String, Object> getWithLocalVariables() {
        return withLocalVariables;
    }

    public ActivatePlanItemDefinitionMapping withLocalVariables(Map<String, Object> withLocalVariables) {
        this.withLocalVariables = withLocalVariables;
        return this;
    }
}
