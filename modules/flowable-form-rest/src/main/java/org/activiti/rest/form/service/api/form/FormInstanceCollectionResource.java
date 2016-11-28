package org.activiti.rest.form.service.api.form;

import org.activiti.rest.api.DataResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormInstanceCollectionResource extends BaseFormInstanceResource {

    @RequestMapping(value = "/form/form-instances", method = RequestMethod.GET, produces = "application/json")
    public DataResponse getFormInstances(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        // Populate query based on request
        FormInstanceQueryRequest queryRequest = new FormInstanceQueryRequest();

        if (allRequestParams.containsKey("id")) {
            queryRequest.setId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("formDefinitionId")) {
            queryRequest.setFormDefinitionId(allRequestParams.get("formDefinitionId"));
        }
        if (allRequestParams.containsKey("taskId")) {
            queryRequest.setTaskId(allRequestParams.get("taskId"));
        }
        if (allRequestParams.containsKey("processInstanceId")) {
            queryRequest.setProcessInstanceId(allRequestParams.get("processInstanceId"));
        }
        if (allRequestParams.containsKey("processDefinitionId")) {
            queryRequest.setProcessDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            queryRequest.setTaskId(allRequestParams.get("tenantId"));
        }

        return getQueryResponse(queryRequest, allRequestParams);
    }

}
