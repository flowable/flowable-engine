package org.flowable.http.bpmn;

import org.apache.http.client.HttpClient;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.http.HttpRequest;
import org.flowable.http.delegate.HttpRequestHandler;

public class BpmnThrowingRequestHandler implements HttpRequestHandler {

  private static final long serialVersionUID = 1L;

  @Override
  public void handleHttpRequest(final VariableContainer execution, final HttpRequest httpRequest, final HttpClient client) {
      throw new BpmnError("httpRequestHandlerError");
  }
}