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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FormAwareServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.impl.bpmn.behavior.WebServiceActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christopher Welsch
 */
public class FormAwareServiceTaskParseHandler extends AbstractServiceTaskParseHandler<FormAwareServiceTask> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return FormAwareServiceTask.class;
    }

    @Override
    public Set<Class<? extends BaseElement>> getHandledTypes() {
        return Set.of(FormAwareServiceTask.class);
    }

}
