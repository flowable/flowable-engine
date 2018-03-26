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
package org.flowable.engine.impl.dynamic;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;

public class DynamicEmbeddedSubProcessBuilder {

    protected String id;
    protected String processDefinitionId;
    protected String dynamicSubProcessId;
    protected int counter = 1;

    public DynamicEmbeddedSubProcessBuilder() {

    }

    public DynamicEmbeddedSubProcessBuilder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DynamicEmbeddedSubProcessBuilder id(String id) {
        this.id = id;
        return this;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public DynamicEmbeddedSubProcessBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public String getDynamicSubProcessId() {
        return dynamicSubProcessId;
    }

    public void setDynamicSubProcessId(String dynamicSubProcessId) {
        this.dynamicSubProcessId = dynamicSubProcessId;
    }

    public String nextSubProcessId(Map<String, FlowElement> flowElementMap) {
        return nextId("dynamicSubProcess", flowElementMap);
    }
    
    public String nextTaskId(Map<String, FlowElement> flowElementMap) {
        return nextId("dynamicTask", flowElementMap);
    }
    
    public String nextFlowId(Map<String, FlowElement> flowElementMap) {
        return nextId("dynamicFlow", flowElementMap);
    }
    
    public String nextForkGatewayId(Map<String, FlowElement> flowElementMap) {
        return nextId("dynamicForkGateway", flowElementMap);
    }
    
    public String nextJoinGatewayId(Map<String, FlowElement> flowElementMap) {
        return nextId("dynamicJoinGateway", flowElementMap);
    }
    
    public String nextStartEventId(Map<String, FlowElement> flowElementMap) {
        return nextId("startEvent", flowElementMap);
    }
    
    public String nextEndEventId(Map<String, FlowElement> flowElementMap) {
        return nextId("endEvent", flowElementMap);
    }

    protected String nextId(String prefix, Map<String, FlowElement> flowElementMap) {
        String nextId = null;
        boolean nextIdNotFound = true;
        while (nextIdNotFound) {
            if (!flowElementMap.containsKey(prefix + counter)) {
                nextId = prefix + counter;
                nextIdNotFound = false;
            }
            
            counter++;
        }
        
        return nextId;
    }
}