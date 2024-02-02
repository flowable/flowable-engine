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

package org.flowable.idm.api;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * Contains the possible properties that can be used by the {@link TokenQuery}.
 * 
 * @author Tijs Rademakers
 */
public class TokenQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, TokenQueryProperty> properties = new HashMap<>();

    public static final TokenQueryProperty TOKEN_ID = new TokenQueryProperty("RES.ID_");
    public static final TokenQueryProperty TOKEN_DATE = new TokenQueryProperty("RES.TOKEN_DATE_");

    private String name;

    public TokenQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static TokenQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
