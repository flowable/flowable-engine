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
package org.flowable.common.engine.impl.agenda;

import java.util.LinkedList;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public abstract class AbstractAgenda implements Agenda {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgenda.class);

    protected CommandContext commandContext;
    protected LinkedList<Runnable> operations = new LinkedList<>();

    public AbstractAgenda(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    @Override
    public Runnable getNextOperation() {
        assertOperationsNotEmpty();
        return operations.poll();
    }

    @Override
    public Runnable peekOperation() {
        assertOperationsNotEmpty();
        return operations.peek();
    }

    protected void assertOperationsNotEmpty() {
        if (operations.isEmpty()) {
            throw new FlowableException("Unable to peek empty agenda.");
        }
    }

    /**
     * Generic method to plan a {@link Runnable}.
     */
    @Override
    public void planOperation(Runnable operation) {
        operations.add(operation);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Operation {} added to agenda", operation.getClass());
        }
    }

    public LinkedList<Runnable> getOperations() {
        return operations;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    public void setCommandContext(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

}
