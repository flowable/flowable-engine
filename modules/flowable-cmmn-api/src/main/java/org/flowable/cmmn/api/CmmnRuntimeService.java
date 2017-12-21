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
package org.flowable.cmmn.api;

import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;

/**
 * @author Joram Barrez
 */
public interface CmmnRuntimeService {

    CaseInstanceBuilder createCaseInstanceBuilder();
    
    void triggerPlanItemInstance(String planItemInstanceId);
    
    void terminateCaseInstance(String caseInstanceId);
    
    void evaluateCriteria(String caseInstanceId);
    
    Map<String, Object> getVariables(String caseInstanceId);
    
    Map<String, Object> getLocalVariables(String planItemInstanceId);
    
    Object getVariable(String caseInstanceId, String variableName);
    
    Object getLocalVariable(String planItemInstanceId, String variableName);
    
    void setVariables(String caseInstanceId, Map<String, Object> variables);
    
    void setLocalVariables(String planItemInstanceId, Map<String, Object> variables);
    
    void removeVariable(String caseInstanceId, String variableName);
    
    void removeLocalVariable(String planItemInstanceId, String variableName);
    
    CaseInstanceQuery createCaseInstanceQuery();
    
    PlanItemInstanceQuery createPlanItemInstanceQuery();
    
    MilestoneInstanceQuery createMilestoneInstanceQuery();
    
}
