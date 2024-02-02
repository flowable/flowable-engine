package org.flowable.test.spring.boot

import flowable.Application
import org.flowable.engine.RuntimeService
import org.flowable.engine.runtime.ProcessInstance
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.crp.flowable.assertions.CrpFlowableAssertions.assertThat

@SpringBootTest(classes = Application.class)
class OneTaskProcessGroovyTest {
    @Autowired
    RuntimeService runtimeService;

    @Test
    void "happy path"() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start()

        assertThat(processInstance).isRunning()
                 .doesNotHaveVariable('nonExistingVariable')
                 .activities().extracting('activityId')
                 .contains('theStart', 'theStart-theTask', 'theTask')

    }
}
