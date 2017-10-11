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

public class CaseTaskExport extends AbstractPlanItemDefinitionExport {
    
    public static void writeCaseTask(CaseTask caseTask, XMLStreamWriter xtw) throws Exception {
        // start case task element
        xtw.writeStartElement(ELEMENT_CASE_TASK);
        writeCommonPlanItemDefinitionAttributes(caseTask, xtw);
        writeBlockingAttribute(xtw, caseTask);
        
        if (StringUtils.isNotEmpty(caseTask.getCaseRef())) {
            xtw.writeAttribute(ATTRIBUTE_CASE_REF, caseTask.getCaseRef());
        }
        
        // end case task element
        xtw.writeEndElement();
    }
}
