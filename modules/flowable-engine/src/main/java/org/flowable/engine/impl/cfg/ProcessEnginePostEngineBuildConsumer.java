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
package org.flowable.engine.impl.cfg;

import java.util.function.Consumer;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;

/**
 * @author Filip Hrisafov
 */
public class ProcessEnginePostEngineBuildConsumer implements Consumer<ProcessEngine> {

    @Override
    public void accept(ProcessEngine processEngine) {
        ProcessEngineConfigurationImpl engineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(processEngine);
            }
        }

        engineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createGlobalEvent(FlowableEngineEventType.ENGINE_CREATED),
                engineConfiguration.getEngineCfgKey());

        if (engineConfiguration.isHandleProcessEngineExecutorsAfterEngineCreate()) {
            processEngine.startExecutors();
        }

        // trigger build of Flowable 5 Engine
        if (engineConfiguration.isFlowable5CompatibilityEnabled()) {
            Flowable5CompatibilityHandler flowable5CompatibilityHandler = engineConfiguration.getFlowable5CompatibilityHandler();
            if (flowable5CompatibilityHandler != null) {
                engineConfiguration.getCommandExecutor().execute(commandContext -> {
                    flowable5CompatibilityHandler.getRawProcessEngine();
                    return null;
                });
            }
        }

        engineConfiguration.postProcessEngineInitialisation();
    }
}
