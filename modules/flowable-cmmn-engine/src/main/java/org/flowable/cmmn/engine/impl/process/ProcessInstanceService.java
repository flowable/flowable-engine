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
package org.flowable.cmmn.engine.impl.process;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.model.IOParameter;

/**
 * @author Joram Barrez
 */
public interface ProcessInstanceService {
    
    String generateNewProcessInstanceId();

    String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String tenantId, 
                    Boolean fallbackToDefaultTenant, Map<String, Object> inParametersMap);

    String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String planItemInstanceId,
                    String tenantId, Boolean fallbackToDefaultTenant, Map<String, Object> inParametersMap);
    
    void triggerCaseTask(String executionId, Map<String, Object> variables);
    
    List<IOParameter> getOutputParametersOfCaseTask(String executionId);

    void deleteProcessInstance(String processInstanceId);

    Map<String, Object> getVariables(String executionId);

}
