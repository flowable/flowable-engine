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
package org.flowable.cmmn.rest.conf;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.rest.service.api.CmmnFormHandlerRestApiInterceptor;
import org.flowable.form.api.FormInfo;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class MockFormHandlerRestApiInterceptor implements CmmnFormHandlerRestApiInterceptor {

    protected final ObjectMapper objectMapper;

    public MockFormHandlerRestApiInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertStartFormInfo(FormInfo formInfo, CaseDefinition caseDefinition) {
        return createBaseNode(formInfo)
                .put("type", "startForm")
                .put("definitionKey", caseDefinition.getKey())
                .toString();
    }

    @Override
    public String convertTaskFormInfo(FormInfo formInfo, Task task) {
        return createBaseNode(formInfo)
                .put("type", "taskForm")
                .put("taskId", task.getId())
                .toString();
    }

    @Override
    public String convertHistoricTaskFormInfo(FormInfo formInfo, HistoricTaskInstance task) {
        return createBaseNode(formInfo)
                .put("type", "historicTaskForm")
                .put("historicTaskId", task.getId())
                .toString();
    }

    protected ObjectNode createBaseNode(FormInfo formInfo) {
        return objectMapper.createObjectNode()
                .put("id", formInfo.getId())
                .put("key", formInfo.getKey())
                .put("name", formInfo.getName());
    }

}
