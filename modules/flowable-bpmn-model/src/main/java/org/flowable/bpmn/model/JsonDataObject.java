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
package org.flowable.bpmn.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Christophe DENEUX
 */
public class JsonDataObject extends ValuedDataObject {

    @Override
    public void setValue(Object value) {
    	if (value instanceof String && !StringUtils.isEmpty(((String) value).trim())) {
    		try {
				this.value = DateFormat.getDateTimeInstance().parse((String) value);
			} catch (ParseException e) {
				System.out.println("Error parsing Date string: " + value);
			}
    	} else if (value instanceof Date) {
    		this.value = value;
    	}
    }

    @Override
    public JsonDataObject clone() {
        JsonDataObject clone = new JsonDataObject();
        clone.setValues(this);
        return clone;
    }
}
