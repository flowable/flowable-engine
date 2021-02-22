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
package org.flowable.cmmn.editor;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

public class CriterionConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.criterionModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        Stage planModelStage = caseModel.getPlanModel();
        
        PlanItemDefinition userEventListener = planModelStage.findPlanItemDefinitionInStageOrDownwards("userEventListener1");
        assertThat(userEventListener).isNotNull();
        
        PlanItem userEventListenerPlanItem = planModelStage.findPlanItemForPlanItemDefinitionInPlanFragmentOrDownwards("userEventListener1");
        assertThat(userEventListenerPlanItem).isNotNull();
        assertThat(userEventListenerPlanItem.getOutgoingAssociations()).hasSize(1);
        Association association = userEventListenerPlanItem.getOutgoingAssociations().get(0);
        assertThat(association.getSourceRef()).isNotNull();
        assertThat(planModelStage.findPlanItemInPlanFragmentOrDownwards(association.getSourceRef()).getPlanItemDefinition().getId()).isEqualTo("userEventListener1");
        assertThat(association.getTargetRef()).isEqualTo("exitCriterion1");
        assertThat(association.getTargetElement()).isInstanceOf(Criterion.class);
        Criterion exitCriterion = (Criterion) association.getTargetElement();
        assertThat(exitCriterion.getAttachedToRefId()).isNull();
        
        assertThat(planModelStage.getExitCriteria()).hasSize(2);
        assertThat(planModelStage.getExitCriteria()).extracting(Criterion::getId)
                .containsExactlyInAnyOrder("exitCriterion1", "exitCriterion2");
        
        Stage claimDecisionStage = (Stage) planModelStage.findPlanItemDefinitionInStageOrDownwards("claimDecisionStage");
        assertThat(claimDecisionStage).isNotNull();
        assertThat(claimDecisionStage.getExitCriteria()).hasSize(2);
        assertThat(claimDecisionStage.getExitCriteria()).extracting(Criterion::getId)
                .containsExactlyInAnyOrder("acceptedExitCriterion", "rejectedExitCriterion");
    }
}
