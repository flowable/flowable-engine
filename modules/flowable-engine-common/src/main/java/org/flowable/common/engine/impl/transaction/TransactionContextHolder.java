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
package org.flowable.common.engine.impl.transaction;

import java.util.Stack;

import org.flowable.common.engine.impl.cfg.TransactionContext;

/**
 * Holder for a threadlocal stack of {@link BaseTransactionContext} objects. Different engines (process/idm/dmn/form/...) use this 'shared' object to see if another engine has already started a
 * transaction or not.
 * 
 * @author Joram Barrez
 */
public class TransactionContextHolder {

    protected static ThreadLocal<Stack<TransactionContext>> transactionContextThreadLocal = new ThreadLocal<>();

    public static TransactionContext getTransactionContext() {
        Stack<TransactionContext> stack = getStack(transactionContextThreadLocal);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public static void setTransactionContext(TransactionContext transactionContext) {
        getStack(transactionContextThreadLocal).push(transactionContext);
    }

    public static void removeTransactionContext() {
        getStack(transactionContextThreadLocal).pop();
    }

    public static boolean isTransactionContextActive() {
        return !getStack(transactionContextThreadLocal).isEmpty();
    }

    protected static <T> Stack<T> getStack(ThreadLocal<Stack<T>> threadLocal) {
        Stack<T> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<>();
            threadLocal.set(stack);
        }
        return stack;
    }

}
