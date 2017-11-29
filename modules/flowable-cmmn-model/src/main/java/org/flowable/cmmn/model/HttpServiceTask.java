package org.flowable.cmmn.model;

/**
 * @author martin.grofcik
 */
public class HttpServiceTask extends ServiceTask {

    public static final String HTTP_TASK = "http";

    public HttpServiceTask() {
        type = HTTP_TASK;
    }

    protected FlowableHttpRequestHandler httpRequestHandler;
    protected FlowableHttpResponseHandler httpResponseHandler;

    public FlowableHttpRequestHandler getHttpRequestHandler() {
        return httpRequestHandler;
    }

    public void setHttpRequestHandler(FlowableHttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }

    public FlowableHttpResponseHandler getHttpResponseHandler() {
        return httpResponseHandler;
    }

    public void setHttpResponseHandler(FlowableHttpResponseHandler httpResponseHandler) {
        this.httpResponseHandler = httpResponseHandler;
    }

    @Override
    public HttpServiceTask clone() {
        HttpServiceTask clone = new HttpServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(HttpServiceTask otherElement) {
        super.setValues(otherElement);

        if (otherElement.getHttpRequestHandler() != null) {
            setHttpRequestHandler(otherElement.getHttpRequestHandler().clone());
        }

        if (otherElement.getHttpResponseHandler() != null) {
            setHttpResponseHandler(otherElement.getHttpResponseHandler().clone());
        }
    }

}
