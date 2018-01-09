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
package org.flowable.job.service;

/**
 * History job processor which is used to process history job entities during the different history job phases.
 *
 * @author Guy Brand
 * @see HistoryJobProcessorContext
 * @see HistoryJobProcessorContext.Phase
 */
public interface HistoryJobProcessor {

    /**
     * Process the given {@link HistoryJobProcessorContext}.
     *
     * @param historyJobProcessorContext the {@link HistoryJobProcessorContext} to process
     */
    void process(HistoryJobProcessorContext historyJobProcessorContext);

}
