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

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationItem;
import org.flowable.dmn.model.InputData;
import org.junit.jupiter.api.Test;

public class DishExampleTest extends AbstractConverterTest {

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

    @Override
    protected String getResource() {
        return "dishExample.dmn";
    }

    private void validateModel(DmnDefinition model) {
        assertThat(model.getInputData()).as("input data")
            .extracting(InputData::getVariable)
            .extracting(InformationItem::getId)
            .containsExactly("dayType_ii", "temperature_ii");

        assertThat(model.getDecisions()).as("decisions")
            .extracting(DmnElement::getId)
            .containsExactly("dish", "season", "guestCount");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getRequiredDecisions)
            .extracting(ir -> ir.getRequiredDecision().getHref())
            .containsExactly("#season", "#guestCount");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getRequiredInputs)
            .extracting(ir -> ir.getRequiredInput().getHref())
            .containsExactly("#temperature_id", "#dayType_id");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getAuthorityRequirements)
            .filteredOn(ar -> ar.getRequiredAuthority() != null)
            .extracting(ar -> ar.getRequiredAuthority().getHref())
            .containsExactly("#host_ks");
    }

}