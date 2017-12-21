/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class CommandStats {

    protected long getTotalCommandTime;

    protected List<Long> commandExecutionTimings = new ArrayList<>();
    protected List<Long> databaseTimings = new ArrayList<>();

    protected Map<String, Long> dbSelects = new HashMap<>();
    protected Map<String, Long> dbInserts = new HashMap<>();
    protected Map<String, Long> dbUpdates = new HashMap<>();
    protected Map<String, Long> dbDeletes = new HashMap<>();

    public CommandStats(List<CommandExecutionResult> executions) {
        for (CommandExecutionResult execution : executions) {
            getTotalCommandTime += execution.getTotalTimeInMs();

            commandExecutionTimings.add(execution.getTotalTimeInMs());
            databaseTimings.add(execution.getDatabaseTimeInMs());

            addToDbOperation(execution.getDbSelects(), dbSelects);
            addToDbOperation(execution.getDbInserts(), dbInserts);
            addToDbOperation(execution.getDbUpdates(), dbUpdates);
            addToDbOperation(execution.getDbDeletes(), dbDeletes);
        }
    }

    protected void addToDbOperation(Map<String, Long> executionMap, Map<String, Long> globalMap) {
        for (String key : executionMap.keySet()) {
            if (!globalMap.containsKey(key)) {
                globalMap.put(key, 0L);
            }
            Long oldValue = globalMap.get(key);
            globalMap.put(key, oldValue + executionMap.get(key));
        }
    }

    public long getCount() {
        return commandExecutionTimings.size();
    }

    public long getGetTotalCommandTime() {
        return getTotalCommandTime;
    }

    public double getAverageExecutionTime() {
        long total = 0;
        for (Long timing : commandExecutionTimings) {
            total += timing.longValue();
        }
        double average = (double) total / (double) commandExecutionTimings.size();
        return Math.round(average * 100.0) / 100.0;
    }

    public double getAverageDatabaseExecutionTimePercentage() {
        double totalAvg = getAverageExecutionTime();
        double databaseAvg = getAverageDatabaseExecutionTime();
        double percentage = 100.0 * (databaseAvg / totalAvg);
        return Math.round(percentage * 100.0) / 100.0;
    }

    public double getAverageDatabaseExecutionTime() {
        long total = 0;
        for (Long timing : databaseTimings) {
            total += timing.longValue();
        }
        double average = (double) total / (double) commandExecutionTimings.size();
        return Math.round(average * 100.0) / 100.0;
    }

    public Map<String, Long> getDbSelects() {
        return dbSelects;
    }

    public void setDbSelects(Map<String, Long> dbSelects) {
        this.dbSelects = dbSelects;
    }

    public Map<String, Long> getDbInserts() {
        return dbInserts;
    }

    public void setDbInserts(Map<String, Long> dbInserts) {
        this.dbInserts = dbInserts;
    }

    public Map<String, Long> getDbUpdates() {
        return dbUpdates;
    }

    public void setDbUpdates(Map<String, Long> dbUpdates) {
        this.dbUpdates = dbUpdates;
    }

    public Map<String, Long> getDbDeletes() {
        return dbDeletes;
    }

    public void setDbDeletes(Map<String, Long> dbDeletes) {
        this.dbDeletes = dbDeletes;
    }

}
