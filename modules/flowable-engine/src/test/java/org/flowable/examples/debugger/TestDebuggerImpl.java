package org.flowable.examples.debugger;

import org.apache.commons.collections.Predicate;
import org.flowable.engine.Agenda;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.agenda.AbstractOperation;
import org.flowable.engine.impl.cmd.NeedsActiveExecutionCmd;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Debugger;

/**
 * This class implements Debugger interface for testing purposes
 */
public class TestDebuggerImpl implements Debugger {

    private final FlowableEngineAgendaFactory agendaFactoryBean;
    private Predicate isBreakPointPredicate = null;
    private Agenda agendaCopy;

    private static ThreadLocal<Boolean> suppressBreakPoints = new ThreadLocal<>();

    static {
        suppressBreakPoints.set(false);
    }

    public TestDebuggerImpl(FlowableEngineAgendaFactory agendaFactoryBean) {
        this.agendaFactoryBean = agendaFactoryBean;
    }

    @Override
    public boolean isBreakPoint(Runnable runnable) {
        boolean isBreakPoint = this.isBreakPointPredicate != null && this.isBreakPointPredicate.evaluate(runnable);
        if (isBreakPoint) {
            if (suppressBreakPoints.get()) {
                suppressBreakPoints.set(false);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void continueOperationExecution(CommandExecutor commandExecutor, final Agenda agenda) {
        suppressBreakPoints.set(true);
        commandExecutor.execute(new RestoreContextCmd(getAgenda()));
    }

    @Override
    public void breakOperationExecution(CommandContext commandContext) {
        this.agendaCopy = createAgenda(commandContext);
        Agenda agenda = Context.getCommandContext().getAgenda();
        while (!agenda.isEmpty()) {
            this.agendaCopy.planOperation(Context.getCommandContext().getAgenda().getNextOperation());
        }
    }

    public void setBreakPoint(Predicate isBreakPointPredicate) {
        this.isBreakPointPredicate = isBreakPointPredicate;
    }

    public Agenda createAgenda(CommandContext commandContext) {
        try {
            if (this.agendaCopy == null) {
                this.agendaCopy = this.agendaFactoryBean.createAgenda(commandContext);
            } else {
                throw new FlowableException("Local Agenda copy is not null");
            }
            return this.agendaCopy;
        } catch (Exception e) {
            throw new FlowableException("Unable to create agenda copy.", e);
        }
    }

    public Agenda getAgenda() {
        return this.agendaCopy;
    }

    public static class RestoreContextCmd extends NeedsActiveExecutionCmd<Void> {

        private final Agenda agenda;

        public RestoreContextCmd(Agenda agenda) {
            super(((AbstractOperation) agenda.peekOperation()).getExecution().getId());
            this.agenda = agenda;
        }

        @Override
        protected Void execute(CommandContext commandContext, ExecutionEntity execution) {
            while (!this.agenda.isEmpty()) {
                Runnable runnable = this.agenda.getNextOperation();
                if (runnable instanceof AbstractOperation) {
                    AbstractOperation operation = (AbstractOperation) runnable;
                    operation.setExecution(execution);
                    operation.setCommandContext(commandContext);
                }
                commandContext.getAgenda().planOperation(runnable);
            }
            return null;
        }

    }

}
