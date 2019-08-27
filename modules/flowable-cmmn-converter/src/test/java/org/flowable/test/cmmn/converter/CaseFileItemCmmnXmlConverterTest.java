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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.model.CaseFileItem;
import org.flowable.cmmn.model.CaseFileItemDefinition;
import org.flowable.cmmn.model.CaseFileItemPropertyDefinition;
import org.flowable.cmmn.model.CaseFileItemPropertyDefinitionTypes;
import org.flowable.cmmn.model.CaseFileModel;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.FileItemSentryOnPart;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CaseFileItemCmmnXmlConverterTest extends AbstractConverterTest {

    @Test
    public void testBasicCaseFileItemProperties() throws Exception {
        CmmnModel cmmnModel = readXMLFile("org/flowable/test/cmmn/converter/case-file-item.cmmn");

        CaseFileModel fileModel = cmmnModel.getPrimaryCase().getFileModel();
        assertNotNull(fileModel);
        assertEquals("myFileModel", fileModel.getId());
        assertEquals(17, fileModel.getXmlRowNumber());

        assertEquals(1, fileModel.getCaseFileItems().size());

        CaseFileItem caseFileItem = fileModel.getCaseFileItems().get(0);
        assertEquals(18, caseFileItem.getXmlRowNumber());
        assertEquals("fileItem1", caseFileItem.getId());
        assertEquals("My File Item", caseFileItem.getName());
        assertEquals(CaseFileItem.CaseFileItemMultiplicity.EXACTLY_ONE, caseFileItem.getMultiplicity());
        assertEquals(0, caseFileItem.getCaseFileItems().size());

        assertEquals("fileItemDefinition1", caseFileItem.getCaseFileItemDefinitionRef());
    }

    @Test
    public void testDefinitionPropertiesSet() throws Exception {
        CmmnModel cmmnModel = readXMLFile("org/flowable/test/cmmn/converter/case-file-item.cmmn");
        CaseFileItem caseFileItem = cmmnModel.getPrimaryCase().getFileModel().getCaseFileItems().get(0);
        CaseFileItemDefinition caseFileItemDefinition = caseFileItem.getCaseFileItemDefinition();

        assertNotNull(caseFileItemDefinition);
        assertEquals("fileItemDefinition1", caseFileItemDefinition.getId());
        assertEquals("My File Item Definition", caseFileItemDefinition.getName());
        assertEquals("http://www.omg.org/spec/CMMN/DefinitionType/CMISFolder", caseFileItemDefinition.getDefinitionType());

        List<CaseFileItemPropertyDefinition> propertyDefinitions = caseFileItemDefinition.getPropertyDefinitions();
        assertEquals(2, propertyDefinitions.size());

        CaseFileItemPropertyDefinition propertyDefinition1 = propertyDefinitions.get(0);
        assertEquals("prop1", propertyDefinition1.getId());
        assertEquals("Name", propertyDefinition1.getName());
        assertEquals(CaseFileItemPropertyDefinitionTypes.TYPE_STRING, propertyDefinition1.getType());

        CaseFileItemPropertyDefinition propertyDefinition2 = propertyDefinitions.get(1);
        assertEquals("prop2", propertyDefinition2.getId());
        assertEquals("Size", propertyDefinition2.getName());
        assertEquals(CaseFileItemPropertyDefinitionTypes.TYPE_INTEGER, propertyDefinition2.getType());
    }

    @Test
    public void testCaseFileItemOnPartReference() throws Exception {
        CmmnModel cmmnModel = readXMLFile("org/flowable/test/cmmn/converter/case-file-item.cmmn");
        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        List<Criterion> entryCriteria = planItemTaskA.getEntryCriteria();
        assertEquals(1, entryCriteria.size());

        Sentry sentry = entryCriteria.get(0).getSentry();
        FileItemSentryOnPart sentryOnPart = (FileItemSentryOnPart) sentry.getOnParts().get(0);
        assertEquals("addChild", sentryOnPart.getStandardEvent());
        assertEquals(1, sentry.getOnParts().size());
        assertEquals("fileItem1", sentryOnPart.getSourceRef());
        assertNotNull(sentryOnPart.getSource());
        assertEquals("My File Item", sentryOnPart.getSource().getName());
    }

}
