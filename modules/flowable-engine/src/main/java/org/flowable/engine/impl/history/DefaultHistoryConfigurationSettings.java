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
package org.flowable.engine.impl.history;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DefaultHistoryConfigurationSettings implements HistoryConfigurationSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHistoryConfigurationSettings.class.getName());

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultHistoryConfigurationSettings(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public boolean isEnableProcessDefinitionHistoryLevel() {
        return processEngineConfiguration.isEnableProcessDefinitionHistoryLevel();
    }

    @Override
    public boolean isHistoryEnabled() {
        HistoryLevel engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
        return engineHistoryLevel != HistoryLevel.NONE;
    }

    protected HistoryLevel getProcessDefinitionHistoryLevel(String processDefinitionId) {
        HistoryLevel processDefinitionHistoryLevel = null;

        try {
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

            Process process = bpmnModel.getProcessById(processDefinition.getKey());
            if (process.getExtensionElements().containsKey("historyLevel")) {
                ExtensionElement historyLevelElement = process.getExtensionElements().get("historyLevel").iterator().next();
                String historyLevelValue = historyLevelElement.getElementText();
                if (StringUtils.isNotEmpty(historyLevelValue)) {
                    try {
                        processDefinitionHistoryLevel = HistoryLevel.getHistoryLevelForKey(historyLevelValue);

                    } catch (Exception e) {
                        // Shouldn't block anything
                    }
                }
            }

            if (processDefinitionHistoryLevel == null) {
                processDefinitionHistoryLevel = this.processEngineConfiguration.getHistoryLevel();
            }

        } catch (Exception e) {
            // Shouldn't block anything
        }

        return processDefinitionHistoryLevel;
    }

    @Override
    public boolean isHistoryEnabled(String processDefinitionId) {
        HistoryLevel engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
        if (isEnableProcessDefinitionHistoryLevel() && processDefinitionId != null) {
            HistoryLevel processDefinitionLevel = getProcessDefinitionHistoryLevel(processDefinitionId);
            if (processDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}", processDefinitionLevel);
                }
                return !processDefinitionLevel.equals(HistoryLevel.NONE);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}", engineHistoryLevel);
                }
                return !engineHistoryLevel.equals(HistoryLevel.NONE);
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current history level: {}", engineHistoryLevel);
            }
            return !engineHistoryLevel.equals(HistoryLevel.NONE);
        }
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level, String processDefinitionId) {
        HistoryLevel engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
        if (isEnableProcessDefinitionHistoryLevel() && processDefinitionId != null) {
            HistoryLevel processDefinitionLevel = getProcessDefinitionHistoryLevel(processDefinitionId);
            if (processDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", processDefinitionLevel, level);
                }
                return processDefinitionLevel.isAtLeast(level);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, level);
                }
                return engineHistoryLevel.isAtLeast(level);
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, level);
            }

            // Comparing enums actually compares the location of values declared in the enum
            return engineHistoryLevel.isAtLeast(level);
        }
    }

    @Override
    public boolean isHistoryEnabledForProcessInstance(String processDefinitionId, String processInstanceId) {
        return isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForActivity(String processDefinitionId, String activityId) {
        HistoryLevel engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
        if (isEnableProcessDefinitionHistoryLevel() && processDefinitionId != null) {
            HistoryLevel processDefinitionLevel = getProcessDefinitionHistoryLevel(processDefinitionId);
            if (processDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", processDefinitionLevel, HistoryLevel.ACTIVITY);
                }

                if (processDefinitionLevel.isAtLeast(HistoryLevel.ACTIVITY)) {
                    return true;

                } else if (!HistoryLevel.NONE.equals(processDefinitionLevel) && StringUtils.isNotEmpty(activityId)) {
                    return includeFlowElementInHistory(processDefinitionId, activityId);

                } else {
                    return false;
                }

            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, HistoryLevel.ACTIVITY);
                }
                return engineHistoryLevel.isAtLeast(HistoryLevel.ACTIVITY);
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, HistoryLevel.ACTIVITY);
            }

            if (engineHistoryLevel.isAtLeast(HistoryLevel.ACTIVITY)) {
                return true;

            } else if (!HistoryLevel.NONE.equals(engineHistoryLevel) && StringUtils.isNotEmpty(activityId)) {
                return includeFlowElementInHistory(processDefinitionId, activityId);

            } else {
                return false;
            }
        }
    }

    protected boolean includeFlowElementInHistory(String processDefinitionId, String activityId) {
        boolean includeInHistory = false;

        if (processDefinitionId != null) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);
            FlowElement flowElement = bpmnModel.getFlowElement(activityId);

            if (flowElement.getExtensionElements().containsKey("includeInHistory")) {
                ExtensionElement historyElement = flowElement.getExtensionElements().get("includeInHistory").iterator().next();
                String historyLevelValue = historyElement.getElementText();
                includeInHistory = Boolean.valueOf(historyLevelValue);
            }
        }

        return includeInHistory;
    }

    @Override
    public boolean isHistoryEnabledForUserTask(String processDefinitionId, String taskId) {
        HistoryLevel engineHistoryLevel = processEngineConfiguration.getHistoryLevel();
        if (isEnableProcessDefinitionHistoryLevel() && processDefinitionId != null) {
            HistoryLevel processDefinitionLevel = getProcessDefinitionHistoryLevel(processDefinitionId);
            if (processDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", processDefinitionLevel, HistoryLevel.TASK);
                }
                return hasTaskHistoryLevel(processDefinitionLevel);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, HistoryLevel.TASK);
                }
                return hasTaskHistoryLevel(engineHistoryLevel);
            }

        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current history level: {}, level required: {}", engineHistoryLevel, HistoryLevel.TASK);
            }

            // Comparing enums actually compares the location of values declared in the enum
            return hasTaskHistoryLevel(engineHistoryLevel);
        }
    }

    protected boolean hasTaskHistoryLevel(HistoryLevel historyLevel) {
        boolean taskHistoryLevel = false;
        if (HistoryLevel.TASK.equals(historyLevel)) {
            taskHistoryLevel = true;

        } else if (historyLevel.isAtLeast(HistoryLevel.AUDIT)) {
            taskHistoryLevel = true;
        }

        return taskHistoryLevel;
    }

    @Override
    public boolean isHistoryEnabledForVariableInstance(String processDefinitionId, String variableInstanceId) {
        return isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForIdentityLink(String processDefinitionId, String identityLinkId) {
        return isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForEntityLink(String processDefinitionId, String entityLinkId) {
        return isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId);
    }

}