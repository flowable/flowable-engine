package org.flowable.crystalball.simulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertTrue(calendar.isEmpty());
        SimulationEvent event = calendar.removeFirstEvent();
        assertNull(event);
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
        assertEquals(event1, event);
        event = calendar.removeFirstEvent();
        assertEquals(event1, event);
        event = calendar.removeFirstEvent();
        assertEquals(event2, event);
    }

    @Test
    public void testClear() throws Exception {
        SimulationEvent event1 = new SimulationEvent.Builder("any type").simulationTime(1).build();
        EventCalendar calendar = new SimpleEventCalendar(clock, comparator);

        calendar.addEvent(event1);

        calendar.clear();
        assertTrue(calendar.isEmpty());
        assertNull(calendar.removeFirstEvent());
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
