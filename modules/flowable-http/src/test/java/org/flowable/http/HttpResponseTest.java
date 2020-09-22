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

import org.flowable.http.common.api.HttpHeaders;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 *
 * @deprecated
 */
@Deprecated
class HttpResponseTest {

    @Test
    void testHttpResponseWithoutApiHttpResponseDelegate() {
        HttpResponse response = new HttpResponse();

        assertThat(response.getStatusCode()).isEqualTo(0);
        assertThat(response.getProtocol()).isNull();
        assertThat(response.getReason()).isNull();
        assertThat(response.getHttpHeaders()).isNull();
        assertThat(response.getBody()).isNull();
        assertThat(response.isBodyResponseHandled()).isFalse();

        response.setStatusCode(200);
        response.setProtocol("protocol");
        response.setReason("reason");
        HttpHeaders httpHeaders = new HttpHeaders();
        response.setHttpHeaders(httpHeaders);
        response.setBody("body test");
        response.setBodyResponseHandled(true);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getProtocol()).isEqualTo("protocol");
        assertThat(response.getReason()).isEqualTo("reason");
        assertThat(response.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(response.getBody()).isEqualTo("body test");
        assertThat(response.isBodyResponseHandled()).isTrue();
    }

    @Test
    void testHttpResponseWithApiHttpResponseDelegate() {
        org.flowable.http.common.api.HttpResponse apiResponse = new org.flowable.http.common.api.HttpResponse();

        assertThat(apiResponse.getStatusCode()).isEqualTo(0);
        assertThat(apiResponse.getProtocol()).isNull();
        assertThat(apiResponse.getReason()).isNull();
        assertThat(apiResponse.getHttpHeaders()).isNull();
        assertThat(apiResponse.getBody()).isNull();
        assertThat(apiResponse.isBodyResponseHandled()).isFalse();

        HttpResponse response = HttpResponse.fromApiHttpResponse(apiResponse);

        assertThat(response.getStatusCode()).isEqualTo(0);
        assertThat(response.getProtocol()).isNull();
        assertThat(response.getReason()).isNull();
        assertThat(response.getHttpHeaders()).isNull();
        assertThat(response.getBody()).isNull();
        assertThat(response.isBodyResponseHandled()).isFalse();

        response.setStatusCode(200);
        response.setProtocol("protocol");
        response.setReason("reason");
        HttpHeaders httpHeaders = new HttpHeaders();
        response.setHttpHeaders(httpHeaders);
        response.setBody("body test");
        response.setBodyResponseHandled(true);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getProtocol()).isEqualTo("protocol");
        assertThat(response.getReason()).isEqualTo("reason");
        assertThat(response.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(response.getBody()).isEqualTo("body test");
        assertThat(response.isBodyResponseHandled()).isTrue();

        assertThat(apiResponse.getStatusCode()).isEqualTo(200);
        assertThat(apiResponse.getProtocol()).isEqualTo("protocol");
        assertThat(apiResponse.getReason()).isEqualTo("reason");
        assertThat(apiResponse.getHttpHeaders()).isSameAs(httpHeaders);
        assertThat(apiResponse.getBody()).isEqualTo("body test");
        assertThat(apiResponse.isBodyResponseHandled()).isTrue();
    }

}
