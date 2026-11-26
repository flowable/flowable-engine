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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.junit.jupiter.api.Test;

class NonAsciiClasspathSchemaValidationTest {

    private static final String XSD_PACKAGE = "org/flowable/impl/bpmn/parser/";
    private static final List<String> SCHEMA_FILES = List.of(
            "BPMN20.xsd", "BPMNDI.xsd", "Semantic.xsd", "DC.xsd", "DI.xsd");

    private static final String MINIMAL_BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         targetNamespace="http://example.com/process">
              <process id="process1" isExecutable="true">
                <startEvent id="start" />
              </process>
            </definitions>
            """;

    @Test
    void shouldValidateSchemaWhenXsdIsLoadedFromNonAsciiPath() throws Exception {
        Path tempDir = Files.createTempDirectory("flowable-流程-");
        try {
            Path parserDir = tempDir.resolve(XSD_PACKAGE);
            Files.createDirectories(parserDir);
            ClassLoader runtimeLoader = BpmnXMLConverter.class.getClassLoader();
            for (String name : SCHEMA_FILES) {
                try (InputStream in = runtimeLoader.getResourceAsStream(XSD_PACKAGE + name)) {
                    assertThat(in).as(name).isNotNull();
                    Files.copy(in, parserDir.resolve(name));
                }
            }

            URL rawFileUrl = new URL("file:" + parserDir.resolve("BPMN20.xsd").toAbsolutePath());
            ClassLoader classLoader = new ClassLoader(runtimeLoader) {
                @Override
                public URL getResource(String name) {
                    if ((XSD_PACKAGE + "BPMN20.xsd").equals(name)) {
                        return rawFileUrl;
                    }
                    return super.getResource(name);
                }
            };

            BpmnXMLConverter converter = new BpmnXMLConverter();
            converter.setClassloader(classLoader);

            BpmnModel model = converter.convertToBpmnModel(
                    new InputStreamSource(new ByteArrayInputStream(MINIMAL_BPMN.getBytes(StandardCharsets.UTF_8))),
                    true, false, "UTF-8");

            assertThat(model).isNotNull();
            assertThat(model.getMainProcess().getId()).isEqualTo("process1");
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort cleanup of the temp schema copy
                }
            });
        }
    }
}
