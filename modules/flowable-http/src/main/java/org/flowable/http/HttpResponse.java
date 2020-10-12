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

import org.flowable.http.common.api.HttpHeaders;

/**
 * @author Harsha Teja Kanna.
 *
 * @deprecated
 */
@Deprecated
public class HttpResponse extends org.flowable.http.common.api.HttpResponse {

    protected final org.flowable.http.common.api.HttpResponse delegate;

    public HttpResponse() {
        this.delegate = null;
    }

    public HttpResponse(int statusCode) {
        this(statusCode, null);
    }

    public HttpResponse(int statusCode, String headers) {
        super(statusCode, headers);
        this.delegate = null;
    }

    protected HttpResponse(org.flowable.http.common.api.HttpResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getStatusCode() {
        return delegate != null ? delegate.getStatusCode() : super.getStatusCode();
    }

    @Override
    public void setStatusCode(int statusCode) {
        if (delegate != null) {
            delegate.setStatusCode(statusCode);
        } else {
            super.setStatusCode(statusCode);
        }
    }

    @Override
    public String getProtocol() {
        return delegate != null ? delegate.getProtocol() : super.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        if (delegate != null) {
            delegate.setProtocol(protocol);
        } else {
            super.setProtocol(protocol);
        }
    }

    @Override
    public String getReason() {
        return delegate != null ? delegate.getReason() : super.getReason();
    }

    @Override
    public void setReason(String reason) {
        if (delegate != null) {
            delegate.setReason(reason);
        } else {
            super.setReason(reason);
        }
    }

    /**
     * @deprecated use {@link #getHttpHeadersAsString()} instead
     */
    @Deprecated
    public String getHeaders() {
        return getHttpHeadersAsString();
    }

    /**
     * @deprecated use {@link #setHttpHeaders(HttpHeaders)} instead
     */
    @Deprecated
    public void setHeaders(String headers) {
        HttpHeaders parsedHeaders = HttpHeaders.parseFromString(headers);
        if (delegate != null) {
            delegate.setHttpHeaders(parsedHeaders);
        } else {
            super.setHttpHeaders(parsedHeaders);
        }
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return delegate != null ? delegate.getHttpHeaders() : super.getHttpHeaders();
    }

    @Override
    public String getHttpHeadersAsString() {
        return delegate != null ? delegate.getHttpHeadersAsString() : super.getHttpHeadersAsString();
    }

    @Override
    public void setHttpHeaders(HttpHeaders httpHeaders) {
        if (delegate != null) {
            delegate.setHttpHeaders(httpHeaders);
        } else {
            super.setHttpHeaders(httpHeaders);
        }
    }

    @Override
    public String getBody() {
        return delegate != null ? delegate.getBody() : super.getBody();
    }

    @Override
    public void setBody(String body) {
        if (delegate != null) {
            delegate.setBody(body);
        } else {
            super.setBody(body);
        }
    }

    @Override
    public boolean isBodyResponseHandled() {
        return delegate != null ? delegate.isBodyResponseHandled() : super.isBodyResponseHandled();
    }

    @Override
    public void setBodyResponseHandled(boolean bodyResponseHandled) {
        if (delegate != null) {
            delegate.setBodyResponseHandled(bodyResponseHandled);
        } else {
            super.setBodyResponseHandled(bodyResponseHandled);
        }
    }
    
    public static HttpResponse fromApiHttpResponse(org.flowable.http.common.api.HttpResponse response) {
        return new HttpResponse(response);
    }
}
