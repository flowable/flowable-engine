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
package org.flowable.http.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class HttpRequestTest {

    @Test
    void setBodyShouldNotBePossibleWhenMultiValuePartsAreSet() {
        HttpRequest request = new HttpRequest();
        request.addMultiValuePart(MultiValuePart.fromText("name", "kermit"));
        assertThatThrownBy(() -> request.setBody("test"))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both body and multi value parts");
        assertThat(request.getBody()).isNull();
    }

    @Test
    void setBodyShouldNotBePossibleWhenFormParametersAreSet() {
        HttpRequest request = new HttpRequest();
        request.addFormParameter("name", "kermit");
        assertThatThrownBy(() -> request.setBody("test"))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both body and form parameters");
        assertThat(request.getBody()).isNull();
    }

    @Test
    void addMultiValuePartShouldNotBePossibleWhenBodyIsSet() {
        HttpRequest request = new HttpRequest();
        request.setBody("test");
        assertThatThrownBy(() -> request.addMultiValuePart(MultiValuePart.fromText("name", "kermit")))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both body and multi value parts");
        assertThat(request.getMultiValueParts()).isNull();
    }

    @Test
    void addMultiValuePartShouldNotBePossibleWhenFormParametersAreSet() {
        HttpRequest request = new HttpRequest();
        request.addFormParameter("name", "kermit");
        assertThatThrownBy(() -> request.addMultiValuePart(MultiValuePart.fromText("name", "kermit")))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both form parameters and multi value parts");
        assertThat(request.getMultiValueParts()).isNull();
    }

    @Test
    void addFormParameterShouldNotBePossibleWhenBodyIsSet() {
        HttpRequest request = new HttpRequest();
        request.setBody("test");
        assertThatThrownBy(() -> request.addFormParameter("name", "kermit"))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both body and form parameters");
        assertThat(request.getFormParameters()).isNull();
    }

    @Test
    void addFormParameterShouldNotBePossibleWhenMultiValuePartsAreSet() {
        HttpRequest request = new HttpRequest();
        request.addMultiValuePart(MultiValuePart.fromText("name", "kermit"));
        assertThatThrownBy(() -> request.addFormParameter("name", "fozzie"))
                .isInstanceOf(FlowableIllegalStateException.class)
                .hasMessage("Cannot set both multi value parts and form parameters");
        assertThat(request.getFormParameters()).isNull();
    }

    @Test
    void getHttpHeadersAsString() {
        HttpRequest request = new HttpRequest();
        assertThat(request.getHttpHeadersAsString()).isNull();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        request.setHttpHeaders(headers);
        assertThat(request.getHttpHeadersAsString()).isEqualTo("Content-Type: application/json");
    }

    @Test
    void getSecureHttpHeadersAsString() {
        HttpRequest request = new HttpRequest();
        assertThat(request.getSecureHttpHeadersAsString()).isNull();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        request.setSecureHttpHeaders(headers);
        assertThat(request.getSecureHttpHeadersAsString()).isEqualTo("Content-Type: *****");
    }
}
