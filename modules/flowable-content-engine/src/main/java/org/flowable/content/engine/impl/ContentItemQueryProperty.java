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

package org.flowable.content.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * Contains the possible properties that can be used in a {@link ContentInstanceQuery}.
 * 
 * @author Tijs Rademakers
 */
public class ContentItemQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, ContentItemQueryProperty> properties = new HashMap<>();

    public static final ContentItemQueryProperty CREATED_DATE = new ContentItemQueryProperty("RES.CREATED_");
    public static final ContentItemQueryProperty TENANT_ID = new ContentItemQueryProperty("RES.TENANT_ID_");

    private String name;

    public ContentItemQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static ContentItemQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
