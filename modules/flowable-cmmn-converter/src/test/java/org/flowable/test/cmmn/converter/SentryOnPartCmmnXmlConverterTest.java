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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class SentryOnPartCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/sentryOnPart.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases()).hasSize(1);

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getId()).isEqualTo("sentryOnPart");

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertThat(planModel).isNotNull();
        assertThat(planModel.getId()).isEqualTo("sentryOnPartPlanModel");
        assertThat(planModel.getName()).isEqualTo("Sentry On Part Plan Model");

        PlanItem planItemTimer = cmmnModel.findPlanItem("planItem1");
        assertThat(planItemTimer).isNotNull();
        assertThat(planItemTimer.getDefinitionRef()).isEqualTo("expireTimer");

        PlanItem planItemTimedTask = cmmnModel.findPlanItem("planItem2");
        assertThat(planItemTimedTask).isNotNull();
        assertThat(planItemTimedTask.getDefinitionRef()).isEqualTo("timedTask");
        assertThat(planItemTimedTask.getExitCriteria())
                .extracting(Criterion::getId, Criterion::getSentryRef)
                .containsOnly(
                        tuple("timedTaskExitSentry", "sentry1")
                );

        assertThat(planModel.getSentries())
                .extracting(Sentry::getId)
                .containsOnly("sentry1");
        assertThat(planModel.getSentries().get(0).getOnParts())
                .extracting(SentryOnPart::getId, SentryOnPart::getSourceRef, SentryOnPart::getStandardEvent)
                .containsOnly(
                        tuple("sentryOnPart1", "planItem1", "occur")
                );

        assertThat(cmmnModel.getAssociations())
                .extracting(Association::getId, Association::getSourceRef, Association::getTargetRef, Association::getTransitionEvent)
                .containsOnly(
                        tuple("CMMNEdge_connector2", "planItem1", "timedTaskExitSentry", "occur")
                );
    }

}
