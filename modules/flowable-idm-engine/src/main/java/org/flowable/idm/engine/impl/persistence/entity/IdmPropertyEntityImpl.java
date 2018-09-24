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

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IdmPropertyEntityImpl extends AbstractIdmEngineEntity implements IdmPropertyEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String value;

    public IdmPropertyEntityImpl() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public Object getPersistentState() {
        return value;
    }

    @Override
    public void setId(String id) {
        throw new FlowableException("only provided id generation allowed for properties");
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "PropertyEntity[name=" + name + ", value=" + value + "]";
    }

}
