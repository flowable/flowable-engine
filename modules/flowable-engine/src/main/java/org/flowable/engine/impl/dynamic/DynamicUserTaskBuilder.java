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

public class DynamicUserTaskBuilder {

    protected String id;
    protected String name;
    protected String assignee;
    protected DynamicUserTaskCallback dynamicUserTaskCallback;
    protected String dynamicTaskId;
    protected int counter = 1;

    public DynamicUserTaskBuilder() {

    }

    public DynamicUserTaskBuilder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DynamicUserTaskBuilder id(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DynamicUserTaskBuilder name(String name) {
        this.name = name;
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public DynamicUserTaskBuilder assignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public DynamicUserTaskCallback getDynamicUserTaskCallback() {
        return dynamicUserTaskCallback;
    }

    public void setDynamicUserTaskCallback(DynamicUserTaskCallback dynamicUserTaskCallback) {
        this.dynamicUserTaskCallback = dynamicUserTaskCallback;
    }

    public DynamicUserTaskBuilder dynamicUserTaskCallback(DynamicUserTaskCallback dynamicUserTaskCallback) {
        this.dynamicUserTaskCallback = dynamicUserTaskCallback;
        return this;
    }
    
    public String getDynamicTaskId() {
        return dynamicTaskId;
    }

    public void setDynamicTaskId(String dynamicTaskId) {
        this.dynamicTaskId = dynamicTaskId;
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