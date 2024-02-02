package org.flowable.assertions.process;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author martin.grofcik
 */
abstract class TestUtils {
    static ProcessInstance createOneTaskProcess(RuntimeService runtimeService) {
        return runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
    }
}
