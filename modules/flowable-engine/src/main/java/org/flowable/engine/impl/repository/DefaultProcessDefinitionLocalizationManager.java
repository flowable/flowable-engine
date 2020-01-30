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

package org.flowable.engine.impl.repository;

import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.repository.InternalProcessDefinitionLocalizationManager;
import org.flowable.engine.repository.ProcessDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author David Lamas
 */
public class DefaultProcessDefinitionLocalizationManager implements InternalProcessDefinitionLocalizationManager {
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultProcessDefinitionLocalizationManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public void localize(ProcessDefinition processDefinition, String locale, boolean withLocalizationFallback) {
        ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) processDefinition;
        processDefinitionEntity.setLocalizedName(null);
        processDefinitionEntity.setLocalizedDescription(null);

        if (locale != null) {
            ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, processDefinitionEntity.getKey(), processDefinition.getId(), withLocalizationFallback);
            if (languageNode != null) {
                JsonNode languageNameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                if (languageNameNode != null && !languageNameNode.isNull()) {
                    processDefinitionEntity.setLocalizedName(languageNameNode.asText());
                }

                JsonNode languageDescriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                if (languageDescriptionNode != null && !languageDescriptionNode.isNull()) {
                    processDefinitionEntity.setLocalizedDescription(languageDescriptionNode.asText());
                }
            }
        }
    }
}
