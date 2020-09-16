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
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;
import java.util.Collections;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class HttpHeadersTest {

    @Test
    void parseFromNullString() {
        HttpHeaders headers = HttpHeaders.parseFromString(null);
        assertThat(headers).isEmpty();
        assertThat(headers.formatAsString()).isEmpty();
    }

    @Test
    void parseFromEmptyString() {
        HttpHeaders headers = HttpHeaders.parseFromString("");
        assertThat(headers).isEmpty();
        assertThat(headers.formatAsString()).isEmpty();
    }

    @Test
    void parseFromStringWithSingleHeader() {
        HttpHeaders headers = HttpHeaders.parseFromString("Content-Type: application/json");

        assertThat(headers)
                .containsOnly(
                        entry("Content-Type", Collections.singletonList("application/json"))
                );
        assertThat(headers.formatAsString()).isEqualTo("Content-Type: application/json");

        headers = HttpHeaders.parseFromString("Content-Type:application/json");

        assertThat(headers)
                .containsOnly(
                        entry("Content-Type", Collections.singletonList("application/json"))
                );
        assertThat(headers.formatAsString()).isEqualTo("Content-Type:application/json");
    }

    @Test
    void parseFromInvalidStringHeader() {
        assertThatThrownBy(() -> HttpHeaders.parseFromString("Content-Type"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Header line 'Content-Type' is invalid");
    }

    @Test
    void parseFromStringWithMultipleHeaders() {
        HttpHeaders headers = HttpHeaders.parseFromString("Content-Type: application/json\nTest: test1\nTest: test2");

        assertThat(headers)
                .containsOnly(
                        entry("Content-Type", Collections.singletonList("application/json")),
                        entry("Test", Arrays.asList("test1", "test2"))
                );
        assertThat(headers.formatAsString()).isEqualTo("Content-Type: application/json\nTest: test1\nTest: test2");
    }

    @Test
    void parseFromStringWithHeaderWithoutValue() {
        HttpHeaders headers = HttpHeaders.parseFromString("Test-NoValue:");

        assertThat(headers)
                .containsOnly(
                        entry("Test-NoValue", Collections.singletonList(""))
                );
        assertThat(headers.formatAsString()).isEqualTo("Test-NoValue:");
    }

    @Test
    void formatAsStringMultipleHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", "application/json");
        headers.add("Test", "test1");
        headers.add("Test", "test2");

        assertThat(headers)
                .containsOnly(
                        entry("Content-Type", Collections.singletonList("application/json")),
                        entry("Test", Arrays.asList("test1", "test2"))
                );
        assertThat(headers.formatAsString()).isEqualTo("Content-Type: application/json\nTest: test1\nTest: test2");
    }

    @Test
    void formatAsStringWithHeaderWithoutValue() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Test-NoValue", null);

        assertThat(headers)
                .containsOnly(
                        entry("Test-NoValue", Collections.singletonList(null))
                );
        assertThat(headers.formatAsString()).isEqualTo("Test-NoValue:");
    }
}