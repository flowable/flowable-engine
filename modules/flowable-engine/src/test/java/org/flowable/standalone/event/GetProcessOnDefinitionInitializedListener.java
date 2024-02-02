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
package org.flowable.standalone.event;

import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

public class GetProcessOnDefinitionInitializedListener extends AbstractFlowableEngineEventListener {

    public static String processId;

    @Override
    protected void entityInitialized(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof ProcessDefinitionEntity) {
            Process process = ProcessDefinitionUtil.getProcess(((ProcessDefinitionEntity) event.getEntity()).getId());
            processId = process.getId();
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
