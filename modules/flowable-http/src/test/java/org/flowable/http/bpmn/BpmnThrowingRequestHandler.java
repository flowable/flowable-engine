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
package org.flowable.http.bpmn;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.http.common.api.HttpRequest;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.http.common.api.delegate.HttpRequestHandler;

public class BpmnThrowingRequestHandler implements HttpRequestHandler {

  @Override
  public void handleHttpRequest(VariableContainer execution, HttpRequest httpRequest, FlowableHttpClient client) {
      throw new BpmnError("httpRequestHandlerError");
  }

}
