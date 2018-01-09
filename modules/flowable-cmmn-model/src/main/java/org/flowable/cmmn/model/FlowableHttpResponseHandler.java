package org.flowable.cmmn.model;

public class FlowableHttpResponseHandler extends AbstractFlowableHttpHandler {
    @Override
    public FlowableHttpResponseHandler clone() {
        FlowableHttpResponseHandler clone = new FlowableHttpResponseHandler();
        clone.setValues(this);
        return clone;
    }

}
