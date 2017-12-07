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
import org.flowable.cmmn.model.ProcessTask;

public class ProcessTaskExport extends AbstractPlanItemDefinitionExport {

    public static void writeProcessTask(ProcessTask processTask, XMLStreamWriter xtw) throws Exception {
        // start process task element
        xtw.writeStartElement(ELEMENT_PROCESS_TASK);
        writeCommonPlanItemDefinitionAttributes(processTask, xtw);
        writeBlockingAttribute(xtw, processTask);

        if (StringUtils.isNotEmpty(processTask.getProcessRef()) || StringUtils.isNotEmpty(processTask.getProcessRefExpression())) {
            xtw.writeStartElement(ELEMENT_PROCESS_REF_EXPRESSION);
            xtw.writeCData(
                    StringUtils.isNotEmpty(processTask.getProcessRef()) ?
                            processTask.getProcessRef():
                            processTask.getProcessRefExpression()
            );
            xtw.writeEndElement();
        }

        // end process task element
        xtw.writeEndElement();
    }
}
