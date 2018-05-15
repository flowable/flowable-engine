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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.hamcrest.core.Is;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author martin.grofcik
 */
public class DecisionTaskJsonConverterTest extends AbstractConverterTest {
    @Override
    protected String getResource() {
        return "test.dmnTaskModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("dmnExportCase", caseModel.getId());
        assertEquals("dmnExportCase", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);
        assertEquals("casePlanModel", planModelStage.getId());

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("dmnTask", planItem.getName());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof DecisionTask);
        DecisionTask decisionTask = (DecisionTask) planItemDefinition;
        assertEquals("sid-F4BCA0C7-8737-4279-B50F-59272C7C65A2", decisionTask.getId());
        assertEquals("dmnTask", decisionTask.getName());

        FieldExtension fieldExtension = new FieldExtension();
        fieldExtension.setFieldName("decisionTaskThrowErrorOnNoHits");
        fieldExtension.setStringValue("false");
        assertThat(((DecisionTask) planItemDefinition).getFieldExtensions(), Is.is(Collections.singletonList(fieldExtension)));
    }
}
