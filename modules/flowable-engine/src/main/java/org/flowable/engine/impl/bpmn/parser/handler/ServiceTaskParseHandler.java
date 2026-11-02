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

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Joram Barrez
 */
public class ServiceTaskParseHandler extends AbstractServiceTaskParseHandler<ServiceTask> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return ServiceTask.class;
    }

    @Override
    public Set<Class<? extends BaseElement>> getHandledTypes() {
        return Set.of(ServiceTask.class);
    }
}
