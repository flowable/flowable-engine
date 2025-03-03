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
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.ChildTask;
import org.flowable.cmmn.model.CmmnModel;

/**
 * @author Valentin Zickner
 */
public abstract class AbstractChildTaskExport<T extends ChildTask> extends AbstractPlanItemDefinitionExport<T> {

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(planItemDefinition.getBusinessKey())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_BUSINESS_KEY, planItemDefinition.getBusinessKey());
        }
        if (planItemDefinition.isInheritBusinessKey()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_INHERIT_BUSINESS_KEY, String.valueOf(planItemDefinition.isInheritBusinessKey()));
        }
    }

    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, T planItemDefinition, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        boolean extensionElementWritten = super.writePlanItemDefinitionExtensionElements(model, planItemDefinition, didWriteExtensionElement, xtw);
        extensionElementWritten = CmmnXmlUtil.writeIOParameters(ELEMENT_CHILD_TASK_IN_PARAMETERS, planItemDefinition.getInParameters(), extensionElementWritten, xtw);
        extensionElementWritten = CmmnXmlUtil.writeIOParameters(ELEMENT_CHILD_TASK_OUT_PARAMETERS, planItemDefinition.getOutParameters(), extensionElementWritten, xtw);
        return extensionElementWritten;
    }

}
