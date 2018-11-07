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
package org.flowable.test.cmmn.converter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.impl.util.io.InputStreamSource;

public abstract class AbstractConverterTest implements CmmnXmlConstants {

    protected CmmnModel readXMLFile(String resource) throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        return new CmmnXmlConverter().convertToCmmnModel(new InputStreamSource(xmlStream), true, false, "UTF-8");
    }

    protected CmmnModel exportAndReadXMLFile(CmmnModel cmmnModel) throws Exception {
        byte[] xml = new CmmnXmlConverter().convertToXML(cmmnModel);
        return new CmmnXmlConverter().convertToCmmnModel(new InputStreamSource(new ByteArrayInputStream(xml)), true, false, "UTF-8");
    }
}
