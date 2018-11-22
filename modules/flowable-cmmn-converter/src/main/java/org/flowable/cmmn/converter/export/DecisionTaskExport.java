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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.DecisionTask;

public class DecisionTaskExport extends AbstractPlanItemDefinitionExport<DecisionTask> {

    @Override
    protected Class<DecisionTask> getExportablePlanItemDefinitionClass() {
        return DecisionTask.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(DecisionTask decisionTask) {
        return ELEMENT_DECISION_TASK;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(DecisionTask decisionTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(decisionTask, xtw);
        TaskExport.writeCommonTaskAttributes(decisionTask, xtw);
    }
    
    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, DecisionTask decisionTask, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        boolean extensionElementWritten = super.writePlanItemDefinitionExtensionElements(model, decisionTask, didWriteExtensionElement, xtw);
        return TaskExport.writeTaskFieldExtensions(decisionTask, extensionElementWritten, xtw);
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, DecisionTask decisionTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(model, decisionTask, xtw);
        if (StringUtils.isNotEmpty(decisionTask.getDecisionRef()) || StringUtils.isNotEmpty(decisionTask.getDecisionRefExpression())) {
            xtw.writeStartElement(ELEMENT_DECISION_REF_EXPRESSION);
            xtw.writeCData(
                    StringUtils.isNotEmpty(decisionTask.getDecisionRef()) ?
                            decisionTask.getDecisionRef() :
                            decisionTask.getDecisionRefExpression()
            );
            xtw.writeEndElement();
        }
    }
}
