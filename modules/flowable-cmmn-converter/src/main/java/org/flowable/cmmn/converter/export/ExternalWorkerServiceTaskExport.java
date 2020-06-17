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
import org.flowable.cmmn.model.ExternalWorkerServiceTask;

public class ExternalWorkerServiceTaskExport extends AbstractPlanItemDefinitionExport<ExternalWorkerServiceTask> {

    @Override
    public String getPlanItemDefinitionXmlElementValue(ExternalWorkerServiceTask casePageTask) {
        return ELEMENT_TASK;
    }

    @Override
    public void writePlanItemDefinitionSpecificAttributes(ExternalWorkerServiceTask externalWorkerServiceTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(externalWorkerServiceTask, xtw);
        TaskExport.writeCommonTaskAttributes(externalWorkerServiceTask, xtw);

        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TYPE, ExternalWorkerServiceTask.TYPE);

        if (!externalWorkerServiceTask.isAsync() ) {
            // Write the exclusive only if not async (otherwise it is added in the TaskExport)
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_EXCLUSIVE, String.valueOf(externalWorkerServiceTask.isExclusive()));
        }

        if (StringUtils.isNotEmpty(externalWorkerServiceTask.getTopic())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_EXTERNAL_WORKER_TOPIC,
                    externalWorkerServiceTask.getTopic());
        }

    }

    @Override
    protected Class<? extends ExternalWorkerServiceTask> getExportablePlanItemDefinitionClass() {
        return ExternalWorkerServiceTask.class;
    }

}
