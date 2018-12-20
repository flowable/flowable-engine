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
package org.flowable.crystalball.simulator.impl;

import org.flowable.crystalball.simulator.delegate.event.impl.AbstractRecordFlowableEventListener;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * This class is factory for recordable process engines
 */
public class RecordableProcessEngineFactory extends SimulationProcessEngineFactory {

    public RecordableProcessEngineFactory(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractRecordFlowableEventListener listener) {
        super(processEngineConfiguration);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    public ProcessEngineImpl getObject() {
        ProcessEngineImpl processEngine = super.getObject();

        return processEngine;
    }
}
