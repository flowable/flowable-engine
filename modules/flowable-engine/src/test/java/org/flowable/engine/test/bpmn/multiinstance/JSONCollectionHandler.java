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
package org.flowable.engine.test.bpmn.multiinstance;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.FlowableCollectionHandler;
import org.flowable.engine.impl.util.json.JSONArray;
import org.flowable.engine.impl.util.json.JSONObject;

/**
 * @author Lori Small
 */
public class JSONCollectionHandler implements FlowableCollectionHandler {

    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("rawtypes")
    public Collection resolveCollection(Object collectionValue, DelegateExecution execution) {
        JSONArray jsonArray = new JSONArray((String) collectionValue);
        ArrayList<String> collection = new ArrayList<>();
        JSONObject jsonObj = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObj = jsonArray.getJSONObject(i);
            collection.add(jsonObj.getString("principal"));
        }

        return collection;
    }
}
