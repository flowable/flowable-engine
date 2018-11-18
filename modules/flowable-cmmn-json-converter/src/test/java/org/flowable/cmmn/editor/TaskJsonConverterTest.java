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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.*;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author shareniu
 */
public class TaskJsonConverterTest  extends AbstractConverterTest{

    @Test
    public void convertJsonToModel() throws Exception {
        CmmnModel cmmnModel = readJsonFile();
        validateModel(cmmnModel);
    }

    protected String getResource() {
        return "test.taskBlockingexpression.json";
    }

    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("shareniu_test", caseModel.getId());
        assertEquals("shareniu_test", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("shareniu_task", planItem.getName());

        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof Task);

        Task task = (Task) planItemDefinition;
        assertEquals("shareniu_task", task.getId());
        assertEquals("shareniu_task", task.getName());
        assertEquals("${shareniu_task}",task.getBlockingExpression());


    }

    protected FieldExtension createFieldExtension(String name, String value) {
        return new FieldExtension(name, value, null);
    }
}
