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
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
public class DefinitionsNamespacesCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/simple-namespaces.cmmn")
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
