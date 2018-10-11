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

import java.util.Map;

/**
 * @author Joram Barrez
 */
public interface ProcessInstanceService {
    
    String startProcessInstanceByKey(String processDefinitionKey, String tenantId, Map<String, Object> inParametersMap);

    String startProcessInstanceByKey(String processDefinitionKey, String planItemInstanceId, String tenantId, Map<String, Object> inParametersMap);

    void deleteProcessInstance(String processInstanceId);

    Map<String, Object> getVariables(String executionId);

}
