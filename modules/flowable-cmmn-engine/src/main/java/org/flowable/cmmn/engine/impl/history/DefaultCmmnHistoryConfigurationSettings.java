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
package org.flowable.cmmn.engine.impl.history;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnHistoryConfigurationSettings implements CmmnHistoryConfigurationSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCmmnHistoryConfigurationSettings.class.getName());

    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCmmnHistoryConfigurationSettings(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public boolean isHistoryEnabled() {
        return cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE;
    }

    @Override
    public boolean isEnableCaseDefinitionHistoryLevel() {
        return cmmnEngineConfiguration.isEnableCaseDefinitionHistoryLevel();
    }

    @Override
    public boolean isHistoryEnabled(String caseDefinitionId) {
        HistoryLevel engineHistoryLevel = cmmnEngineConfiguration.getHistoryLevel();
        if (isEnableCaseDefinitionHistoryLevel() && caseDefinitionId != null) {
            HistoryLevel caseDefinitionLevel = getCaseDefinitionHistoryLevel(caseDefinitionId);
            if (caseDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}", caseDefinitionLevel);
                }
                return !caseDefinitionLevel.equals(HistoryLevel.NONE);
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
    public boolean isHistoryLevelAtLeast(HistoryLevel level, String caseDefinitionId) {
        HistoryLevel engineHistoryLevel = cmmnEngineConfiguration.getHistoryLevel();
        if (isEnableCaseDefinitionHistoryLevel() && caseDefinitionId != null) {
            HistoryLevel caseDefinitionLevel = getCaseDefinitionHistoryLevel(caseDefinitionId);
            if (caseDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", caseDefinitionLevel, level);
                }
                return caseDefinitionLevel.isAtLeast(level);
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
    public boolean isHistoryEnabledForCaseInstance(String caseDefinitionId, String caseInstanceId) {
        return isHistoryLevelAtLeast(HistoryLevel.INSTANCE, caseDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForActivity(String caseDefinitionId, String activityId) {
        HistoryLevel engineHistoryLevel = cmmnEngineConfiguration.getHistoryLevel();
        if (isEnableCaseDefinitionHistoryLevel() && caseDefinitionId != null) {
            HistoryLevel caseDefinitionLevel = getCaseDefinitionHistoryLevel(caseDefinitionId);
            if (caseDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", caseDefinitionLevel, HistoryLevel.ACTIVITY);
                }

                if (caseDefinitionLevel.isAtLeast(HistoryLevel.ACTIVITY)) {
                    return true;

                } else if (!HistoryLevel.NONE.equals(caseDefinitionLevel) && StringUtils.isNotEmpty(activityId)) {
                    return includePlanItemDefinitionInHistory(caseDefinitionId, activityId);

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
                return includePlanItemDefinitionInHistory(caseDefinitionId, activityId);

            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isHistoryEnabledForUserTask(String caseDefinitionId, String taskId) {
        HistoryLevel engineHistoryLevel = cmmnEngineConfiguration.getHistoryLevel();
        if (isEnableCaseDefinitionHistoryLevel() && caseDefinitionId != null) {
            HistoryLevel caseDefinitionLevel = getCaseDefinitionHistoryLevel(caseDefinitionId);
            if (caseDefinitionLevel != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current history level: {}, level required: {}", caseDefinitionLevel, HistoryLevel.TASK);
                }
                return hasTaskHistoryLevel(caseDefinitionLevel);
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
    public boolean isHistoryEnabledForVariableInstance(String caseDefinitionId, String variableInstanceId) {
        return isHistoryLevelAtLeast(HistoryLevel.AUDIT, caseDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForIdentityLink(String caseDefinitionId, String identityLinkId) {
        return isHistoryLevelAtLeast(HistoryLevel.AUDIT, caseDefinitionId);
    }

    @Override
    public boolean isHistoryEnabledForEntityLink(String caseDefinitionId, String entityLinkId) {
        return isHistoryLevelAtLeast(HistoryLevel.AUDIT, caseDefinitionId);
    }

    protected HistoryLevel getCaseDefinitionHistoryLevel(String caseDefinitionId) {
        HistoryLevel caseDefinitionHistoryLevel = null;

        try {
            CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);

            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinitionId);

            Case caze = cmmnModel.getCaseById(caseDefinition.getKey());
            if (caze.getPlanModel().getExtensionElements().containsKey("historyLevel")) {
                ExtensionElement historyLevelElement = caze.getPlanModel().getExtensionElements().get("historyLevel").iterator().next();
                String historyLevelValue = historyLevelElement.getElementText();
                if (StringUtils.isNotEmpty(historyLevelValue)) {
                    try {
                        caseDefinitionHistoryLevel = HistoryLevel.getHistoryLevelForKey(historyLevelValue);

                    } catch (Exception e) {}
                }
            }

            if (caseDefinitionHistoryLevel == null) {
                caseDefinitionHistoryLevel = this.cmmnEngineConfiguration.getHistoryLevel();
            }

        } catch (Exception e) {}

        return caseDefinitionHistoryLevel;
    }

    protected boolean includePlanItemDefinitionInHistory(String caseDefinitionId, String activityId) {
        boolean includeInHistory = false;

        if (caseDefinitionId != null) {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinitionId);
            PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(activityId);
            if (planItemDefinition == null) {
                PlanItem planItem = cmmnModel.findPlanItem(activityId);
                planItemDefinition = planItem.getPlanItemDefinition();
            }

            if (planItemDefinition.getExtensionElements().containsKey("includeInHistory")) {
                ExtensionElement historyElement = planItemDefinition.getExtensionElements().get("includeInHistory").iterator().next();
                String historyLevelValue = historyElement.getElementText();
                includeInHistory = Boolean.valueOf(historyLevelValue);
            }
        }

        return includeInHistory;
    }

}
