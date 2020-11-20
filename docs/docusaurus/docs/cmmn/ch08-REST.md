---
id: ch08-REST
title: REST API
---

## General Flowable REST principles

### Installation and Authentication

Flowable includes a REST API to the Flowable CMMN engine that can be installed by deploying the flowable-rest.war file to a servlet container like Apache Tomcat. However, it can also be used in another web-application by including the servlet (and/or its mappings) in your application and add all flowable-rest dependencies to the classpath.

By default Flowable REST will connect to an in-memory H2 database. You can change the database settings in the flowable-app.properties file in the *WEB-INF/META-INF/flowable-app* folder. The REST API uses JSON format (<http://www.json.org>) and is built upon the Spring MVC (<http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html>).

All REST-resources require a valid user with the *rest-access-api* privilege to be authenticated by default. If any valid user should be able to call the REST API, the *flowable.rest.app.authentication-mode* can be set to *any-user* (this was the default in older versions of Flowable).

A default user that can access the REST API can be configured by settings the following properties:

    flowable.rest.app.admin.user-id=rest-admin
    flowable.rest.app.admin.password=test
    flowable.rest.app.admin.first-name=Rest
    flowable.rest.app.admin.last-name=Admin

When the REST app boots up, the user is created if it doesn’t exist or fetched otherwise. This user will be given the *access-rest-api* privilege which is needed by default to access the REST API. **Do not forget to change the password of this user afterwards**. If the *flowable.rest.app.admin.user-id* is not set, no users or privileges will be created. So, after the initial setup, removing this property will not remove the user nor the privilege that has been configured before.

Basic HTTP access authentication is used, so you should always include a *Authorization: Basic …​==* HTTP-header when performing requests or include the username and password in the request-url (for example, *<http://username:password@localhost:8080/xyz>*).

**We recommend that you use Basic Authentication in combination with HTTPS.**

### Configuration

The Flowable REST web application uses the Spring Boot Flowable starter for starting the Flowable CMMN engine, defining the basic authentication security using Spring security, and to define the variable converters for specific variable handling.
A small number of properties can be defined by changing the flowable-app.properties file .


### Usage in Tomcat

Due to [default security properties on Tomcat](http://tomcat.apache.org/tomcat-8.0-doc/security-howto.html), **escaped forward slashes (%2F and %5C) are not allowed by default (400-result is returned).** This may have an impact on the deployment resources and their data-URL, as the URL can potentially contain escaped forward slashes.

When issues are experienced with unexpected 400-results, set the following system-property:

*-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW\_ENCODED\_SLASH=true*

It’s best practice to always set the **Accept** and **Content-Type** (in case of posting/putting JSON) headers to **application/json** on the HTTP requests described below.

### Methods and return-codes

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Method</th>
<th>Operations</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>GET</p></td>
<td><p>Get a single resource or get a collection of resources.</p></td>
</tr>
<tr class="even">
<td><p>POST</p></td>
<td><p>Create a new resource. Also used for executing resource-queries which have a too complex request-structure to fit in the query-URL of a GET-request.</p></td>
</tr>
<tr class="odd">
<td><p>PUT</p></td>
<td><p>Update properties of an existing resource. Also used for invoking actions on an existing resource.</p></td>
</tr>
<tr class="even">
<td><p>DELETE</p></td>
<td><p>Delete an existing resource.</p></td>
</tr>
</tbody>
</table>

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200 - Ok</p></td>
<td><p>The operation was successful and a response has been returned (GET and PUT requests).</p></td>
</tr>
<tr class="even">
<td><p>201 - Created</p></td>
<td><p>The operation was successful and the entity has been created and is returned in the response-body (POST request).</p></td>
</tr>
<tr class="odd">
<td><p>204 - No content</p></td>
<td><p>The operation was successful and entity has been deleted and therefore there is no response-body returned (DELETE request).</p></td>
</tr>
<tr class="even">
<td><p>401 - Unauthorized</p></td>
<td><p>The operation failed. The operation requires an Authentication header to be set. If this was present in the request, the supplied credentials are not valid or the user is not authorized to perform this operation.</p></td>
</tr>
<tr class="odd">
<td><p>403 - Forbidden</p></td>
<td><p>The operation is forbidden and should not be re-attempted. This implies an issue with authorization not authentication, it’s an operation that is not allowed. Example: deleting a task that is part of a running case is not allowed and will never be allowed, regardless of the user or case/task state.</p></td>
</tr>
<tr class="even">
<td><p>404 - Not found</p></td>
<td><p>The operation failed. The requested resource was not found.</p></td>
</tr>
<tr class="odd">
<td><p>405 - Method not allowed</p></td>
<td><p>The operation failed. The method is not allowed for this resource. For example, trying to update (PUT) a deployment-resource will result in a 405 status.</p></td>
</tr>
<tr class="even">
<td><p>409 - Conflict</p></td>
<td><p>The operation failed. The operation causes an update of a resource that has been updated by another operation, which makes the update no longer valid. Can also indicate a resource that is being created in a collection where a resource with that identifier already exists.</p></td>
</tr>
<tr class="odd">
<td><p>415 - Unsupported Media Type</p></td>
<td><p>The operation failed. The request body contains an unsupported media type. Also occurs when the request-body JSON contains an unknown attribute or value that doesn’t have the right format/type to be accepted.</p></td>
</tr>
<tr class="even">
<td><p>500 - Internal server error</p></td>
<td><p>The operation failed. An unexpected exception occurred while executing the operation. The response-body contains details about the error.</p></td>
</tr>
</tbody>
</table>

The media-type of the HTTP-responses is always application/json unless binary content is requested (for example, deployment resource data), the media-type of the content is used.

### Error response body

When an error occurs (both client and server, 4XX and 5XX status-codes) the response body contains an object describing the error that occurred. An example for a 404-status when a task is not found:

    {
      "statusCode" : 404,
      "errorMessage" : "Could not find a task with id '444'."
    }

### Request parameters

#### URL fragments

Parameters that are part of the URL (for example, the deploymentId parameter in http://host/flowable-rest/form-api/form-repository/deployments/{deploymentId})
need to be properly escaped (see [URL-encoding or Percent-encoding](https://en.wikipedia.org/wiki/Percent-encoding)) in case the segment contains special characters. Most frameworks have this functionality built in, but it should be taken into account. Especially for segments that can contain forward-slashes (for example, deployment resource), this is required.

#### Rest URL query parameters

Parameters added as query-string in the URL (for example, the name parameter used in http://host/flowable-rest/form-api/form-repository/deployments?name=Deployment) can have the following types and are mentioned in the corresponding REST-API documentation:

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type</th>
<th>Format</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>String</p></td>
<td><p>Plain text parameters. Can contain any valid characters that are allowed in URLs. In the case of a XXXLike parameter, the string should contain the wildcard character % (properly URL-encoded). This allows you to specify the intent of the like-search. For example, 'Tas%' matches all values, starting with 'Tas'.</p></td>
</tr>
<tr class="even">
<td><p>Integer</p></td>
<td><p>Parameter representing an integer value. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.</p></td>
</tr>
<tr class="odd">
<td><p>Long</p></td>
<td><p>Parameter representing a long value. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.</p></td>
</tr>
<tr class="even">
<td><p>Boolean</p></td>
<td><p>Parameter representing a boolean value. Can be either true or false. All other values other than these will cause a '405 - Bad request' response.</p></td>
</tr>
<tr class="odd">
<td><p>Date</p></td>
<td><p>Parameter representing a date value. Use the ISO-8601 date-format (see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO-8601 on wikipedia</a>) using both time and date-components (e.g. 2013-04-03T23:45Z).</p></td>
</tr>
</tbody>
</table>

#### JSON body parameters

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type</th>
<th>Format</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>String</p></td>
<td><p>Plain text parameters. In the case of a XXXLike parameter, the string should contain the wildcard character %. This allows you to specify the intent of the like-search. For example, 'Tas%' matches all values, starting with 'Tas'.</p></td>
</tr>
<tr class="even">
<td><p>Integer</p></td>
<td><p>Parameter representing an integer value, using a JSON number. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.</p></td>
</tr>
<tr class="odd">
<td><p>Long</p></td>
<td><p>Parameter representing a long value, using a JSON number. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.</p></td>
</tr>
<tr class="even">
<td><p>Date</p></td>
<td><p>Parameter representing a date value, using a JSON text. Use the ISO-8601 date-format (see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO-8601 on wikipedia</a>) using both time and date-components (for example, 2013-04-03T23:45Z).</p></td>
</tr>
</tbody>
</table>

#### Paging and sorting

Paging and order parameters can be added as query-string in the URL (for example, the name parameter used in http://host/flowable-rest/form-api/form-repository/deployments?sort=name).

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>sort</p></td>
<td><p>different per query implementation</p></td>
<td><p>Name of the sort key, for which the default value and the allowed values are different per query implementation.</p></td>
</tr>
<tr class="even">
<td><p>order</p></td>
<td><p>asc</p></td>
<td><p>Sorting order which can be 'asc' or 'desc'.</p></td>
</tr>
<tr class="odd">
<td><p>start</p></td>
<td><p>0</p></td>
<td><p>Parameter to allow for paging of the result. By default the result will start at 0.</p></td>
</tr>
<tr class="even">
<td><p>size</p></td>
<td><p>10</p></td>
<td><p>Parameter to allow for paging of the result. By default the size will be 10.</p></td>
</tr>
</tbody>
</table>

> **Note**
>
> Bear in mind that the start parameter is used as the offset of the query. For example, to get tasks in three pages of three items each (9 items), we would use:
>
>     GET /runtime/tasks?start=0&size=3
>     GET /runtime/tasks?start=3&size=3
>     GET /runtime/tasks?start=6&size=3

#### JSON query variable format

    {
      "name" : "variableName",
      "value" : "variableValue",
      "operation" : "equals",
      "type" : "string"
    }

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>Name of the variable to include in a query. Can be empty in the case where 'equals' is used in some queries to query for resources that have <strong>any variable name</strong> with the given value.</p></td>
</tr>
<tr class="even">
<td><p>value</p></td>
<td><p>Yes</p></td>
<td><p>Value of the variable included in the query, should include a correct format for the given type.</p></td>
</tr>
<tr class="odd">
<td><p>operator</p></td>
<td><p>Yes</p></td>
<td><p>Operator to use in query, can have the following values: equals, notEquals, equalsIgnoreCase, notEqualsIgnoreCase, lessThan, greaterThan, lessThanOrEquals, greaterThanOrEquals and like.</p></td>
</tr>
<tr class="even">
<td><p>type</p></td>
<td><p>No</p></td>
<td><p>Type of variable to use. When omitted, the type will be deduced from the value parameter. Any JSON text-values will be considered of type string, JSON booleans of type boolean, JSON numbers of type long or integer depending on the size of the number. We recommended you include an explicit type when in doubt. Types supported out of the box are listed below.</p></td>
</tr>
</tbody>
</table>

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type name</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>string</p></td>
<td><p>Value is treated as and converted to a java.lang.String.</p></td>
</tr>
<tr class="even">
<td><p>short</p></td>
<td><p>Value is treated as and converted to a java.lang.Integer.</p></td>
</tr>
<tr class="odd">
<td><p>integer</p></td>
<td><p>Value is treated as and converted to a java.lang.Integer.</p></td>
</tr>
<tr class="even">
<td><p>long</p></td>
<td><p>Value is treated as and converted to a java.lang.Long.</p></td>
</tr>
<tr class="odd">
<td><p>double</p></td>
<td><p>Value is treated as and converted to a java.lang.Double.</p></td>
</tr>
<tr class="even">
<td><p>boolean</p></td>
<td><p>Value is treated as and converted to a java.lang.Boolean.</p></td>
</tr>
<tr class="odd">
<td><p>date</p></td>
<td><p>Value is treated as and converted to a java.util.Date. The JSON string will be converted using ISO-8601 date format.</p></td>
</tr>
</tbody>
</table>

#### Variable representation

When working with variables (execute decision), the REST API uses some common principles and JSON-format for both reading and writing. The JSON representation of a variable looks like this:

    {
      "name" : "variableName",
      "value" : "variableValue",
      "valueUrl" : "http://...",
      "type" : "string"
    }

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>name</p></td>
<td><p>Yes</p></td>
<td><p>Name of the variable.</p></td>
</tr>
<tr class="even">
<td><p>value</p></td>
<td><p>No</p></td>
<td><p>Value of the variable. When writing a variable and value is omitted, null will be used as value.</p></td>
</tr>
<tr class="odd">
<td><p>valueUrl</p></td>
<td><p>No</p></td>
<td><p>When reading a variable of type binary or serializable, this attribute will point to the URL from where the raw binary data can be fetched.</p></td>
</tr>
<tr class="even">
<td><p>type</p></td>
<td><p>No</p></td>
<td><p>Type of the variable. See table below for additional information on types. When writing a variable and this value is omitted, the type will be deduced from the raw JSON-attribute request type and is limited to either string, double, integer and boolean. We advise you to always include a type to make sure no wrong assumption about the type are made.</p></td>
</tr>
</tbody>
</table>

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type name</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>string</p></td>
<td><p>Value is treated as a java.lang.String. Raw JSON-text value is used when writing a variable.</p></td>
</tr>
<tr class="even">
<td><p>integer</p></td>
<td><p>Value is treated as a java.lang.Integer. When writing, JSON number value is used as base for conversion, falls back to JSON text.</p></td>
</tr>
<tr class="odd">
<td><p>short</p></td>
<td><p>Value is treated as a java.lang.Short. When writing, JSON number value is used as base for conversion, falls back to JSON text.</p></td>
</tr>
<tr class="even">
<td><p>long</p></td>
<td><p>Value is treated as a java.lang.Long. When writing, JSON number value is used as base for conversion, falls back to JSON text.</p></td>
</tr>
<tr class="odd">
<td><p>double</p></td>
<td><p>Value is treated as a java.lang.Double. When writing, JSON number value is used as base for conversion, falls back to JSON text.</p></td>
</tr>
<tr class="even">
<td><p>boolean</p></td>
<td><p>Value is treated as a java.lang.Boolean. When writing, JSON boolean value is used for conversion.</p></td>
</tr>
<tr class="odd">
<td><p>date</p></td>
<td><p>Value is treated as a java.util.Date. When writing, the JSON text will be converted using ISO-8601 date format.</p></td>
</tr>
</tbody>
</table>

It’s possible to support additional variable-types with a custom JSON representation (either simple value or complex/nested JSON object). By extending the initializeVariableConverters() method on org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory, you can add additional org.flowable.rest.variable.RestVariableConverter classes to support converting your POJOs to a format suitable for transferring through REST and converting the REST-value back to your POJO. The actual transformation to JSON is done by Jackson.

## Deployment

**When using Tomcat, please read [Usage in Tomcat](#usage-in-tomcat).**

### List of Deployments

Queries all cmmn deployments and returns matching results.

    GET cmmn-repository/deployments
    
| Parameter | Required | Value | Description |
| ------------- | ------------- |  ------------- | ------------- | 
| name | No | String | Only return deployments with the given name. |
| nameLike | No | String | Only return deployments where the name is like the given string. |
| category | No | String | Only return deployments with the given category. |
| categoryLike | No | String | Only return deployments where the category is like the given string. |
| categoryNotEquals | No | String | Only return deployments where the category does not match the given category. |
| parentDeploymentId | No | String | Only return deployments with the given parent deployment id. |
| parentDeploymentId | No | String | Only return deployments where the parent deployment id is like the given string. |
| tenantId | No | String | Only return deployments with the given tenant identifier. |
| tenantIdLike | No | String | Only return deployments where the tenant identifier is like the given string. |
| withoutTenantId | No | String | Only return deployments that are without a tenant identifier. |
| sort | No | String | Property to sort on, to be used together with order. Possible values: id/name/deployTime/tenantId |

| Response Code | Description |
| ---- | ---- |
| 200 | Indicates the request was successful. |

Example response body:

```
{
    "data": [
        {
            "id": "59eca679-3c6b-11ea-8548-38c986587585",
            "name": "test-case",
            "deploymentTime": "2020-01-21T17:30:35.607+01:00",
            "category": null,
            "parentDeploymentId": null,
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585",
            "tenantId": ""
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "id",
    "order": "asc",
    "size": 1
}
```

### Get a deployment

    GET cmmn-repository/deployments/{deploymentId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|deploymentId|Yes|String|The id of the deployment to get.|

|Response code|Description}
| - | - |
|200|Indicates the deployment was found and returned.|
|404|Indicates the requested deployment was not found.|

*Success response body:*

```
{
    "id": "59eca679-3c6b-11ea-8548-38c986587585",
    "name": "test-case",
    "deploymentTime": "2020-01-21T17:30:35.607+01:00",
    "category": null,
    "parentDeploymentId": null,
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585",
    "tenantId": ""
}
```


### Create a new deployment

    POST cmmn-repository/deployments

*Request body:*

The request body should contain data of type _multipart/form-data_. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. If multiple resources need to be deployed in a single deployment, compress the resources in a zip and make sure the file-name ends with +.bar+ or +.zip+.

An additional parameter (form-field) can be passed in the request body with name +tenantId+. The value of this field will be used as the id of the tenant this deployment is done in.

|Response code|Description|
| - | - |
|201|Indicates the deployment was created.|
|400|Indicates there was no content present in the request body or the content mime-type is not supported for deployment. The status-description contains additional information.|

*Success response body:*

```
{
    "id": "59eca679-3c6b-11ea-8548-38c986587585",
    "name": "test-case",
    "deploymentTime": "2020-01-21T17:30:35.607+01:00",
    "category": null,
    "parentDeploymentId": null,
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585",
    "tenantId": ""
}
```

### Delete a deployment

    DELETE repository/deployments/{deploymentId}

|Parameter|Required|Value|Description|
|- |- |- |- |
|deploymentId|Yes|String|The id of the deployment to delete.|

|Response code|Description|
|- |- |
|204|Indicates the deployment was found and has been deleted. Response-body is intentionally empty.|
|404|Indicates the requested deployment was not found.|

### List resources in a deployment

    GET cmmn-repository/deployments/{deploymentId}/resources

|Parameter|Required|Value|Description|
|- |- |- |- |
|deploymentId|Yes|String|The id of the deployment to get the resources for.|

|Response code|Description|
|- |- |
|200|Indicates the deployment was found and the resource list has been returned.|
|404|Indicates the requested deployment was not found.|

*Success response body:*

```
[
    {
        "id": "test-case.cmmn.xml",
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.cmmn.xml",
        "contentUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resourcedata/test-case.cmmn.xml",
        "mediaType": "text/xml",
        "type": "caseDefinition"
    },
    {
        "id": "test-case.testCase.png",
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.testCase.png",
        "contentUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resourcedata/test-case.testCase.png",
        "mediaType": "image/png",
        "type": "resource"
    }
]
```

The contentUrl property in the resulting JSON for a single resource contains the actual URL to use for retrieving the binary resource.


### Get a deployment resource

    GET cmmn-repository/deployments/{deploymentId}/resources/{resourceId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|deploymentId|Yes|String|The id of the deployment the requested resource is part of.|
|resourceId|Yes|String|The id of the resource to get. *Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use 'diagrams%2Fmy-case.cmmn.xml' instead of 'diagrams/Fmy-case.cmmn.xml'.*|

|Response code|Description |
| - | - |
|200|Indicates both deployment and resource have been found and the resource has been returned.|
|404|Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.|

*Success response body:*

```
{
    "id": "test-case.cmmn.xml",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.cmmn.xml",
    "contentUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resourcedata/test-case.cmmn.xml",
    "mediaType": "text/xml",
    "type": "caseDefinition"
}
```

### Get a deployment resource content

    GET cmmn-repository/deployments/{deploymentId}/resourcedata/{resourceId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|deploymentId|Yes|String|The id of the deployment the requested resource is part of.}
|resourceId|Yes|String|The id of the resource to get the data for. *Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use 'diagrams%2Fmy-case.cmmn.xml' instead of 'diagrams/Fmy-case.cmmn.xml'.*|

|Response code|Description|
| - | - |
|200|Indicates both deployment and resource have been found and the resource data has been returned.|
|404|Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.|

*Success response body:*

The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources 'mimeType' property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.

## Case Definitions


### List of case definitions

    GET cmmn-repository/case-definitions

|Parameter|Required|Value|Description|
| - | - | - | - |
|version|No|integer|Only return case definitions with the given version.
|name|No|String|Only return case definitions with the given name.
|nameLike|No|String|Only return case definitions with a name like the given name.
|key|No|String|Only return case definitions with the given key.
|keyLike|No|String|Only return case definitions with a name like the given key.
|resourceName|No|String|Only return case definitions with the given resource name.
|resourceNameLike|No|String|Only return case definitions with a name like the given resource name.
|category|No|String|Only return case definitions with the given category.
|categoryLike|No|String|Only return case definitions with a category like the given name.
|categoryNotEquals|No|String|Only return case definitions which don't have the given category.
|deploymentId|No|String|Only return case definitions which are part of a deployment with the given id.
|startableByUser|No|String|Only return case definitions which can be started by the given user.
|latest|No|Boolean|Only return the latest case definition versions. Can only be used together with 'key' and 'keyLike' parameters, using any other parameter will result in a 400-response.
|suspended|No|Boolean|If +true+, only returns case definitions which are suspended. If +false+, only active case definitions (which are not suspended) are returned.
|sort|No|'name' (default), 'id', 'key', 'category', 'deploymentId' and 'version'|Property to sort on, to be used together with the 'order'.

|Response code|Description|
| - | - |
|200|Indicates request was successful and the case definitions are returned|
|400|Indicates a parameter was passed in the wrong format or that 'latest' is used with other parameters other than 'key' and 'keyLike'. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "59fd213c-3c6b-11ea-8548-38c986587585",
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "key": "testCase",
            "version": 1,
            "name": "Test Case",
            "description": null,
            "tenantId": "",
            "deploymentId": "59eca679-3c6b-11ea-8548-38c986587585",
            "deploymentUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585",
            "resource": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.cmmn.xml",
            "diagramResource": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.testCase.png",
            "category": "http://flowable.org/cmmn",
            "graphicalNotationDefined": true,
            "startFormDefined": false
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "name",
    "order": "asc",
    "size": 1
}
```


* `graphicalNotationDefined`: Indicates the case definition contains graphical information (BPMN DI).
* `resource`: Contains the actual deployed CMMN 1.1 xml.
* `diagramResource`: Contains a graphical representation of the case definition, null when no diagram is available.


### Get a case definition

    GET cmmn-repository/case-definitions/{caseDefinitionId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition to get.|

|Response code|Description|
| - | - |
|200|Indicates the case definition was found and returned.|
|404|Indicates the requested case definition was not found|

*Success response body:*

```
{
    "id": "59fd213c-3c6b-11ea-8548-38c986587585",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "key": "testCase",
    "version": 1,
    "name": "Test Case",
    "description": null,
    "tenantId": "",
    "deploymentId": "59eca679-3c6b-11ea-8548-38c986587585",
    "deploymentUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585",
    "resource": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.cmmn.xml",
    "diagramResource": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/deployments/59eca679-3c6b-11ea-8548-38c986587585/resources/test-case.testCase.png",
    "category": "http://flowable.org/cmmn",
    "graphicalNotationDefined": true,
    "startFormDefined": false
}
```

* `graphicalNotationDefined`: Indicates the case definition contains graphical information (BPMN DI).
* `resource`: Contains the actual deployed BPMN 2.0 xml.
* `diagramResource`: Contains a graphical representation of the case, null when no diagram is available.


### Update category for a case definition

    PUT cmmn-repository/case-definitions/{caseDefinitionId}

*Body JSON:*

```
{
  "category" : "updatedcategory"
}
```


|Response code|Description|
| - | - |
|200|Indicates the case was category was altered.|
|400|Indicates no category was defined in the request body.|
|404|Indicates the requested case definition was not found.|

### Get a case definition resource content

    GET cmmn-repository/case-definitions/{caseDefinitionId}/resourcedata

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition to get the resource data for.|

*Response:*

Exactly the same response codes/boy as +GET cmmn-repository/deployment/{deploymentId}/resourcedata/{resourceId}+.


### Get a case definition BPMN model

    GET cmmn-repository/case-definitions/{caseDefinitionId}/model

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition to get the model for.|

|Response code|Description|
| - | - |
|200|Indicates the case definition was found and the model is returned.|
|404|Indicates the requested case definition was not found.|

*Response body:*
The response body is a JSON representation of the +org.flowable.cmmn.model.CmmnModel+ and contains the full case definition model.

### Get all candidate starters for a case-definition

    GET repository/case-definitions/{caseDefinitionId}/identitylinks

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition to get the identity links for.|

|Response code|Description|
| - | - |
|200|Indicates the case definition was found and the requested identity links are returned.|
|404|Indicates the requested case definition was not found.|

*Success response body:*

```
[
    {
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585/identitylinks/groups/flowableUser",
        "user": null,
        "group": "flowableUser",
        "type": "candidate"
    }
]
```


### Add a candidate starter to a case definition

    POST cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition.|

*Request body (user):*

```
{
  "user" : "kermit"
}
```

*Request body (group):*

```
{
  "groupId" : "sales"
}
```

|Response code|Description|
| - | - |
|201|Indicates the case definition was found and the identity link was created.|
|404|Indicates the requested case definition was not found.|

*Success response body:*

### Delete a candidate starter from a case definition

    DELETE cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks/{family}/{identityId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition.|
|family|Yes|String|Either +users+ or +groups+, depending on the type of identity link.|
|identityId|Yes|String|Either the userId or groupId of the identity to remove as candidate starter.|

|Response code|Description|
|- | - |
|204|Indicates the case definition was found and the identity link was removed. The response body is intentionally empty.|
|404|Indicates the requested case definition was not found or the case definition doesn't have an identity-link that matches the url.|

### Get a candidate starter from a case definition

    GET cmmn-repository/case-definitions/{caseDefinitionId}/identitylinks/{family}/{identityId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseDefinitionId|Yes|String|The id of the case definition.|
|family|Yes|String|Either +users+ or +groups+, depending on the type of identity link.|
|identityId|Yes|String|Either the userId or groupId of the identity to get as candidate starter.|

|Response code|Description|
| - | - |
|200|Indicates the case definition was found and the identity link was returned.|
|404|Indicates the requested case definition was not found or the case definition doesn't have an identity-link that matches the url.|


## Case Instances

### Get a case instance

    GET cmmn-runtime/case-instances/{caseInstanceId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseInstanceId|Yes|String|The id of the case instance to get.|

|Response code|Description|
| - | - |
|200|Indicates the case instance was found and returned.|
|404|Indicates the requested case instance was not found.|

*Success response body:*

```
{
    "data": [
        {
            "id": "095fd94e-3c78-11ea-8548-38c986587585",
            "name": null,
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "businessKey": null,
            "startTime": "2020-01-21T19:01:23.923+01:00",
            "startUserId": "rest-admin",
            "state": "active",
            "ended": false,
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionName": "Test Case",
            "caseDefinitionDescription": null,
            "parentId": null,
            "callbackId": null,
            "callbackType": null,
            "referenceId": null,
            "referenceType": null,
            "variables": [],
            "tenantId": "",
            "completed": false
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "id",
    "order": "asc",
    "size": 1
}
```

### Delete a case instance

    DELETE cmmn-runtime/case-instances/{caseInstanceId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|caseInstanceId|Yes|String|The id of the case instance to delete.|

|Response code|Description|
| - | - |
|204|Indicates the case instance was found and deleted. Response body is left empty intentionally.|
|404|Indicates the requested case instance was not found.|

### Start a case instance

    POST cmmn-runtime/case-instances

*Request body (start by case definition id):*

```
{
      "caseDefinitionId":"59fd213c-3c6b-11ea-8548-38c986587585",
      "variables": [
         {
           "name":"myVar",
           "value":"This is a variable"
         }
      ]
   }
```

*Request body (start by case definition key):*

```
{
   "caseDefinitionKey":"testCase",
   "businessKey":"myBusinessKey",
   "tenantId": "tenant1",
   "variables": [
      {
        "name":"myVar",
        "value":"This is a variable"
      }
   ]
}
```

Note that also a _transientVariables_ property is accepted as part of this json, that follows the same structure as the _variables_ property.

The _returnVariables_ property can be used to get the existing variables in the case instance context back in the response. By default the variables are not returned.

Only one of +caseDefinitionId+ or +caseDefinitionKey+  can be used in the request body. Parameters +businessKey+, +variables+ and +tenantId+ are optional. If +tenantId+ is omitted, the default tenant will be used.


|Response code|Description|
| - | - |
|201|Indicates the case instance was created.|
|400|Indicates either the case-definition was not found (based on id or key) or an invalid variable has been passed. Status description contains additional information about the error.}

*Success response body:*

```
{
    "id": "095fd94e-3c78-11ea-8548-38c986587585",
    "name": null,
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "businessKey": null,
    "startTime": "2020-01-21T19:01:23.923+01:00",
    "startUserId": "rest-admin",
    "state": "active",
    "ended": false,
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionName": "Test Case",
    "caseDefinitionDescription": null,
    "parentId": null,
    "callbackId": null,
    "callbackType": null,
    "referenceId": null,
    "referenceType": null,
    "variables": [
        {
            "name": "myVar",
            "type": "string",
            "value": "This is a variable",
            "scope": "local"
        },
        {
            "name": "initiator",
            "type": "string",
            "value": "rest-admin",
            "scope": "local"
        }
    ],
    "tenantId": "",
    "completed": false
}
```

### List of case instances

    GET cmmn-runtime/case-instances

|Parameter|Required|Value|Description
| - | - | - | - |
|id|No|String|Only return case instance with the given id.|
|caseDefinitionKey|No|String|Only return case instances with the given case definition key.|
|caseDefinitionId|No|String|Only return case instances with the given case definition id.|
|businessKey|No|String|Only return case instances with the given businessKey.|
|involvedUser|No|String|Only return case instances in which the given user is involved.|
|includeCaseVariables|No|Boolean|Indication to include case variables in the result.
|tenantId|No|String|Only return case instances with the given tenantId.
|tenantIdLike|No|String|Only return case instances with a tenantId like the given value.
|withoutTenantId|No|Boolean|If +true+, only returns case instances without a tenantId set. If +false+, the +withoutTenantId+ parameter is ignored.
|sort|No|String|Sort field, should be either one of +id+ (default), +caseDefinitionId+, +tenantId+ or +caseDefinitionKey+.

|Response code|Description|
| - | - |
|200|Indicates request was successful and the case-instances are returned|
|400|Indicates a parameter was passed in the wrong format . The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "095fd94e-3c78-11ea-8548-38c986587585",
            "name": null,
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "businessKey": null,
            "startTime": "2020-01-21T19:01:23.923+01:00",
            "startUserId": "rest-admin",
            "state": "active",
            "ended": false,
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionName": "Test Case",
            "caseDefinitionDescription": null,
            "parentId": null,
            "callbackId": null,
            "callbackType": null,
            "referenceId": null,
            "referenceType": null,
            "variables": [],
            "tenantId": "",
            "completed": false
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "id",
    "order": "asc",
    "size": 1
}
```

### Query case instances

Alternative to the GET method, which takes a body with query parameters

    POST query/case-instances


*Request body:*

```
{
  "caseDefinitionKey":"myCase",
  "variables":
  [
    {
        "name" : "myVariable",
        "value" : 1234,
        "operation" : "equals",
        "type" : "long"
    }
  ]
}
```

|Response code|Description|
| - | - |
|200|Indicates request was successful and the case-instances are returned|
|400|Indicates a parameter was passed in the wrong format . The status-message contains additional information.|


### Get diagram for a case instance

    GET cmmn-runtime/case-instances/{caseInstanceId}/diagram

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to get the diagram for.|

|Response code|Description|
| - | - |
|200|Indicates the case instance was found and the diagram was returned.|
|400|Indicates the requested case instance was not found but the case doesn't contain any graphical information (CMMN:DI) and no diagram can be created.}
|404|Indicates the requested case instance was not found.|

### Get involved people for case instance

    GET cmmn-runtime/case-instances/{caseInstanceId}/identitylinks

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the links for.|

|Response code|Description|
| - | - |
|200|Indicates the case instance was found and links are returned.|
|404|Indicates the requested case instance was not found.|


*Success response body:*

```
[
    {
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585/identitylinks/users/rest-admin/starter",
        "user": "rest-admin",
        "group": null,
        "type": "starter"
    },
    {
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585/identitylinks/users/rest-admin/participant",
        "user": "rest-admin",
        "group": null,
        "type": "participant"
    }
]
```


### Add an involved user to a case instance

    POST cmmn-runtime/case-instances/{caseInstanceId}/identitylinks

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the links for.|

*Request body:*

```
{
  "userId":"kermit",
  "type":"participant"
}
```


Both +userId+ and +type+ are required.

|Response code|Description|
| - | - |
|201|Indicates the case instance was found and the link is created.|
|400|Indicates the requested body did not contain a userId or a type.|
|404|Indicates the requested case instance was not found.|

### Remove an involved user to from case instance

    DELETE cmmn-runtime/case-instances/{caseInstanceId}/identitylinks/users/{userId}/{type}


|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance.|
|userId|Yes|String|The id of the user to delete link for.|
|type|Yes|String|Type of link to delete.|

|Response code|Description|
| - | - |
|204|Indicates the case instance was found and the link has been deleted. Response body is left empty intentionally.|
|404|Indicates the requested case instance was not found or the link to delete doesn't exist. The response status contains additional information about the error.|

### List of variables for a case instance

    GET cmmn-runtime/case-instances/{caseInstanceId}/variables

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the variables for.|

|Response code|Description|
| - | - |
|200|Indicates the case instance was found and variables are returned.|
|404|Indicates the requested case instance was not found.|


*Success response body:*

```
| - | - | - | - | 
```


In case the variable is a binary variable or serializable, the +valueUrl+ points to an URL to fetch the raw value. If it's a plain variable, the value is present in the response.


### Get a variable for a case instance

    GET cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the variables for.|
|variableName|Yes|String|Name of the variable to get.|

|Response code|Description|
| - | - |
|200|Indicates both the case instance and variable were found and variable is returned.|
|400|Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.|
|404|Indicates the requested case instance was not found or the case instance does not have a variable with the given name. Status description contains additional information about the error.|

*Success response body:*

```
{
    "name": "myVar",
    "type": "string",
    "value": "This is a variable",
    "scope": null
}
```


In case the variable is a binary variable or serializable, the +valueUrl+ points to an URL to fetch the raw value. If it's a plain variable, the value is present in the response.  Note that only +local+ scoped variables are returned, as there is no +global+ scope for case-instance variables.


### Create (or update) variables on a case instance

    POST cmmn-runtime/case-instances/{caseInstanceId}/variables

    PUT cmmn-runtime/case-instances/{caseInstanceId}/variables


When using +POST+, all variables that are passed are created. In case one of the variables already exists on the case instance, the request results in an error (409 - CONFLICT). When +PUT+ is used, nonexistent variables are created on the case-instance and existing ones are overridden without any error.

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the variables for.|


*Request body:*

```
[
   {
      "name":"intProcVar",
      "type":"integer",
      "value":123
   },

   ...
]
```


Any number of variables can be passed into the request body array.

|Response code|Description|
| - | - |
|201|Indicates the case instance was found and variable is created.|
|400|Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.|
|404|Indicates the requested case instance was not found.|
|409|Indicates the case instance was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead.|

### Update a single variable on a case instance

    PUT cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to the variables for.|
|variableName|Yes|String|Name of the variable to get.|

```
 {
    "name":"intProcVar"
    "type":"integer"
    "value":123
 }
```


|Response code|Description|
| - | - |
|200|Indicates both the case instance and variable were found and variable is updated.|
|404|Indicates the requested case instance was not found or the case instance does not have a variable with the given name. Status description contains additional information about the error.|


In case the variable is a binary variable or serializable, the +valueUrl+ points to an URL to fetch the raw value. If it's a plain variable, the value is present in the response.

### Create a new binary variable on a case-instance

    POST runtime/case-instances/{caseInstanceId}/variables


|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to create the new variable for.|

*Request body:*

The request should be of type +multipart/form-data+. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

* `name`: Required name of the variable.
* `type`: Type of variable that is created. If omitted, +binary+ is assumed and the binary data in the request will be stored as an array of bytes.


*Success response body:*

```
{
    "name": "test",
    "type": "binary",
    "value": null,
    "valueUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585/variables/test/data",
    "scope": "local"
}
```

|Response code|Description|
| - | - |
|201|Indicates the variable was created and the result is returned.|
|400|Indicates the name of the variable to create was missing. Status message provides additional information.|
|404|Indicates the requested case instance was not found.|
|409|Indicates the case instance already has a variable with the given name. Use the PUT method to update the task variable instead.|
|415|Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.|

### Update an existing binary variable on a case-instance

    PUT runtime/case-instances/{caseInstanceId}/variables

|Parameter|Required|Value|Description|
| - | - | - | - | 
|caseInstanceId|Yes|String|The id of the case instance to create the new variable for.|

*Request body:*
The request should be of type +multipart/form-data+. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

* `name`: Required name of the variable.
* `type`: Type of variable that is created. If omitted, +binary+ is assumed and the binary data in the request will be stored as an array of bytes.


|Response code|Description|
| - | - |
|200|Indicates the variable was updated and the result is returned.|
|400|Indicates the name of the variable to update was missing. Status message provides additional information.|
|404|Indicates the requested case instance was not found or the case instance does not have a variable with the given name.|
|415|Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.|

### Get a stage overview

    GET /cmmn-runtime/{caseInstanceId}/stage-overview
    
|Parameter|Required|Value|Description|
| - | - | - | - |
|caseInstanceId|No|String|The case instance id for which the stage overview is retrieved.|    

*Success response body* {

```
[
    {
        "id": "stage1",
        "name": "First Stage",
        "current": true
        "ended": false,
        "endTime": null,
    },
    {
        "id": "stage2",
        "name": "Second stage",
        "current": false
        "ended": false,
        "endTime": null,
    }
]
```

## Plan Item instances

### List of plan item instances

    GET cmmn-runtime/plan-item-instances

|Parameter|Required|Value|Description|
| - | - | - | - |
|id|No|String|Only return plan item instances with the given id.|
|caseDefinitionId|No|String|Only return plan item instances with the given case definition id.|
|caseInstanceId|No|String|Only return plan item instances with the given case instance id.|
|stageInstanceId|No|String|Only return plan item instances with the given stage instance id (which is a plan item instance itself).|
|planItemDefinitionId|No|String|Only return plan item instances with the given plan item definition id.|
|planItemDefinitionType|No|String|Only return plan item instances with the given plan item definition type (e.g. stage, milestone, etc.).|
|planItemDefinitionTypes|No|String|Only return plan item instances which have any of the passed types. The types are passed as a comma-separated list.|
|state|No|String|Only return plan item instances which are in the given state.|
|name|No|String|Only return plan item instances with the given name.|
|elementId|No|String|Only return plan item instances with the given element id (as given in the CMMN model).|
|referenceId|No|String|Only return plan item instances with the given reference id (e.g. when the plan item is a process task).|
|referenceType|No|Date|Only return plan item instances with the given reference type.|
|createdBefore|No|Date|Only return plan item instances which were started before the given date.|
|createdAfter|No|String|Only return plan item instances which were started after the given date.|
|starUserId|No|String|Only return plan item instances which were started by the given user.|
|tenantId|No|String|Only return plan item instances with the given tenant id.|
|withoutTenantId|No|String|Only return plan item instances which have no tenant id.|
|sort|No|'name' and 'createTime'|Property to sort on, to be used together with the 'order'.|

|Response code|Description|
| - | - |
|200|Indicates request was successful and the plan item instances are returned.|
|400|Indicates a parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "09618702-3c78-11ea-8548-38c986587585",
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/plan-item-instances/09618702-3c78-11ea-8548-38c986587585",
            "name": null,
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "derivedCaseDefinitionId": null,
            "derivedCaseDefinitionUrl": null,
            "stageInstanceId": null,
            "stageInstanceUrl": null,
            "planItemDefinitionId": "expandedStage1",
            "planItemDefinitionType": "stage",
            "state": "active",
            "stage": true,
            "elementId": "planItem2",
            "createTime": "2020-01-21T19:01:23.934+01:00",
            "lastAvailableTime": "2020-01-21T19:01:23.942+01:00",
            "lastEnabledTime": null,
            "lastDisabledTime": null,
            "lastStartedTime": "2020-01-21T19:01:23.974+01:00",
            "lastSuspendedTime": null,
            "completedTime": null,
            "occurredTime": null,
            "terminatedTime": null,
            "exitTime": null,
            "endedTime": null,
            "startUserId": null,
            "referenceId": null,
            "referenceType": null,
            "completable": false,
            "entryCriterionId": null,
            "exitCriterionId": null,
            "formKey": null,
            "extraValue": null,
            "tenantId": ""
        },
        ...
    ],
    "total": 3,
    "start": 0,
    "sort": "createTime",
    "order": "asc",
    "size": 3
}
```

### Get a single plan item instance

    GET cmmn-runtime/plan-item-instances/{planItemInstanceId}
    
|Parameter|Required|Value|Description|
| - | - | - | - | 
|planItemInstanceId|Yes|String|The id of the plan item instance.|

|Response code|Description|
| - | - |
|200|Indicates request was successful and the plan item instance is returned.|
|404|Indicates the plan item instance was not found.|

*Success response body:*

```
{
    "id": "09618702-3c78-11ea-8548-38c986587585",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/plan-item-instances/09618702-3c78-11ea-8548-38c986587585",
    "name": null,
    "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
    "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "derivedCaseDefinitionId": null,
    "derivedCaseDefinitionUrl": null,
    "stageInstanceId": null,
    "stageInstanceUrl": null,
    "planItemDefinitionId": "expandedStage1",
    "planItemDefinitionType": "stage",
    "state": "active",
    "stage": true,
    "elementId": "planItem2",
    "createTime": "2020-01-21T19:01:23.934+01:00",
    "lastAvailableTime": "2020-01-21T19:01:23.942+01:00",
    "lastEnabledTime": null,
    "lastDisabledTime": null,
    "lastStartedTime": "2020-01-21T19:01:23.974+01:00",
    "lastSuspendedTime": null,
    "completedTime": null,
    "occurredTime": null,
    "terminatedTime": null,
    "exitTime": null,
    "endedTime": null,
    "startUserId": null,
    "referenceId": null,
    "referenceType": null,
    "completable": false,
    "entryCriterionId": null,
    "exitCriterionId": null,
    "formKey": null,
    "extraValue": null,
    "tenantId": ""
}
```

### Execute an action on a plan item instance

    PUT cmmn-runtime/plan-item-instances/{planItemInstanceId}
    
Using a JSON body like

```
{
    "action": "start"
}
```    

Possible actions:

|Action|Description|
| - | - |
|start|Starts a plan item (which needs to be in the enabled state).|
|trigger|Triggers a plan item (which is waiting for a trigger).|
|enable|Enables a plan item (which is currently disabled).|
|disable|Triggers a plan item (which is currently enabled).|
|evaluateCriteria|Evaluates the plan item instances sentries (and anything that would have a consequence of doing that).|


## Tasks

### List of tasks

    GET cmmn-runtime/tasks

|Parameter|Required|Value|Description
| - | - | - | - |
|name|No|String|Only return tasks with the given name.
|nameLike|No|String|Only return tasks with a name like the given name.
|description|No|String|Only return tasks with the given description.
|priority|No|Integer|Only return tasks with the given priority.
|minimumPriority|No|Integer|Only return tasks with a priority greater than the given value.
|maximumPriority|No|Integer|Only return tasks with a priority lower than the given value.
|assignee|No|String|Only return tasks assigned to the given user.
|assigneeLike|No|String|Only return tasks assigned with an assignee like the given value.
|owner|No|String|Only return tasks owned by the given user.
|ownerLike|No|String|Only return tasks assigned with an owner like the given value.
|unassigned|No|Boolean|Only return tasks that are not assigned to anyone. If +false+ is passed, the value is ignored.
|delegationState|No|String|Only return tasks that have the given delegation state. Possible values are +pending+ and +resolved+.
|candidateUser|No|String|Only return tasks that can be claimed by the given user. This includes both tasks where the user is an explicit candidate for and task that are claimable by a group that the user is a member of.
|candidateGroup|No|String|Only return tasks that can be claimed by a user in the given group.
|candidateGroups|No|String|Only return tasks that can be claimed by a user in the given groups. Values split by comma.
|involvedUser|No|String|Only return tasks in which the given user is involved.
|taskDefinitionKey|No|String|Only return tasks with the given task definition id.
|taskDefinitionKeyLike|No|String|Only return tasks with a given task definition id like the given value.
|caseInstanceId|No|String|Only return tasks which are part of the case instance with the given id.
|caseDefinitionId|No|String|Only return tasks which are part of the case definition with the given id.
|createdOn|No|ISO Date|Only return tasks which are created on the given date.
|createdBefore|No|ISO Date|Only return tasks which are created before the given date.
|createdAfter|No|ISO Date|Only return tasks which are created after the given date.
|dueOn|No|ISO Date|Only return tasks which are due on the given date.
|dueBefore|No|ISO Date|Only return tasks which are due before the given date.
|dueAfter|No|ISO Date|Only return tasks which are due after the given date.
|withoutDueDate|No|boolean|Only return tasks which don't have a due date. The property is ignored if the value is +false+.
|excludeSubTasks|No|Boolean|Only return tasks that are not a subtask of another task.
|active|No|Boolean|If +true+, only return tasks that are not suspended (either part of a process that is not suspended or not part of a process at all). If false, only tasks that are part of suspended process instances are returned.
|includeTaskLocalVariables|No|Boolean|Indication to include task local variables in the result.
|tenantId|No|String|Only return tasks with the given tenantId.
|tenantIdLike|No|String|Only return tasks with a tenantId like the given value.
|withoutTenantId|No|Boolean|If +true+, only returns tasks without a tenantId set. If +false+, the +withoutTenantId+ parameter is ignored.
|candidateOrAssigned|No|String|Select tasks that has been claimed or assigned to user or waiting to claim by user (candidate user or groups).
|category|No|string|Select tasks with the given category. Note that this is the task category, not the category of the case definition.


|Response code|Description|
| - | -|
|200|Indicates request was successful and the tasks are returned|
|400|Indicates a parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "09688be5-3c78-11ea-8548-38c986587585",
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/tasks/09688be5-3c78-11ea-8548-38c986587585",
            "owner": null,
            "assignee": "rest-admin",
            "delegationState": null,
            "name": "Human task",
            "description": null,
            "createTime": "2020-01-21T19:01:23.975+01:00",
            "dueDate": null,
            "priority": 50,
            "suspended": false,
            "claimTime": null,
            "taskDefinitionKey": "humanTask1",
            "tenantId": "",
            "category": null,
            "formKey": null,
            "parentTaskId": null,
            "parentTaskUrl": null,
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "variables": []
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "id",
    "order": "asc",
    "size": 1
}
```

### Get a task

    GET cmmn-runtime/tasks/{taskId}

|Parameter|Required|Value|Description|
| - | - | - | - |
|taskId|Yes|String|The id of the task to get.

|Response code|Description|
|- | - |
|200|Indicates the task was found and returned.|
|404|Indicates the requested task was not found.|

*Success response body:*

```
{
    "id": "09688be5-3c78-11ea-8548-38c986587585",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/tasks/09688be5-3c78-11ea-8548-38c986587585",
    "owner": null,
    "assignee": "rest-admin",
    "delegationState": null,
    "name": "Human task",
    "description": null,
    "createTime": "2020-01-21T19:01:23.975+01:00",
    "dueDate": null,
    "priority": 50,
    "suspended": false,
    "claimTime": null,
    "taskDefinitionKey": "humanTask1",
    "tenantId": "",
    "category": null,
    "formKey": null,
    "parentTaskId": null,
    "parentTaskUrl": null,
    "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
    "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "variables": []
}
```

### Update a task

    PUT cmmn-runtime/tasks/{taskId}

*Body JSON:*

```
{
  "assignee" : "assignee",
  "delegationState" : "resolved",
  "description" : "New task description",
  "dueDate" : "2020-04-17T13:06:02.438+02:00",
  "name" : "New task name",
  "owner" : "owner",
  "parentTaskId" : "3",
  "priority" : 20
}
```

All request values are optional. For example, you can only include the 'assignee' attribute in the request body JSON-object, only updating the assignee of the task, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the task-value will be updated to null. Example: +{"dueDate" : null}+ will clear the duedate of the task).

Following parameters are available to update:

|Parameter|Type|
|-|-|
|name|String|
|description|String|
|dueDate|Date|
|assignee|String|
|owner|String|
|parentTaskId|String|
|priority|integer|
|category|String|
|formKey|String|
|delegationState|String|
|tenantId|String|


|Response code|Description
|-|-|
|200|Indicates the task was updated.
|404|Indicates the requested task was not found.
|409|Indicates the requested task was updated simultaneously.

### Task actions

    POST cmmn-runtime/tasks/{taskId}

*Complete a task - Body JSON:*

```
{
  "action" : "complete",
  "variables" : []
}
```

|Parameter|Type|Description|
|-|-|-|
|action|String|Can be complete, claim, delegate or resolve.|
|assignee|String|The assignee of the task|
|outcome|String|Used when completing the task to designate the outcome of the task.|
|variables|String|Variables to be set.|
|transientVariables|String|Transient variables to set|

*Claim a task - Body JSON:*

```
{
  "action" : "claim",
  "assignee" : "userWhoClaims"
}
```

Claims the task by the given assignee. If the assignee is +null+, the task is assigned to no-one, claimable again.


|Response code|Description|
|-|-|
|200|Indicates the action was executed.|
|400|When the body contains an invalid value or when the assignee is missing when the action requires it.|
|404|Indicates the requested task was not found.|
|409|Indicates the action cannot be performed due to a conflict. Either the task was updates simultaneously or the task was claimed by another user, in case of the '`claim`' action.|

### Delete a task

    DELETE cmmn-runtime/tasks/{taskId}?cascadeHistory={cascadeHistory}&deleteReason={deleteReason}


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to delete.|
|cascadeHistory|False|Boolean|Whether or not to delete the HistoricTask instance when deleting the task (if applicable). If not provided, this value defaults to false.|
|deleteReason|False|String|Reason why the task is deleted. This value is ignored when +cascadeHistory+ is true.|

|Response code|Description|
|-|-|
|204|Indicates the task was found and has been deleted. Response-body is intentionally empty.|
|403|Indicates the requested task cannot be deleted because it's part of a case instance.|
|404|Indicates the requested task was not found.|

### Get all variables for a task

    GET cmmn-runtime/tasks/{taskId}/variables?scope={scope}


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to get variables for.|
|scope|False|String|Scope of variables to be returned. When '`local`', only task-local variables are returned. When '`global`', only variables from the task's parent execution-hierarchy are returned. When the parameter is omitted, both local and global variables are returned.|

|Response code|Description|
|-|-|
|200|Indicates the task was found and the requested variables are returned.|
|404|Indicates the requested task was not found.|

*Success response body:*

```
[
    {
        "name": "myVar",
        "type": "string",
        "value": "This is a variable",
        "scope": "global"
    },
    {
        "name": "test",
        "type": "binary",
        "value": null,
        "valueUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/tasks/09688be5-3c78-11ea-8548-38c986587585/variables/test/data",
        "scope": "global"
    },
    {
        "name": "initiator",
        "type": "string",
        "value": "rest-admin",
        "scope": "global"
    }
]
```

### Get a variable from a task

    GET cmmn-runtime/tasks/{taskId}/variables/{variableName}?scope={scope}


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to get a variable for.|
|variableName|Yes|String|The name of the variable to get.|
|scope|False|String|Scope of variable to be returned. When '`local`', only task-local variable value is returned. When '`global`', only variable value from the task's parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.|

|Response code|Description|
|-|-|
|200|Indicates the task was found and the requested variables are returned.|
|404|Indicates the requested task was not found or the task doesn't have a variable with the given name (in the given scope). Status message provides additional information.|


*Success response body:*

```
{
    "name": "myVar",
    "type": "string",
    "value": "This is a variable",
    "scope": "global"
}
```


### Get the binary data for a variable

    GET cmmn-runtime/tasks/{taskId}/variables/{variableName}/data?scope={scope}


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to get a variable data for.|
|variableName|Yes|String|The name of the variable to get data for. Only variables of type +binary+ and +serializable+ can be used. If any other type of variable is used, a +404+ is returned.|
|scope|False|String|Scope of variable to be returned. When '`local`', only task-local variable value is returned. When '`global`', only variable value from the task's parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.|

|Response code|Description|
|-|-|
|200|Indicates the task was found and the requested variables are returned.|
|404|Indicates the requested task was not found or the task doesn't have a variable with the given name (in the given scope) or the variable doesn't have a binary stream available. Status message provides additional information.|

*Success response body:*

The response body contains the binary value of the variable. When the variable is of type +binary+, the content-type of the response is set to +application/octet-stream+, regardless of the content of the variable or the request accept-type header. In case of +serializable+, +application/x-java-serialized-object+ is used as content-type.


### Create new variables on a task

    POST cmmn-runtime/tasks/{taskId}/variables

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to create the new variable for.|


*Request body for creating simple (non-binary) variables:*

```
[
  {
    "name" : "myTaskVariable",
    "scope" : "local",
    "type" : "string",
    "value" : "Hello World"
  },
  {
    ...
  }
]
```


The request body should be an array containing one or more JSON-objects representing the variables that should be created.

* `name`: Required name of the variable
* `scope`: Scope of variable that is created. If omitted, +local+ is assumed.
* `type`: Type of variable that is created. If omitted, reverts to raw JSON-value type (string, boolean, integer or double).
* `value`: Variable value.

|Response code|Description|
|-|-|
|201|Indicates the variables were created and the result is returned.|
|400|Indicates the name of a variable to create was missing or that an attempt is done to create a variable on a standalone task (without a process associated) with scope +global+ or an empty array of variables was included in the request or request did not contain an array of variables. Status message provides additional information.|
|404|Indicates the requested task was not found.|
|409|Indicates the task already has a variable with the given name. Use the PUT method to update the task variable instead.|

### Create a new binary variable on a task

    POST cmmn-runtime/tasks/{taskId}/variables


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to create the new variable for.|


*Request body:*

The request should be of type +multipart/form-data+. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

* `name`: Required name of the variable.
* `scope`: Scope of variable that is created. If omitted, +local+ is assumed.
* `type`: Type of variable that is created. If omitted, +binary+ is assumed and the binary data in the request will be stored as an array of bytes.

|Response code|Description|
|-|-|
|201|Indicates the variable was created and the result is returned.|
|400|Indicates the name of the variable to create was missing or that an attempt is done to create a variable on a standalone task (without a process associated) with scope +global+. Status message provides additional information.|
|404|Indicates the requested task was not found.|
|409|Indicates the task already has a variable with the given name. Use the PUT method to update the task variable instead.|
|415|Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.|

### Update an existing variable on a task

    PUT cmmn-runtime/tasks/{taskId}/variables/{variableName}

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to update the variable for.|
|variableName|Yes|String|The name of the variable to update.|

*Request body for updating simple (non-binary) variables:*

```
{
  "name" : "myTaskVariable",
  "scope" : "local",
  "type" : "string",
  "value" : "Hello World"
}
```

* `name`: Required name of the variable
* `scope`: Scope of variable that is updated. If omitted, +local+ is assumed.
* `type`: Type of variable that is updated. If omitted, reverts to raw JSON-value type (string, boolean, integer or double).
* `value`: Variable value.

|Response code|Description|
|-|-|
|200|Indicates the variables was updated and the result is returned.|
|400|Indicates the name of a variable to update was missing or that an attempt is done to update a variable on a standalone task (without a process associated) with scope +global+. Status message provides additional information.|
|404|Indicates the requested task was not found or the task doesn't have a variable with the given name in the given scope. Status message contains additional information about the error.|

### Updating a binary variable on a task

    PUT cmmn-runtime/tasks/{taskId}/variables/{variableName}

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to update the variable for.|
|variableName|Yes|String|The name of the variable to update.|

*Request body:*

The request should be of type +multipart/form-data+. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

* `name`: Required name of the variable.
* `scope`: Scope of variable that is updated. If omitted, +local+ is assumed.
* `type`: Type of variable that is updated. If omitted, +binary+ is assumed and the binary data in the request will be stored as an array of bytes.

|Response code|Description|
|-|-|
|200|Indicates the variable was updated and the result is returned.|
|400|Indicates the name of the variable to update was missing or that an attempt is done to update a variable on a standalone task (without a process associated) with scope +global+. Status message provides additional information.|
|404|Indicates the requested task was not found or the variable to update doesn't exist for the given task in the given scope.|
|415|Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.|

### Delete a variable on a task

    DELETE cmmn-runtime/tasks/{taskId}/variables/{variableName}?scope={scope}

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task the variable to delete belongs to.|
|variableName|Yes|String|The name of the variable to delete.|
|scope|No|String|Scope of variable to delete in. Can be either +local+ or +global+. If omitted, +local+ is assumed.|


|Response code|Description|
|-|-|
|204|Indicates the task variable was found and has been deleted. Response-body is intentionally empty.|
|404|Indicates the requested task was not found or the task doesn't have a variable with the given name. Status message contains additional information about the error.|

### Delete all local variables on a task

    DELETE cmmn-runtime/tasks/{taskId}/variables

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task the variable to delete belongs to.|


|Response code|Description|
|-|-|
|204|Indicates all local task variables have been deleted. Response-body is intentionally empty.|
|404|Indicates the requested task was not found.|

### Get all identity links for a task

    GET cmmn-runtime/tasks/{taskId}/identitylinks


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task to get the identity links for.|

|Response code|Description|
|-|-|
|200|Indicates the task was found and the requested identity links are returned.|
|404|Indicates the requested task was not found.|

*Success response body:*

```
[
    {
        "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-runtime/tasks/09688be5-3c78-11ea-8548-38c986587585/identitylinks/users/rest-admin/assignee",
        "user": "rest-admin",
        "group": null,
        "type": "assignee"
    }
]
```


### Get all identitylinks for a task for either groups or users

    GET cmmn-runtime/tasks/{taskId}/identitylinks/users
    GET cmmn-runtime/tasks/{taskId}/identitylinks/groups


Returns only identity links targeting either users or groups. Response body and status-codes are exactly the same as when getting the full list of identity links for a task.


### Get a single identity link on a task

    GET cmmn-runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task .|
|family|Yes|String|Either +groups+ or +users+, depending on what kind of identity is targeted.|
|identityId|Yes|String|The id of the identity.|
|type|Yes|String|The type of identity link.|

|Response code|Description|
|-|-|
|200|Indicates the task and identity link was found and returned.|
|404|Indicates the requested task was not found or the task doesn't have the requested identityLink. The status contains additional information about this error.|

### Create an identity link on a task

    POST cmmn-runtime/tasks/{taskId}/identitylinks

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task.|

*Request body (user):*

```
{
  "userId" : "kermit",
  "type" : "candidate",
}
```

*Request body (group):*

```
{
  "groupId" : "sales",
  "type" : "candidate",
}
```

|Response code|Description|
|-|-|
|201|Indicates the task was found and the identity link was created.|
|404|Indicates the requested task was not found or the task doesn't have the requested identityLink. The status contains additional information about this error.|

*Success response body:*

```
{
  "userId" : null,
  "groupId" : "sales",
  "type" : "candidate",
  "url" : "http://localhost:8081/flowable-rest/service/runtime/tasks/100/identitylinks/groups/sales/candidate"
}
```

### Delete an identity link on a task

    DELETE cmmn-runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|Yes|String|The id of the task.|
|family|Yes|String|Either +groups+ or +users+, depending on what kind of identity is targeted.|
|identityId|Yes|String|The id of the identity.|
|type|Yes|String|The type of identity link.|

|Response code|Description|
|-|-|
|204|Indicates the task and identity link were found and the link has been deleted. Response-body is intentionally empty.|
|404|Indicates the requested task was not found or the task doesn't have the requested identityLink. The status contains additional information about this error.|

## History

### List of historic case instances

    GET cmmn-history/historic-case-instances

URL parameters:

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|An id of the historic case instance.
|caseDefinitionKey|No|String|The case definition key of the historic case instance.
|caseDefinitionId|No|String|The case definition id of the historic case instance.
|businessKey|No|String|The business key of the historic case instance.
|involvedUser|No|String|An involved user of the historic case instance.
|finished|No|Boolean|Indication if the historic case instance is finished.
|finishedAfter|No|Date|Return only historic case instances that were finished after this date.
|finishedBefore|No|Date|Return only historic case instances that were finished before this date.
|startedAfter|No|Date|Return only historic case instances that were started after this date.
|startedBefore|No|Date|Return only historic case instances that were started before this date.
|startedBy|No|String|Return only historic case instances that were started by this user.
|includeCaseVariables|No|Boolean|An indication if the historic case instance variables should be returned as well.
|tenantId|No|String|Only return instances with the given tenantId.
|tenantIdLike|No|String|Only return instances with a tenantId like the given value.
|withoutTenantId|No|Boolean|If +true+, only returns instances without a tenantId set. If +false+, the +withoutTenantId+ parameter is ignored.

|Response code|Description|
|-|-|
|200|Indicates that historic case instances could be queried.|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "095fd94e-3c78-11ea-8548-38c986587585",
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "name": null,
            "businessKey": null,
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionName": "Test Case",
            "caseDefinitionDescription": null,
            "startTime": "2020-01-21T19:01:23.923+01:00",
            "endTime": null,
            "startUserId": "rest-admin",
            "superProcessInstanceId": null,
            "variables": [],
            "tenantId": "",
            "state": "active",
            "callbackId": null,
            "callbackType": null,
            "referenceId": null,
            "referenceType": null
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "caseInstanceId",
    "order": "asc",
    "size": 1
}
```

### Query for historic case instances

    POST query/historic-case-instances

*Request body:*

```
{
  "caseDefinitionKey" : "myCase",


  "variables" : [
    {
      "name" : "myVariable",
      "value" : 1234,
      "operation" : "equals",
      "type" : "long"
    }
  ]
}
```

All supported JSON parameter fields allowed are exactly the same as the parameters in the previous section, but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri's that are too long. On top of that, the query allows for filtering based on case variables. The +variables+ property is a JSON-array containing objects using the regular Flowable REST format.


|Response code|Description|
|-|-|
|200|Indicates request was successful and the tasks are returned|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

### Get a historic case instance

    GET cmmn-history/historic-case-instances/{caseInstanceId}
    
|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The id of the historic case instance.|    

|Response code|Description|
|-|-|
|200|Indicates that the historic case instances could be found.|
|404|Indicates that the historic case instances could not be found.|

*Success response body:*

```
{
    "id": "095fd94e-3c78-11ea-8548-38c986587585",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "name": null,
    "businessKey": null,
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionName": "Test Case",
    "caseDefinitionDescription": null,
    "startTime": "2020-01-21T19:01:23.923+01:00",
    "endTime": null,
    "startUserId": "rest-admin",
    "superProcessInstanceId": null,
    "variables": [],
    "tenantId": "",
    "state": "active",
    "callbackId": null,
    "callbackType": null,
    "referenceId": null,
    "referenceType": null
}
```

### Delete a historic case instance

    DELETE cmmn-history/historic-case-instances/{caseInstanceId}

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The id of the historic case instance.|    

|Response code|Description|
|-|-|
|200|Indicates that the historic case instance was deleted.|
|404|Indicates that the historic case instance could not be found.|

### Get the identity links of a historic case instance

    GET cmmn-history/historic-case-instance/{caseInstanceId}/identitylinks

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The id of the historic case instance.|    

|Response code|Description|
|-|-|
|200|Indicates request was successful and the identity links are returned.|
|404|Indicates the case instance could not be found.|

*Success response body:*

```
[
    {
        "type": "starter",
        "userId": "rest-admin",
        "groupId": null,
        "taskId": null,
        "taskUrl": null,
        "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
        "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585"
    },
    {
        "type": "participant",
        "userId": "rest-admin",
        "groupId": null,
        "taskId": null,
        "taskUrl": null,
        "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
        "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585"
    }
]
```

### Get the binary data for a historic case instance variable

    GET cmmn-history/historic-case-instances/{caseInstanceId}/variables/{variableName}/data

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The id of the historic case instance.|
|variableName|The name of the variable of which the data needs to be returned.|

|Response code|Description|
|-|-|
|200|Indicates the case instance was found and the requested variable data is returned.|
|404|Indicates the requested case instance was not found or the case instance doesn't have a variable with the given name or the variable doesn't have a binary stream available. Status message provides additional information.|

*Success response body:*

The response body contains the binary value of the variable. When the variable is of type +binary+, the content-type of the response is set to +application/octet-stream+, regardless of the content of the variable or the request accept-type header. In case of +serializable+, +application/x-java-serialized-object+ is used as content-type.


### Get the stage overview for a historic case instance

    GET cmmn-history/historic-case-instances/{caseInstanceId}/stage-overview
    
|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The id of the historic case instance.|    

*Success response body:*

```
[
    {
        "id": "stage1",
        "name": "First Stage",
        "current": true
        "ended": false,
        "endTime": null,
    },
    {
        "id": "stage2",
        "name": "Second stage",
        "current": false
        "ended": false,
        "endTime": null,
    }
]
```

### List of historic milestones

    GET cmmn-history/historic-milestone-instances
    
Note: a milestone is in fact represented as a plan item instance.    
    
|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|Only return milestone instances which belong to the case instance with the given id.|
|caseDefinitionId|No|String|Only return milestone instances which belong to the case definition with the given id.|
|mileStoneId|No|String|Only return milestone instances with the given id (i.e. plan item instance id).|
|mileStoneName|No|String|Only return milestone instances with the given name.|
|reachedBefore|No|Date|Only return milestone instances that have been reached before the provided date.|
|reachedBefore|No|Date|Only return milestone instances that have been reached after the provided date.|

|Response code|Description|
|-|-|
|200|Indicates the milestone instance was found and the requested variable data is returned.|
|404|The milestone instance was not found.|

*success response body:* 

```
{
    "data": [
        {
            "id": "095fd94e-3c78-11ea-8548-38c986587585",
            "name": "My milestone",
            "elementId": "milestone1",
            "caseInstanceId: "123fd94e-3c78-23ea-8548-38c986587585",
            "caseDefinitionId: "567fd94e-3c78-23ea-8548-38c981236545",
            "timestamp": "2020-01-21T19:01:23.923+01:00"
            ...
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "caseInstanceId",
    "order": "asc",
    "size": 1
}
```

### Get a single historic milestone instance

    GET cmmn-history/historic-milestone-instances/{milestoneInstanceId}
    
|Parameter|Required|Value|Description|
|-|-|-|-|
|milestoneInstanceId|No|String|The id of the historic milestone instance.|       

|Response code|Description|
|-|-|
|200|Indicates the milestone instance was found and the requested variable data is returned.|
|404|The milestone instance was not found.|

*Success response body:*

```
{
    "id": "095fd94e-3c78-11ea-8548-38c986587585",
    "name": "My milestone",
    "elementId": "milestone1",
    "caseInstanceId: "123fd94e-3c78-23ea-8548-38c986587585",
    "caseDefinitionId: "567fd94e-3c78-23ea-8548-38c981236545",
    "timestamp": "2020-01-21T19:01:23.923+01:00"
    ...
}
```

### List historic plan item instances

    GET cmmn-history/historic-planitem-instance
   
URL parameters:       

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|Only return plan item instances which belong to the case instance with the given id.|
|caseDefinitionId|No|String|Only return plan item instances which belong to the case definition with the given id.|
|planItemInstanceId|No|String|Only return plan item instances with the given id.|
|planItemInstanceName|No|String|Only return plan item instances with the given name.|
|planItemInstanceState|No|String|Only return plan item instances with the given state.|   
|stageInstanceId|No|String|Only return plan item instances part of the stage plan item instance with the given id.|
|elementId|No|String|Only return plan item instances with the give element (i.e. from the model) id.|
|planItemDefinitionId|No|String|Only return plan item instances with the given plan item definition id.|
|planItemDefinitionType|No|String|Only return plan item instances with the given plan item definition type.|
|referenceId|No|String|Only return plan item instances with the given reference id.|
|referenceType|No|String|Only return plan item instances with the given reference type.|
|startUserId|No|Date|Only return plan item instances which were started by the given user.|
|createdBefore|No|Date|Only return plan item instances created before the given date.|
|createdAfter|No|Date|Only return plan item instances created after the given date.|
|tenantId|No|String|Only return plan item instances belonging to the given tenant id.|
|withoutTenantId|No|String|Only return plan item instances not belonging to any tenant.|

Due to the fact a plan item instance can move between different states, following state timestamp parameters are also available (all date parameters):

* lastAvailableBefore
* lastAvailableAfter
* lastEnabledBefore
* lastEnabledAfter
* lastDisabledBefore
* lastDisabledAfter
* lastStartedBefore
* lastStartedAfter
* lastSuspendedBefore
* lastSuspendedAfter
* completedBefore
* completedAfter
* terminatedBefore
* terminatedAfter 
* occurredBefore
* occurredAfter
* exitBefore
* exitAfter
* endedBefore (catch-all for all terminal states)
* endedAfter (catch-all for all terminal states)
  

|Response code|Description|
|-|-|
|200|Indicates request was successful and the identity links are returned.|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "09618702-3c78-11ea-8548-38c986587585",
            "name": null,
            "state": "active",
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "derivedCaseDefinitionId": null,
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "stageInstanceId": null,
            "stage": true,
            "elementId": "planItem2",
            "planItemDefinitionId": "expandedStage1",
            "planItemDefinitionType": "stage",
            "createTime": "2020-01-21T19:01:23.934+01:00",
            "lastAvailableTime": "2020-01-21T19:01:23.942+01:00",
            "lastEnabledTime": null,
            "lastDisabledTime": null,
            "lastStartedTime": "2020-01-21T19:01:23.974+01:00",
            "lastSuspendedTime": null,
            "completedTime": null,
            "occurredTime": null,
            "terminatedTime": null,
            "exitTime": null,
            "endedTime": null,
            "lastUpdatedTime": "2020-01-21T19:01:23.974+01:00",
            "startUserId": null,
            "referenceId": null,
            "referenceType": null,
            "entryCriterionId": null,
            "exitCriterionId": null,
            "formKey": null,
            "extraValue": null,
            "showInOverview": true,
            "tenantId": "",
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-planitem-instances/09618702-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
            "derivedCaseDefinitionUrl": null,
            "stageInstanceUrl": null
        },
        ...
    ],
    "total": 3,
    "start": 0,
    "sort": "createTime",
    "order": "asc",
    "size": 3
}
```

### Get a historic plan item instance

    GET cmmn-history/historic-planitem-instances/{planItemInstanceId|
    
|Parameter|Required|Value|Description|
|-|-|-|-|
|planItemInstanceId|No|String|The id of the historic plan item instance.|       

|Response code|Description|
|-|-|
|200|Indicates the milestone instance was found and the requested variable data is returned.|
|404|The plan item instance was not found.|    

*Success response body*

```
{
    "id": "09618702-3c78-11ea-8548-38c986587585",
    "name": "My Stage",
    "state": "active",
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "derivedCaseDefinitionId": null,
    "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
    "stageInstanceId": null,
    "stage": true,
    "elementId": "planItem2",
    "planItemDefinitionId": "expandedStage1",
    "planItemDefinitionType": "stage",
    "createTime": "2020-01-21T19:01:23.934+01:00",
    "lastAvailableTime": "2020-01-21T19:01:23.942+01:00",
    "lastEnabledTime": null,
    "lastDisabledTime": null,
    "lastStartedTime": "2020-01-21T19:01:23.974+01:00",
    "lastSuspendedTime": null,
    "completedTime": null,
    "occurredTime": null,
    "terminatedTime": null,
    "exitTime": null,
    "endedTime": null,
    "lastUpdatedTime": "2020-01-21T19:01:23.974+01:00",
    "startUserId": null,
    "referenceId": null,
    "referenceType": null,
    "entryCriterionId": null,
    "exitCriterionId": null,
    "formKey": null,
    "extraValue": null,
    "showInOverview": true,
    "tenantId": "",
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-planitem-instances/09618702-3c78-11ea-8548-38c986587585",
    "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585",
    "derivedCaseDefinitionUrl": null,
    "stageInstanceUrl": null
}
```

### Get historic task instances

    GET cmmn-history/historic-task-instances


|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|No|String|An id of the historic task instance.
|caseInstanceId|No|String|The case instance id of the historic task instance.
|caseDefinitionId|No|String|The case definition id of the historic task instance.
|taskDefinitionKey|No|String|The task definition key for tasks part of a case
|taskName|No|String|The task name of the historic task instance.
|taskNameLike|No|String|The task name with 'like' operator for the historic task instance.
|taskDescription|No|String|The task description of the historic task instance.
|taskDescriptionLike|No|String|The task description with 'like' operator for the historic task instance.
|taskDefinitionKey|No|String|The task identifier from the case definition for the historic task instance.
|taskCategory|No|String|Select tasks with the given category. Note that this is the task category, not the category of the case definition (namespace within the BPMN Xml).
|taskDeleteReason|No|String|The task delete reason of the historic task instance.
|taskDeleteReasonLike|No|String|The task delete reason with 'like' operator for the historic task instance.
|taskAssignee|No|String|The assignee of the historic task instance.
|taskAssigneeLike|No|String|The assignee with 'like' operator for the historic task instance.
|taskOwner|No|String|The owner of the historic task instance.
|taskOwnerLike|No|String|The owner with 'like' operator for the historic task instance.
|taskInvolvedUser|No|String|An involved user of the historic task instance.
|taskPriority|No|String|The priority of the historic task instance.
|finished|No|Boolean|Indication if the historic task instance is finished.
|caseFinished|No|Boolean|Indication if the case instance of the historic task instance is finished.
|parentTaskId|No|String|An optional parent task id of the historic task instance.
|dueDate|No|Date|Return only historic task instances that have a due date equal this date.
|dueDateAfter|No|Date|Return only historic task instances that have a due date after this date.
|dueDateBefore|No|Date|Return only historic task instances that have a due date before this date.
|withoutDueDate|No|Boolean|Return only historic task instances that have no due-date. When +false+ is provided as value, this parameter is ignored.
|taskCompletedOn|No|Date|Return only historic task instances that have been completed on this date.
|taskCompletedAfter|No|Date|Return only historic task instances that have been completed after this date.
|taskCompletedBefore|No|Date|Return only historic task instances that have been completed before this date.
|taskCreatedOn|No|Date|Return only historic task instances that were created on this date.
|taskCreatedBefore|No|Date|Return only historic task instances that were created before this date.
|taskCreatedAfter|No|Date|Return only historic task instances that were created after this date.
|includeTaskLocalVariables|No|Boolean|An indication if the historic task instance local variables should be returned as well.
|tenantId|No|String|Only return historic task instances with the given tenantId.
|tenantIdLike|No|String|Only return historic task instances with a tenantId like the given value.
|withoutTenantId|No|Boolean|If +true+, only returns historic task instances without a tenantId set. If +false+, the +withoutTenantId+ parameter is ignored.

|Response code|Description|
|-|-|
|200|Indicates that historic case instances could be queried.|
|400|Indicates a parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "09688be5-3c78-11ea-8548-38c986587585",
            "name": "Human task",
            "description": null,
            "deleteReason": null,
            "owner": null,
            "assignee": "rest-admin",
            "startTime": "2020-01-21T19:01:23.975+01:00",
            "endTime": null,
            "durationInMillis": null,
            "workTimeInMillis": null,
            "claimTime": null,
            "taskDefinitionKey": "humanTask1",
            "formKey": null,
            "priority": 50,
            "dueDate": null,
            "parentTaskId": null,
            "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-task-instances/09688be5-3c78-11ea-8548-38c986587585",
            "variables": [],
            "tenantId": "",
            "category": null,
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
            "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585"
        }
    ],
    "total": 1,
    "start": 0,
    "sort": "taskInstanceId",
    "order": "asc",
    "size": 1
}
```

### Get a single historic task instance

    GET cmmn-history/historic-task-instances/{taskId}

|Parameter|Required|Value|Description|
|-|-|-|-|
|taskId|No|String|The id of the historic task.|    

|Response code|Description|
|200|Indicates that the historic task instance could be found.
|404|Indicates that the historic task instance could not be found.

*Success response body:*

```
{
    "id": "09688be5-3c78-11ea-8548-38c986587585",
    "name": "Human task",
    "description": null,
    "deleteReason": null,
    "owner": null,
    "assignee": "rest-admin",
    "startTime": "2020-01-21T19:01:23.975+01:00",
    "endTime": null,
    "durationInMillis": null,
    "workTimeInMillis": null,
    "claimTime": null,
    "taskDefinitionKey": "humanTask1",
    "formKey": null,
    "priority": 50,
    "dueDate": null,
    "parentTaskId": null,
    "url": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-task-instances/09688be5-3c78-11ea-8548-38c986587585",
    "variables": [],
    "tenantId": "",
    "category": null,
    "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
    "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
    "caseDefinitionId": "59fd213c-3c6b-11ea-8548-38c986587585",
    "caseDefinitionUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-repository/case-definitions/59fd213c-3c6b-11ea-8548-38c986587585"
}
```

### Query for historic task instances

    POST query/historic-task-instances

All supported JSON parameter fields allowed are exactly the same as the parameters found in the previous section, but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri's that are too long. On top of that, the query allows for filtering based on case variables.

|Response code|Description|
|-|-|
|200|Indicates request was successful and the tasks are returned.|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

### Delete a historic task instance

    DELETE cmmn-history/historic-task-instances/{taskId}

|Response code|Description|
|200|Indicates that the historic task instance was deleted.|
|404|Indicates that the historic task instance could not be found.|

### Get the identity links of a historic task instance

    GET cmmn-history/historic-task-instance/{taskId}/identitylinks

|Response code|Description|
|-|-|
|200|Indicates request was successful and the identity links are returned/|
|404|Indicates the task instance could not be found.|

*Success response body:*

```
[
    {
        "type": "assignee",
        "userId": "rest-admin",
        "groupId": null,
        "taskId": "09688be5-3c78-11ea-8548-38c986587585",
        "taskUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-task-instances/09688be5-3c78-11ea-8548-38c986587585",
        "caseInstanceId": null,
        "caseInstanceUrl": null
    }
]
```

### Get the binary data for a historic task instance variable

    GET cmmn-history/historic-task-instances/{taskId}/variables/{variableName}/data

|Response code|Description|
|-|-|
|200|Indicates the task instance was found and the requested variable data is returned.|
|404|Indicates the requested task instance was not found or the case instance doesn't have a variable with the given name or the variable doesn't have a binary stream available. Status message provides additional information.|

*Success response body:*

The response body contains the binary value of the variable. When the variable is of type +binary+, the content-type of the response is set to +application/octet-stream+, regardless of the content of the variable or the request accept-type header. In case of +serializable+, +application/x-java-serialized-object+ is used as content-type.

### List of historic variable instances

    GET cmmn-history/historic-variable-instances

URL parameters:

|Parameter|Required|Value|Description|
|-|-|-|-|
|caseInstanceId|No|String|The case instance id of the historic variable instance.
|taskId|No|String|The task id of the historic variable instance.
|excludeTaskVariables|No|Boolean|Indication to exclude the task variables from the result.
|variableName|No|String|The variable name of the historic variable instance.
|variableNameLike|No|String|The variable name using the 'like' operator for the historic variable instance.

|Response code|Description|
|-|-|
|200|Indicates that historic variable instances could be queried.|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

*Success response body:*

```
{
    "data": [
        {
            "id": "09604e80-3c78-11ea-8548-38c986587585",
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "taskId": null,
            "variable": {
                "name": "initiator",
                "type": "string",
                "value": "rest-admin",
                "scope": null
            }
        },
        {
            "id": "0960c3b1-3c78-11ea-8548-38c986587585",
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "taskId": null,
            "variable": {
                "name": "myVar",
                "type": "string",
                "value": "This is a variable",
                "scope": null
            }
        },
        {
            "id": "06436c38-3c7a-11ea-8548-38c986587585",
            "caseInstanceId": "095fd94e-3c78-11ea-8548-38c986587585",
            "caseInstanceUrl": "http://localhost:8080/flowable-rest/cmmn-api/cmmn-history/historic-case-instances/095fd94e-3c78-11ea-8548-38c986587585",
            "taskId": null,
            "variable": {
                "name": "test",
                "type": "binary",
                "value": null,
                "scope": null
            }
        }
    ],
    "total": 3,
    "start": 0,
    "sort": "variableName",
    "order": "asc",
    "size": 3
}
```

### Query for historic variable instances

    POST query/historic-variable-instances

*Request body:*

```
{
  "caseDefinitionId" : "59fd213c-3c6b-11ea-8548-38c986587585",
  ...

  "variables" : [
    {
      "name" : "myVariable",
      "value" : 1234,
      "operation" : "equals",
      "type" : "long"
    }
  ]
}
```


All supported JSON parameter fields allowed are exactly the same as the parameters found in the previous section, but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri's that are too long. On top of that, the query allows for filtering based on case variables. The +variables+ property is a JSON-array containing objects with the regular Flowable variables format.

|Response code|Description|
|-|-|
|200|Indicates request was successful and the tasks are returned.|
|400|Indicates an parameter was passed in the wrong format. The status-message contains additional information.|

### Get the binary data for a historic task instance variable

    GET cmmn-history/historic-variable-instances/{varInstanceId}/data

|Parameter|Required|Value|Description|
|-|-|-|-|
|varInstanceId|No|String|The historic variable id of which the data is needed.|

|Response code|Description|
|-|-|
|200|Indicates the variable instance was found and the requested variable data is returned.|
|404|Indicates the requested variable instance was not found or the variable instance doesn't have a variable with the given name or the variable doesn't have a binary stream available. Status message provides additional information.|

*Success response body:*

The response body contains the binary value of the variable. When the variable is of type +binary+, the content-type of the response is set to +application/octet-stream+, regardless of the content of the variable or the request accept-type header. In case of +serializable+, +application/x-java-serialized-object+ is used as content-type.

## Job management

The CMMN REST API allows full access to manage async jobs, timer jobs, suspended jobs and deadletter jobs.
The API is exactly the same as the one [for the BPMN engine](../bpmn/ch15-REST#jobs), except `management` is replaced with `cmmn-management`.

