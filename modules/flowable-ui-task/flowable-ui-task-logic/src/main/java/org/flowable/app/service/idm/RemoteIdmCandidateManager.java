package org.flowable.app.service.idm;

import org.flowable.app.model.common.RemoteUser;
import org.flowable.engine.CandidateManager;
import org.flowable.idm.api.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * Replacement for the {@link org.flowable.engine.DefaultCandidateManager} which requires an Idm engine to be registered.
 * Instead uses the {@link RemoteIdmService} instead of a {@link org.flowable.engine.IdentityService}.
 *
 * This is no longer needed when the flowable-task app configures a Idm engine
 */
public class RemoteIdmCandidateManager implements CandidateManager {

    private final RemoteIdmService remoteIdmService;

    public RemoteIdmCandidateManager(RemoteIdmService remoteIdmService) {
        this.remoteIdmService = remoteIdmService;
    }

    @Override
    public List<String> getGroupsForCandidateUser(String candidateUser) {
        RemoteUser remoteUser = remoteIdmService.getUser(candidateUser);
        List<String> groupIds = new ArrayList<>();

        if(remoteUser != null && remoteUser.getGroups() != null) {
            for (Group group : remoteUser.getGroups()) {
                groupIds.add(group.getId());
            }
        }

        return groupIds;
    }
}
