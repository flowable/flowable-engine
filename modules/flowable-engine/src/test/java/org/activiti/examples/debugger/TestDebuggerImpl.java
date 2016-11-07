package org.activiti.examples.debugger;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.Agenda;
import org.activiti.engine.FlowableEngineAgendaFactory;
import org.activiti.engine.impl.agenda.AbstractOperation;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.runtime.Debugger;
import org.apache.commons.collections.Predicate;

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
                throw new ActivitiException("Local Agenda copy is not null");
            }
            return this.agendaCopy;
        } catch (Exception e) {
            throw new ActivitiException("Unable to create agenda copy.", e);
        }
    }

    public Agenda getAgenda() {
        return this.agendaCopy;
    }

    public static class RestoreContextCmd implements Command<Void> {

        private final Agenda agenda;

        public RestoreContextCmd(Agenda agenda) {
            this.agenda = agenda;
        }

        @Override
        public Void execute(CommandContext commandContext) {
            while (!this.agenda.isEmpty()) {
                Runnable runnable = this.agenda.getNextOperation();
                if (runnable instanceof AbstractOperation) {
                    AbstractOperation operation = (AbstractOperation) runnable;
                    operation.setCommandContext(commandContext);
                }
                commandContext.getAgenda().planOperation(runnable);
            }
            return null;
        }
    }

}
