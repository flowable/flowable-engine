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

import org.flowable.common.engine.impl.db.IdBlock;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class GetNextIdBlockCmd implements Command<IdBlock> {

    private static final long serialVersionUID = 1L;
    protected int idBlockSize;

    public GetNextIdBlockCmd(int idBlockSize) {
        this.idBlockSize = idBlockSize;
    }

    @Override
    public IdBlock execute(CommandContext commandContext) {
        PropertyEntity property = (PropertyEntity) CommandContextUtil.getPropertyEntityManager(commandContext).findById("next.dbid");
        long oldValue = Long.parseLong(property.getValue());
        long newValue = oldValue + idBlockSize;
        property.setValue(Long.toString(newValue));
        return new IdBlock(oldValue, newValue - 1);
    }
}
