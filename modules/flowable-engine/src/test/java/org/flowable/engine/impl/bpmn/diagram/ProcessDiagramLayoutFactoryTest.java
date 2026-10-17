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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.flowable.common.engine.api.FlowableException;
import org.junit.jupiter.api.Test;

public class ProcessDiagramLayoutFactoryTest {

	@Test
	public void testParseXmlXXEProtection() {
		String maliciousBpmn = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE bpmn2:definitions [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
    <bpmn2:definitions id="&xxe;">
    </bpmn2:definitions>""";

		ProcessDiagramLayoutFactory factory = new ProcessDiagramLayoutFactory();

		assertThatThrownBy(() -> factory.parseXml(new ByteArrayInputStream(maliciousBpmn.getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(FlowableException.class)
			.hasMessageContaining("Error while parsing BPMN model");
	}
}
