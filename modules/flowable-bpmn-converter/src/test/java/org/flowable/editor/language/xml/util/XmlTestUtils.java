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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.common.engine.impl.util.io.InputStreamSource;

/**
 * @author Filip Hrisafov
 */
public class XmlTestUtils {

    public static BpmnModel readXmlExportAndReadAgain(String resource) {
        BpmnModel model = readXMLFile(resource);
        return exportAndReadXMLFile(model);
    }

    public static BpmnModel exportAndReadXMLFile(BpmnModel model) {
        byte[] xml = new BpmnXMLConverter().convertToXML(model);
        return new BpmnXMLConverter().convertToBpmnModel(new InputStreamSource(new ByteArrayInputStream(xml)), true, false, "UTF-8");
    }

    public static BpmnModel readXMLFile(String resource) {
        return new BpmnXMLConverter().convertToBpmnModel(new ClasspathStreamResource(resource), true, false);
    }

    protected static class ClasspathStreamResource implements InputStreamProvider {

        protected final String resource;

        public ClasspathStreamResource(String resource) {
            this.resource = resource;
        }

        @Override
        public InputStream getInputStream() {
            return this.getClass().getClassLoader().getResourceAsStream(resource);
        }
    }
}
