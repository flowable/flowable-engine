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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.flowable.http.common.api.HttpHeaders;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 *
 * @deprecated
 */
@Deprecated
class HttpRequestTest {

    @Test
    void testHttpRequestWithoutApiHttpRequestDelegate() {
        HttpRequest request = new HttpRequest();

        assertThat(request.getMethod()).isNull();
        assertThat(request.getUrl()).isNull();
        assertThat(request.getHttpHeaders()).isNull();
        assertThat(request.getBody()).isNull();
        assertThat(request.getBodyEncoding()).isNull();
        assertThat(request.getTimeout()).isEqualTo(0);
        assertThat(request.isNoRedirects()).isFalse();

        request.setMethod("GET");
        request.setUrl("flowable.org");
        HttpHeaders httpHeaders = new HttpHeaders();
        request.setHttpHeaders(httpHeaders);
        request.setBody("body test");
        request.setBodyEncoding("UTF-8");
        request.setTimeout(1000);
        request.setNoRedirects(true);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getUrl()).isEqualTo("flowable.org");
        assertThat(request.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(request.getBody()).isEqualTo("body test");
        assertThat(request.getBodyEncoding()).isEqualTo("UTF-8");
        assertThat(request.getTimeout()).isEqualTo(1000);
        assertThat(request.isNoRedirects()).isTrue();
    }

    @Test
    void testHttpRequestWithApiHttpRequestDelegate() {
        org.flowable.http.common.api.HttpRequest apiRequest = new org.flowable.http.common.api.HttpRequest();

        assertThat(apiRequest.getMethod()).isNull();
        assertThat(apiRequest.getUrl()).isNull();
        assertThat(apiRequest.getHttpHeaders()).isNull();
        assertThat(apiRequest.getBody()).isNull();
        assertThat(apiRequest.getBodyEncoding()).isNull();
        assertThat(apiRequest.getTimeout()).isEqualTo(0);
        assertThat(apiRequest.isNoRedirects()).isFalse();

        HttpRequest request = HttpRequest.fromApiHttpRequest(apiRequest);

        assertThat(request.getMethod()).isNull();
        assertThat(request.getUrl()).isNull();
        assertThat(request.getHttpHeaders()).isNull();
        assertThat(request.getBody()).isNull();
        assertThat(request.getBodyEncoding()).isNull();
        assertThat(request.getTimeout()).isEqualTo(0);
        assertThat(request.isNoRedirects()).isFalse();

        request.setMethod("GET");
        request.setUrl("flowable.org");
        HttpHeaders httpHeaders = new HttpHeaders();
        request.setHttpHeaders(httpHeaders);
        request.setBody("body test");
        request.setBodyEncoding("UTF-8");
        request.setTimeout(1000);
        request.setNoRedirects(true);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getUrl()).isEqualTo("flowable.org");
        assertThat(request.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(request.getBody()).isEqualTo("body test");
        assertThat(request.getBodyEncoding()).isEqualTo("UTF-8");
        assertThat(request.getTimeout()).isEqualTo(1000);
        assertThat(request.isNoRedirects()).isTrue();

        assertThat(apiRequest.getMethod()).isEqualTo("GET");
        assertThat(apiRequest.getUrl()).isEqualTo("flowable.org");
        assertThat(apiRequest.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(apiRequest.getBody()).isEqualTo("body test");
        assertThat(apiRequest.getBodyEncoding()).isEqualTo("UTF-8");
        assertThat(apiRequest.getTimeout()).isEqualTo(1000);
        assertThat(apiRequest.isNoRedirects()).isTrue();
    }

}
