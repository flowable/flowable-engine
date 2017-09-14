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
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.util.io.InputStreamSource;
import org.flowable.engine.common.impl.util.io.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnParserImpl implements CmmnParser {
    
    private final Logger logger = LoggerFactory.getLogger(CmmnParserImpl.class);
    
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    
    public CmmnParseResult parse(CmmnResourceEntity resourceEntity) {
        CmmnParseResult parseResult = new CmmnParseResult();
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(resourceEntity.getBytes())) {
            Pair<CmmnModel, List<CaseDefinitionEntity>> pair = parse(resourceEntity, parseResult, new InputStreamSource(inputStream));
            for (CaseDefinitionEntity caseDefinitionEntity : pair.getRight()) {
                parseResult.addCaseDefinition(caseDefinitionEntity, resourceEntity, pair.getLeft());
            }
        } catch (IOException e) {
            logger.error("Could not read bytes from CMMN resource", e);
        }
        return parseResult;
    }
    
    public Pair<CmmnModel, List<CaseDefinitionEntity>> parse(CmmnResourceEntity resourceEntity, CmmnParseResult parseResult, StreamSource cmmnSource) {
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
    
    protected List<CaseDefinitionEntity> processCmmnElements(CmmnResourceEntity resourceEntity, CmmnModel cmmnModel) {
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
                planItem.setBehavior(activityBehaviorFactory.createStageActivityBehavoir(planItem, stage));
                
            } else if (planItemDefinition instanceof CaseTask) {
                CaseTask caseTask = (CaseTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createCaseTaskActivityBehavior(planItem, caseTask));
                
            } else if (planItemDefinition instanceof ProcessTask) {
                ProcessTask processTask = (ProcessTask) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createProcessTaskActivityBehavior(planItem, processTask));
                
            } else if (planItemDefinition instanceof Milestone) {
                Milestone milestone = (Milestone) planItemDefinition;
                planItem.setBehavior(activityBehaviorFactory.createMilestoneActivityBehavior(planItem, milestone));
                
            } else if (planItemDefinition instanceof Task) {
                Task task = (Task) planItemDefinition;
                
                if (StringUtils.isEmpty(task.getClassName())) {
                    planItem.setBehavior(activityBehaviorFactory.createTaskActivityBehavior(planItem, task));
                } else {
                    planItem.setBehavior(activityBehaviorFactory.createCmmnClassDelegate(planItem, task));
                }
            
            }
            
            if (planItemDefinition instanceof PlanFragment) {
                processPlanFragment((PlanFragment) planItemDefinition);
            }
            
        }

    }

    public CmmnActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public void setActivityBehaviorFactory(CmmnActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
    }

}
