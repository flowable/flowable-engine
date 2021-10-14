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

import org.flowable.batch.api.BatchPartQuery;
import org.flowable.common.engine.api.query.QueryProperty;

/**
 * Contains the possible properties that can be used in a {@link BatchPartQuery}.
 */
public class BatchPartQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, BatchPartQueryProperty> properties = new HashMap<>();

    public static final BatchPartQueryProperty BATCH_ID = new BatchPartQueryProperty("RES.BATCH_ID_");
    public static final BatchPartQueryProperty CREATE_TIME = new BatchPartQueryProperty("RES.CREATE_TIME_");

    private String name;

    public BatchPartQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

}
