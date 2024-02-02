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
package org.flowable.engine.impl.persistence.entity;

import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

public abstract class AbstractBpmnEngineVariableScopeEntity extends VariableScopeImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public String getIdPrefix() {
        return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX;
    }
}
