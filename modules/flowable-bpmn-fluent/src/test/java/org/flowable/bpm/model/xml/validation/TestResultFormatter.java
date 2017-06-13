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
package org.flowable.bpm.model.xml.validation;

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.testmodel.instance.FlyingAnimal;

import java.io.StringWriter;
import java.util.Formatter;

@SuppressWarnings("resource")
public class TestResultFormatter
        implements ValidationResultFormatter {

    @Override
    public void formatElement(StringWriter writer, ModelElementInstance element) {
        Formatter formatter = new Formatter(writer);

        if (element instanceof FlyingAnimal) {
            formatter.format("%s%n", ((FlyingAnimal) element).getId());
        } else {
            formatter.format("%s%n", element.getElementType().getTypeName());
        }
        formatter.flush();
    }

    @Override
    public void formatResult(StringWriter writer, ValidationResult result) {
        new Formatter(writer)
                .format("\t%s (%d): %s%n", result.getType(), result.getCode(), result.getMessage())
                .flush();
    }
}
