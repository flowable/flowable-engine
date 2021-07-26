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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Case extends CmmnElement implements HasLifecycleListeners {

    protected String name;
    protected String initiatorVariableName;
    protected Stage planModel;
    protected String startEventType;
    protected List<String> candidateStarterUsers = new ArrayList<>();
    protected List<String> candidateStarterGroups = new ArrayList<>();
    protected boolean async;
    protected Map<String, CaseElement> allCaseElements = new HashMap<>();
    protected List<FlowableListener> lifecycleListeners = new ArrayList<>();
    /** Having a reactivation case listener marks the case eligible for reactivation once completed. */
    protected ReactivateEventListener reactivateEventListener;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInitiatorVariableName() {
        return initiatorVariableName;
    }

    public void setInitiatorVariableName(String initiatorVariableName) {
        this.initiatorVariableName = initiatorVariableName;
    }

    public Stage getPlanModel() {
        return planModel;
    }

    public void setPlanModel(Stage planModel) {
        this.planModel = planModel;
    }

    public String getStartEventType() {
        return startEventType;
    }

    public void setStartEventType(String startEventType) {
        this.startEventType = startEventType;
    }

    public List<String> getCandidateStarterUsers() {
        return candidateStarterUsers;
    }

    public void setCandidateStarterUsers(List<String> candidateStarterUsers) {
        this.candidateStarterUsers = candidateStarterUsers;
    }

    public List<String> getCandidateStarterGroups() {
        return candidateStarterGroups;
    }

    public void setCandidateStarterGroups(List<String> candidateStarterGroups) {
        this.candidateStarterGroups = candidateStarterGroups;
    }
    
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public Map<String, CaseElement> getAllCaseElements() {
        return allCaseElements;
    }

    public void setAllCaseElements(Map<String, CaseElement> allCaseElements) {
        this.allCaseElements = allCaseElements;
    }

    public <T extends PlanItemDefinition> List<T> findPlanItemDefinitionsOfType(Class<T> type) {
        return planModel.findPlanItemDefinitionsOfType(type, true);
    }

    public ReactivateEventListener getReactivateEventListener() {
        return reactivateEventListener;
    }

    public void setReactivateEventListener(ReactivateEventListener reactivateEventListener) {
        this.reactivateEventListener = reactivateEventListener;
    }

    @Override
    public List<FlowableListener> getLifecycleListeners() {
        return this.lifecycleListeners;
    }

    @Override
    public void setLifecycleListeners(List<FlowableListener> lifecycleListeners) {
        this.lifecycleListeners = lifecycleListeners;
    }
}
