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
