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
package org.flowable.engine.impl.jobexecutor;

import org.flowable.job.service.JobHandler;

/**
 * @author Dennis Federico
 */
public abstract class AbstractProcessInstanceMigrationJobHandler implements JobHandler {

    //TODO WIP - Review how to encode the processMigration batch id in the handlerCfg - JSON?
    protected static String getBatchIdFromHandlerCfg(String handlerCfg) {
        if (handlerCfg != null) {
            String[] split = handlerCfg.split(":");
            if (split.length >= 2) {
                return split[1];
            }
        }
        return null;
    }

    public static String getHandlerCfgForBatchId(String batchId) {
        return "BatchId:" + batchId;
    }
}

