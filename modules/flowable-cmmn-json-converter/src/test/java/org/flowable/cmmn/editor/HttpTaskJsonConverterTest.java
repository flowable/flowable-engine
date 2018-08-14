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
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.hamcrest.core.Is;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author martin.grofcik
 */
public class HttpTaskJsonConverterTest extends AbstractConverterTest {
    @Override
    protected String getResource() {
        return "test.httpTaskModel.json";
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
        assertEquals("HttpTaskName", planItem.getName());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof ServiceTask);
        ServiceTask serviceTask = (ServiceTask) planItemDefinition;
        assertEquals("http", serviceTask.getType());
        assertEquals("sid-24B595C4-EE31-4A3E-A94F-CF2D2B13247B", serviceTask.getId());
        assertEquals("HttpTaskName", serviceTask.getName());
        assertEquals("documentation", serviceTask.getDocumentation());
        assertEquals("AbcClass", serviceTask.getImplementation());

        List<FieldExtension> fieldExtensions = new ArrayList<>();
        fieldExtensions.add(createFieldExtension("requestMethod", "httptaskrequestmethod"));
        fieldExtensions.add(createFieldExtension("requestUrl", "httptaskrequesturl"));
        fieldExtensions.add(createFieldExtension("requestHeaders", "httptaskrequestheaders"));
        fieldExtensions.add(createFieldExtension("requestBodyEncoding", "httptaskrequestbodyencoding"));
        fieldExtensions.add(createFieldExtension("requestTimeout", "httptaskrequesttimeout"));
        fieldExtensions.add(createFieldExtension("disallowRedirects", "httptaskdisallowredirects"));
        fieldExtensions.add(createFieldExtension("failStatusCodes", "httptaskfailstatuscodes"));
        fieldExtensions.add(createFieldExtension("handleStatusCodes", "httptaskhandlestatuscodes"));
        fieldExtensions.add(createFieldExtension("ignoreException","httptaskignoreexception"));
        fieldExtensions.add(createFieldExtension("saveRequestVariables","httptasksaverequestvariables"));
        fieldExtensions.add(createFieldExtension("saveResponseParameters","httptasksaveresponseparameters"));
        fieldExtensions.add(createFieldExtension("resultVariablePrefix","httptaskresultvariableprefix"));

        assertThat(((ServiceTask) planItemDefinition).getFieldExtensions(), Is.is(
                    fieldExtensions
                )
        );
    }

    protected FieldExtension createFieldExtension(String name, String value) {
        return new FieldExtension(name, value, null);
    }
}
