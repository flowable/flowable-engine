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
package org.flowable.bpm.model.xml.impl.util;

import org.flowable.bpm.model.xml.ModelException;

import java.lang.reflect.Type;

/**
 * Thrown in case a value cannot be converted to or from the requested type
 */
public class ModelTypeException
        extends ModelException {

    private static final long serialVersionUID = 1L;

    public ModelTypeException(String message) {
        super(message);
    }

    public ModelTypeException(String value, Type type) {
        super("Illegal value " + value + " for type " + type);
    }

}
