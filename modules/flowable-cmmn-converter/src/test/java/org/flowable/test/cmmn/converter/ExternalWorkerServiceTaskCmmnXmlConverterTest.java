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
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerServiceTaskCmmnXmlConverterTest{

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/external-worker-service-task.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        // Case
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId)
                .containsExactly("externalWorkerCase");

        Stage planModel = cmmnModel.getCases().get(0).getPlanModel();

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertThat(planItemDefinitions).hasSize(2);
        assertThat(planModel.findPlanItemDefinitionsOfType(ExternalWorkerServiceTask.class, false)).hasSize(2);

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems).hasSize(2);

        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        PlanItemDefinition planItemDefinition = planItemTaskA.getPlanItemDefinition();
        assertThat(planItemTaskA.getEntryCriteria()).isEmpty();
        assertThat(planItemDefinition).isInstanceOf(ExternalWorkerServiceTask.class);

        ExternalWorkerServiceTask taskA = (ExternalWorkerServiceTask) planItemDefinition;
        assertThat(taskA.getType()).isEqualTo(ExternalWorkerServiceTask.TYPE);
        assertThat(taskA.getName()).isEqualTo("A");
        assertThat(taskA.getTopic()).isEqualTo("simple");
        assertThat(taskA.isAsync()).isFalse();
        assertThat(taskA.isExclusive()).isFalse();
        assertThat(taskA.getExtensionElements()).containsOnlyKeys("customValue");
        assertThat(taskA.getExtensionElements().get("customValue"))
                .extracting(ExtensionElement::getNamespacePrefix, ExtensionElement::getName, ExtensionElement::getElementText)
                .containsOnly(
                        tuple("flowable", "customValue", "test")
                );

        PlanItem planItemTaskB = cmmnModel.findPlanItem("planItemTaskB");
        planItemDefinition = planItemTaskB.getPlanItemDefinition();
        assertThat(planItemTaskB.getEntryCriteria()).hasSize(1);
        assertThat(planItemDefinition).isInstanceOf(ExternalWorkerServiceTask.class);
        ExternalWorkerServiceTask taskB = (ExternalWorkerServiceTask) planItemDefinition;
        assertThat(taskB.getType()).isEqualTo(ExternalWorkerServiceTask.TYPE);
        assertThat(taskB.getName()).isEqualTo("B");
        assertThat(taskB.getTopic()).isNull();
        assertThat(taskB.isAsync()).isFalse();
        assertThat(taskB.isExclusive()).isTrue();
        assertThat(taskB.getExtensionElements()).isEmpty();
    }

}
