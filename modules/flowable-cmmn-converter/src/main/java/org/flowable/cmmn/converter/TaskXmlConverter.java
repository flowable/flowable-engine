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
import org.flowable.cmmn.model.Task;

/**
 * @author Joram Barrez
 */
public class TaskXmlConverter extends PlanItemDefinitiomXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_TASK;
    }
    
    @Override
    public boolean isCmmnElement() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Task task = new Task();
        convertCommonTaskAttributes(xtr, task);
        return task;
    }

    protected void convertCommonTaskAttributes(XMLStreamReader xtr, Task task) {
        task.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        
        String isBlockingString = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IS_BLOCKING);
        if (StringUtils.isNotEmpty(isBlockingString)) {
            task.setBlocking(Boolean.valueOf(isBlockingString));
        }
        
        String className = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CLASS);
        if (StringUtils.isNotEmpty(className)) {
            task.setClassName(className);
        }
    }
    
}