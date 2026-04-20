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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class ExtensionElementsCharEventsCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/extensionElementsCharEvents.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases()).hasSize(1);

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getId()).isEqualTo("myCase");

        Stage planModel = caze.getPlanModel();

        Task task = (Task) planModel.findPlanItemDefinitionInStageOrUpwards("taskA");
        assertThat(task.getExtensionElements()).hasSize(1);
        List<ExtensionElement> extensionElements = task.getExtensionElements().get("taskTest");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("taskTest", "a&b&c&d&e&f&g"));

        Milestone milestone = (Milestone) planModel.findPlanItemDefinitionInStageOrUpwards("mileStoneOne");
        assertThat(milestone.getExtensionElements()).hasSize(1);
        extensionElements = milestone.getExtensionElements().get("milestoneTest");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("milestoneTest", "a&b&c&d&e&f&g"));

        PlanItem planItem = planModel.findPlanItemInPlanFragmentOrDownwards("planItemTaskA");
        assertThat(planItem.getExtensionElements()).hasSize(1);
        extensionElements = planItem.getExtensionElements().get("test");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("test", "a&b&c&d&e&f&g"));

        List<Sentry> sentries = planModel.getSentries();
        assertThat(sentries).hasSize(3);
        Sentry sentry = sentries.get(0);
        assertThat(sentry.getExtensionElements()).hasSize(1);
        extensionElements = sentry.getExtensionElements().get("sentryTest");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("sentryTest", "a&b&c&d&e&f&g"));
    }

}
