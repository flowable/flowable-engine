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

import org.flowable.bpm.model.xml.testmodel.instance.Bird;

public class IllegalBirdValidator
        implements ModelElementValidator<Bird> {

    protected String nameOfBird;

    public IllegalBirdValidator(String nameOfBird) {
        this.nameOfBird = nameOfBird;
    }

    @Override
    public Class<Bird> getElementType() {
        return Bird.class;
    }

    @Override
    public void validate(Bird bird, ValidationResultCollector validationResultCollector) {

        if (nameOfBird.equals(bird.getId())) {
            validationResultCollector.addError(20, String.format("Bird %s is illegal", nameOfBird));
        }
    }
}
