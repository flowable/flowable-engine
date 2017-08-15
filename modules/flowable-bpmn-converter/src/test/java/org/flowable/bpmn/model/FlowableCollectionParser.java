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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Lori Small
 */
public class FlowableCollectionParser extends AbstractFlowableCollectionParser {

    public FlowableCollectionParser clone() {
    	FlowableCollectionParser clone = new FlowableCollectionParser();
        clone.setValues(this);
        return clone;
    }

	@Override
	@SuppressWarnings("rawtypes")
	public Collection parse(String collectionString) {
		// TODO Add parsing logic here for runtime test
			return new ArrayList();
	}
}
