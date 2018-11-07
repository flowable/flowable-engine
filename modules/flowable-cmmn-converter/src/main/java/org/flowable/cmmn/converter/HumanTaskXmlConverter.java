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
package org.flowable.cmmn.converter;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.HumanTask;

/**
 * @author Tijs Rademakers
 */
public class HumanTaskXmlConverter extends TaskXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_HUMAN_TASK;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        HumanTask task = new HumanTask();
        convertCommonTaskAttributes(xtr, task);
        
        task.setAssignee(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_ASSIGNEE));
        task.setOwner(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_OWNER));
        task.setPriority(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_PRIORITY));
        task.setFormKey(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_FORM_KEY));
        task.setDueDate(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_DUE_DATE));
        task.setCategory(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CATEGORY));
        
        String candidateUsersString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CANDIDATE_USERS);
        if (StringUtils.isNotEmpty(candidateUsersString)) {
            String[] candidateUsers = candidateUsersString.split(",");
            for (String candidateUser : candidateUsers) {
                task.getCandidateUsers().add(candidateUser);
            }
        }
        
        String candidateGroupsString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CANDIDATE_GROUPS);
        if (StringUtils.isNotEmpty(candidateGroupsString)) {
            String[] candidateGroups = candidateGroupsString.split(",");
            for (String candidateGroup : candidateGroups) {
                task.getCandidateGroups().add(candidateGroup);
            }
        }
        
        return task;
    }

}