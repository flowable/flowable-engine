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
package org.flowable.crystalball.simulator.delegate.event.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;

/**
 * This class provides abstract base to records engine events
 * 
 * @author martin.grofcik
 */
public abstract class AbstractRecordFlowableEventListener extends AbstractFlowableEventListener {
    
    protected List<Function<FlowableEvent, SimulationEvent>> transformers;

    public AbstractRecordFlowableEventListener(List<Function<FlowableEvent, SimulationEvent>> transformers) {
        this.transformers = transformers;
    }

    public abstract Collection<SimulationEvent> getSimulationEvents();

    @Override
    public void onEvent(FlowableEvent event) {
        Collection<SimulationEvent> simulationEvents = transform(event);
        store(simulationEvents);
    }

    protected abstract void store(Collection<SimulationEvent> simulationEvents);

    protected Collection<SimulationEvent> transform(FlowableEvent event) {
        List<SimulationEvent> simEvents = new ArrayList<>();
        for (Function<FlowableEvent, SimulationEvent> t : transformers) {
            SimulationEvent simEvent = t.apply(event);
            if (simEvent != null)
                simEvents.add(simEvent);
        }
        return simEvents;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
    
}
