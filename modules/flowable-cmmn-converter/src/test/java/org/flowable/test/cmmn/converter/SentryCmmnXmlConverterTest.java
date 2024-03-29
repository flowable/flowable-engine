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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class SentryCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/sentryIfPart.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases()).hasSize(1);

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getId()).isEqualTo("sentryIfPart");

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertThat(planModel).isNotNull();
        assertThat(planModel.getId()).isEqualTo("sentryCasePlanModel");
        assertThat(planModel.getName()).isEqualTo("Sentry Case plan model");

        assertThat(planModel.getSentries())
                .extracting(Sentry::getId, Sentry::getName, Sentry::getDocumentation)
                .containsOnly(
                        tuple("sentry1", "sentry name", "sentry doc")
                );

        SentryIfPart sentryIfPart = planModel.getSentries().get(0).getSentryIfPart();
        assertThat(sentryIfPart).isNotNull();
        assertThat(sentryIfPart.getId()).isEqualTo("sentryIfPart_sentry1");
        assertThat(sentryIfPart.getCondition()).isEqualTo("${true}");

        PlanItem planItemTask1 = cmmnModel.findPlanItem("planItem1");
        assertThat(planItemTask1).isNotNull();
        assertThat(planItemTask1.getEntryCriteria())
                .extracting(Criterion::getSentryRef)
                .containsOnly("sentry1");
    }

}
