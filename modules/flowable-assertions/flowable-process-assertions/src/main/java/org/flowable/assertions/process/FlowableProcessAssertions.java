package org.flowable.assertions.process;

import org.assertj.core.api.Assertions;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author martin.grofcik
 */
public class FlowableProcessAssertions extends Assertions {

    public static ProcessInstanceAssert assertThat(ProcessInstance processInstance) {
        return new ProcessInstanceAssert(processInstance);
    }
    public static HistoricProcessInstanceAssert assertThat(HistoricProcessInstance historicProcessInstance) {
        return new HistoricProcessInstanceAssert(historicProcessInstance);
    }

}
