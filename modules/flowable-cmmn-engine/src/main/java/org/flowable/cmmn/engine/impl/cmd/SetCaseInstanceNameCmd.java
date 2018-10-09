package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * A command to set or change the name of a case instance.
 *
 * @author Micha Kiener
 */
public class SetCaseInstanceNameCmd implements Command<Void> {

    protected String caseInstanceId;
    protected String caseName;

    public SetCaseInstanceNameCmd(String caseInstanceId, String caseName) {
        this.caseInstanceId = caseInstanceId;
        this.caseName = caseName;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("You need to provide the case instance id in order to set its name");
        }

        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id " + caseInstanceId, CaseInstanceEntity.class);
        }
        caseInstanceEntity.setName(caseName);

        CommandContextUtil.getCaseInstanceEntityManager(commandContext).update(caseInstanceEntity);
        CommandContextUtil.getCmmnHistoryManager().recordUpdateCaseInstanceName(caseInstanceEntity, caseName);

        return null;
    }

}
