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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.flowable.cmmn.model.CmmnModel;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class DefinitionsNamespacesCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/simple-namespaces.cmmn";

    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }

    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getNamespaces())
                .containsOnly(
                        entry("dc", "http://www.omg.org/spec/CMMN/20151109/DC"),
                        entry("di", "http://www.omg.org/spec/CMMN/20151109/DI"),
                        entry("cmmndi", "http://www.omg.org/spec/CMMN/20151109/CMMNDI"),
                        entry("xsi", "http://www.w3.org/2001/XMLSchema-instance"),
                        entry("flowable", "http://flowable.org/cmmn"),
                        entry("custom", "http://flowable.org/custom")
                );
    }

}
