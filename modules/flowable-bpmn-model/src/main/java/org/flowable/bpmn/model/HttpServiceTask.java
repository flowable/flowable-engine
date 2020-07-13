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
package org.flowable.bpmn.model;

/**
 * @author Tijs Rademakers
 */
public class HttpServiceTask extends ServiceTask {

    protected FlowableHttpRequestHandler httpRequestHandler;
    protected FlowableHttpResponseHandler httpResponseHandler;
    protected Boolean parallelInSameTransaction;

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

    public Boolean getParallelInSameTransaction() {
        return parallelInSameTransaction;
    }

    public void setParallelInSameTransaction(Boolean parallelInSameTransaction) {
        this.parallelInSameTransaction = parallelInSameTransaction;
    }

    @Override
    public HttpServiceTask clone() {
        HttpServiceTask clone = new HttpServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(HttpServiceTask otherElement) {
        super.setValues(otherElement);
        
        setParallelInSameTransaction(otherElement.getParallelInSameTransaction());

        if (otherElement.getHttpRequestHandler() != null) {
            setHttpRequestHandler(otherElement.getHttpRequestHandler().clone());
        }
        
        if (otherElement.getHttpResponseHandler() != null) {
            setHttpResponseHandler(otherElement.getHttpResponseHandler().clone());
        }
    }
}
