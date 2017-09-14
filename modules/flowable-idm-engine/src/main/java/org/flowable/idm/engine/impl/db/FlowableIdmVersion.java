package org.flowable.idm.engine.impl.db;

import java.util.Collections;
import java.util.List;

/**
 * This class is used for auto-upgrade purposes.
 * 
 * The idea is that instances of this class are put in a sequential order, and that the current version is determined from the ACT_GE_PROPERTY table.
 * 
 * Since sometimes in the past, a version is ambiguous (eg. 5.12 => 5.12, 5.12.1, 5.12T) this class act as a wrapper with a smarter matches() method.
 * 
 * @author Joram Barrez
 */
public class FlowableIdmVersion {

    protected String mainVersion;
    protected List<String> alternativeVersionStrings;

    public FlowableIdmVersion(String mainVersion) {
        this.mainVersion = mainVersion;
        this.alternativeVersionStrings = Collections.singletonList(mainVersion);
    }

    public FlowableIdmVersion(String mainVersion, List<String> alternativeVersionStrings) {
        this.mainVersion = mainVersion;
        this.alternativeVersionStrings = alternativeVersionStrings;
    }

    public String getMainVersion() {
        return mainVersion;
    }

    public boolean matches(String version) {
        if (version.equals(mainVersion)) {
            return true;
        } else if (!alternativeVersionStrings.isEmpty()) {
            return alternativeVersionStrings.contains(version);
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FlowableIdmVersion)) {
            return false;
        }
        FlowableIdmVersion other = (FlowableIdmVersion) obj;
        boolean mainVersionEqual = mainVersion.equals(other.mainVersion);
        if (!mainVersionEqual) {
            return false;
        } else {
            if (alternativeVersionStrings != null) {
                return alternativeVersionStrings.equals(other.alternativeVersionStrings);
            } else {
                return other.alternativeVersionStrings == null;
            }
        }
    }

}
