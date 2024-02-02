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

package org.flowable.batch.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.batch.api.BatchQuery;
import org.flowable.common.engine.api.query.QueryProperty;

/**
 * Contains the possible properties that can be used in a {@link BatchQuery}.
 */
public class BatchQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, BatchQueryProperty> properties = new HashMap<>();

    public static final BatchQueryProperty BATCH_ID = new BatchQueryProperty("RES.ID_");
    public static final BatchQueryProperty CREATETIME = new BatchQueryProperty("RES.CREATE_TIME_");
    public static final BatchQueryProperty TENANT_ID = new BatchQueryProperty("RES.TENANT_ID_");

    private String name;

    public BatchQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static BatchQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
