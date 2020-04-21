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
package org.flowable.variable.service.impl.types;

import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;

/**
 * A {@link CommandContextCloseListener} that holds one {@link TraceableObject} instance that is added by {@link MutableVariableType}(s).
 * 
 * On the {@link #closing(CommandContext)} of the {@link CommandContext}, the {@link TraceableObject} will be verified if it is dirty.
 * If so, it will update the right entities such that changes will be flushed.
 * 
 * It's important that this happens in the {@link #closing(CommandContext)}, as this happens before the {@link CommandContext#close()} is called
 * and when all the sessions are flushed (including the {@link DbSqlSession} in the relational DB case (the data needs to be ready then).
 * 
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class TraceableVariablesCommandContextCloseListener implements CommandContextCloseListener {

    protected TraceableObject<?, ?> traceableObject;

    public TraceableVariablesCommandContextCloseListener(TraceableObject<?, ?> traceableObject) {
        this.traceableObject = traceableObject;
    }

    @Override
    public void closing(CommandContext commandContext) {
        traceableObject.updateIfValueChanged();
    }

    @Override
    public void closed(CommandContext commandContext) {

    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {

    }

    @Override
    public void closeFailure(CommandContext commandContext) {

    }

    @Override
    public Integer order() {
        return 1;
    }
    
    @Override
    public boolean multipleAllowed() {
        return true;
    }
}
