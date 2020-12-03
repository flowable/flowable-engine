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

import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;

public class HumanTaskExport extends AbstractPlanItemDefinitionExport<HumanTask> {

    @Override
    protected Class<HumanTask> getExportablePlanItemDefinitionClass() {
        return HumanTask.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(HumanTask planItemDefinition) {
        return ELEMENT_HUMAN_TASK;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(HumanTask humanTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(humanTask, xtw);

        TaskExport.writeCommonTaskAttributes(humanTask, xtw);
        if (StringUtils.isNotEmpty(humanTask.getAssignee())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_ASSIGNEE, humanTask.getAssignee());
        }

        if (StringUtils.isNotEmpty(humanTask.getOwner())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_OWNER, humanTask.getOwner());
        }

        if (humanTask.getCandidateUsers() != null && humanTask.getCandidateUsers().size() > 0) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CANDIDATE_USERS,
                    convertListToCommaSeparatedString(humanTask.getCandidateUsers()));
        }

        if (humanTask.getCandidateGroups() != null && humanTask.getCandidateGroups().size() > 0) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CANDIDATE_GROUPS,
                    convertListToCommaSeparatedString(humanTask.getCandidateGroups()));
        }

        if (StringUtils.isNotEmpty(humanTask.getFormKey())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_KEY, humanTask.getFormKey());
        }

        if (!humanTask.isSameDeployment()) {
            // default is true
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SAME_DEPLOYMENT, "false");
        }
        
        if (StringUtils.isNotEmpty(humanTask.getValidateFormFields())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_FIELD_VALIDATION, humanTask.getValidateFormFields());
        }

        if (StringUtils.isNotEmpty(humanTask.getPriority())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_PRIORITY, humanTask.getPriority());
        }
        
        if (StringUtils.isNotEmpty(humanTask.getDueDate())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_DUE_DATE, humanTask.getDueDate());
        }
        
        if (StringUtils.isNotEmpty(humanTask.getCategory())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CATEGORY, humanTask.getCategory());
        }

        if (StringUtils.isNotEmpty(humanTask.getTaskIdVariableName())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_ID_VARIABLE_NAME, humanTask.getTaskIdVariableName());
        }
    }

    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, HumanTask humanTask, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        boolean extensionElementsWritten = super.writePlanItemDefinitionExtensionElements(model, humanTask, didWriteExtensionElement, xtw);
        return FlowableListenerExport.writeFlowableListeners(xtw, CmmnXmlConstants.ELEMENT_TASK_LISTENER, humanTask.getTaskListeners(), extensionElementsWritten);
    }

    protected static String convertListToCommaSeparatedString(List<String> values) {
        StringBuilder valueBuilder = new StringBuilder();
        for (String value : values) {
            if (valueBuilder.length() > 0) {
                valueBuilder.append(",");
            }

            valueBuilder.append(value);
        }

        return valueBuilder.toString();
    }
}
