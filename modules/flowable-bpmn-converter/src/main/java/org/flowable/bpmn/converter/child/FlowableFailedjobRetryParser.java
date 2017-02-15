package org.flowable.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;

public class FlowableFailedjobRetryParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return FAILED_JOB_RETRY_TIME_CYCLE;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof Activity))
            return;
        String cycle = xtr.getElementText();
        if (cycle == null || cycle.isEmpty()) {
            return;
        }
        ((Activity) parentElement).setFailedJobRetryTimeCycleValue(cycle);
    }

}
