package org.activiti.engine;

import java.util.List;

public interface CandidateManager {

  List<String> getGroupsForCandidateUser(String candidateUser);
  
}
