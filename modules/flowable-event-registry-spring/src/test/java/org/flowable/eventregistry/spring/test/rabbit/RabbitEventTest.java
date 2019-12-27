package org.flowable.eventregistry.spring.test.rabbit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flowable.eventregistry.spring.test.FlowableEventSpringExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Filip Hrisafov
 */
@SpringJUnitConfig(classes = EventRegistryRabbitConfiguration.class)
@ExtendWith(FlowableEventSpringExtension.class)
@Tag("docker")
@Tag("rabbit")
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RabbitEventTest {

}
