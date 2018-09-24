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
package org.flowable.job.service.impl.history.async;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class AsyncHistoryCommittedTransactionListener implements TransactionListener {
    
    protected List<Runnable> runnables = new ArrayList<>();

    public void addRunnable(Runnable runnable) {
        runnables.add(runnable);
    }
    
    @Override
    public void execute(CommandContext commandContext) {
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    public List<Runnable> getRunnables() {
        return runnables;
    }

    public void setRunnables(List<Runnable> runnables) {
        this.runnables = runnables;
    }
    
}
