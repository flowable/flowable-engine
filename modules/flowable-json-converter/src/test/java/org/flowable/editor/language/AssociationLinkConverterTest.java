package org.flowable.editor.language;

import org.flowable.bpmn.model.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssociationLinkConverterTest extends AbstractConverterTest {
    @Override
    protected String getResource() {
        return "test.association.json";
    }

    @Test
    public void testBoundaryCompensationConverter() throws Exception {
        final BpmnModel bpmnModel = readJsonFile();
        final List<BoundaryEvent> boundaryEvents =
                bpmnModel.getMainProcess().findFlowElementsOfType(BoundaryEvent.class);
        assertEquals(1, boundaryEvents.size());
        final List<EventDefinition> eventDefinitions = boundaryEvents.get(0).getEventDefinitions();
        assertEquals(1, eventDefinitions.size());
        assertTrue(eventDefinitions.get(0) instanceof CompensateEventDefinition);
        final List<Association> associations =
                bpmnModel.getMainProcess().findAssociationsWithSourceRefRecursive("userTask1");
        assertEquals(1, associations.size());
    }
}
