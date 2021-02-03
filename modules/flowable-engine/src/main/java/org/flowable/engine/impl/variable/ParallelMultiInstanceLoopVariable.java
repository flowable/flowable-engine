package org.flowable.engine.impl.variable;

/**
 * @author Filip Hrisafov
 */
public class ParallelMultiInstanceLoopVariable {

    public static final String COMPLETED_INSTANCES = "completed";
    public static final String ACTIVE_INSTANCES = "active";

    protected final String executionId;
    protected final String type;

    public ParallelMultiInstanceLoopVariable(String executionId, String type) {
        this.executionId = executionId;
        this.type = type;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getType() {
        return type;
    }

    public static ParallelMultiInstanceLoopVariable completed(String executionId) {
        return new ParallelMultiInstanceLoopVariable(executionId, COMPLETED_INSTANCES);
    }

    public static ParallelMultiInstanceLoopVariable active(String executionId) {
        return new ParallelMultiInstanceLoopVariable(executionId, ACTIVE_INSTANCES);
    }
}
