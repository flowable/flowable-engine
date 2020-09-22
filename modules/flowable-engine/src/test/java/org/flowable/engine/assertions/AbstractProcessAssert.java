package org.flowable.engine.assertions;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.api.JobQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;

public abstract class AbstractProcessAssert<S extends AbstractProcessAssert<S, A>, A> extends AbstractAssert<S, A> {

    protected ProcessEngine engine;

    private static ThreadLocal<Map<Class<?>, AbstractProcessAssert<?, ?>>>
            lastAsserts = new ThreadLocal<Map<Class<?>, AbstractProcessAssert<?, ?>>>();

    protected AbstractProcessAssert(ProcessEngine engine, A actual, Class<?> selfType) {
        super(actual, selfType);
        this.engine = engine;
        setLastAssert(selfType, this);
    }

    /*
     * Delivers the the actual object under test.
     */
    public A getActual() {
        return actual;
    }

    /*
     * Method definition meant to deliver the current/refreshed persistent state of
     * the actual object under test and expecting that such a current state actually exists.
     */
    protected A getExistingCurrent() {
        Assertions.assertThat(actual)
                .overridingErrorMessage("Expecting assertion to be called on non-null object, but found it to be null.")
                .isNotNull();
        A current = getCurrent();
        Assertions.assertThat(current)
                .overridingErrorMessage(
                        "Expecting %s to be unfinished, but found that it already finished.",
                        toString(actual))
                .isNotNull();
        return current;
    }

    /*
     * Abstract method definition meant to deliver the current/refreshed persistent state of
     * the actual object under test. Needs to be correctly implemented by implementations of this.
     */
    protected abstract A getCurrent();

    /*
     * Abstract method definition meant to deliver a loggable string representation of the
     * given object of same type as the actual object under test.
     */
    protected abstract String toString(A object);

    public static void resetLastAsserts() {
        getLastAsserts().clear();
    }

    @SuppressWarnings("unchecked")
    protected static <S extends AbstractProcessAssert<?, ?>> S getLastAssert(Class<S> assertClass) {
        return (S) getLastAsserts().get(assertClass);
    }

    private static void setLastAssert(Class<?> assertClass, AbstractProcessAssert<?, ?> assertInstance) {
        getLastAsserts().put(assertClass, assertInstance);
    }

    private static Map<Class<?>, AbstractProcessAssert<?, ?>> getLastAsserts() {
        Map<Class<?>, AbstractProcessAssert<?, ?>> asserts = lastAsserts.get();
        if (asserts == null)
            lastAsserts.set(asserts = new HashMap<Class<?>, AbstractProcessAssert<?, ?>>());
        return asserts;
    }

    protected RepositoryService repositoryService() {
        return engine.getRepositoryService();
    }

    protected RuntimeService runtimeService() {
        return engine.getRuntimeService();
    }

    protected FormService formService() {
        return engine.getFormService();
    }

    protected TaskService taskService() {
        return engine.getTaskService();
    }

    protected HistoryService historyService() {
        return engine.getHistoryService();
    }

    protected ManagementService managementService() {
        return engine.getManagementService();
    }

    /*
     * TaskQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or {@link ProcessDefinition})
     * by overriding this method in sub classes specialised to verify a specific
     * process engine domain class.
     */
    protected TaskQuery taskQuery() {
        return taskService().createTaskQuery();
    }

    /*
     * JobQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or {@link ProcessDefinition})
     * by overriding this method in sub classes specialised to verify a specific
     * process engine domain class.
     */
    protected JobQuery jobQuery() {
        return managementService().createJobQuery();
    }

    /*
     * ProcessInstanceQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or
     * {@link ProcessDefinition}) by overriding this method in sub classes specialised to
     * verify a specific process engine domain class.
     */
    protected ProcessInstanceQuery processInstanceQuery() {
        return runtimeService().createProcessInstanceQuery();
    }

    /*
     * ExecutionQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or {@link ProcessDefinition})
     * by overriding this method in sub classes specialised to verify a specific
     * process engine domain class.
     */
    protected ExecutionQuery executionQuery() {
        return runtimeService().createExecutionQuery();
    }

    /*
     * HistoricActivityInstanceQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or
     * {@link ProcessDefinition}) by overriding this method in sub classes specialised to
     * verify a specific process engine domain class.
     */
    protected HistoricActivityInstanceQuery historicActivityInstanceQuery() {
        return historyService().createHistoricActivityInstanceQuery();
    }

    /*
     * HistoricProcessInstanceQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or
     * {@link ProcessDefinition}) by overriding this method in sub classes specialised to
     * verify a specific process engine domain class.
     */
    protected HistoricProcessInstanceQuery historicProcessInstanceQuery() {
        return historyService().createHistoricProcessInstanceQuery();
    }

    /*
     * HistoricVariableInstanceQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or
     * {@link ProcessDefinition}) by overriding this method in sub classes specialised to
     * verify a specific process engine domain class.
     */
    protected HistoricVariableInstanceQuery historicVariableInstanceQuery() {
        return historyService().createHistoricVariableInstanceQuery();
    }

    /*
     * ProcessDefinitionQuery, unnarrowed. Narrow this to {@link ProcessInstance} (or
     * {@link ProcessDefinition}) by overriding this method in sub classes specialised to
     * verify a specific process engine domain class.
     */
    protected ProcessDefinitionQuery processDefinitionQuery() {
        return repositoryService().createProcessDefinitionQuery();
    }
}
