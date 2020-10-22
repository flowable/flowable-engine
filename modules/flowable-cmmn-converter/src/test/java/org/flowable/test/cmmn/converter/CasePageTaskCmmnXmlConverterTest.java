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
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class CasePageTaskCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/case-page-task.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        // Case
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId)
                .containsExactly("casePageCase");

        Stage planModel = cmmnModel.getCases().get(0).getPlanModel();

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertThat(planItemDefinitions).hasSize(2);
        assertThat(planModel.findPlanItemDefinitionsOfType(CasePageTask.class, false)).hasSize(2);

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems).hasSize(2);

        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        assertThat(planItemTaskA.getEntryCriteria()).isEmpty();
        assertThat(planItemTaskA.getItemControl()).isNotNull();
        assertThat(planItemTaskA.getItemControl().getParentCompletionRule()).isNotNull();
        assertThat(planItemTaskA.getItemControl().getParentCompletionRule().getType()).isEqualTo(ParentCompletionRule.IGNORE);

        PlanItemDefinition planItemDefinition = planItemTaskA.getPlanItemDefinition();
        assertThat(planItemDefinition)
                .isInstanceOfSatisfying(CasePageTask.class, taskA -> {
                    assertThat(planItemTaskA.getEntryCriteria()).isEmpty();
                    assertThat(taskA.getType()).isEqualTo(CasePageTask.TYPE);
                    assertThat(taskA.getName()).isEqualTo("A");
                    assertThat(taskA.getFormKey()).isEqualTo("testKey");
                    assertThat(taskA.getLabel()).isEqualTo("Label 1");
                    assertThat(taskA.getIcon()).isEqualTo("Icon 1");
                    assertThat(taskA.getAssignee()).isEqualTo("johndoe");
                    assertThat(taskA.getOwner()).isEqualTo("janedoe");
                    assertThat(taskA.getCandidateUsers()).containsExactly("johndoe", "janedoe");
                    assertThat(taskA.getCandidateGroups()).containsExactly("sales", "management");
                });

        PlanItem planItemTaskB = cmmnModel.findPlanItem("planItemTaskB");
        planItemDefinition = planItemTaskB.getPlanItemDefinition();
        assertThat(planItemTaskB.getEntryCriteria()).hasSize(1);
        assertThat(planItemTaskB.getItemControl()).isNotNull();
        assertThat(planItemTaskB.getItemControl().getParentCompletionRule()).isNotNull();
        assertThat(planItemTaskB.getItemControl().getParentCompletionRule().getType()).isEqualTo(ParentCompletionRule.IGNORE);
        assertThat(planItemDefinition)
                .isInstanceOfSatisfying(CasePageTask.class, taskB -> {
                    assertThat(taskB.getType()).isEqualTo(CasePageTask.TYPE);
                    assertThat(taskB.getName()).isEqualTo("B");
                    assertThat(taskB.getExtensionElements()).hasSize(1);
                    List<ExtensionElement> extensionElements = taskB.getExtensionElements().get("index");
                    assertThat(extensionElements).hasSize(1);
                    assertThat(extensionElements)
                            .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                            .containsExactly(tuple("index", "0"));
                });
    }

}
