package org.flowable.test.spring.boot

import flowable.Application
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.runtime.ProcessInstance
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.crp.flowable.assertions.CrpFlowableAssertions.assertThat

@SpringBootTest(classes = Application.class)
@ContextConfiguration
class OneTaskProcessSpec extends Specification {
    @Autowired
    RuntimeService runtimeService
    @Autowired
    TaskService taskService

    def 'happy path'() {
        given:
            path = []

        when:
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start()

        then:
        assertThat(processInstance).isRunning()
                .doesNotHaveVariable('nonExistingVariable')
                .activities().extracting('activityId')
                .contains(path << ['theStart', 'theStart-theTask', 'theTask'])

        when:

        cleanup:
        runtimeService.deleteProcessInstance(processInstance.id, 'removing test process instance')
    }

}
