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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;

/**
 * @author martin.grofcik
 */
public class InMemoryRecordFlowableEventListener extends AbstractRecordFlowableEventListener {

    private Collection<SimulationEvent> events;

    public InMemoryRecordFlowableEventListener(List<Function<FlowableEvent, SimulationEvent>> transformers) {
        super(transformers);
        events = new HashSet<>();
    }

    @Override
    public Collection<SimulationEvent> getSimulationEvents() {
        return events;
    }

    @Override
    protected void store(Collection<SimulationEvent> simulationEvents) {
        events.addAll(simulationEvents);
    }

}
