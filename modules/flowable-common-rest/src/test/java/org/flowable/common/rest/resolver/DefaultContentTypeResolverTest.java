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
package org.flowable.common.rest.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Filip Hrisafov
 */
class DefaultContentTypeResolverTest {

    @ParameterizedTest
    @MethodSource("knownFileExtensionsParameters")
    void knownFileExtensions(String resourceName, String contentType) {
        ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        assertThat(contentTypeResolver.resolveContentType(resourceName)).isEqualTo(contentType);
    }

    @ParameterizedTest
    @MethodSource("knownFileExtensionsParameters")
    void knownFileExtensionsCaseInsensitive(String resourceName, String contentType) {
        ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        assertThat(contentTypeResolver.resolveContentType(resourceName.toUpperCase(Locale.ROOT))).isEqualTo(contentType);
    }

    @Test
    void unknownFileExtension() {
        ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        assertThat(contentTypeResolver.resolveContentType("test.docx")).isEqualTo("application/octet-stream");
    }

    @Test
    void unknownFileExtensionWithCustomUnknown() {
        DefaultContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        contentTypeResolver.setUnknownFileContentType("application/unknown");
        assertThat(contentTypeResolver.resolveContentType("test.docx")).isEqualTo("application/unknown");
    }

    @Test
    void addFileExtensionMapping() {
        DefaultContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        contentTypeResolver.addFileExtensionMapping("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(contentTypeResolver.resolveContentType("test.docx")).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(contentTypeResolver.resolveContentType("test.doc")).isEqualTo("application/octet-stream");
    }

    @Test
    void missingResourceName() {
        ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();
        assertThat(contentTypeResolver.resolveContentType(null)).isNull();
        assertThat(contentTypeResolver.resolveContentType("")).isNull();
    }

    static Stream<Arguments> knownFileExtensionsParameters() {
        return Stream.of(
                Arguments.of("oneTaskProcess.bpmn", "text/xml"),
                Arguments.of("oneTaskProcess.bpmn20.xml", "text/xml"),
                Arguments.of("oneTaskCase.cmmn.xml", "text/xml"),
                Arguments.of("oneTaskCase.cmmn", "text/xml"),
                Arguments.of("test.dmn", "text/xml"),
                Arguments.of("test.dmn.xml", "text/xml"),
                Arguments.of("test.app", "application/json"),
                Arguments.of("test.channel", "application/json"),
                Arguments.of("test.event", "application/json"),
                Arguments.of("test.form", "application/json"),
                Arguments.of("test.png", "image/png"),
                Arguments.of("test.txt", "text/plain")
        );
    }
}