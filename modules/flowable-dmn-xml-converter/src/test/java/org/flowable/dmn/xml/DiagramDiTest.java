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
package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.GraphicInfo;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

public class DiagramDiTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition dmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(dmnModel);
        validateModel(parsedModel);
    }

    @Test
    public void convertToModelAndBackAndEnsureXmlIsTheSame() throws Exception {
        DmnDefinition dmnModel = readXMLFile();
        String originalModelAsXml = new String(new DmnXMLConverter().convertToXML(dmnModel), Charset.defaultCharset());
        DmnDefinition parsedModel = exportAndReadXMLFile(dmnModel);
        String convertedModelAsXml = new String(new DmnXMLConverter().convertToXML(parsedModel), Charset.defaultCharset());

        assertThat(convertedModelAsXml).isEqualTo(originalModelAsXml);
    }

    @Override
    protected String getResource() {
        return "dmndi.dmn";
    }

    private void validateModel(DmnDefinition model) {

        // validate divider to contain height and width (need for JSON conversion)
        assertThat(model.getDecisionServiceDividerGraphicInfo("decisionServiceTest"))
            .extracting(
                GraphicInfo::getX,
                GraphicInfo::getY,
                GraphicInfo::getWidth,
                GraphicInfo::getHeight)
            .as("x, y, width, height")
            .containsExactly(
                tuple(30.0, 285.0, 729.0, 240.0),
                tuple(759.0, 285.0, 729.0, 330.0)
            );
    }
}