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
package org.activiti.camel.util;

/**
 * Simulates some real work, to delay the message on the Camel route.
 * 
 * @author stefan.schulze@accelsis.biz
 * 
 */
public class TimeConsumingService {

    /**
     * Spend some time.
     * 
     * @throws InterruptedException
     */
    public void doWork() throws InterruptedException {
        Thread.sleep(100);
    }

}
