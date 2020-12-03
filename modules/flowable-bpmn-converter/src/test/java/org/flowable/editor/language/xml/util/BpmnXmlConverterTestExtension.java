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
package org.flowable.editor.language.xml.util;

import static org.flowable.editor.language.xml.util.XmlTestUtils.readXMLFile;
import static org.flowable.editor.language.xml.util.XmlTestUtils.readXmlExportAndReadAgain;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Filip Hrisafov
 */
public class BpmnXmlConverterTestExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationSupport.isAnnotated(context.getTestMethod(), BpmnXmlConverterTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        String resource = AnnotationSupport.findAnnotation(
                context.getTestMethod(),
                BpmnXmlConverterTest.class
        )
                .map(BpmnXmlConverterTest::value)
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new IllegalArgumentException("No resource has been provided"));
        return Stream.of(
                new ConvertBpmnModelTestInvocationContext("xmlToModel", () -> readXMLFile(resource)),
                new ConvertBpmnModelTestInvocationContext("xmlToModelAndBack", () -> readXmlExportAndReadAgain(resource))
        );
    }

}
