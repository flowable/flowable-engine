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
package org.flowable.camel.error.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.camel.util.TimeConsumingService;

/**
 * @author stefan.schulze@accelsis.biz
 * 
 */
public class InboundErrorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("seda:inbound").routeId("inbound")
                // give Flowable some time to reach synchronization point
                .bean(TimeConsumingService.class).log(LoggingLevel.INFO, "Returning result ...").to("flowable:ErrorHandling:ReceiveResult");

        from("seda:dlq").routeId("dlq").log(LoggingLevel.INFO, "Error handled by camel ...");
    }
}
