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
package org.activiti.engine.dynamic;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Pardo David on 5/12/2016.
 */
public class UserTaskPropertiesParser extends BasePropertiesParser {

    @Override
    protected ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper) {
        UserTask userTask = (UserTask) flowElement;

        ObjectNode taskNameNode = objectMapper.createObjectNode();
        putPropertyValue(BPMN_MODEL_VALUE, userTask.getName(), taskNameNode);
        putPropertyValue(DYNAMIC_VALUE, flowElementNode.path(USER_TASK_NAME), taskNameNode);

        ObjectNode assigneeNode = objectMapper.createObjectNode();
        putPropertyValue(BPMN_MODEL_VALUE, userTask.getAssignee(), assigneeNode);
        putPropertyValue(DYNAMIC_VALUE, flowElementNode.path(USER_TASK_ASSIGNEE), assigneeNode);

        ObjectNode candidateUsersNode = objectMapper.createObjectNode();
        putPropertyValue(BPMN_MODEL_VALUE, userTask.getCandidateUsers(), candidateUsersNode);
        putPropertyValue(DYNAMIC_VALUE, flowElementNode.path(USER_TASK_CANDIDATE_USERS), candidateUsersNode);

        ObjectNode candidateGroupsNode = objectMapper.createObjectNode();
        putPropertyValue(BPMN_MODEL_VALUE, userTask.getCandidateGroups(), candidateGroupsNode);
        putPropertyValue(DYNAMIC_VALUE, flowElementNode.path(USER_TASK_CANDIDATE_GROUPS), candidateGroupsNode);

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.set(USER_TASK_NAME, taskNameNode);
        propertiesNode.set(USER_TASK_ASSIGNEE, assigneeNode);
        propertiesNode.set(USER_TASK_CANDIDATE_USERS, candidateUsersNode);
        propertiesNode.set(USER_TASK_CANDIDATE_GROUPS, candidateGroupsNode);

        return propertiesNode;
    }

    @Override
    public boolean supports(FlowElement flowElement) {
        return flowElement instanceof UserTask;
    }
}
