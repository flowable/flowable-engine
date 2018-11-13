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
package org.flowable.cmmn.engine.impl.parser.handler;

import java.util.Collection;
import java.util.Collections;

import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;

/**
 * @author Joram Barrez
 */
public class CaseParseHandler extends AbstractCmmnParseHandler<Case> {

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return Collections.singletonList(Case.class);
    }

    @Override
    protected void executeParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, Case caze) {

        CaseDefinitionEntity caseDefinitionEntity = CommandContextUtil.getCaseDefinitionEntityManager().create();
        caseDefinitionEntity.setKey(caze.getId());
        caseDefinitionEntity.setName(caze.getName());
        caseDefinitionEntity.setDescription(caze.getDocumentation());
        caseDefinitionEntity.setCategory(cmmnParseResult.getCmmnModel().getTargetNamespace());
        caseDefinitionEntity.setDeploymentId(cmmnParseResult.getResourceEntity().getDeploymentId());

        cmmnParseResult.addCaseDefinition(caseDefinitionEntity, cmmnParseResult.getResourceEntity(), cmmnParseResult.getCmmnModel());

        processPlanFragment(cmmnParser, cmmnParseResult, caze.getPlanModel());
    }

}
