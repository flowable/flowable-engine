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
package org.flowable.http;

/**
 * @author Harsha Teja Kanna.
 */
public class HttpResponse {
    
    protected int statusCode;
    protected String protocol;
    protected String reason;
    protected String headers;
    protected String body;
    protected boolean bodyResponseHandled;

    public HttpResponse() {
    }

    public HttpResponse(int statusCode) {
        this(statusCode, null);
    }

    public HttpResponse(int statusCode, String headers) {
        this.statusCode = statusCode;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isBodyResponseHandled() {
        return bodyResponseHandled;
    }

    public void setBodyResponseHandled(boolean bodyResponseHandled) {
        this.bodyResponseHandled = bodyResponseHandled;
    }
}
