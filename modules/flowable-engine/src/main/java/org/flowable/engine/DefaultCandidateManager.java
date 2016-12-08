package org.flowable.engine;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.AbstractManager;
import org.flowable.idm.api.Group;

public class DefaultCandidateManager extends AbstractManager implements CandidateManager {

  public DefaultCandidateManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  @Override
  public List<String> getGroupsForCandidateUser(String candidateUser) {
    IdentityService identityService = getProcessEngineConfiguration().getIdentityService();
    List<Group> groups = identityService.createGroupQuery().groupMember(candidateUser).list();
    List<String> groupIds = new ArrayList<String>();
    for (Group group : groups) {
      groupIds.add(group.getId());
    }
    return groupIds;
  }
}
