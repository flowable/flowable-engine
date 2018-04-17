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
package org.flowable.cmmn.engine.impl.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.converter.CmmnXMLException;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnParserImpl implements CmmnParser {

    private final Logger logger = LoggerFactory.getLogger(CmmnParserImpl.class);

    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected ExpressionManager expressionManager;

    public CmmnParseResult parse(EngineResource resourceEntity) {
        CmmnParseResult parseResult = new CmmnParseResult();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(resourceEntity.getBytes())) {
            Pair<CmmnModel, List<CaseDefinitionEntity>> pair = parse(resourceEntity, parseResult, new InputStreamSource(inputStream));
            for (CaseDefinitionEntity caseDefinitionEntity : pair.getRight()) {
                parseResult.addCaseDefinition(caseDefinitionEntity, resourceEntity, pair.getLeft());
            }

            processDI(pair.getLeft(), pair.getRight());

        } catch (IOException e) {
            logger.error("Could not read bytes from CMMN resource", e);
        }
        return parseResult;
    }

    public Pair<CmmnModel, List<CaseDefinitionEntity>> parse(EngineResource resourceEntity, CmmnParseResult parseResult, StreamSource cmmnSource) {
        try {
            boolean enableSafeBpmnXml = false;
            String encoding = null;
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
            if (cmmnEngineConfiguration != null) {
                enableSafeBpmnXml = cmmnEngineConfiguration.isEnableSafeCmmnXml();
                encoding = cmmnEngineConfiguration.getXmlEncoding();
            }
            CmmnModel cmmnModel = new CmmnXmlConverter().convertToCmmnModel(cmmnSource, true, enableSafeBpmnXml, encoding);
            List<CaseDefinitionEntity> caseDefinitionEntities = processCmmnElements(resourceEntity, cmmnModel);
            return Pair.of(cmmnModel, caseDefinitionEntities);

        } catch (Exception e) {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else if (e instanceof CmmnXMLException) {
                throw (CmmnXMLException) e;
            } else {
                throw new FlowableException("Error parsing XML", e);
            }
        }
    }

    protected List<CaseDefinitionEntity> processCmmnElements(EngineResource resourceEntity, CmmnModel cmmnModel) {
        List<CaseDefinitionEntity> caseDefinitionEntities = new ArrayList<>();
        for (Case caze : cmmnModel.getCases()) {

            CaseDefinitionEntity caseDefinitionEntity = CommandContextUtil.getCaseDefinitionEntityManager().create();
            caseDefinitionEntity.setKey(caze.getId());
            caseDefinitionEntity.setName(caze.getName());
            caseDefinitionEntity.setCategory(cmmnModel.getTargetNamespace());
            caseDefinitionEntity.setDeploymentId(resourceEntity.getDeploymentId());
            caseDefinitionEntities.add(caseDefinitionEntity);

            processPlanFragment(caze.getPlanModel());
        }
        return caseDefinitionEntities;
    }

    protected void processPlanFragment(PlanFragment planFragment) {

        // TODO: do with parse handlers like bpmn engine?
        for (PlanItem planItem : planFragment.getPlanItems()) {

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createStageActivityBehavior(planItem, stage));

            } else if (planItemDefinition instanceof HumanTask) {
                HumanTask humanTask = (HumanTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createHumanTaskActivityBehavior(planItem, humanTask));

            } else if (planItemDefinition instanceof CaseTask) {
                CaseTask caseTask = (CaseTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createCaseTaskActivityBehavior(planItem, caseTask));

            } else if (planItemDefinition instanceof ProcessTask) {
                ProcessTask processTask = (ProcessTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createProcessTaskActivityBehavior(planItem, processTask));

            } else if (planItemDefinition instanceof DecisionTask) {
                DecisionTask decisionTask = (DecisionTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createDecisionTaskActivityBehavior(planItem, decisionTask));

            } else if (planItemDefinition instanceof Milestone) {
                Milestone milestone = (Milestone) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createMilestoneActivityBehavior(planItem, milestone));

            } else if (planItemDefinition instanceof TimerEventListener) {
                TimerEventListener timerEventListener = (TimerEventListener) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createTimerEventListenerActivityBehavior(planItem, timerEventListener));

            } else if (planItemDefinition instanceof UserEventListener) {
                UserEventListener userEventListener = (UserEventListener) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createUserEventListenerActivityBehavior(planItem, userEventListener));

            } else if (planItemDefinition instanceof ScriptServiceTask) {
                //ScriptServiceTask Is-A ServiceTask thus should be check before
                planItem.setBehavior(activityBehaviorFactory.createScriptTaskActivityBehavior(planItem, (ScriptServiceTask) planItemDefinition));

            } else if (planItemDefinition instanceof ServiceTask) {
                ServiceTask serviceTask = (ServiceTask) planItemDefinition;

                switch (serviceTask.getType()) {
                    case HttpServiceTask.HTTP_TASK:
                        planItem.setBehavior(activityBehaviorFactory.createHttpActivityBehavior(planItem, serviceTask));
                        break;
                    default:
                        // java task type was not set in the version <= 6.2.0 that's why we have to assume that default
                        // service task type is java
                        if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
                            if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
                                planItem.setBehavior(activityBehaviorFactory.createCmmnClassDelegate(planItem, serviceTask));

                            } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(serviceTask.getImplementationType())) {
                                planItem.setBehavior(activityBehaviorFactory.createPlanItemExpressionActivityBehavior(planItem, serviceTask));

                            } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
                                planItem.setBehavior(activityBehaviorFactory.createPlanItemDelegateExpressionActivityBehavior(planItem, serviceTask));
                            }
                        }
                        break;
                }
            } else if (planItemDefinition instanceof Task) {
                planItem.setBehavior(activityBehaviorFactory.createTaskActivityBehavior(planItem, (Task) planItemDefinition));
            }

            if (planItemDefinition instanceof PlanFragment) {
                processPlanFragment((PlanFragment) planItemDefinition);
            }
        }
    }

    public void processDI(CmmnModel cmmnModel, List<CaseDefinitionEntity> caseDefinitions) {

        if (caseDefinitions.isEmpty()) {
            return;
        }

        if (!cmmnModel.getLocationMap().isEmpty()) {

            List<String> planModelIds = new ArrayList<>();
            for (Case caseObject : cmmnModel.getCases()) {
                planModelIds.add(caseObject.getPlanModel().getId());
            }

            // Verify if all referenced elements exist
            for (String cmmnReference : cmmnModel.getLocationMap().keySet()) {

                if (planModelIds.contains(cmmnReference)) {
                    continue;
                }

                if (cmmnModel.findPlanItem(cmmnReference) == null && cmmnModel.getCriterion(cmmnReference) == null) {
                    logger.warn("Invalid reference in diagram interchange definition: could not find {}", cmmnReference);
                }
            }

            for (Case caseObject : cmmnModel.getCases()) {
                CaseDefinitionEntity caseDefinition = getCaseDefinition(caseObject.getId(), caseDefinitions);
                if (caseDefinition != null) {
                    caseDefinition.setHasGraphicalNotation(true);
                }
            }
        }
    }

    public CaseDefinitionEntity getCaseDefinition(String caseDefinitionKey, List<CaseDefinitionEntity> caseDefinitions) {
        for (CaseDefinitionEntity caseDefinition : caseDefinitions) {
            if (caseDefinition.getKey().equals(caseDefinitionKey)) {
                return caseDefinition;
            }
        }
        return null;
    }

    public CmmnActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public void setActivityBehaviorFactory(CmmnActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public void setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

}
