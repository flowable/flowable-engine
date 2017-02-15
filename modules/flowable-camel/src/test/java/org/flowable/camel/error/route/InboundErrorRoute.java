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
