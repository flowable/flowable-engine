package org.flowable.http.bpmn;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.http.HttpResponse;
import org.flowable.http.delegate.HttpResponseHandler;

public class BpmnThrowingResponseHandler implements HttpResponseHandler {

  private static final long serialVersionUID = 1L;

  @Override
  public void handleHttpResponse(final VariableContainer execution, final HttpResponse httpResponse) {
      throw new BpmnError("httpResponseHandlerError");
  }
}