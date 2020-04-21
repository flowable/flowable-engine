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
package org.flowable.cmmn.converter.export;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;

public class CaseTaskExport extends AbstractChildTaskExport<CaseTask> {

    @Override
    protected Class<CaseTask> getExportablePlanItemDefinitionClass() {
        return CaseTask.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(CaseTask caseTask) {
        return ELEMENT_CASE_TASK;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(CaseTask caseTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(caseTask, xtw);
        TaskExport.writeCommonTaskAttributes(caseTask, xtw);
        if (caseTask.getFallbackToDefaultTenant() != null) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, caseTask.getFallbackToDefaultTenant().toString());
        }
        if (caseTask.isSameDeployment()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SAME_DEPLOYMENT, "true");
        }
        if (StringUtils.isNotEmpty(caseTask.getCaseInstanceIdVariableName())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_ID_VARIABLE_NAME, caseTask.getCaseInstanceIdVariableName());
        }
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, CaseTask caseTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(model, caseTask, xtw);

        // Always export the case reference as an expression, even if the caseRef is set
        if (StringUtils.isNotEmpty(caseTask.getCaseRef()) || StringUtils.isNotEmpty(caseTask.getCaseRefExpression())) {
            xtw.writeStartElement(ELEMENT_CASE_REF_EXPRESSION);
            xtw.writeCData(
                StringUtils.isNotEmpty(caseTask.getCaseRef()) ?
                    caseTask.getCaseRef() :
                    caseTask.getCaseRefExpression()
            );
            xtw.writeEndElement();
        }
    }

}
