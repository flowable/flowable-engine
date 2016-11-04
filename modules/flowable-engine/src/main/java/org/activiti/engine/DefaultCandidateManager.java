package org.activiti.engine;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.AbstractManager;
import org.activiti.idm.api.Group;

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
