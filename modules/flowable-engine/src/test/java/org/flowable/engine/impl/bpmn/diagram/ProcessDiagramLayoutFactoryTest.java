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

package org.flowable.engine.impl.bpmn.diagram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.flowable.common.engine.api.FlowableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

public class ProcessDiagramLayoutFactoryTest {

    private final ProcessDiagramLayoutFactory factory = new ProcessDiagramLayoutFactory();

    @Test
    public void testExternalEntityInElementTextIsRejected(@TempDir Path tempDir) throws Exception {
        // The entity must be referenced in element text content. In an attribute value the JDK
        // rejects an external entity reference even when the parser is not hardened, so such a
        // test would also pass without the fix.
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET-CONTENT");

        String maliciousBpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE definitions [<!ENTITY xxe SYSTEM "%s">]>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                  <documentation>&xxe;</documentation>
                </definitions>
                """.formatted(secret.toUri());

        assertThatThrownBy(() -> factory.parseXml(new ByteArrayInputStream(maliciousBpmn.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Error while parsing BPMN model");
    }

    @Test
    public void testDoctypeIsRejected() {
        String bpmnWithDoctype = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE definitions [<!ENTITY a "b">]>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"/>
                """;

        assertThatThrownBy(() -> factory.parseXml(new ByteArrayInputStream(bpmnWithDoctype.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(FlowableException.class);
    }

    @Test
    public void testValidBpmnStillParses() {
        String validBpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
                  <process id="p1"/>
                </definitions>
                """;

        Document document = factory.parseXml(new ByteArrayInputStream(validBpmn.getBytes(StandardCharsets.UTF_8)));

        assertThat(document.getDocumentElement().getLocalName()).isEqualTo("definitions");
    }
}
