package org.flowable.common.engine.impl.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class CandidateUtil {

    public static Collection<String> extractCandidates(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (value instanceof ArrayNode) {
            ArrayNode valueArrayNode = (ArrayNode) value;
            Collection<String> candidates = new ArrayList<>(valueArrayNode.size());
            for (JsonNode node : valueArrayNode) {
                candidates.add(node.asText());
            }

            return candidates;
        } else if (value != null) {
            String str = value.toString();
            if (StringUtils.isNotEmpty(str)) {
                return Arrays.asList(value.toString().split("[\\s]*,[\\s]*"));
            }
        }

        return Collections.emptyList();
    }

}
