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
package org.flowable.idm.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.impl.persistence.entity.AbstractEntity;

/**
 * @author Tom Baeyens
 */
public class GroupEntityImpl extends AbstractEntity implements GroupEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String type;

    public GroupEntityImpl() {
    }

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<String, Object>();
        persistentState.put("name", name);
        persistentState.put("type", type);
        return persistentState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
