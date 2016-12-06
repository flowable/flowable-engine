package org.flowable.engine;

import java.util.List;

public interface CandidateManager {

  List<String> getGroupsForCandidateUser(String candidateUser);
  
}
