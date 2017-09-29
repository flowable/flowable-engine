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

import org.flowable.cmmn.model.Task;

public class TaskExport extends AbstractPlanItemDefinitionExport {
    
    public static void writeTask(Task task, XMLStreamWriter xtw) throws Exception {
        // start task element
        xtw.writeStartElement(ELEMENT_TASK);
        writeCommonPlanItemDefinitionAttributes(task, xtw);
        writeBlockingAttribute(xtw, task);
        
        // end task element
        xtw.writeEndElement();
    }
}
