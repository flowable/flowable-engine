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

package org.flowable.engine.impl;

import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.InternalProcessLocalizationManager;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.runtime.ProcessInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author David Lamas
 */
public class DefaultProcessLocalizationManager implements InternalProcessLocalizationManager {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultProcessLocalizationManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public void localize(ProcessInstance processInstance, String locale, boolean withLocalizationFallback) {
        ExecutionEntity processInstanceExecution = (ExecutionEntity) processInstance;
        processInstanceExecution.setLocalizedName(null);
        processInstanceExecution.setLocalizedDescription(null);

        if (locale != null) {
            String processDefinitionId = processInstanceExecution.getProcessDefinitionId();
            if (processDefinitionId != null) {
                ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, processInstanceExecution.getProcessDefinitionKey(), processDefinitionId, withLocalizationFallback);
                if (languageNode != null) {
                    JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                    if (languageNameNode != null && !languageNameNode.isNull()) {
                        processInstanceExecution.setLocalizedName(languageNameNode.asText());
                    }

                    JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                    if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                        processInstanceExecution.setLocalizedDescription(languageDescriptionNode.asText());
                    }
                }
            }
        }
    }

    @Override
    public void localize(HistoricProcessInstance historicProcessInstance, String locale, boolean withLocalizationFallback) {
        HistoricProcessInstanceEntity processInstanceEntity = (HistoricProcessInstanceEntity) historicProcessInstance;
        processInstanceEntity.setLocalizedName(null);
        processInstanceEntity.setLocalizedDescription(null);

        if (locale != null) {
            String processDefinitionId = processInstanceEntity.getProcessDefinitionId();
            if (processDefinitionId != null) {
                ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, processInstanceEntity.getProcessDefinitionKey(), processDefinitionId, withLocalizationFallback);
                if (languageNode != null) {
                    JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                    if (languageNameNode != null && !languageNameNode.isNull()) {
                        processInstanceEntity.setLocalizedName(languageNameNode.asText());
                    }

                    JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                    if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                        processInstanceEntity.setLocalizedDescription(languageDescriptionNode.asText());
                    }
                }
            }
        }
    }


}
