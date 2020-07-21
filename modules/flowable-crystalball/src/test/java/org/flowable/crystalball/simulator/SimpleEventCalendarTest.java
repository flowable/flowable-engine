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
package org.flowable.crystalball.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Comparator;
import java.util.Date;

import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DefaultClockImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class SimpleEventCalendarTest {
    protected Comparator<SimulationEvent> comparator = new SimulationEventComparator();
    protected Clock clock = new DefaultClockImpl();

    @Before
    public void setUp() {
        this.clock.setCurrentTime(new Date(0));
    }

    @Test
    public void testIsEmpty() throws Exception {
        EventCalendar calendar = new SimpleEventCalendar(clock, comparator);
        assertThat(calendar.isEmpty()).isTrue();
        SimulationEvent event = calendar.removeFirstEvent();
        assertThat(event).isNull();
    }

    @Test
    public void testAddEventsAndRemoveFirst() throws Exception {
        SimulationEvent event1 = new SimulationEvent.Builder("any type").simulationTime(1).build();
        SimulationEvent event2 = new SimulationEvent.Builder("any type").simulationTime(2).build();
        EventCalendar calendar = new SimpleEventCalendar(clock, comparator);

        calendar.addEvent(event1);
        calendar.addEvent(event2);
        calendar.addEvent(event1);

        SimulationEvent event = calendar.removeFirstEvent();
        assertThat(event).isEqualTo(event1);
        event = calendar.removeFirstEvent();
        assertThat(event).isEqualTo(event1);
        event = calendar.removeFirstEvent();
        assertThat(event).isEqualTo(event2);
    }

    @Test
    public void testClear() throws Exception {
        SimulationEvent event1 = new SimulationEvent.Builder("any type").simulationTime(1).build();
        EventCalendar calendar = new SimpleEventCalendar(clock, comparator);

        calendar.addEvent(event1);

        calendar.clear();
        assertThat(calendar.isEmpty()).isTrue();
        assertThat(calendar.removeFirstEvent()).isNull();
    }

    @Test(expected = RuntimeException.class)
    public void testRunEventFromPast() throws Exception {
        SimulationEvent event1 = new SimulationEvent.Builder("any type").simulationTime(1).build();
        EventCalendar calendar = new SimpleEventCalendar(clock, comparator);

        calendar.addEvent(event1);
        this.clock.setCurrentTime(new Date(2));
        calendar.removeFirstEvent();
        fail("RuntimeException expected");
    }
}
