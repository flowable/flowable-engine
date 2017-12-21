package org.activiti.engine.test.api.task;

import java.util.HashSet;
import java.util.Set;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;

public class DelegateTaskTestTaskListener implements TaskListener {

    public static final String VARNAME_CANDIDATE_USERS = "candidateUsers";
    public static final String VARNAME_CANDIDATE_GROUPS = "candidateGroups";

    public void notify(DelegateTask delegateTask) {
        Set<IdentityLink> candidates = delegateTask.getCandidates();
        Set<String> candidateUsers = new HashSet<String>();
        Set<String> candidateGroups = new HashSet<String>();
        for (IdentityLink candidate : candidates) {
            if (candidate.getUserId() != null) {
                candidateUsers.add(candidate.getUserId());
            } else if (candidate.getGroupId() != null) {
                candidateGroups.add(candidate.getGroupId());
            }
        }
        delegateTask.setVariable(VARNAME_CANDIDATE_USERS, candidateUsers);
        delegateTask.setVariable(VARNAME_CANDIDATE_GROUPS, candidateGroups);
    }

}
