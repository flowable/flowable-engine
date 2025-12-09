/*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.de.odysseus.el.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.junit.jupiter.api.Test;

class NodePrinterTest {

	@Test
	void testDump() throws IOException {
		Tree tree = new Builder(Builder.Feature.METHOD_INVOCATIONS).build("${foo.bar[baz] + foobar}");
		StringWriter writer = new StringWriter();
		NodePrinter.dump(new PrintWriter(writer), tree.getRoot());
		String[] expected = {
				"+- ${...}",
				"   |",
				"   +- '+'",
				"      |",
				"      +- [...]",
				"      |  |",
				"      |  +- . bar",
				"      |  |  |",
				"      |  |  +- foo",
				"      |  |",
				"      |  +- baz",
				"      |",
				"      +- foobar",
				null
		};
		BufferedReader reader = new BufferedReader(new StringReader(writer.toString()));
		for (String line : expected) {
			assertThat(reader.readLine()).isEqualTo(line);
		}
	}

}
