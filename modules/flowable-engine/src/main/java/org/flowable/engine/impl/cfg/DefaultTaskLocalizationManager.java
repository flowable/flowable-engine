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

package org.flowable.engine.impl.cfg;

import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.InternalTaskLocalizationManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DefaultTaskLocalizationManager implements InternalTaskLocalizationManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultTaskLocalizationManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
    
    @Override
    public void localize(Task task, String locale, boolean withLocalizationFallback) {
        task.setLocalizedName(null);
        task.setLocalizedDescription(null);

        if (locale != null) {
            String processDefinitionId = task.getProcessDefinitionId();
            if (processDefinitionId != null) {
                ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, task.getTaskDefinitionKey(), processDefinitionId, withLocalizationFallback);
                if (languageNode != null) {
                    JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                    if (languageNameNode != null && !languageNameNode.isNull()) {
                        task.setLocalizedName(languageNameNode.asText());
                    }

                    JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                    if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                        task.setLocalizedDescription(languageDescriptionNode.asText());
                    }
                }
            }
        }
    }

    @Override
    public void localize(HistoricTaskInstance task, String locale, boolean withLocalizationFallback) {
        HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) task;
        taskEntity.setLocalizedName(null);
        taskEntity.setLocalizedDescription(null);

        if (locale != null) {
            String processDefinitionId = task.getProcessDefinitionId();
            if (processDefinitionId != null) {
                ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, task.getTaskDefinitionKey(), processDefinitionId, withLocalizationFallback);
                if (languageNode != null) {
                    JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                    if (languageNameNode != null && !languageNameNode.isNull()) {
                        taskEntity.setLocalizedName(languageNameNode.asText());
                    }

                    JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                    if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                        taskEntity.setLocalizedDescription(languageDescriptionNode.asText());
                    }
                }
            }
        }
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return processEngineConfiguration.getExecutionEntityManager();
    }
}
