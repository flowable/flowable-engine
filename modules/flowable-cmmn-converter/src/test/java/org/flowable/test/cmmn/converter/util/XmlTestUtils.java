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
package org.flowable.test.cmmn.converter.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.common.engine.impl.util.io.InputStreamSource;

/**
 * @author Filip Hrisafov
 */
public class XmlTestUtils {

    public static CmmnModel readXmlExportAndReadAgain(String resource) {
        CmmnModel model = readXMLFile(resource);
        return exportAndReadXMLFile(model);
    }

    public static CmmnModel exportAndReadXMLFile(CmmnModel model) {
        byte[] xml = new CmmnXmlConverter().convertToXML(model);
        return new CmmnXmlConverter().convertToCmmnModel(new InputStreamSource(new ByteArrayInputStream(xml)), true, false, "UTF-8");
    }

    public static CmmnModel readXMLFile(String resource) {
        return new CmmnXmlConverter().convertToCmmnModel(new ClasspathStreamResource(resource), true, false);
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
