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
import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_ELEMENT_NAME;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

public class NameWithNewLineTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/nameWithNewLineTestCase.cmmn")
    public void validateModelWithNewLinesInPlanItemDefinitionNames(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        PlanItemDefinition itemDefinition = cmmnModel.findPlanItemDefinition("cmmnStage_1");
        assertThat(itemDefinition.getName()).isEqualTo("stage\ntest");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnTask_2");
        assertThat(itemDefinition.getName()).isEqualTo("human\ntask");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnEventListener_3");
        assertThat(itemDefinition.getName()).isEqualTo("timer\nevent");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnPlanFragment_6");
        assertThat(itemDefinition.getName()).isEqualTo("plan\nfragment");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnMilestone_7");
        assertThat(itemDefinition.getName()).isEqualTo("mile\nstone");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

    }

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/nameWithoutNewLineTestCase.cmmn")
    public void validateModelWithoutNewLinesInPlanItemDefinitionNames(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        PlanItemDefinition itemDefinition = cmmnModel.findPlanItemDefinition("cmmnStage_1");
        assertThat(itemDefinition.getName()).isEqualTo("stagetest");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnTask_2");
        assertThat(itemDefinition.getName()).isEqualTo("humantask");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnEventListener_3");
        assertThat(itemDefinition.getName()).isEqualTo("timerevent");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnPlanFragment_6");
        assertThat(itemDefinition.getName()).isEqualTo("planfragment");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

        itemDefinition = cmmnModel.findPlanItemDefinition("cmmnMilestone_7");
        assertThat(itemDefinition.getName()).isEqualTo("milestone");
        assertThat(itemDefinition.getExtensionElements().get(ATTRIBUTE_ELEMENT_NAME)).isNull();

    }

}
