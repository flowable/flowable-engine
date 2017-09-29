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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Task;

public class AbstractPlanItemDefinitionExport implements CmmnXmlConstants {
    
    public static void writeCommonPlanItemDefinitionAttributes(PlanItemDefinition planItemDefinition, XMLStreamWriter xtw) throws Exception {
        xtw.writeAttribute(ATTRIBUTE_ID, planItemDefinition.getId());

        if (StringUtils.isNotEmpty(planItemDefinition.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, planItemDefinition.getName());
        }

        if (StringUtils.isNotEmpty(planItemDefinition.getDocumentation())) {

            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(planItemDefinition.getDocumentation());
            xtw.writeEndElement();
        }
    }

    public static void writeBlockingAttribute(XMLStreamWriter xtw, Task task) throws XMLStreamException {
        if (StringUtils.isEmpty(task.getBlockingExpression())) {
            if (!task.isBlocking()) { // if omitted, by default assumed true
                xtw.writeAttribute(ATTRIBUTE_IS_BLOCKING, "false");
            }
        } else {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_BLOCKING_EXPRESSION, task.getBlockingExpression());
        }
    }

}
