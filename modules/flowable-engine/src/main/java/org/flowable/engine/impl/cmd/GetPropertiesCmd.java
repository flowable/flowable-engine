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

package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class GetPropertiesCmd implements Command<Map<String, String>>, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> execute(CommandContext commandContext) {
        List<PropertyEntity> propertyEntities = CommandContextUtil.getPropertyEntityManager(commandContext).findAll();

        Map<String, String> properties = new HashMap<>();
        for (PropertyEntity propertyEntity : propertyEntities) {
            properties.put(propertyEntity.getName(), propertyEntity.getValue());
        }
        return properties;
    }

}
