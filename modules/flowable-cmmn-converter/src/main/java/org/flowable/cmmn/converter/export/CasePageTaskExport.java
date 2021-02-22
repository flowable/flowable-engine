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
import org.flowable.cmmn.model.CasePageTask;

public class CasePageTaskExport extends AbstractPlanItemDefinitionExport<CasePageTask> {

    @Override
    public String getPlanItemDefinitionXmlElementValue(CasePageTask casePageTask) {
        return ELEMENT_TASK;
    }

    @Override
    public void writePlanItemDefinitionSpecificAttributes(CasePageTask casePageTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(casePageTask, xtw);
        TaskExport.writeCommonTaskAttributes(casePageTask, xtw);

        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TYPE, CasePageTask.TYPE);
        
        if (StringUtils.isNotEmpty(casePageTask.getFormKey())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_KEY, casePageTask.getFormKey());
        }

        if (!casePageTask.isSameDeployment()) {
            // default is true
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SAME_DEPLOYMENT, "false");
        }
        
        if (StringUtils.isNotEmpty(casePageTask.getLabel())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_LABEL, casePageTask.getLabel());
        }
        
        if (StringUtils.isNotEmpty(casePageTask.getIcon())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_ICON, casePageTask.getIcon());
        }
        
        if (StringUtils.isNotEmpty(casePageTask.getAssignee())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_ASSIGNEE, casePageTask.getAssignee());
        }
        
        if (StringUtils.isNotEmpty(casePageTask.getOwner())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_OWNER, casePageTask.getOwner());
        }
        
        if (casePageTask.getCandidateUsers() != null && !casePageTask.getCandidateUsers().isEmpty()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CANDIDATE_USERS,
                    convertListToCommaSeparatedString(casePageTask.getCandidateUsers()));
        }

        if (casePageTask.getCandidateGroups() != null && !casePageTask.getCandidateGroups().isEmpty()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CANDIDATE_GROUPS,
                    convertListToCommaSeparatedString(casePageTask.getCandidateGroups()));
        }
    }

    @Override
    protected Class<? extends CasePageTask> getExportablePlanItemDefinitionClass() {
        return CasePageTask.class;
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
