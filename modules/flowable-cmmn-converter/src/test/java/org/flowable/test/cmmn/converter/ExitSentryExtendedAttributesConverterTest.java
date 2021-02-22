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
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_EXIT;
import static org.flowable.cmmn.model.Criterion.EXIT_EVENT_TYPE_FORCE_COMPLETE;
import static org.flowable.cmmn.model.Criterion.EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES;
import static org.flowable.cmmn.model.Criterion.EXIT_TYPE_ACTIVE_INSTANCES;
import static org.flowable.cmmn.model.Criterion.EXIT_TYPE_DEFAULT;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

public class ExitSentryExtendedAttributesConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/exitSentryAttributesOnStageAndPlanModel.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        assertCriterionExitEventType(cmmnModel, "exitCriterion1", null);
        assertCriterionExitEventType(cmmnModel, "exitCriterion2", EXIT_EVENT_TYPE_EXIT);
        assertCriterionExitEventType(cmmnModel, "exitCriterion3", EXIT_EVENT_TYPE_COMPLETE);
        assertCriterionExitEventType(cmmnModel, "exitCriterion4", EXIT_EVENT_TYPE_FORCE_COMPLETE);

        assertCriterionExitEventType(cmmnModel, "exitCriterion5", null);
        assertCriterionExitEventType(cmmnModel, "exitCriterion6", EXIT_EVENT_TYPE_EXIT);
        assertCriterionExitEventType(cmmnModel, "exitCriterion7", EXIT_EVENT_TYPE_COMPLETE);
        assertCriterionExitEventType(cmmnModel, "exitCriterion8", EXIT_EVENT_TYPE_FORCE_COMPLETE);

        assertCriterionExitType(cmmnModel, "exitCriterion9", null);
        assertCriterionExitType(cmmnModel, "exitCriterion10", EXIT_TYPE_DEFAULT);
        assertCriterionExitType(cmmnModel, "exitCriterion11", EXIT_TYPE_ACTIVE_INSTANCES);
        assertCriterionExitType(cmmnModel, "exitCriterion12", EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES);
    }

    protected void assertCriterionExitEventType(CmmnModel cmmnModel, String criterionId, String expectedExitEventType) {
        assertThat(cmmnModel.getCriterion(criterionId)).isNotNull();
        assertThat(cmmnModel.getCriterion(criterionId).getExitEventType()).isEqualTo(expectedExitEventType);
    }

    protected void assertCriterionExitType(CmmnModel cmmnModel, String criterionId, String expectedExitType) {
        assertThat(cmmnModel.getCriterion(criterionId)).isNotNull();
        assertThat(cmmnModel.getCriterion(criterionId).getExitType()).isEqualTo(expectedExitType);
    }
}
