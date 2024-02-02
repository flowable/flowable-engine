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

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.converter.CmmnXMLException;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.validation.CaseValidator;
import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CmmnParserImpl implements CmmnParser {

    private static final Logger logger = LoggerFactory.getLogger(CmmnParserImpl.class);

    protected CmmnParseHandlers cmmnParseHandlers;
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected ExpressionManager expressionManager;

    @Override
    public CmmnParseResult parse(CmmnParseContext context) {
        EngineResource resourceEntity = context.resource();
        CmmnParseResult cmmnParseResult = parse(context, new BytesStreamSource(resourceEntity.getBytes()));
        processDI(cmmnParseResult.getCmmnModel(), cmmnParseResult.getAllCaseDefinitions());
        return cmmnParseResult;
    }

    public CmmnParseResult parse(CmmnParseContext context, StreamSource cmmnSource) {
        try {
            CmmnParseResult cmmnParseResult = new CmmnParseResult();
            cmmnParseResult.setResourceEntity(context.resource());

            CmmnModel cmmnModel = convertToCmmnModel(context, cmmnSource);
            cmmnParseResult.setCmmnModel(cmmnModel);

            if (context.validateCmmnModel()) {
                validateCmmnModel(context.caseValidator(), cmmnModel);
            }

            processCmmnElements(cmmnModel, cmmnParseResult);

            return cmmnParseResult;

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

    protected CmmnModel convertToCmmnModel(CmmnParseContext context, StreamSource cmmnSource) {
        boolean enableSafeBpmnXml = context.enableSafeXml();
        String encoding = context.xmlEncoding();
        boolean validateCmmnXml = context.validateXml();

        return new CmmnXmlConverter().convertToCmmnModel(cmmnSource, validateCmmnXml, enableSafeBpmnXml, encoding);
    }

    protected void validateCmmnModel(CaseValidator caseValidator, CmmnModel cmmnModel) {
        if (caseValidator == null) {
            logger.warn("Case should be validated, but no case validator is configured on the case engine configuration!");
        } else {
            List<ValidationEntry> validationEntries = caseValidator.validate(cmmnModel);
            if (validationEntries != null && !validationEntries.isEmpty()) {

                StringBuilder warningBuilder = new StringBuilder();
                StringBuilder errorBuilder = new StringBuilder();

                for (ValidationEntry entry : validationEntries) {
                    if (entry.getLevel() == ValidationEntry.Level.Warning) {
                        warningBuilder.append(entry).append("\n");
                    } else {
                        errorBuilder.append(entry).append("\n");
                    }
                }

                // Throw exception if there is any error
                if (errorBuilder.length() > 0) {
                    throw new FlowableException("Errors while parsing:\n" + errorBuilder);
                }

                // Write out warnings (if any)
                if (warningBuilder.length() > 0) {
                    logger.warn("Following warnings encountered during case validation: {}", warningBuilder);
                }

            }
        }
    }

    public void processCmmnElements(CmmnModel cmmnModel, CmmnParseResult parseResult) {
        for (Case caze : cmmnModel.getCases()) {
            cmmnParseHandlers.parseElement(this, parseResult, caze);
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

    public CmmnParseHandlers getCmmnParseHandlers() {
        return cmmnParseHandlers;
    }

    public void setCmmnParseHandlers(CmmnParseHandlers cmmnParseHandlers) {
        this.cmmnParseHandlers = cmmnParseHandlers;
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
