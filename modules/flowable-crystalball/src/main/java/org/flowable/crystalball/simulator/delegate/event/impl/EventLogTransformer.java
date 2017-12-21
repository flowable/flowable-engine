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

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.engine.event.EventLogEntry;

/**
 * This class transforms event log events into simulation events
 */
public class EventLogTransformer {
    protected List<Function<EventLogEntry, SimulationEvent>> transformers;

    public EventLogTransformer(List<Function<EventLogEntry, SimulationEvent>> transformers) {
        this.transformers = transformers;
    }

    public List<SimulationEvent> transform(List<EventLogEntry> eventLog) {
        List<SimulationEvent> simulationEvents = new ArrayList<>();
        for (EventLogEntry logEntry : eventLog) {
            simulationEvents.addAll(transformEntry(logEntry));
        }
        return simulationEvents;
    }

    protected Collection<SimulationEvent> transformEntry(EventLogEntry event) {
        List<SimulationEvent> simEvents = new ArrayList<>();
        for (Function<EventLogEntry, SimulationEvent> t : transformers) {
            SimulationEvent simEvent = t.apply(event);
            if (simEvent != null)
                simEvents.add(simEvent);
        }
        return simEvents;
    }

}
