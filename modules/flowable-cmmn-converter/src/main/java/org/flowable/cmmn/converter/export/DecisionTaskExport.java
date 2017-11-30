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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.DecisionTask;

import javax.xml.stream.XMLStreamWriter;

import static org.flowable.cmmn.converter.export.ServiceTaskExport.writeExtensions;

public class DecisionTaskExport extends AbstractPlanItemDefinitionExport {

    public static void writeDecisionTask(DecisionTask decisionTask, XMLStreamWriter xtw) throws Exception {
        // start decision task element
        xtw.writeStartElement(ELEMENT_DECISION_TASK);
        writeCommonPlanItemDefinitionAttributes(decisionTask, xtw);
        writeBlockingAttribute(xtw, decisionTask);

        writeExtensions(decisionTask, xtw);

        if (StringUtils.isNotEmpty(decisionTask.getDecisionRef()) || StringUtils.isNotEmpty(decisionTask.getDecisionRefExpression())) {
            xtw.writeStartElement(ELEMENT_DECISION_REF_EXPRESSION);
                xtw.writeCData(
                        StringUtils.isNotEmpty(decisionTask.getDecisionRef()) ?
                            decisionTask.getDecisionRef():
                            decisionTask.getDecisionRefExpression()
                );
            xtw.writeEndElement();
        }

        // end decision task element
        xtw.writeEndElement();
    }
}
