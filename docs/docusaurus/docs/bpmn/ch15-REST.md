---
id: ch15-REST
title: REST API
---

## General Flowable REST principles

### Installation and Authentication

Flowable includes a REST API to the Flowable engine that can be installed by deploying the flowable-rest.war file to a servlet container like Apache Tomcat. However, it can also be used in another web-application by including the servlet (and/or its mappings) in your application and add all flowable-rest dependencies to the classpath.

By default the Flowable engine will connect to an in-memory H2 database. You can change the database settings in the flowable-app.properties file in the *WEB-INF/META-INF/flowable-app* folder. The REST API uses JSON format (<http://www.json.org>) and is built upon the Spring MVC (<http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html>).

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

The Flowable REST web application uses Spring Java Configuration for starting the Flowable Form engine, defining the basic authentication security using Spring security, and to define the variable converters for specific variable handling.
A small number of properties can be defined by changing the flowable-app.properties file you can find in the WEB-INF/META-INF/flowable-app/flowable-app.properties folder.
If you need more advanced configuration options there’s the possibility to override the default Spring beans in XML in the flowable-custom-context.xml file you can also find in the WEB-INF/classes folder.
An example configuration is already in comments in this file. This is also the place to override the default RestResponseFactory by defining a new Spring bean with the name restResponsefactory and use your custom implementation class for it.

### Usage in Tomcat

Due to [default security properties on Tomcat](http://tomcat.apache.org/tomcat-8.0-doc/security-howto.html), **escaped forward slashes (%2F and %5C) are not allowed by default (400-result is returned).** This may have an impact on the deployment resources and their data-URL, as the URL can potentially contain escaped forward slashes.

When issues are experienced with unexpected 400-results, set the following system-property:

*-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW\_ENCODED\_SLASH=true*

It’s best practice to always set the **Accept** and **Content-Type** (in case of posting/putting JSON) headers to **application/json** on the HTTP requests described below.

### Methods and return-codes

<table>
<caption>HTTP-methods and corresponding operations</caption>
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
<caption>HTTP-methods response codes</caption>
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
<td><p>The operation is forbidden and should not be re-attempted. This implies an issue with authorization not authentication, it’s an operation that is not allowed. Example: deleting a task that is part of a running process is not allowed and will never be allowed, regardless of the user or process/task state.</p></td>
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
<caption>URL query parameter types</caption>
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
<caption>JSON parameter types</caption>
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
<caption>Variable query JSON parameters</caption>
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
<caption>Variable query JSON parameters</caption>
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
<caption>Default query JSON types</caption>
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
<caption>Variable JSON attributes</caption>
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
<caption>Variable Types</caption>
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

It’s possible to support additional variable-types with a custom JSON representation (either simple value or complex/nested JSON object). By extending the initializeVariableConverters() method on org.flowable.dmn.rest.service.api.DmnRestResponseFactory, you can add additional org.flowable.rest.variable.RestVariableConverter classes to support converting your POJOs to a format suitable for transferring through REST and converting the REST-value back to your POJO. The actual transformation to JSON is done by Jackson.

## Deployment

**When using Tomcat, please read [Usage in Tomcat](#usage-In-tomcat).**

### List of Deployments

    GET repository/deployments

<table>
<caption>URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given name.</p></td>
</tr>
<tr class="even">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with a name like the given name.</p></td>
</tr>
<tr class="odd">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given category.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments which don’t have the given category.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns deployments without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'id' (default), 'name', 'deployTime' or 'tenantId'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>REST Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the request was successful.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id": "10",
          "name": "flowable-examples.bar",
          "deploymentTime": "2010-10-13T14:54:26.750+02:00",
          "category": "examples",
          "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10",
          "tenantId": null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "id",
      "order": "asc",
      "size": 1
    }

### Get a deployment

    GET repository/deployments/{deploymentId}

<table>
<caption>Get a deployment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the deployment to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the deployment was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "10",
      "name": "flowable-examples.bar",
      "deploymentTime": "2010-10-13T14:54:26.750+02:00",
      "category": "examples",
      "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10",
      "tenantId" : null
    }

### Create a new deployment

    POST repository/deployments

**Request body:**

The request body should contain data of type *multipart/form-data*. There should be exactly one file in the request, any additional files will be ignored. The deployment name is the name of the file-field passed in. If multiple resources need to be deployed in a single deployment, compress the resources in a zip and make sure the file-name ends with .bar or .zip.

An additional parameter (form-field) can be passed in the request body with name tenantId. The value of this field will be used as the id of the tenant this deployment is done in.

<table>
<caption>Create a new deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the deployment was created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates there was no content present in the request body or the content mime-type is not supported for deployment. The status-description contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "10",
      "name": "flowable-examples.bar",
      "deploymentTime": "2010-10-13T14:54:26.750+02:00",
      "category": null,
      "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10",
      "tenantId" : "myTenant"
    }

### Delete a deployment

    DELETE repository/deployments/{deploymentId}

<table>
<caption>Delete a deployment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the deployment to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the deployment was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found.</p></td>
</tr>
</tbody>
</table>

### List resources in a deployment

    GET repository/deployments/{deploymentId}/resources

<table>
<caption>List resources in a deployment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the deployment to get the resources for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List resources in a deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the deployment was found and the resource list has been returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "id": "diagrams/my-process.bpmn20.xml",
        "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resources/diagrams%2Fmy-process.bpmn20.xml",
        "dataUrl": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resourcedata/diagrams%2Fmy-process.bpmn20.xml",
        "mediaType": "text/xml",
        "type": "processDefinition"
      },
      {
        "id": "image.png",
        "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resources/image.png",
        "dataUrl": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resourcedata/image.png",
        "mediaType": "image/png",
        "type": "resource"
      }
    ]

-   mediaType: Contains the media-type the resource has. This is resolved using a (pluggable) MediaTypeResolver and contains, by default, a limited number of mime-type mappings.

-   type: Type of resource, possible values:

-   resource: Plain old resource.

-   processDefinition: Resource that contains one or more process-definitions. This resource is picked up by the deployer.

-   processImage: Resource that represents a deployed process definition’s graphical layout.

*The dataUrl property in the resulting JSON for a single resource contains the actual URL to use for retrieving the binary resource.*

### Get a deployment resource

    GET repository/deployments/{deploymentId}/resources/{resourceId}

<table>
<caption>Get a deployment resource - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the deployment the requested resource is part of.</p></td>
</tr>
<tr class="even">
<td><p>resourceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the resource to get. <strong>Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use 'diagrams%2Fmy-process.bpmn20.xml' instead of 'diagrams/Fmy-process.bpmn20.xml'.</strong></p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a deployment resource - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both deployment and resource have been found and the resource has been returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "diagrams/my-process.bpmn20.xml",
      "url": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resources/diagrams%2Fmy-process.bpmn20.xml",
      "dataUrl": "http://localhost:8081/flowable-rest/service/repository/deployments/10/resourcedata/diagrams%2Fmy-process.bpmn20.xml",
      "mediaType": "text/xml",
      "type": "processDefinition"
    }

-   mediaType: Contains the media-type the resource has. This is resolved using a (pluggable) MediaTypeResolver and contains, by default, a limited number of mime-type mappings.

-   type: Type of resource, possible values:

-   resource: Plain old resource.

-   processDefinition: Resource that contains one or more process-definitions. This resource is picked up by the deployer.

-   processImage: Resource that represents a deployed process definition’s graphical layout.

### Get a deployment resource content

    GET repository/deployments/{deploymentId}/resourcedata/{resourceId}

<table>
<caption>Get a deployment resource content - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the deployment the requested resource is part of.</p></td>
</tr>
<tr class="even">
<td><p>resourceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the resource to get the data for. <strong>Make sure you URL-encode the resourceId in case it contains forward slashes. Eg: use 'diagrams%2Fmy-process.bpmn20.xml' instead of 'diagrams/Fmy-process.bpmn20.xml'.</strong></p></td>
</tr>
</tbody>
</table>

    .Get a deployment resource content - Response codes

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both deployment and resource have been found and the resource data has been returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found or there is no resource with the given id present in the deployment. The status-description contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources 'mimeType' property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.

## Process Definitions

### List of process definitions

    GET repository/process-definitions

<table>
<caption>List of process definitions - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>version</p></td>
<td><p>No</p></td>
<td><p>integer</p></td>
<td><p>Only return process definitions with the given version.</p></td>
</tr>
<tr class="even">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with the given name.</p></td>
</tr>
<tr class="odd">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with a name like the given name.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with the given key.</p></td>
</tr>
<tr class="odd">
<td><p>keyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with a name like the given key.</p></td>
</tr>
<tr class="even">
<td><p>resourceName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with the given resource name.</p></td>
</tr>
<tr class="odd">
<td><p>resourceNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with a name like the given resource name.</p></td>
</tr>
<tr class="even">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with the given category.</p></td>
</tr>
<tr class="odd">
<td><p>categoryLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions with a category like the given name.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions which don’t have the given category.</p></td>
</tr>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions which are part of a deployment with the given id.</p></td>
</tr>
<tr class="even">
<td><p>startableByUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process definitions which can be started by the given user.</p></td>
</tr>
<tr class="odd">
<td><p>latest</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Only return the latest process definition versions. Can only be used together with 'key' and 'keyLike' parameters, using any other parameter will result in a 400-response.</p></td>
</tr>
<tr class="even">
<td><p>suspended</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns process definitions which are suspended. If false, only active process definitions (which are not suspended) are returned.</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'name' (default), 'id', 'key', 'category', 'deploymentId' and 'version'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of process definitions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the process-definitions are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format or that 'latest' is used with other parameters other than 'key' and 'keyLike'. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "oneTaskProcess:1:4",
          "url" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "version" : 1,
          "key" : "oneTaskProcess",
          "category" : "Examples",
          "suspended" : false,
          "name" : "The One Task Process",
          "description" : "This is a process for testing purposes",
          "deploymentId" : "2",
          "deploymentUrl" : "http://localhost:8081/repository/deployments/2",
          "graphicalNotationDefined" : true,
          "resource" : "http://localhost:8182/repository/deployments/2/resources/testProcess.xml",
          "diagramResource" : "http://localhost:8182/repository/deployments/2/resources/testProcess.png",
          "startFormDefined" : false
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

-   graphicalNotationDefined: Indicates the process definition contains graphical information (BPMN DI).

-   resource: Contains the actual deployed BPMN 2.0 xml.

-   diagramResource: Contains a graphical representation of the process, null when no diagram is available.

### Get a process definition

    GET repository/process-definitions/{processDefinitionId}

<table>
<caption>Get a process definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process definition was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id" : "oneTaskProcess:1:4",
      "url" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
      "version" : 1,
      "key" : "oneTaskProcess",
      "category" : "Examples",
      "suspended" : false,
      "name" : "The One Task Process",
      "description" : "This is a process for testing purposes",
      "deploymentId" : "2",
      "deploymentUrl" : "http://localhost:8081/repository/deployments/2",
      "graphicalNotationDefined" : true,
      "resource" : "http://localhost:8182/repository/deployments/2/resources/testProcess.xml",
      "diagramResource" : "http://localhost:8182/repository/deployments/2/resources/testProcess.png",
      "startFormDefined" : false
    }

-   graphicalNotationDefined: Indicates the process definition contains graphical information (BPMN DI).

-   resource: Contains the actual deployed BPMN 2.0 xml.

-   diagramResource: Contains a graphical representation of the process, null when no diagram is available.

### Update category for a process definition

    PUT repository/process-definitions/{processDefinitionId}

**Body JSON:**

    {
      "category" : "updatedcategory"
    }

<table>
<caption>Update category for a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process was category was altered.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates no category was defined in the request body.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for repository/process-definitions/{processDefinitionId}.

### Get a process definition resource content

    GET repository/process-definitions/{processDefinitionId}/resourcedata

<table>
<caption>Get a process definition resource content - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition to get the resource data for.</p></td>
</tr>
</tbody>
</table>

**Response:**

Exactly the same response codes/boy as GET repository/deployment/{deploymentId}/resourcedata/{resourceId}.

### Get a process definition BPMN model

    GET repository/process-definitions/{processDefinitionId}/model

<table>
<caption>Get a process definition BPMN model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition to get the model for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a process definition BPMN model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process definition was found and the model is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
</tbody>
</table>

**Response body:**
The response body is a JSON representation of the org.flowable.bpmn.model.BpmnModel and contains the full process definition model.

    {
       "processes":[
          {
             "id":"oneTaskProcess",
             "xmlRowNumber":7,
             "xmlColumnNumber":60,
             "extensionElements":{

             },
             "name":"The One Task Process",
             "executable":true,
             "documentation":"One task process description",

        ]
    }

### Suspend a process definition

    PUT repository/process-definitions/{processDefinitionId}

**Body JSON:**

    {
      "action" : "suspend",
      "includeProcessInstances" : "false",
      "date" : "2013-04-15T00:42:12Z"
    }

<table>
<caption>Suspend a process definition - JSON Body parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>action</p></td>
<td><p>Action to perform. Either activate or suspend.</p></td>
<td><p>Yes</p></td>
</tr>
<tr class="even">
<td><p>includeProcessInstances</p></td>
<td><p>Whether or not to suspend/activate running process-instances for this process-definition. If omitted, the process-instances are left in the state they are.</p></td>
<td><p>No</p></td>
</tr>
<tr class="odd">
<td><p>date</p></td>
<td><p>Date (ISO-8601) when the suspension/activation should be executed. If omitted, the suspend/activation is effective immediately.</p></td>
<td><p>No</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Suspend a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process was suspended.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
<tr class="odd">
<td><p>409</p></td>
<td><p>Indicates the requested process definition is already suspended.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for repository/process-definitions/{processDefinitionId}.

### Activate a process definition

    PUT repository/process-definitions/{processDefinitionId}

**Body JSON:**

    {
      "action" : "activate",
      "includeProcessInstances" : "true",
      "date" : "2013-04-15T00:42:12Z"
    }

See suspend process definition [JSON Body parameters](processDefinitionActionBodyParameters).

<table>
<caption>Activate a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process was activated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
<tr class="odd">
<td><p>409</p></td>
<td><p>Indicates the requested process definition is already active.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for repository/process-definitions/{processDefinitionId}.

### Get all candidate starters for a process-definition

    GET repository/process-definitions/{processDefinitionId}/identitylinks

<table>
<caption>Get all candidate starters for a process-definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition to get the identity links for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get all candidate starters for a process-definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process definition was found and the requested identity links are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "url":"http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4/identitylinks/groups/admin",
          "user":null,
          "group":"admin",
          "type":"candidate"
       },
       {
          "url":"http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4/identitylinks/users/kermit",
          "user":"kermit",
          "group":null,
          "type":"candidate"
       }
    ]

### Add a candidate starter to a process definition

    POST repository/process-definitions/{processDefinitionId}/identitylinks

<table>
<caption>Add a candidate starter to a process definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition.</p></td>
</tr>
</tbody>
</table>

**Request body (user):**

    {
      "user" : "kermit"
    }

**Request body (group):**

    {
      "groupId" : "sales"
    }

<table>
<caption>Add a candidate starter to a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the process definition was found and the identity link was created.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "url":"http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4/identitylinks/users/kermit",
      "user":"kermit",
      "group":null,
      "type":"candidate"
    }

### Delete a candidate starter from a process definition

    DELETE repository/process-definitions/{processDefinitionId}/identitylinks/{family}/{identityId}

<table>
<caption>Delete a candidate starter from a process definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition.</p></td>
</tr>
<tr class="even">
<td><p>family</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either users or groups, depending on the type of identity link.</p></td>
</tr>
<tr class="odd">
<td><p>identityId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either the userId or groupId of the identity to remove as candidate starter.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a candidate starter from a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the process definition was found and the identity link was removed. The response body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found or the process definition doesn’t have an identity-link that matches the url.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "url":"http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4/identitylinks/users/kermit",
      "user":"kermit",
      "group":null,
      "type":"candidate"
    }

### Get a candidate starter from a process definition

    GET repository/process-definitions/{processDefinitionId}/identitylinks/{family}/{identityId}

<table>
<caption>Get a candidate starter from a process definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process definition.</p></td>
</tr>
<tr class="even">
<td><p>family</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either users or groups, depending on the type of identity link.</p></td>
</tr>
<tr class="odd">
<td><p>identityId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either the userId or groupId of the identity to get as candidate starter.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a candidate starter from a process definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process definition was found and the identity link was returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process definition was not found or the process definition doesn’t have an identity-link that matches the url.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "url":"http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4/identitylinks/users/kermit",
      "user":"kermit",
      "group":null,
      "type":"candidate"
    }

## Models

### Get a list of models

    GET repository/models

<table>
<caption>Get a list of models - URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with the given id.</p></td>
</tr>
<tr class="even">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with the given category.</p></td>
</tr>
<tr class="odd">
<td><p>categoryLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with a category like the given value. Use the % character as wildcard.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models without the given category.</p></td>
</tr>
<tr class="odd">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with the given name.</p></td>
</tr>
<tr class="even">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with a name like the given value. Use the % character as wildcard.</p></td>
</tr>
<tr class="odd">
<td><p>key</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with the given key.</p></td>
</tr>
<tr class="even">
<td><p>deploymentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models which are deployed in the given deployment.</p></td>
</tr>
<tr class="odd">
<td><p>version</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Only return models with the given version.</p></td>
</tr>
<tr class="even">
<td><p>latestVersion</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only return models which are the latest version. Best used in combination with key. If false is passed in as value, this is ignored and all versions are returned.</p></td>
</tr>
<tr class="odd">
<td><p>deployed</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only deployed models are returned. If false, only undeployed models are returned (deploymentId is null).</p></td>
</tr>
<tr class="even">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with the given tenantId.</p></td>
</tr>
<tr class="odd">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return models with a tenantId like the given value.</p></td>
</tr>
<tr class="even">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns models without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'id' (default), 'category', 'createTime', 'key', 'lastUpdateTime', 'name', 'version' or 'tenantId'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a list of models - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the models are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "name":"Model name",
             "key":"Model key",
             "category":"Model category",
             "version":2,
             "metaInfo":"Model metainfo",
             "deploymentId":"7",
             "id":"10",
             "url":"http://localhost:8182/repository/models/10",
             "createTime":"2013-06-12T14:31:08.612+0000",
             "lastUpdateTime":"2013-06-12T14:31:08.612+0000",
             "deploymentUrl":"http://localhost:8182/repository/deployments/7",
             "tenantId":null
          },

          ...

       ],
       "total":2,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":2
    }

### Get a model

    GET repository/models/{modelId}

<table>
<caption>Get a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"5",
       "url":"http://localhost:8182/repository/models/5",
       "name":"Model name",
       "key":"Model key",
       "category":"Model category",
       "version":2,
       "metaInfo":"Model metainfo",
       "deploymentId":"2",
       "deploymentUrl":"http://localhost:8182/repository/deployments/2",
       "createTime":"2013-06-12T12:31:19.861+0000",
       "lastUpdateTime":"2013-06-12T12:31:19.861+0000",
       "tenantId":null
    }

### Update a model

    PUT repository/models/{modelId}

**Request body:**

    {
       "name":"Model name",
       "key":"Model key",
       "category":"Model category",
       "version":2,
       "metaInfo":"Model metainfo",
       "deploymentId":"2",
       "tenantId":"updatedTenant"
    }

All request values are optional. For example, you can only include the 'name' attribute in the request body JSON-object, only updating the name of the model, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the model-value will be updated to null. Example: {"metaInfo" : null} will clear the metaInfo of the model).

<table>
<caption>Update a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"5",
       "url":"http://localhost:8182/repository/models/5",
       "name":"Model name",
       "key":"Model key",
       "category":"Model category",
       "version":2,
       "metaInfo":"Model metainfo",
       "deploymentId":"2",
       "deploymentUrl":"http://localhost:8182/repository/deployments/2",
       "createTime":"2013-06-12T12:31:19.861+0000",
       "lastUpdateTime":"2013-06-12T12:31:19.861+0000",
       "tenantId":""updatedTenant"
    }

### Create a model

    POST repository/models

**Request body:**

    {
       "name":"Model name",
       "key":"Model key",
       "category":"Model category",
       "version":1,
       "metaInfo":"Model metainfo",
       "deploymentId":"2",
       "tenantId":"tenant"
    }

All request values are optional. For example, you can only include the 'name' attribute in the request body JSON-object, only setting the name of the model, leaving all other fields null.

<table>
<caption>Create a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the model was created.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"5",
       "url":"http://localhost:8182/repository/models/5",
       "name":"Model name",
       "key":"Model key",
       "category":"Model category",
       "version":1,
       "metaInfo":"Model metainfo",
       "deploymentId":"2",
       "deploymentUrl":"http://localhost:8182/repository/deployments/2",
       "createTime":"2013-06-12T12:31:19.861+0000",
       "lastUpdateTime":"2013-06-12T12:31:19.861+0000",
       "tenantId":"tenant"
    }

### Delete a model

    DELETE repository/models/{modelId}

<table>
<caption>Delete a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the model was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

### Get the editor source for a model

    GET repository/models/{modelId}/source

<table>
<caption>Get the editor source for a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the editor source for a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and source is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

Response body contains the model’s raw editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the source.

### Set the editor source for a model

    PUT repository/models/{modelId}/source

<table>
<caption>Set the editor source for a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the source.

<table>
<caption>Set the editor source for a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and the source has been updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

Response body contains the model’s raw editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the source.

### Get the extra editor source for a model

    GET repository/models/{modelId}/source-extra

<table>
<caption>Get the extra editor source for a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the extra editor source for a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and source is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

Response body contains the model’s raw extra editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the extra source.

### Set the extra editor source for a model

    PUT repository/models/{modelId}/source-extra

<table>
<caption>Set the extra editor source for a model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>modelId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the model.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the extra source.

<table>
<caption>Set the extra editor source for a model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the model was found and the extra source has been updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested model was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

Response body contains the model’s raw editor source. The response’s content-type is set to application/octet-stream, regardless of the content of the source.

## Process Instances

### Get a process instance

    GET runtime/process-instances/{processInstanceId}

<table>
<caption>Get a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"7",
       "url":"http://localhost:8182/runtime/process-instances/7",
       "businessKey":"myBusinessKey",
       "suspended":false,
       "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/processOne%3A1%3A4",
       "activityId":"processTask",
       "tenantId": null
    }

### Delete a process instance

    DELETE runtime/process-instances/{processInstanceId}

<table>
<caption>Delete a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the process instance was found and deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

### Activate or suspend a process instance

    PUT runtime/process-instances/{processInstanceId}

<table>
<caption>Activate or suspend a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to activate/suspend.</p></td>
</tr>
</tbody>
</table>

**Request response body (suspend):**

    {
       "action":"suspend"
    }

**Request response body (activate):**

    {
       "action":"activate"
    }

<table>
<caption>Activate or suspend a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and action was executed.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an invalid action was supplied.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the requested process instance action cannot be executed since the process-instance is already activated/suspended.</p></td>
</tr>
</tbody>
</table>

### Start a process instance

    POST runtime/process-instances

**Request body (start by process definition id):**

    {
       "processDefinitionId":"oneTaskProcess:1:158",
       "businessKey":"myBusinessKey",
       "returnVariables":true,
       "variables": [
          {
            "name":"myVar",
            "value":"This is a variable"
          }
       ]
    }

**Request body (start by process definition key):**

    {
       "processDefinitionKey":"oneTaskProcess",
       "businessKey":"myBusinessKey",
       "returnVariables":false,
       "tenantId": "tenant1",
       "variables": [
          {
            "name":"myVar",
            "value":"This is a variable"
          }
       ]
    }

**Request body (start by message):**

    {
       "message":"newOrderMessage",
       "businessKey":"myBusinessKey",
       "tenantId": "tenant1",
       "variables": [
          {
            "name":"myVar",
            "value":"This is a variable"
          }
       ]
    }

Note that also a *transientVariables* property is accepted as part of this json, that follows the same structure as the *variables* property.

The *returnVariables* property can be used to get the existing variables in the process instance context back in the response. By default the variables are not returned.

Only one of processDefinitionId, processDefinitionKey or message can be used in the request body. Parameters businessKey, variables and tenantId are optional. If tenantId is omitted, the default tenant will be used. More information about the variable format can be found in [the REST variables section](restVariables). Note that the variable-scope that is supplied is ignored, process-variables are always local.

<table>
<caption>Start a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the process instance was created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates either the process-definition was not found (based on id or key), no process is started by sending the given message or an invalid variable has been passed. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"7",
       "url":"http://localhost:8182/runtime/process-instances/7",
       "businessKey":"myBusinessKey",
       "suspended":false,
       "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/processOne%3A1%3A4",
       "activityId":"processTask",
       "tenantId" : null
    }

### List of process instances

    GET runtime/process-instances

<table>
<caption>List of process instances - URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instance with the given id.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances with the given process definition key.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances with the given process definition id.</p></td>
</tr>
<tr class="even">
<td><p>businessKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances with the given businessKey.</p></td>
</tr>
<tr class="odd">
<td><p>involvedUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances in which the given user is involved.</p></td>
</tr>
<tr class="even">
<td><p>suspended</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only return process instance which are suspended. If false, only return process instances which are not suspended (active).</p></td>
</tr>
<tr class="odd">
<td><p>superProcessInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances which have the given super process-instance id (for processes that have a call-activities).</p></td>
</tr>
<tr class="even">
<td><p>subProcessInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances which have the given sub process-instance id (for processes started as a call-activity).</p></td>
</tr>
<tr class="odd">
<td><p>excludeSubprocesses</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Return only process instances which aren’t sub processes.</p></td>
</tr>
<tr class="even">
<td><p>includeProcessVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to include process variables in the result.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return process instances with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns process instances without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Sort field, should be either one of id (default), processDefinitionId, tenantId or processDefinitionKey.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of process instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the process-instances are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format . The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"7",
             "url":"http://localhost:8182/runtime/process-instances/7",
             "businessKey":"myBusinessKey",
             "suspended":false,
             "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/processOne%3A1%3A4",
             "activityId":"processTask",
             "tenantId" : null
          }


       ],
       "total":2,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":2
    }

### Query process instances

    POST query/process-instances

**Request body:**

    {
      "processDefinitionKey":"oneTaskProcess",
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

The request body can contain all possible filters that can be used in the [List process instances](bpmn/ch15-REST.md#list-of-process-instances) URL query. On top of these, it’s possible to provide an array of variables
to include in the query, with their format [described here](#json-query-variable-format).

The general [paging and sorting query-parameters](paging-and-sorting) can be used for this URL.

<table>
<caption>Query process instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the process-instances are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format . The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"7",
             "url":"http://localhost:8182/runtime/process-instances/7",
             "businessKey":"myBusinessKey",
             "suspended":false,
             "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/processOne%3A1%3A4",
             "activityId":"processTask",
             "tenantId" : null
          }


       ],
       "total":2,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":2
    }

### Get diagram for a process instance

    GET runtime/process-instances/{processInstanceId}/diagram

<table>
<caption>Get diagram for a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to get the diagram for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get diagram for a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and the diagram was returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the requested process instance was not found but the process doesn’t contain any graphical information (BPMN:DI) and no diagram can be created.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    The response is a Blob object containing the binary data or null.
    

### Get involved people for process instance

    GET runtime/process-instances/{processInstanceId}/identitylinks

<table>
<caption>Get involved people for process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the links for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get involved people for process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and links are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "url":"http://localhost:8182/runtime/process-instances/5/identitylinks/users/john/customType",
          "user":"john",
          "group":null,
          "type":"customType"
       },
       {
          "url":"http://localhost:8182/runtime/process-instances/5/identitylinks/users/paul/candidate",
          "user":"paul",
          "group":null,
          "type":"candidate"
       }
    ]

Note that the groupId will always be null, as it’s only possible to involve users with a process-instance.

### Add an involved user to a process instance

    POST runtime/process-instances/{processInstanceId}/identitylinks

<table>
<caption>Add an involved user to a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the links for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    {
      "userId":"kermit",
      "type":"participant"
    }

Both userId and type are required.

<table>
<caption>Add an involved user to a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the process instance was found and the link is created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the requested body did not contain a userId or a type.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "url":"http://localhost:8182/runtime/process-instances/5/identitylinks/users/john/customType",
       "user":"john",
       "group":null,
       "type":"customType"
    }

Note that the groupId will always be null, as it’s only possible to involve users with a process-instance.

### Remove an involved user to from process instance

    DELETE runtime/process-instances/{processInstanceId}/identitylinks/users/{userId}/{type}

<table>
<caption>Remove an involved user to from process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance.</p></td>
</tr>
<tr class="even">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to delete link for.</p></td>
</tr>
<tr class="odd">
<td><p>type</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Type of link to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Remove an involved user to from process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the process instance was found and the link has been deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the link to delete doesn’t exist. The response status contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "url":"http://localhost:8182/runtime/process-instances/5/identitylinks/users/john/customType",
       "user":"john",
       "group":null,
       "type":"customType"
    }

Note that the groupId will always be null, as it’s only possible to involve users with a process-instance.

### List of variables for a process instance

    GET runtime/process-instances/{processInstanceId}/variables

<table>
<caption>List of variables for a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the variables for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of variables for a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and variables are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"local"
       },
       {
          "name":"byteArrayProcVar",
          "type":"binary",
          "value":null,
          "valueUrl":"http://localhost:8182/runtime/process-instances/5/variables/byteArrayProcVar/data",
          "scope":"local"
       }
    ]

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response.
Note that only local scoped variables are returned, as there is no global scope for process-instance variables.

### Get a variable for a process instance

    GET runtime/process-instances/{processInstanceId}/variables/{variableName}

<table>
<caption>Get a variable for a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the variables for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Name of the variable to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a variable for a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both the process instance and variable were found and variable is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"local"
       }

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response. Note that only local scoped variables are returned, as there is no global scope for process-instance variables.

### Create (or update) variables on a process instance

    POST runtime/process-instances/{processInstanceId}/variables

    PUT runtime/process-instances/{processInstanceId}/variables

When using POST, all variables that are passed are created. In case one of the variables already exists on the process instance, the request results in an error (409 - CONFLICT). When PUT is used, nonexistent variables are created on the process-instance and existing ones are overridden without any error.

<table>
<caption>Create (or update) variables on a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the variables for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    [
       {
          "name":"intProcVar",
          "type":"integer",
          "value":123
       },

       ...
    ]

Any number of variables can be passed into the request body array. More information about the variable format can be found in [the REST variables section](restVariables). Note that scope is ignored, only local variables can be set in a process instance.

<table>
<caption>Create (or update) variables on a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the process instance was found and variable is created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the process instance was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"local"
       },

       ...

    ]

### Update a single variable on a process instance

    PUT runtime/process-instances/{processInstanceId}/variables/{variableName}

<table>
<caption>Update a single variable on a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to the variables for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Name of the variable to get.</p></td>
</tr>
</tbody>
</table>

**Request body:**

     {
        "name":"intProcVar"
        "type":"integer"
        "value":123
     }

More information about the variable format can be found in [the REST variables section](restVariables). Note that scope is ignored, only local variables can be set in a process instance.

<table>
<caption>Update a single variable on a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both the process instance and variable were found and variable is updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "name":"intProcVar",
      "type":"integer",
      "value":123,
      "scope":"local"
    }

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response. Note that only local scoped variables are returned, as there is no global scope for process-instance variables.

### Create a new binary variable on a process-instance

    POST runtime/process-instances/{processInstanceId}/variables

<table>
<caption>Create a new binary variable on a process-instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to create the new variable for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   type: Type of variable that is created. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/process-instances/123/variables/binaryVariable/data"
    }

<table>
<caption>Create a new binary variable on a process-instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the variable was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to create was missing. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the process instance already has a variable with the given name. Use the PUT method to update the task variable instead.</p></td>
</tr>
<tr class="odd">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

### Update an existing binary variable on a process-instance

    PUT runtime/process-instances/{processInstanceId}/variables

<table>
<caption>Update an existing binary variable on a process-instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to create the new variable for.</p></td>
</tr>
</tbody>
</table>

**Request body:**
The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   type: Type of variable that is created. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/process-instances/123/variables/binaryVariable/data"
    }

<table>
<caption>Update an existing binary variable on a process-instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the variable was updated and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to update was missing. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the process instance does not have a variable with the given name.</p></td>
</tr>
<tr class="even">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

## Executions

### Get an execution

    GET runtime/executions/{executionId}

<table>
<caption>Get an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the execution was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the execution was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"5",
       "url":"http://localhost:8182/runtime/executions/5",
       "parentId":null,
       "parentUrl":null,
       "processInstanceId":"5",
       "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
       "suspended":false,
       "activityId":null,
       "tenantId": null
    }

### Execute an action on an execution

    PUT runtime/executions/{executionId}

<table>
<caption>Execute an action on an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to execute action on.</p></td>
</tr>
</tbody>
</table>

**Request body (signal an execution):**

    {
      "action":"signal"
    }

Both a *variables* and *transientVariables* property is accepted with following structure:

    {
      "action":"signal",
      "variables" : [
        {
          "name": "myVar",
          "value": "someValue"
        }
      ]
    }

**Request body (signal event received for execution):**

    {
      "action":"signalEventReceived",
      "signalName":"mySignal"
      "variables": [  ]
    }

Notifies the execution that a signal event has been received, requires a signalName parameter. Optional variables can be passed that are set on the execution before the action is executed.

**Request body (signal event received for execution):**

    {
      "action":"messageEventReceived",
      "messageName":"myMessage"
      "variables": [  ]
    }

Notifies the execution that a message event has been received, requires a messageName parameter. Optional variables can be passed that are set on the execution before the action is executed.

<table>
<caption>Execute an action on an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the execution was found and the action is performed.</p></td>
</tr>
<tr class="even">
<td><p>204</p></td>
<td><p>Indicates the execution was found, the action was performed and the action caused the execution to end.</p></td>
</tr>
<tr class="odd">
<td><p>400</p></td>
<td><p>Indicates an illegal action was requested, required parameters are missing in the request body or illegal variables are passed in. Status description contains additional information about the error.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the execution was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body (in case execution is not ended due to action):**

    {
       "id":"5",
       "url":"http://localhost:8182/runtime/executions/5",
       "parentId":null,
       "parentUrl":null,
       "processInstanceId":"5",
       "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
       "suspended":false,
       "activityId":null,
       "tenantId" : null
    }

### Get active activities in an execution

    GET runtime/executions/{executionId}/activities

Returns all activities which are active in the execution and in all child-executions (and their children, recursively), if any.

<table>
<caption>Get active activities in an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to get activities for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get active activities in an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the execution was found and activities are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the execution was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      "userTaskForManager",
      "receiveTask"
    ]

### List of executions

    GET runtime/executions

<table>
<caption>List of executions - URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with the given id.</p></td>
</tr>
<tr class="even">
<td><p>activityId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with the given activity id.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with the given process definition key.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with the given process definition id.</p></td>
</tr>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions which are part of the process instance with the given id.</p></td>
</tr>
<tr class="even">
<td><p>messageEventSubscriptionName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions which are subscribed to a message with the given name.</p></td>
</tr>
<tr class="odd">
<td><p>signalEventSubscriptionName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions which are subscribed to a signal with the given name.</p></td>
</tr>
<tr class="even">
<td><p>parentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions which are a direct child of the given execution.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return executions with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns executions without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Sort field, should be either one of processInstanceId (default), processDefinitionId, processDefinitionKey or tenantId.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of executions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the executions are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format . The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"5",
             "url":"http://localhost:8182/runtime/executions/5",
             "parentId":null,
             "parentUrl":null,
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "suspended":false,
             "activityId":null,
             "tenantId":null
          },
          {
             "id":"7",
             "url":"http://localhost:8182/runtime/executions/7",
             "parentId":"5",
             "parentUrl":"http://localhost:8182/runtime/executions/5",
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "suspended":false,
             "activityId":"processTask",
             "tenantId":null
          }
       ],
       "total":2,
       "start":0,
       "sort":"processInstanceId",
       "order":"asc",
       "size":2
    }

### Query executions

    POST query/executions

**Request body:**

    {
      "processDefinitionKey":"oneTaskProcess",
      "variables":
      [
        {
            "name" : "myVariable",
            "value" : 1234,
            "operation" : "equals",
            "type" : "long"
        }
      ],
      "processInstanceVariables":
      [
        {
            "name" : "processVariable",
            "value" : "some string",
            "operation" : "equals",
            "type" : "string"
        }
      ]
    }

The request body can contain all possible filters that can be used in the [List executions](bpmn/ch15-REST.md#list-of-executions) URL query. On top of these, it’s possible to provide an array of variables and processInstanceVariables
to include in the query, with their format [described here](#json-query-variable-format).

The general [paging and sorting query-parameters](paging-and-sorting) can be used for this URL.

<table>
<caption>Query executions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the executions are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format . The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"5",
             "url":"http://localhost:8182/runtime/executions/5",
             "parentId":null,
             "parentUrl":null,
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "suspended":false,
             "activityId":null,
             "tenantId":null
          },
          {
             "id":"7",
             "url":"http://localhost:8182/runtime/executions/7",
             "parentId":"5",
             "parentUrl":"http://localhost:8182/runtime/executions/5",
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "suspended":false,
             "activityId":"processTask",
             "tenantId":null
          }
       ],
       "total":2,
       "start":0,
       "sort":"processInstanceId",
       "order":"asc",
       "size":2
    }

### List of variables for an execution

    GET runtime/executions/{executionId}/variables?scope={scope}

<table>
<caption>List of variables for an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to the variables for.</p></td>
</tr>
<tr class="even">
<td><p>scope</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Either local or global. If omitted, both local and global scoped variables are returned.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of variables for an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the execution was found and variables are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested execution was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"global"
       },
       {
          "name":"byteArrayProcVar",
          "type":"binary",
          "value":null,
          "valueUrl":"http://localhost:8182/runtime/process-instances/5/variables/byteArrayProcVar/data",
          "scope":"local"
       }


    ]

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response.

### Get a variable for an execution

    GET runtime/executions/{executionId}/variables/{variableName}?scope={scope}

<table>
<caption>Get a variable for an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to the variables for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Name of the variable to get.</p></td>
</tr>
<tr class="odd">
<td><p>scope</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Either local or global. If omitted, local variable is returned (if exists). If not, a global variable is returned (if exists).</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a variable for an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both the execution and variable were found and variable is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested execution was not found or the execution does not have a variable with the given name in the requested scope (in case scope-query parameter was omitted, variable doesn’t exist in local and global scope). Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"local"
       }

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response.

### Create (or update) variables on an execution

    POST runtime/executions/{executionId}/variables

    PUT runtime/executions/{executionId}/variables

When using POST, all variables that are passed are created. In case one of the variables already exists on the execution in the requested scope, the request results in an error (409 - CONFLICT). When PUT is used, nonexistent variables are created on the execution and existing ones are overridden without any error.

<table>
<caption>Create (or update) variables on an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to the variables for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    [
       {
          "name":"intProcVar"
          "type":"integer"
          "value":123,
          "scope":"local"
       }



    ]

\*Note that you can only provide variables that have the same scope. If the request-body array contains variables from mixed scopes, the request results in an error (400 - BAD REQUEST).\*Any number of variables can be passed into the request body array. More information about the variable format can be found in [the REST variables section](restVariables). Note that scope is ignored, only local variables can be set in a process instance.

<table>
<caption>Create (or update) variables on an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the execution was found and variable is created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the request body is incomplete or contains illegal values. The status description contains additional information about the error.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested execution was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the execution was found but already contains a variable with the given name (only thrown when POST method is used). Use the update-method instead.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"local"
       }



    ]

### Update a variable on an execution

    PUT runtime/executions/{executionId}/variables/{variableName}

<table>
<caption>Update a variable on an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to update the variables for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Name of the variable to update.</p></td>
</tr>
</tbody>
</table>

**Request body:**

     {
        "name":"intProcVar"
        "type":"integer"
        "value":123,
        "scope":"global"
     }

More information about the variable format can be found in [the REST variables section](restVariables).

<table>
<caption>Update a variable on an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both the process instance and variable were found and variable is updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the process instance does not have a variable with the given name. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

       {
          "name":"intProcVar",
          "type":"integer",
          "value":123,
          "scope":"global"
       }

In case the variable is a binary variable or serializable, the valueUrl points to an URL to fetch the raw value. If it’s a plain variable, the value is present in the response.

### Create a new binary variable on an execution

    POST runtime/executions/{executionId}/variables

<table>
<caption>Create a new binary variable on an execution - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to create the new variable for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   type: Type of variable that is created. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

-   scope: Scope of variable that is created. If omitted, local is assumed.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/executions/123/variables/binaryVariable/data"
    }

<table>
<caption>Create a new binary variable on an execution - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the variable was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to create was missing. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested execution was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the execution already has a variable with the given name. Use the PUT method to update the task variable instead.</p></td>
</tr>
<tr class="odd">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

### Update an existing binary variable on a process-instance

    PUT runtime/executions/{executionId}/variables/{variableName}

<table>
<caption>Update an existing binary variable on a process-instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the execution to create the new variable for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to update.</p></td>
</tr>
</tbody>
</table>

**Request body:**
The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   type: Type of variable that is created. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

-   scope: Scope of variable that is created. If omitted, local is assumed.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/executions/123/variables/binaryVariable/data"
    }

<table>
<caption>Update an existing binary variable on a process-instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the variable was updated and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to update was missing. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested execution was not found or the execution does not have a variable with the given name.</p></td>
</tr>
<tr class="even">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

## Tasks

### Get a task

    GET runtime/tasks/{taskId}

<table>
<caption>Get a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "assignee" : "kermit",
      "createTime" : "2013-04-17T10:17:43.902+0000",
      "delegationState" : "pending",
      "description" : "Task description",
      "dueDate" : "2013-04-17T10:17:43.902+0000",
      "execution" : "http://localhost:8182/runtime/executions/5",
      "id" : "8",
      "name" : "My task",
      "owner" : "owner",
      "parentTask" : "http://localhost:8182/runtime/tasks/9",
      "priority" : 50,
      "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
      "processInstanceUrl" : "http://localhost:8182/runtime/process-instances/5",
      "suspended" : false,
      "taskDefinitionKey" : "theTask",
      "url" : "http://localhost:8182/runtime/tasks/8",
      "tenantId" : null
    }

-   delegationState: Delegation-state of the task, can be null, "pending" or "resolved".

### List of tasks

    GET runtime/tasks

<table>
<caption>List of tasks - URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with the given name.</p></td>
</tr>
<tr class="even">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with a name like the given name.</p></td>
</tr>
<tr class="odd">
<td><p>description</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with the given description.</p></td>
</tr>
<tr class="even">
<td><p>priority</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Only return tasks with the given priority.</p></td>
</tr>
<tr class="odd">
<td><p>minimumPriority</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Only return tasks with a priority greater than the given value.</p></td>
</tr>
<tr class="even">
<td><p>maximumPriority</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Only return tasks with a priority lower than the given value.</p></td>
</tr>
<tr class="odd">
<td><p>assignee</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks assigned to the given user.</p></td>
</tr>
<tr class="even">
<td><p>assigneeLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks assigned with an assignee like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>owner</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks owned by the given user.</p></td>
</tr>
<tr class="even">
<td><p>ownerLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks assigned with an owner like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>unassigned</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Only return tasks that are not assigned to anyone. If false is passed, the value is ignored.</p></td>
</tr>
<tr class="even">
<td><p>delegationState</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks that have the given delegation state. Possible values are pending and resolved.</p></td>
</tr>
<tr class="odd">
<td><p>candidateUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks that can be claimed by the given user. This includes both tasks where the user is an explicit candidate for and task that are claimable by a group that the user is a member of.</p></td>
</tr>
<tr class="even">
<td><p>candidateGroup</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks that can be claimed by a user in the given group.</p></td>
</tr>
<tr class="odd">
<td><p>candidateGroups</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks that can be claimed by a user in the given groups. Values split by comma.</p></td>
</tr>
<tr class="even">
<td><p>involvedUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks in which the given user is involved.</p></td>
</tr>
<tr class="odd">
<td><p>taskDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with the given task definition id.</p></td>
</tr>
<tr class="even">
<td><p>taskDefinitionKeyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with a given task definition id like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of the process instance with the given id.</p></td>
</tr>
<tr class="even">
<td><p>processInstanceBusinessKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of the process instance with the given business key.</p></td>
</tr>
<tr class="odd">
<td><p>processInstanceBusinessKeyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of the process instance which has a business key like the given value.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of a process instance which has a process definition with the given id.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of a process instance which has a process definition with the given key.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionKeyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of a process instance which has a process definition with a key like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of a process instance which has a process definition with the given name.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of a process instance which has a process definition with a name like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks which are part of the execution with the given id.</p></td>
</tr>
<tr class="even">
<td><p>createdOn</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are created on the given date.</p></td>
</tr>
<tr class="odd">
<td><p>createdBefore</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are created before the given date.</p></td>
</tr>
<tr class="even">
<td><p>createdAfter</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are created after the given date.</p></td>
</tr>
<tr class="odd">
<td><p>dueOn</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are due on the given date.</p></td>
</tr>
<tr class="even">
<td><p>dueBefore</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are due before the given date.</p></td>
</tr>
<tr class="odd">
<td><p>dueAfter</p></td>
<td><p>No</p></td>
<td><p>ISO Date</p></td>
<td><p>Only return tasks which are due after the given date.</p></td>
</tr>
<tr class="even">
<td><p>withoutDueDate</p></td>
<td><p>No</p></td>
<td><p>boolean</p></td>
<td><p>Only return tasks which don’t have a due date. The property is ignored if the value is false.</p></td>
</tr>
<tr class="odd">
<td><p>excludeSubTasks</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Only return tasks that are not a subtask of another task.</p></td>
</tr>
<tr class="even">
<td><p>active</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only return tasks that are not suspended (either part of a process that is not suspended or not part of a process at all). If false, only tasks that are part of suspended process instances are returned.</p></td>
</tr>
<tr class="odd">
<td><p>includeTaskLocalVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to include task local variables in the result.</p></td>
</tr>
<tr class="even">
<td><p>includeProcessVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to include process variables in the result.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return tasks with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns tasks without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>candidateOrAssigned</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Select tasks that has been claimed or assigned to user or waiting to claim by user (candidate user or groups).</p></td>
</tr>
<tr class="odd">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>string</p></td>
<td><p>Select tasks with the given category. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of tasks - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the tasks are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format or that 'delegationState' has an invalid value (other than 'pending' and 'resolved'). The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "assignee" : "kermit",
          "createTime" : "2013-04-17T10:17:43.902+0000",
          "delegationState" : "pending",
          "description" : "Task description",
          "dueDate" : "2013-04-17T10:17:43.902+0000",
          "execution" : "http://localhost:8182/runtime/executions/5",
          "id" : "8",
          "name" : "My task",
          "owner" : "owner",
          "parentTask" : "http://localhost:8182/runtime/tasks/9",
          "priority" : 50,
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceUrl" : "http://localhost:8182/runtime/process-instances/5",
          "suspended" : false,
          "taskDefinitionKey" : "theTask",
          "url" : "http://localhost:8182/runtime/tasks/8",
          "tenantId" : null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for tasks

    POST query/tasks

**Request body:**

    {
      "name" : "My task",
      "description" : "The task description",

      ...

      "taskVariables" : [
        {
          "name" : "myVariable",
          "value" : 1234,
          "operation" : "equals",
          "type" : "long"
        }
      ],

        "processInstanceVariables" : [
          {
             ...
          }
        ]
      ]
    }

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of tasks](bpmn/ch15-REST.md#list-of-tasks) (except for candidateGroupIn which is only available in this POST task query REST service), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long. On top of that, the query allows for filtering based on task and process variables. The taskVariables and processInstanceVariables are both JSON-arrays containing objects with the format [as described here.](#json-query-variable-format)

<table>
<caption>Query for tasks - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the tasks are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format or that 'delegationState' has an invalid value (other than 'pending' and 'resolved'). The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "assignee" : "kermit",
          "createTime" : "2013-04-17T10:17:43.902+0000",
          "delegationState" : "pending",
          "description" : "Task description",
          "dueDate" : "2013-04-17T10:17:43.902+0000",
          "execution" : "http://localhost:8182/runtime/executions/5",
          "id" : "8",
          "name" : "My task",
          "owner" : "owner",
          "parentTask" : "http://localhost:8182/runtime/tasks/9",
          "priority" : 50,
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceUrl" : "http://localhost:8182/runtime/process-instances/5",
          "suspended" : false,
          "taskDefinitionKey" : "theTask",
          "url" : "http://localhost:8182/runtime/tasks/8",
          "tenantId" : null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Update a task

    PUT runtime/tasks/{taskId}

**Body JSON:**

    {
      "assignee" : "assignee",
      "delegationState" : "resolved",
      "description" : "New task description",
      "dueDate" : "2013-04-17T13:06:02.438+02:00",
      "name" : "New task name",
      "owner" : "owner",
      "parentTaskId" : "3",
      "priority" : 20
    }

All request values are optional. For example, you can only include the 'assignee' attribute in the request body JSON-object, only updating the assignee of the task, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the task-value will be updated to null. Example: {"dueDate" : null} will clear the duedate of the task).

<table>
<caption>Update a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
<tr class="odd">
<td><p>409</p></td>
<td><p>Indicates the requested task was updated simultaneously.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for runtime/tasks/{taskId}.

### Task actions

    POST runtime/tasks/{taskId}

**Complete a task - Body JSON:**

    {
      "action" : "complete",
      "variables" : []
    }

Completes the task. Optional variable array can be passed in using the variables property. More information about the variable format can be found in [the REST variables section](restVariables). Note that the variable-scope that is supplied is ignored and the variables are set on the parent-scope unless a variable exists in a local scope, which is overridden in this case. This is the same behavior as the TaskService.completeTask(taskId, variables) invocation.

Note that also a *transientVariables* property is accepted as part of this json, that follows the same structure as the *variables* property.

**Claim a task - Body JSON:**

    {
      "action" : "claim",
      "assignee" : "userWhoClaims"
    }

Claims the task by the given assignee. If the assignee is null, the task is assigned to no-one, claimable again.

**Delegate a task - Body JSON:**

    {
      "action" : "delegate",
      "assignee" : "userToDelegateTo"
    }

Delegates the task to the given assignee. The assignee is required.

**Resolve a task - Body JSON:**

    {
      "action" : "resolve"
    }

Resolves the task delegation. The task is assigned back to the task owner (if any).

<table>
<caption>Task actions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the action was executed.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>When the body contains an invalid value or when the assignee is missing when the action requires it.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the action cannot be performed due to a conflict. Either the task was updates simultaneously or the task was claimed by another user, in case of the 'claim' action.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for runtime/tasks/{taskId}.

### Delete a task

    DELETE runtime/tasks/{taskId}?cascadeHistory={cascadeHistory}&deleteReason={deleteReason}

<table>
<caption>&gt;Delete a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to delete.</p></td>
</tr>
<tr class="even">
<td><p>cascadeHistory</p></td>
<td><p>False</p></td>
<td><p>Boolean</p></td>
<td><p>Whether or not to delete the HistoricTask instance when deleting the task (if applicable). If not provided, this value defaults to false.</p></td>
</tr>
<tr class="odd">
<td><p>deleteReason</p></td>
<td><p>False</p></td>
<td><p>String</p></td>
<td><p>Reason why the task is deleted. This value is ignored when cascadeHistory is true.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>&gt;Delete a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the task was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>403</p></td>
<td><p>Indicates the requested task cannot be deleted because it’s part of a workflow.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get all variables for a task

    GET runtime/tasks/{taskId}/variables?scope={scope}

<table>
<caption>Get all variables for a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get variables for.</p></td>
</tr>
<tr class="even">
<td><p>scope</p></td>
<td><p>False</p></td>
<td><p>String</p></td>
<td><p>Scope of variables to be returned. When 'local', only task-local variables are returned. When 'global', only variables from the task’s parent execution-hierarchy are returned. When the parameter is omitted, both local and global variables are returned.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get all variables for a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the requested variables are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "name" : "doubleTaskVar",
        "scope" : "local",
        "type" : "double",
        "value" : 99.99
      },
      {
        "name" : "stringProcVar",
        "scope" : "global",
        "type" : "string",
        "value" : "This is a ProcVariable"
      }



    ]

The variables are returned as a JSON array. Full response description can be found in the general [REST-variables section](restVariables).

### Get a variable from a task

    GET runtime/tasks/{taskId}/variables/{variableName}?scope={scope}

<table>
<caption>Get a variable from a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get a variable for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to get.</p></td>
</tr>
<tr class="odd">
<td><p>scope</p></td>
<td><p>False</p></td>
<td><p>String</p></td>
<td><p>Scope of variable to be returned. When 'local', only task-local variable value is returned. When 'global', only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a variable from a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the requested variables are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have a variable with the given name (in the given scope). Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "name" : "myTaskVariable",
      "scope" : "local",
      "type" : "string",
      "value" : "Hello my friend"
    }

Full response body description can be found in the general [REST-variables section](restVariables).

### Get the binary data for a variable

    GET runtime/tasks/{taskId}/variables/{variableName}/data?scope={scope}

<table>
<caption>Get the binary data for a variable - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get a variable data for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to get data for. Only variables of type binary and serializable can be used. If any other type of variable is used, a 404 is returned.</p></td>
</tr>
<tr class="odd">
<td><p>scope</p></td>
<td><p>False</p></td>
<td><p>String</p></td>
<td><p>Scope of variable to be returned. When 'local', only task-local variable value is returned. When 'global', only variable value from the task’s parent execution-hierarchy are returned. When the parameter is omitted, a local variable will be returned if it exists, otherwise a global variable.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the binary data for a variable - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the requested variables are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have a variable with the given name (in the given scope) or the variable doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.

### Create new variables on a task

    POST runtime/tasks/{taskId}/variables

<table>
<caption>Create new variables on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to create the new variable for.</p></td>
</tr>
</tbody>
</table>

**Request body for creating simple (non-binary) variables:**

    [
      {
        "name" : "myTaskVariable",
        "scope" : "local",
        "type" : "string",
        "value" : "Hello my friend"
      },
      {

      }
    ]

The request body should be an array containing one or more JSON-objects representing the variables that should be created.

-   name: Required name of the variable

-   scope: Scope of variable that is created. If omitted, local is assumed.

-   type: Type of variable that is created. If omitted, reverts to raw JSON-value type (string, boolean, integer or double).

-   value: Variable value.

More information about the variable format can be found in [the REST variables section](restVariables).

**Success response body:**

    [
      {
        "name" : "myTaskVariable",
        "scope" : "local",
        "type" : "string",
        "value" : "Hello my friend"
      },
      {

      }
    ]

<table>
<caption>Create new variables on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the variables were created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of a variable to create was missing or that an attempt is done to create a variable on a standalone task (without a process associated) with scope global or an empty array of variables was included in the request or request did not contain an array of variables. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the task already has a variable with the given name. Use the PUT method to update the task variable instead.</p></td>
</tr>
</tbody>
</table>

### Create a new binary variable on a task

    POST runtime/tasks/{taskId}/variables

<table>
<caption>Create a new binary variable on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to create the new variable for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   scope: Scope of variable that is created. If omitted, local is assumed.

-   type: Type of variable that is created. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/tasks/123/variables/binaryVariable/data"
    }

<table>
<caption>Create a new binary variable on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the variable was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to create was missing or that an attempt is done to create a variable on a standalone task (without a process associated) with scope global. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the task already has a variable with the given name. Use the PUT method to update the task variable instead.</p></td>
</tr>
<tr class="odd">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

### Update an existing variable on a task

    PUT runtime/tasks/{taskId}/variables/{variableName}

<table>
<caption>Update an existing variable on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to update the variable for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to update.</p></td>
</tr>
</tbody>
</table>

**Request body for updating simple (non-binary) variables:**

    {
      "name" : "myTaskVariable",
      "scope" : "local",
      "type" : "string",
      "value" : "Hello my friend"
    }

-   name: Required name of the variable

-   scope: Scope of variable that is updated. If omitted, local is assumed.

-   type: Type of variable that is updated. If omitted, reverts to raw JSON-value type (string, boolean, integer or double).

-   value: Variable value.

More information about the variable format can be found in [the REST variables section](restVariables).

**Success response body:**

    {
      "name" : "myTaskVariable",
      "scope" : "local",
      "type" : "string",
      "value" : "Hello my friend"
    }

<table>
<caption>Update an existing variable on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the variables was updated and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of a variable to update was missing or that an attempt is done to update a variable on a standalone task (without a process associated) with scope global. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have a variable with the given name in the given scope. Status message contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Updating a binary variable on a task

    PUT runtime/tasks/{taskId}/variables/{variableName}

<table>
<caption>Updating a binary variable on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to update the variable for.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to update.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   scope: Scope of variable that is updated. If omitted, local is assumed.

-   type: Type of variable that is updated. If omitted, binary is assumed and the binary data in the request will be stored as an array of bytes.

**Success response body:**

    {
      "name" : "binaryVariable",
      "scope" : "local",
      "type" : "binary",
      "value" : null,
      "valueUrl" : "http://.../runtime/tasks/123/variables/binaryVariable/data"
    }

<table>
<caption>Updating a binary variable on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the variable was updated and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the name of the variable to update was missing or that an attempt is done to update a variable on a standalone task (without a process associated) with scope global. Status message provides additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the variable to update doesn’t exist for the given task in the given scope.</p></td>
</tr>
<tr class="even">
<td><p>415</p></td>
<td><p>Indicates the serializable data contains an object for which no class is present in the JVM running the Flowable engine and therefore cannot be deserialized.</p></td>
</tr>
</tbody>
</table>

### Delete a variable on a task

    DELETE runtime/tasks/{taskId}/variables/{variableName}?scope={scope}

<table>
<caption>Delete a variable on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task the variable to delete belongs to.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the variable to delete.</p></td>
</tr>
<tr class="odd">
<td><p>scope</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Scope of variable to delete in. Can be either local or global. If omitted, local is assumed.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a variable on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the task variable was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have a variable with the given name. Status message contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Delete all local variables on a task

    DELETE runtime/tasks/{taskId}/variables

<table>
<caption>Delete all local variables on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task the variable to delete belongs to.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete all local variables on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates all local task variables have been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get all identity links for a task

    GET runtime/tasks/{taskId}/identitylinks

<table>
<caption>Get all identity links for a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the identity links for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get all identity links for a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the requested identity links are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "userId" : "kermit",
        "groupId" : null,
        "type" : "candidate",
        "url" : "http://localhost:8081/flowable-rest/service/runtime/tasks/100/identitylinks/users/kermit/candidate"
      },
      {
        "userId" : null,
        "groupId" : "sales",
        "type" : "candidate",
        "url" : "http://localhost:8081/flowable-rest/service/runtime/tasks/100/identitylinks/groups/sales/candidate"
      },

      ...
    ]

### Get all identitylinks for a task for either groups or users

    GET runtime/tasks/{taskId}/identitylinks/users
    GET runtime/tasks/{taskId}/identitylinks/groups

Returns only identity links targeting either users or groups. Response body and status-codes are exactly the same as when getting the full list of identity links for a task.

### Get a single identity link on a task

    GET runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}

<table>
<caption>Get all identitylinks for a task for either groups or users - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task .</p></td>
</tr>
<tr class="even">
<td><p>family</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either groups or users, depending on what kind of identity is targeted.</p></td>
</tr>
<tr class="odd">
<td><p>identityId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the identity.</p></td>
</tr>
<tr class="even">
<td><p>type</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The type of identity link.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get all identitylinks for a task for either groups or users - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task and identity link was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "userId" : null,
      "groupId" : "sales",
      "type" : "candidate",
      "url" : "http://localhost:8081/flowable-rest/service/runtime/tasks/100/identitylinks/groups/sales/candidate"
    }

### Create an identity link on a task

    POST runtime/tasks/{taskId}/identitylinks

<table>
<caption>Create an identity link on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task .</p></td>
</tr>
</tbody>
</table>

**Request body (user):**

    {
      "userId" : "kermit",
      "type" : "candidate",
    }

**Request body (group):**

    {
      "groupId" : "sales",
      "type" : "candidate",
    }

<table>
<caption>Create an identity link on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the task was found and the identity link was created.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "userId" : null,
      "groupId" : "sales",
      "type" : "candidate",
      "url" : "http://localhost:8081/flowable-rest/service/runtime/tasks/100/identitylinks/groups/sales/candidate"
    }

### Delete an identity link on a task

    DELETE runtime/tasks/{taskId}/identitylinks/{family}/{identityId}/{type}

<table>
<caption>Delete an identity link on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task.</p></td>
</tr>
<tr class="even">
<td><p>family</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>Either groups or users, depending on what kind of identity is targeted.</p></td>
</tr>
<tr class="odd">
<td><p>identityId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the identity.</p></td>
</tr>
<tr class="even">
<td><p>type</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The type of identity link.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete an identity link on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the task and identity link were found and the link has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have the requested identityLink. The status contains additional information about this error.</p></td>
</tr>
</tbody>
</table>

### Create a new comment on a task

    POST runtime/tasks/{taskId}/comments

<table>
<caption>Create a new comment on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to create the comment for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    {
      "message" : "This is a comment on the task.",
      "saveProcessInstanceId" : true
    }

Parameter saveProcessInstanceId is optional, if true save process instance id of task with comment.

**Success response body:**

    {
      "id" : "123",
      "taskUrl" : "http://localhost:8081/flowable-rest/service/runtime/tasks/101/comments/123",
      "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/123",
      "message" : "This is a comment on the task.",
      "author" : "kermit",
      "time" : "2014-07-13T13:13:52.232+08:00"
      "taskId" : "101",
      "processInstanceId" : "100"
    }

<table>
<caption>Create a new comment on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the comment was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the comment is missing from the request.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get all comments on a task

    GET runtime/tasks/{taskId}/comments

<table>
<caption>Get all comments on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the comments for.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "id" : "123",
        "taskUrl" : "http://localhost:8081/flowable-rest/service/runtime/tasks/101/comments/123",
        "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/123",
        "message" : "This is a comment on the task.",
        "author" : "kermit"
        "time" : "2014-07-13T13:13:52.232+08:00"
        "taskId" : "101",
        "processInstanceId" : "100"
      },
      {
        "id" : "456",
        "taskUrl" : "http://localhost:8081/flowable-rest/service/runtime/tasks/101/comments/456",
        "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/456",
        "message" : "This is another comment on the task.",
        "author" : "gonzo",
        "time" : "2014-07-13T13:13:52.232+08:00"
        "taskId" : "101",
        "processInstanceId" : "100"
      }
    ]

<table>
<caption>Get all comments on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the comments are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get a comment on a task

    GET runtime/tasks/{taskId}/comments/{commentId}

<table>
<caption>Get a comment on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the comment for.</p></td>
</tr>
<tr class="even">
<td><p>commentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the comment.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id" : "123",
      "taskUrl" : "http://localhost:8081/flowable-rest/service/runtime/tasks/101/comments/123",
      "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/123",
      "message" : "This is a comment on the task.",
      "author" : "kermit",
      "time" : "2014-07-13T13:13:52.232+08:00"
      "taskId" : "101",
      "processInstanceId" : "100"
    }

<table>
<caption>Get a comment on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task and comment were found and the comment is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the tasks doesn’t have a comment with the given ID.</p></td>
</tr>
</tbody>
</table>

### Delete a comment on a task

    DELETE runtime/tasks/{taskId}/comments/{commentId}

<table>
<caption>Delete a comment on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to delete the comment for.</p></td>
</tr>
<tr class="even">
<td><p>commentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the comment.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a comment on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the task and comment were found and the comment is deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the tasks doesn’t have a comment with the given ID.</p></td>
</tr>
</tbody>
</table>

### Get all events for a task

    GET runtime/tasks/{taskId}/events

<table>
<caption>Get all events for a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the events for.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "action" : "AddUserLink",
        "id" : "4",
        "message" : [ "gonzo", "contributor" ],
        "taskUrl" : "http://localhost:8182/runtime/tasks/2",
        "time" : "2013-05-17T11:50:50.000+0000",
        "url" : "http://localhost:8182/runtime/tasks/2/events/4",
        "userId" : null
      }

    ]

<table>
<caption>Get all events for a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the events are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get an event on a task

    GET runtime/tasks/{taskId}/events/{eventId}

<table>
<caption>Get an event on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the event for.</p></td>
</tr>
<tr class="even">
<td><p>eventId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the event.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "action" : "AddUserLink",
      "id" : "4",
      "message" : [ "gonzo", "contributor" ],
      "taskUrl" : "http://localhost:8182/runtime/tasks/2",
      "time" : "2013-05-17T11:50:50.000+0000",
      "url" : "http://localhost:8182/runtime/tasks/2/events/4",
      "userId" : null
    }

<table>
<caption>Get an event on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task and event were found and the event is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the tasks doesn’t have an event with the given ID.</p></td>
</tr>
</tbody>
</table>

### Create a new attachment on a task, containing a link to an external resource

    POST runtime/tasks/{taskId}/attachments

<table>
<caption>Create a new attachment on a task, containing a link to an external resource - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to create the attachment for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    {
      "name":"Simple attachment",
      "description":"Simple attachment description",
      "type":"simpleType",
      "externalUrl":"http://flowable.org"
    }

Only the attachment name is required to create a new attachment.

**Success response body:**

    {
      "id":"3",
      "url":"http://localhost:8182/runtime/tasks/2/attachments/3",
      "name":"Simple attachment",
      "description":"Simple attachment description",
      "type":"simpleType",
      "taskUrl":"http://localhost:8182/runtime/tasks/2",
      "processInstanceUrl":null,
      "externalUrl":"http://flowable.org",
      "contentUrl":null
    }

<table>
<caption>Create a new attachment on a task, containing a link to an external resource - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the attachment was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the attachment name is missing from the request.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Create a new attachment on a task, with an attached file

    POST runtime/tasks/{taskId}/attachments

<table>
<caption>Create a new attachment on a task, with an attached file - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to create the attachment for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:

-   name: Required name of the variable.

-   description: Description of the attachment, optional.

-   type: Type of attachment, optional. Supports any arbitrary string or a valid HTTP content-type.

**Success response body:**

    {
        "id":"5",
        "url":"http://localhost:8182/runtime/tasks/2/attachments/5",
        "name":"Binary attachment",
        "description":"Binary attachment description",
        "type":"binaryType",
        "taskUrl":"http://localhost:8182/runtime/tasks/2",
        "processInstanceUrl":null,
        "externalUrl":null,
        "contentUrl":"http://localhost:8182/runtime/tasks/2/attachments/5/content"
    }

<table>
<caption>Create a new attachment on a task, with an attached file - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the attachment was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the attachment name is missing from the request or no file was present in the request. The error-message contains additional information.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get all attachments on a task

    GET runtime/tasks/{taskId}/attachments

<table>
<caption>Get all attachments on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the attachments for.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "id":"3",
        "url":"http://localhost:8182/runtime/tasks/2/attachments/3",
        "name":"Simple attachment",
        "description":"Simple attachment description",
        "type":"simpleType",
        "taskUrl":"http://localhost:8182/runtime/tasks/2",
        "processInstanceUrl":null,
        "externalUrl":"http://flowable.org",
        "contentUrl":null
      },
      {
        "id":"5",
        "url":"http://localhost:8182/runtime/tasks/2/attachments/5",
        "name":"Binary attachment",
        "description":"Binary attachment description",
        "type":"binaryType",
        "taskUrl":"http://localhost:8182/runtime/tasks/2",
        "processInstanceUrl":null,
        "externalUrl":null,
        "contentUrl":"http://localhost:8182/runtime/tasks/2/attachments/5/content"
      }
    ]

<table>
<caption>Get all attachments on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task was found and the attachments are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get an attachment on a task

    GET runtime/tasks/{taskId}/attachments/{attachmentId}

<table>
<caption>Get an attachment on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get the attachment for.</p></td>
</tr>
<tr class="even">
<td><p>attachmentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the attachment.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id":"5",
      "url":"http://localhost:8182/runtime/tasks/2/attachments/5",
      "name":"Binary attachment",
      "description":"Binary attachment description",
      "type":"binaryType",
      "taskUrl":"http://localhost:8182/runtime/tasks/2",
      "processInstanceUrl":null,
      "externalUrl":null,
      "contentUrl":"http://localhost:8182/runtime/tasks/2/attachments/5/content"
    }

-   externalUrl - contentUrl:In case the attachment is a link to an external resource, the externalUrl contains the URL to the external content. If the attachment content is present in the Flowable engine, the contentUrl will contain an URL where the binary content can be streamed from.

-   type:Can be any arbitrary value. When a valid formatted media-type (e.g. application/xml, text/plain) is included, the binary content HTTP response content-type will be set the given value.

<table>
<caption>Get an attachment on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task and attachment were found and the attachment is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the tasks doesn’t have a attachment with the given ID.</p></td>
</tr>
</tbody>
</table>

### Get the content for an attachment

    GET runtime/tasks/{taskId}/attachment/{attachmentId}/content

<table>
<caption>Get the content for an attachment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to get a variable data for.</p></td>
</tr>
<tr class="even">
<td><p>attachmentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the attachment, a 404 is returned when the attachment points to an external URL rather than content attached in Flowable.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the content for an attachment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task and attachment was found and the requested content is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the task doesn’t have an attachment with the given id or the attachment doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary content. By default, the content-type of the response is set to application/octet-stream unless the attachment type contains a valid Content-type.

### Delete an attachment on a task

    DELETE runtime/tasks/{taskId}/attachments/{attachmentId}

<table>
<caption>Delete an attachment on a task - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the task to delete the attachment for.</p></td>
</tr>
<tr class="even">
<td><p>attachmentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the attachment.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete an attachment on a task - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the task and attachment were found and the attachment is deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the tasks doesn’t have a attachment with the given ID.</p></td>
</tr>
</tbody>
</table>

## History

### Get a historic process instance

    GET history/historic-process-instances/{processInstanceId}

<table>
<caption>Get a historic process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that the historic process instances could be found.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates that the historic process instances could not be found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "businessKey" : "myKey",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "startUserId" : "kermit",
          "startActivityId" : "startEvent",
          "endActivityId" : "endEvent",
          "deleteReason" : null,
          "superProcessInstanceId" : "3",
          "url" : "http://localhost:8182/history/historic-process-instances/5",
          "variables": null,
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### List of historic process instances

    GET history/historic-process-instances

<table>
<caption>List of historic process instances - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An id of the historic process instance.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition key of the historic process instance.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition id of the historic process instance.</p></td>
</tr>
<tr class="even">
<td><p>businessKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The business key of the historic process instance.</p></td>
</tr>
<tr class="odd">
<td><p>involvedUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An involved user of the historic process instance.</p></td>
</tr>
<tr class="even">
<td><p>finished</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication if the historic process instance is finished.</p></td>
</tr>
<tr class="odd">
<td><p>superProcessInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An optional parent process id of the historic process instance.</p></td>
</tr>
<tr class="even">
<td><p>excludeSubprocesses</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Return only historic process instances which aren’t sub processes.</p></td>
</tr>
<tr class="odd">
<td><p>finishedAfter</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic process instances that were finished after this date.</p></td>
</tr>
<tr class="even">
<td><p>finishedBefore</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic process instances that were finished before this date.</p></td>
</tr>
<tr class="odd">
<td><p>startedAfter</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic process instances that were started after this date.</p></td>
</tr>
<tr class="even">
<td><p>startedBefore</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic process instances that were started before this date.</p></td>
</tr>
<tr class="odd">
<td><p>startedBy</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Return only historic process instances that were started by this user.</p></td>
</tr>
<tr class="even">
<td><p>includeProcessVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>An indication if the historic process instance variables should be returned as well.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return instances with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return instances with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of historic process instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that historic process instances could be queried.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "businessKey" : "myKey",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "startUserId" : "kermit",
          "startActivityId" : "startEvent",
          "endActivityId" : "endEvent",
          "deleteReason" : null,
          "superProcessInstanceId" : "3",
          "url" : "http://localhost:8182/history/historic-process-instances/5",
          "variables": [
            {
              "name": "test",
              "variableScope": "local",
              "value": "myTest"
            }
          ],
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for historic process instances

    POST query/historic-process-instances

**Request body:**

    {
      "processDefinitionId" : "oneTaskProcess%3A1%3A4",


      "variables" : [
        {
          "name" : "myVariable",
          "value" : 1234,
          "operation" : "equals",
          "type" : "long"
        }
      ]
    }

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of historic process instances](bpmn/ch15-REST.md#list-of-historic-process-instances), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long. On top of that, the query allows for filtering based on process variables. The variables property is a JSON-array containing objects with the format [as described here.](#json-query-variable-format)

<table>
<caption>Query for historic process instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the tasks are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "businessKey" : "myKey",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "startUserId" : "kermit",
          "startActivityId" : "startEvent",
          "endActivityId" : "endEvent",
          "deleteReason" : null,
          "superProcessInstanceId" : "3",
          "url" : "http://localhost:8182/history/historic-process-instances/5",
          "variables": [
            {
              "name": "test",
              "variableScope": "local",
              "value": "myTest"
            }
          ],
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Delete a historic process instance

    DELETE history/historic-process-instances/{processInstanceId}

<table>
<caption>Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that the historic process instance was deleted.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates that the historic process instance could not be found.</p></td>
</tr>
</tbody>
</table>

### Get the identity links of a historic process instance

    GET history/historic-process-instance/{processInstanceId}/identitylinks

<table>
<caption>Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the identity links are returned</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the process instance could not be found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
     {
      "type" : "participant",
      "userId" : "kermit",
      "groupId" : null,
      "taskId" : null,
      "taskUrl" : null,
      "processInstanceId" : "5",
      "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/5"
     }
    ]

### Get the binary data for a historic process instance variable

    GET history/historic-process-instances/{processInstanceId}/variables/{variableName}/data

<table>
<caption>Get the binary data for a historic process instance variable - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and the requested variable data is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested process instance was not found or the process instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.

### Create a new comment on a historic process instance

    POST history/historic-process-instances/{processInstanceId}/comments

<table>
<caption>Create a new comment on a historic process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to create the comment for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

    {
      "message" : "This is a comment.",
      "saveProcessInstanceId" : true
    }

Parameter saveProcessInstanceId is optional, if true save process instance id of task with comment.

**Success response body:**

    {
      "id" : "123",
      "taskUrl" : "http://localhost:8081/flowable-rest/service/runtime/tasks/101/comments/123",
      "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/123",
      "message" : "This is a comment on the task.",
      "author" : "kermit",
      "time" : "2014-07-13T13:13:52.232+08:00",
      "taskId" : "101",
      "processInstanceId" : "100"
    }

<table>
<caption>Create a new comment on a historic process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the comment was created and the result is returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the comment is missing from the request.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested historic process instance was not found.</p></td>
</tr>
</tbody>
</table>

### Get all comments on a historic process instance

    GET history/historic-process-instances/{processInstanceId}/comments

<table>
<caption>Get all comments on a process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the process instance to get the comments for.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
      {
        "id" : "123",
        "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/123",
        "message" : "This is a comment on the task.",
        "author" : "kermit",
        "time" : "2014-07-13T13:13:52.232+08:00",
        "processInstanceId" : "100"
      },
      {
        "id" : "456",
        "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/456",
        "message" : "This is another comment.",
        "author" : "gonzo",
        "time" : "2014-07-14T15:16:52.232+08:00",
        "processInstanceId" : "100"
      }
    ]

<table>
<caption>Get all comments on a process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the process instance was found and the comments are returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found.</p></td>
</tr>
</tbody>
</table>

### Get a comment on a historic process instance

    GET history/historic-process-instances/{processInstanceId}/comments/{commentId}

<table>
<caption>Get a comment on a historic process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the historic process instance to get the comment for.</p></td>
</tr>
<tr class="even">
<td><p>commentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the comment.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id" : "123",
      "processInstanceUrl" : "http://localhost:8081/flowable-rest/service/history/historic-process-instances/100/comments/456",
      "message" : "This is another comment.",
      "author" : "gonzo",
      "time" : "2014-07-14T15:16:52.232+08:00",
      "processInstanceId" : "100"
    }

<table>
<caption>Get a comment on a historic process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the historic process instance and comment were found and the comment is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested historic process instance was not found or the historic process instance doesn’t have a comment with the given ID.</p></td>
</tr>
</tbody>
</table>

### Delete a comment on a historic process instance

    DELETE history/historic-process-instances/{processInstanceId}/comments/{commentId}

<table>
<caption>Delete a comment on a historic process instance - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the historic process instance to delete the comment for.</p></td>
</tr>
<tr class="even">
<td><p>commentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the comment.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a comment on a historic process instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the historic process instance and comment were found and the comment is deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task was not found or the historic process instance doesn’t have a comment with the given ID.</p></td>
</tr>
</tbody>
</table>

### Get a single historic task instance

    GET history/historic-task-instances/{taskId}

<table>
<caption>Get a single historic task instance - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that the historic task instances could be found.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates that the historic task instances could not be found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id" : "5",
      "processDefinitionId" : "oneTaskProcess%3A1%3A4",
      "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
      "processInstanceId" : "3",
      "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/3",
      "executionId" : "4",
      "name" : "My task name",
      "description" : "My task description",
      "deleteReason" : null,
      "owner" : "kermit",
      "assignee" : "fozzie",
      "startTime" : "2013-04-17T10:17:43.902+0000",
      "endTime" : "2013-04-18T14:06:32.715+0000",
      "durationInMillis" : 86400056,
      "workTimeInMillis" : 234890,
      "claimTime" : "2013-04-18T11:01:54.715+0000",
      "taskDefinitionKey" : "taskKey",
      "formKey" : null,
      "priority" : 50,
      "dueDate" : "2013-04-20T12:11:13.134+0000",
      "parentTaskId" : null,
      "url" : "http://localhost:8182/history/historic-task-instances/5",
      "variables": null,
      "tenantId":null
    }

### Get historic task instances

    GET history/historic-task-instances

<table>
<caption>Get historic task instances - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An id of the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance id of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition key of the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionKeyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition key of the historic task instance, which matches the given value.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition id of the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition name of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition name of the historic task instance, which matches the given value.</p></td>
</tr>
<tr class="even">
<td><p>processBusinessKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance business key of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>processBusinessKeyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance business key of the historic task instance that matches the given value.</p></td>
</tr>
<tr class="even">
<td><p>executionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The execution id of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task definition key for tasks part of a process</p></td>
</tr>
<tr class="even">
<td><p>taskName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task name of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task name with 'like' operator for the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>taskDescription</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task description of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskDescriptionLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task description with 'like' operator for the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>taskDefinitionKey</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task identifier from the process definition for the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskCategory</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Select tasks with the given category. Note that this is the task category, not the category of the process definition (namespace within the BPMN Xml).</p></td>
</tr>
<tr class="even">
<td><p>taskDeleteReason</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task delete reason of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskDeleteReasonLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task delete reason with 'like' operator for the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>taskAssignee</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The assignee of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskAssigneeLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The assignee with 'like' operator for the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>taskOwner</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The owner of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskOwnerLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The owner with 'like' operator for the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>taskInvolvedUser</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An involved user of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>taskPriority</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The priority of the historic task instance.</p></td>
</tr>
<tr class="even">
<td><p>finished</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication if the historic task instance is finished.</p></td>
</tr>
<tr class="odd">
<td><p>processFinished</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication if the process instance of the historic task instance is finished.</p></td>
</tr>
<tr class="even">
<td><p>parentTaskId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An optional parent task id of the historic task instance.</p></td>
</tr>
<tr class="odd">
<td><p>dueDate</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have a due date equal this date.</p></td>
</tr>
<tr class="even">
<td><p>dueDateAfter</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have a due date after this date.</p></td>
</tr>
<tr class="odd">
<td><p>dueDateBefore</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have a due date before this date.</p></td>
</tr>
<tr class="even">
<td><p>withoutDueDate</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Return only historic task instances that have no due-date. When false is provided as value, this parameter is ignored.</p></td>
</tr>
<tr class="odd">
<td><p>taskCompletedOn</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have been completed on this date.</p></td>
</tr>
<tr class="even">
<td><p>taskCompletedAfter</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have been completed after this date.</p></td>
</tr>
<tr class="odd">
<td><p>taskCompletedBefore</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that have been completed before this date.</p></td>
</tr>
<tr class="even">
<td><p>taskCreatedOn</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that were created on this date.</p></td>
</tr>
<tr class="odd">
<td><p>taskCreatedBefore</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that were created before this date.</p></td>
</tr>
<tr class="even">
<td><p>taskCreatedAfter</p></td>
<td><p>No</p></td>
<td><p>Date</p></td>
<td><p>Return only historic task instances that were created after this date.</p></td>
</tr>
<tr class="odd">
<td><p>includeTaskLocalVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>An indication if the historic task instance local variables should be returned as well.</p></td>
</tr>
<tr class="even">
<td><p>includeProcessVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>An indication if the historic task instance global variables should be returned as well.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return historic task instances with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return historic task instances with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns historic task instances without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get historic task instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that historic process instances could be queried.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceId" : "3",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/3",
          "executionId" : "4",
          "name" : "My task name",
          "description" : "My task description",
          "deleteReason" : null,
          "owner" : "kermit",
          "assignee" : "fozzie",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "workTimeInMillis" : 234890,
          "claimTime" : "2013-04-18T11:01:54.715+0000",
          "taskDefinitionKey" : "taskKey",
          "formKey" : null,
          "priority" : 50,
          "dueDate" : "2013-04-20T12:11:13.134+0000",
          "parentTaskId" : null,
          "url" : "http://localhost:8182/history/historic-task-instances/5",
          "taskVariables": [
            {
              "name": "test",
              "variableScope": "local",
              "value": "myTest"
            }
          ],
          "processVariables": [
            {
              "name": "processTest",
              "variableScope": "global",
              "value": "myProcessTest"
            }
          ],
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for historic task instances

    POST query/historic-task-instances

**Query for historic task instances - Request body:**

    {
      "processDefinitionId" : "oneTaskProcess%3A1%3A4",
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

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of historic task instances](bpmn/ch15-REST.md#get-historic-task-instances), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long. On top of that, the query allows for filtering based on process variables. The taskVariables and processVariables properties are JSON-arrays containing objects with the format [as described here.](#json-query-variable-format)

<table>
<caption>Query for historic task instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the tasks are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceId" : "3",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/3",
          "executionId" : "4",
          "name" : "My task name",
          "description" : "My task description",
          "deleteReason" : null,
          "owner" : "kermit",
          "assignee" : "fozzie",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "workTimeInMillis" : 234890,
          "claimTime" : "2013-04-18T11:01:54.715+0000",
          "taskDefinitionKey" : "taskKey",
          "formKey" : null,
          "priority" : 50,
          "dueDate" : "2013-04-20T12:11:13.134+0000",
          "parentTaskId" : null,
          "url" : "http://localhost:8182/history/historic-task-instances/5",
          "taskVariables": [
            {
              "name": "test",
              "variableScope": "local",
              "value": "myTest"
            }
          ],
          "processVariables": [
            {
              "name": "processTest",
              "variableScope": "global",
              "value": "myProcessTest"
            }
          ],
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Delete a historic task instance

    DELETE history/historic-task-instances/{taskId}

<table>
<caption>Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that the historic task instance was deleted.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates that the historic task instance could not be found.</p></td>
</tr>
</tbody>
</table>

### Get the identity links of a historic task instance

    GET history/historic-task-instance/{taskId}/identitylinks

<table>
<caption>Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the identity links are returned</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the task instance could not be found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
     {
      "type" : "assignee",
      "userId" : "kermit",
      "groupId" : null,
      "taskId" : "6",
      "taskUrl" : "http://localhost:8182/history/historic-task-instances/5",
      "processInstanceId" : null,
      "processInstanceUrl" : null
     }
    ]

### Get the binary data for a historic task instance variable

    GET history/historic-task-instances/{taskId}/variables/{variableName}/data

<table>
<caption>Get the binary data for a historic task instance variable - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the task instance was found and the requested variable data is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested task instance was not found or the process instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.

### Get historic activity instances

    GET history/historic-activity-instances

<table>
<caption>Get historic activity instances - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>activityId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An id of the activity instance.</p></td>
</tr>
<tr class="even">
<td><p>activityInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>An id of the historic activity instance.</p></td>
</tr>
<tr class="odd">
<td><p>activityName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The name of the historic activity instance.</p></td>
</tr>
<tr class="even">
<td><p>activityType</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The element type of the historic activity instance.</p></td>
</tr>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The execution id of the historic activity instance.</p></td>
</tr>
<tr class="even">
<td><p>finished</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication if the historic activity instance is finished.</p></td>
</tr>
<tr class="odd">
<td><p>taskAssignee</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The assignee of the historic activity instance.</p></td>
</tr>
<tr class="even">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance id of the historic activity instance.</p></td>
</tr>
<tr class="odd">
<td><p>processDefinitionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process definition id of the historic activity instance.</p></td>
</tr>
<tr class="even">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return instances with the given tenantId.</p></td>
</tr>
<tr class="odd">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return instances with a tenantId like the given value.</p></td>
</tr>
<tr class="even">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns instances without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get historic activity instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that historic activity instances could be queried.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "activityId" : "4",
          "activityName" : "My user task",
          "activityType" : "userTask",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceId" : "3",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/3",
          "executionId" : "4",
          "taskId" : "4",
          "calledProcessInstanceId" : null,
          "assignee" : "fozzie",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for historic activity instances

    POST query/historic-activity-instances

**Request body:**

    {
      "processDefinitionId" : "oneTaskProcess%3A1%3A4"
    }

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of historic task instances](bpmn/ch15-REST.md#get-historic-task-instances), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long.

<table>
<caption>Query for historic activity instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the activities are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "5",
          "activityId" : "4",
          "activityName" : "My user task",
          "activityType" : "userTask",
          "processDefinitionId" : "oneTaskProcess%3A1%3A4",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4",
          "processInstanceId" : "3",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/3",
          "executionId" : "4",
          "taskId" : "4",
          "calledProcessInstanceId" : null,
          "assignee" : "fozzie",
          "startTime" : "2013-04-17T10:17:43.902+0000",
          "endTime" : "2013-04-18T14:06:32.715+0000",
          "durationInMillis" : 86400056,
          "tenantId":null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### List of historic variable instances

    GET history/historic-variable-instances

<table>
<caption>List of historic variable instances - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance id of the historic variable instance.</p></td>
</tr>
<tr class="even">
<td><p>taskId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task id of the historic variable instance.</p></td>
</tr>
<tr class="odd">
<td><p>excludeTaskVariables</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to exclude the task variables from the result.</p></td>
</tr>
<tr class="even">
<td><p>variableName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The variable name of the historic variable instance.</p></td>
</tr>
<tr class="odd">
<td><p>variableNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The variable name using the 'like' operator for the historic variable instance.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of historic variable instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that historic variable instances could be queried.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "14",
          "processInstanceId" : "5",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/5",
          "taskId" : "6",
          "variable" : {
            "name" : "myVariable",
            "variableScope", "global",
            "value" : "test"
          }
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for historic variable instances

    POST query/historic-variable-instances

**Request body:**

    {
      "processDefinitionId" : "oneTaskProcess%3A1%3A4",
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

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of historic process instances](bpmn/ch15-REST.md#list-of-historic-variable-instances), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long. On top of that, the query allows for filtering based on process variables. The variables property is a JSON-array containing objects with the format [as described here.](#json-query-variable-format)

<table>
<caption>Query for historic variable instances - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the tasks are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "14",
          "processInstanceId" : "5",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/5",
          "taskId" : "6",
          "variable" : {
            "name" : "myVariable",
            "variableScope", "global",
            "value" : "test"
          }
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

====Get the binary data for a historic task instance variable

    GET history/historic-variable-instances/{varInstanceId}/data

<table>
<caption>Get the binary data for a historic task instance variable - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the variable instance was found and the requested variable data is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested variable instance was not found or the variable instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.

### Get historic detail

    GET history/historic-detail

<table>
<caption>Get historic detail - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The id of the historic detail.</p></td>
</tr>
<tr class="even">
<td><p>processInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The process instance id of the historic detail.</p></td>
</tr>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The execution id of the historic detail.</p></td>
</tr>
<tr class="even">
<td><p>activityInstanceId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The activity instance id of the historic detail.</p></td>
</tr>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>The task id of the historic detail.</p></td>
</tr>
<tr class="even">
<td><p>selectOnlyFormProperties</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to only return form properties in the result.</p></td>
</tr>
<tr class="odd">
<td><p>selectOnlyVariableUpdates</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Indication to only return variable updates in the result.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get historic detail - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that historic detail could be queried.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "26",
          "processInstanceId" : "5",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/5",
          "executionId" : "6",
          "activityInstanceId", "10",
          "taskId" : "6",
          "taskUrl" : "http://localhost:8182/history/historic-task-instances/6",
          "time" : "2013-04-17T10:17:43.902+0000",
          "detailType" : "variableUpdate",
          "revision" : 2,
          "variable" : {
            "name" : "myVariable",
            "variableScope", "global",
            "value" : "test"
          },
          "propertyId": null,
          "propertyValue": null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Query for historic details

    POST query/historic-detail

**Request body:**

    {
      "processInstanceId" : "5",
    }

All supported JSON parameter fields allowed are exactly the same as the parameters found for [getting a collection of historic process instances](bpmn/ch15-REST.md#get-historic-detail), but passed in as JSON-body arguments rather than URL-parameters to allow for more advanced querying and preventing errors with request-uri’s that are too long.

<table>
<caption>Query for historic details - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the historic details are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id" : "26",
          "processInstanceId" : "5",
          "processInstanceUrl" : "http://localhost:8182/history/historic-process-instances/5",
          "executionId" : "6",
          "activityInstanceId", "10",
          "taskId" : "6",
          "taskUrl" : "http://localhost:8182/history/historic-task-instances/6",
          "time" : "2013-04-17T10:17:43.902+0000",
          "detailType" : "variableUpdate",
          "revision" : 2,
          "variable" : {
            "name" : "myVariable",
            "variableScope", "global",
            "value" : "test"
          },
          "propertyId" : null,
          "propertyValue" : null
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Get the binary data for a historic detail variable

    GET history/historic-detail/{detailId}/data

<table>
<caption>Get the binary data for a historic detail variable - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the historic detail instance was found and the requested variable data is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested historic detail instance was not found or the historic detail instance doesn’t have a variable with the given name or the variable doesn’t have a binary stream available. Status message provides additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body contains the binary value of the variable. When the variable is of type binary, the content-type of the response is set to application/octet-stream, regardless of the content of the variable or the request accept-type header. In case of serializable, application/x-java-serialized-object is used as content-type.

## Forms

### Get form data

    GET form/form-data

<table>
<caption>Get form data - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>taskId</p></td>
<td><p>Yes (if no processDefinitionId)</p></td>
<td><p>String</p></td>
<td><p>The task id corresponding to the form data that needs to be retrieved.</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionId</p></td>
<td><p>Yes (if no taskId)</p></td>
<td><p>String</p></td>
<td><p>The process definition id corresponding to the start event form data that needs to be retrieved.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get form data - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates that form data could be queried.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates that form data could not be found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "formKey" : null,
          "deploymentId" : "2",
          "processDefinitionId" : "3",
          "processDefinitionUrl" : "http://localhost:8182/repository/process-definition/3",
          "taskId" : "6",
          "taskUrl" : "http://localhost:8182/runtime/task/6",
          "formProperties" : [
            {
              "id" : "room",
              "name" : "Room",
              "type" : "string",
              "value" : null,
              "readable" : true,
              "writable" : true,
              "required" : true,
              "datePattern" : null,
              "enumValues" : [
                {
                  "id" : "normal",
                  "name" : "Normal bed"
                },
                {
                  "id" : "kingsize",
                  "name" : "Kingsize bed"
                },
              ]
            }
          ]
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Submit task form data

    POST form/form-data

**Request body for task form:**

    {
      "taskId" : "5",
      "properties" : [
        {
          "id" : "room",
          "value" : "normal"
        }
      ]
    }

**Request body for start event form:**

    {
      "processDefinitionId" : "5",
      "businessKey" : "myKey",
      "properties" : [
        {
          "id" : "room",
          "value" : "normal"
        }
      ]
    }

<table>
<caption>Submit task form data - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the form data was submitted</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an parameter was passed in the wrong format. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body for start event form data (no response for task form data):**

    {
      "id" : "5",
      "url" : "http://localhost:8182/history/historic-process-instances/5",
      "businessKey" : "myKey",
      "suspended": false,
      "processDefinitionId" : "3",
      "processDefinitionUrl" : "http://localhost:8182/repository/process-definition/3",
      "activityId" : "myTask"
    }

## Database tables

### List of tables

    GET management/tables

<table>
<caption>List of tables - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the request was successful.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    [
       {
          "name":"ACT_RU_VARIABLE",
          "url":"http://localhost:8182/management/tables/ACT_RU_VARIABLE",
          "count":4528
       },
       {
          "name":"ACT_RU_EVENT_SUBSCR",
          "url":"http://localhost:8182/management/tables/ACT_RU_EVENT_SUBSCR",
          "count":3
       }

    ]

### Get a single table

    GET management/tables/{tableName}

<table>
<caption>Get a single table - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>tableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the table to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
          "name":"ACT_RE_PROCDEF",
          "url":"http://localhost:8182/management/tables/ACT_RE_PROCDEF",
          "count":60
    }

<table>
<caption>Get a single table - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the table exists and the table count is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested table does not exist.</p></td>
</tr>
</tbody>
</table>

### Get column info for a single table

    GET management/tables/{tableName}/columns

<table>
<caption>Get column info for a single table - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>tableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the table to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "tableName":"ACT_RU_VARIABLE",
       "columnNames":[
          "ID_",
          "REV_",
          "TYPE_",
          "NAME_"


       ],
       "columnTypes":[
          "VARCHAR",
          "INTEGER",
          "VARCHAR",
          "VARCHAR"


       ]
    }

<table>
<caption>Get column info for a single table - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the table exists and the table column info is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested table does not exist.</p></td>
</tr>
</tbody>
</table>

### Get row data for a single table

    GET management/tables/{tableName}/data

<table>
<caption>Get row data for a single table - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>tableName</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The name of the table to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get row data for a single table - URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>start</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Index of the first row to fetch. Defaults to 0.</p></td>
</tr>
<tr class="even">
<td><p>size</p></td>
<td><p>No</p></td>
<td><p>Integer</p></td>
<td><p>Number of rows to fetch, starting from start. Defaults to 10.</p></td>
</tr>
<tr class="odd">
<td><p>orderAscendingColumn</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Name of the column to sort the resulting rows on, ascending.</p></td>
</tr>
<tr class="even">
<td><p>orderDescendingColumn</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Name of the column to sort the resulting rows on, descending.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "total":3,
       "start":0,
       "sort":null,
       "order":null,
       "size":3,

       "data":[
          {
             "TASK_ID_":"2",
             "NAME_":"var1",
             "REV_":1,
             "TEXT_":"123",
             "LONG_":123,
             "ID_":"3",
             "TYPE_":"integer"
          }



       ]

    }

<table>
<caption>Get row data for a single table - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the table exists and the table row data is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested table does not exist.</p></td>
</tr>
</tbody>
</table>

## Engine

### Get engine properties

    GET management/properties

Returns a read-only view of the properties used internally in the engine.

**Success response body:**

    {
       "next.dbid":"101",
       "schema.history":"create(5.15)",
       "schema.version":"5.15"
    }

<table>
<caption>Get engine properties - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the properties are returned.</p></td>
</tr>
</tbody>
</table>

### Get engine info

    GET management/engine

Returns a read-only view of the engine that is used in this REST-service.

**Success response body:**

    {
       "name":"default",
       "version":"5.15",
       "resourceUrl":"file://flowable/flowable.cfg.xml",
       "exception":null
    }

<table>
<caption>Get engine info - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the engine info is returned.</p></td>
</tr>
</tbody>
</table>

## Runtime

### Signal event received

    POST runtime/signals

Notifies the engine that a signal event has been received, not explicitly related to a specific execution.

**Body JSON:**

    {
      "signalName": "My Signal",
      "tenantId" : "execute",
      "async": true,
      "variables": [
          {"name": "testVar", "value": "This is a string"}

      ]
    }

<table>
<caption>Signal event received - JSON Body parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>signalName</p></td>
<td><p>Name of the signal</p></td>
<td><p>Yes</p></td>
</tr>
<tr class="even">
<td><p>tenantId</p></td>
<td><p>ID of the tenant that the signal event should be processed in</p></td>
<td><p>No</p></td>
</tr>
<tr class="odd">
<td><p>async</p></td>
<td><p>If true, handling of the signal will happen asynchronously. Return code will be 202 - Accepted to indicate the request is accepted but not yet executed. If false,
handling the signal will be done immediately and result (200 - OK) will only return after this completed successfully. Defaults to false if omitted.</p></td>
<td><p>No</p></td>
</tr>
<tr class="even">
<td><p>variables</p></td>
<td><p>Array of variables (in the general variables format) to use as payload to pass along with the signal. Cannot be used in case async is set to true, this will result in an error.</p></td>
<td><p>No</p></td>
</tr>
</tbody>
</table>

**Success response body:**

<table>
<caption>Signal event received - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicated signal has been processed and no errors occurred.</p></td>
</tr>
<tr class="even">
<td><p>202</p></td>
<td><p>Indicated signal processing is queued as a job, ready to be executed.</p></td>
</tr>
<tr class="odd">
<td><p>400</p></td>
<td><p>Signal not processed. The signal name is missing or variables are used together with async, which is not allowed. Response body contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

## Jobs

### Get a single job

    GET management/jobs/{jobId}

<table>
<caption>Get a single job - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the job to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"8",
       "url":"http://localhost:8182/management/jobs/8",
       "processInstanceId":"5",
       "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
       "processDefinitionId":"timerProcess:1:4",
       "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/timerProcess%3A1%3A4",
       "executionId":"7",
       "executionUrl":"http://localhost:8182/runtime/executions/7",
       "retries":3,
       "exceptionMessage":null,
       "dueDate":"2013-06-04T22:05:05.474+0000",
       "tenantId":null
    }

<table>
<caption>Get a single job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the job exists and is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested job does not exist.</p></td>
</tr>
</tbody>
</table>

### Delete a job

    DELETE management/jobs/{jobId}

<table>
<caption>Delete a job - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the job to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the job was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested job was not found.</p></td>
</tr>
</tbody>
</table>

### Execute a single job

    POST management/jobs/{jobId}

**Body JSON:**

    {
      "action" : "execute"
    }

<table>
<caption>Execute a single job - JSON Body parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>action</p></td>
<td><p>Action to perform. Only execute is supported.</p></td>
<td><p>Yes</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Execute a single job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the job was executed. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested job was not found.</p></td>
</tr>
<tr class="odd">
<td><p>500</p></td>
<td><p>Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.</p></td>
</tr>
</tbody>
</table>

### Get the exception stacktrace for a job

    GET management/jobs/{jobId}/exception-stacktrace

<table>
<caption>Get the exception stacktrace for a job - URL parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Id of the job to get the stacktrace for.</p></td>
<td><p>Yes</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the exception stacktrace for a job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Get a list of jobs

    GET management/jobs

<table>
<caption>Get a list of jobs - URL query parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Type</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>Only return job with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>processInstanceId</p></td>
<td><p>Only return jobs part of a process with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Only return jobs part of an execution with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionId</p></td>
<td><p>Only return jobs with the given process definition id</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>withRetriesLeft</p></td>
<td><p>If true, only return jobs with retries left. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>executable</p></td>
<td><p>If true, only return jobs which are executable. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="odd">
<td><p>timersOnly</p></td>
<td><p>If true, only return jobs which are timers. If false, this parameter is ignored. Cannot be used together with 'messagesOnly'.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>messagesOnly</p></td>
<td><p>If true, only return jobs which are messages. If false, this parameter is ignored. Cannot be used together with 'timersOnly'</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="odd">
<td><p>withException</p></td>
<td><p>If true, only return jobs for which an exception occurred while executing it. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>dueBefore</p></td>
<td><p>Only return jobs which are due to be executed before the given date. Jobs without duedate are never returned using this parameter.</p></td>
<td><p>Date</p></td>
</tr>
<tr class="odd">
<td><p>dueAfter</p></td>
<td><p>Only return jobs which are due to be executed after the given date. Jobs without duedate are never returned using this parameter.</p></td>
<td><p>Date</p></td>
</tr>
<tr class="even">
<td><p>exceptionMessage</p></td>
<td><p>Only return jobs with the given exception message</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>Only return jobs with the given tenantId.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>Only return jobs with a tenantId like the given value.</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>If true, only returns jobs without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>withoutScopeType</p></td>
<td><p>If true, only returns jobs without a scope type set. If false, the withoutScopeType parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>Field to sort results on, should be one of id, dueDate, executionId, processInstanceId, retries or tenantId.</p></td>
<td><p>String</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"13",
             "url":"http://localhost:8182/management/jobs/13",
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "processDefinitionId":"timerProcess:1:4",
             "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/timerProcess%3A1%3A4",
             "executionId":"12",
             "executionUrl":"http://localhost:8182/runtime/executions/12",
             "retries":0,
             "exceptionMessage":"Can't find scripting engine for 'unexistinglanguage'",
             "dueDate":"2013-06-07T10:00:24.653+0000",
             "tenantId":null
          }



       ],
       "total":2,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":2
    }

<table>
<caption>Get a list of jobs - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested jobs were returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an illegal value has been used in a url query parameter or the both 'messagesOnly' and 'timersOnly' are used as parameters. Status description contains additional details about the error.</p></td>
</tr>
</tbody>
</table>

### Get a single deadletter job

    GET management/deadletter-jobs/{jobId}

<table>
<caption>Get a single dead letter job - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the dead letter job to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"8",
       "url":"http://localhost:8182/management/jobs/8",
       "processInstanceId":"5",
       "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
       "processDefinitionId":"timerProcess:1:4",
       "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/timerProcess%3A1%3A4",
       "executionId":"7",
       "executionUrl":"http://localhost:8182/runtime/executions/7",
       "retries":0,
       "exceptionMessage":"Can't find scripting engine for 'unexistinglanguage'",
       "dueDate":"2013-06-04T22:05:05.474+0000",
       "tenantId":null
    }

<table>
<caption>Get a single dead letter job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the dead letter job exists and is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested dead letter job does not exist.</p></td>
</tr>
</tbody>
</table>

### Delete a dead letter job

    DELETE management/deadletter-jobs/{jobId}

<table>
<caption>Delete a dead letter job - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the dead letter job to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a dead letter job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the dead letter job was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested dead letter job was not found.</p></td>
</tr>
</tbody>
</table>

### Resume and execute a dead letter job

    POST management/deadletter-jobs/{jobId}

**Body JSON:**

    {
      "action" : "move"
    }

<table>
<caption>Resume and execute a dead letter job - JSON Body parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>action</p></td>
<td><p>Action to perform. Only execute is supported.</p></td>
<td><p>Yes</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Resume and execute a dead letter job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the dead letter job was executed. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested dead letter job was not found.</p></td>
</tr>
<tr class="odd">
<td><p>500</p></td>
<td><p>Indicates the an exception occurred while executing the dead letter job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.</p></td>
</tr>
</tbody>
</table>

### Get the exception stacktrace for a deadletter job

    GET management/deadletter-jobs/{jobId}/exception-stacktrace

<table>
<caption>Get the exception stacktrace for a dead letter job - URL parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Required</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>jobId</p></td>
<td><p>Id of the dead letter job to get the stacktrace for.</p></td>
<td><p>Yes</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get the exception stacktrace for a dead letter job - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested dead letter job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested dead letter job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Get a list of dead letterjobs

    GET management/deadletter-jobs

<table>
<caption>Get a list of dead letter jobs - URL query parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Type</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>Only return job with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>processInstanceId</p></td>
<td><p>Only return jobs part of a process with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>executionId</p></td>
<td><p>Only return jobs part of an execution with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>processDefinitionId</p></td>
<td><p>Only return jobs with the given process definition id</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>executable</p></td>
<td><p>If true, only return jobs which are executable. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>timersOnly</p></td>
<td><p>If true, only return jobs which are timers. If false, this parameter is ignored. Cannot be used together with 'messagesOnly'.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="odd">
<td><p>messagesOnly</p></td>
<td><p>If true, only return jobs which are messages. If false, this parameter is ignored. Cannot be used together with 'timersOnly'</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>withException</p></td>
<td><p>If true, only return jobs for which an exception occurred while executing it. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="odd">
<td><p>dueBefore</p></td>
<td><p>Only return jobs which are due to be executed before the given date. Jobs without duedate are never returned using this parameter.</p></td>
<td><p>Date</p></td>
</tr>
<tr class="even">
<td><p>dueAfter</p></td>
<td><p>Only return jobs which are due to be executed after the given date. Jobs without duedate are never returned using this parameter.</p></td>
<td><p>Date</p></td>
</tr>
<tr class="odd">
<td><p>withException</p></td>
<td><p>If true, for which an exception occurred while executing it. If false, this parameter is ignored.</p></td>
<td><p>Boolean</p></td>
</tr>
<tr class="even">
<td><p>tenantId</p></td>
<td><p>String</p></td>
<td><p>Only return jobs with exception.</p></td>
</tr>
<tr class="odd">
<td><p>tenantIdLike</p></td>
<td><p>String</p></td>
<td><p>Only return jobs with a tenantId like the given value.</p></td>
</tr>
<tr class="even">
<td><p>withoutTenantId</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns jobs without a tenantId set. If false, this parameter is ignored.</p></td>
</tr>
<tr class="odd">
<td><p>locked</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns jobs which are locked. If false, this parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>unlocked</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns jobs which are unlocked. If false, this parameter is ignored.</p></td>
</tr>
<tr class="odd">
<td><p>withoutScopeType</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns jobs without a scope type set. If false, the withoutScopeType parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>Field to sort results on, should be one of id, dueDate, executionId, processInstanceId, retries or tenantId.</p></td>
<td><p>String</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"13",
             "url":"http://localhost:8182/management/jobs/13",
             "processInstanceId":"5",
             "processInstanceUrl":"http://localhost:8182/runtime/process-instances/5",
             "processDefinitionId":"timerProcess:1:4",
             "processDefinitionUrl":"http://localhost:8182/repository/process-definitions/timerProcess%3A1%3A4",
             "executionId":"12",
             "executionUrl":"http://localhost:8182/runtime/executions/12",
             "retries":0,
             "withException":"Can't find scripting engine for 'unexistinglanguage'",
             "dueDate":"2013-06-07T10:00:24.653+0000",
             "createTime":"2013-06-06T08:08:24.007+0000"
             "tenantId":null
          }
       ],
       "total":2,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":2
    }

<table>
<caption>Get a list of dead letter jobs - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested jobs were returned.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates an illegal value has been used in a url query parameter or the both 'messagesOnly' and 'timersOnly' are used as parameters. Status description contains additional details about the error.</p></td>
</tr>
</tbody>
</table>

## Users

### Get a single user

    GET identity/users/{userId}

<table>
<caption>Get a single user - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"testuser",
       "firstName":"Fred",
       "lastName":"McDonald",
       "url":"http://localhost:8182/identity/users/testuser",
       "email":"no-reply@flowable.org"
    }

<table>
<caption>Get a single user - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user exists and is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user does not exist.</p></td>
</tr>
</tbody>
</table>

### Get a list of users

    GET identity/users

<table>
<caption>Get a list of users - URL query parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Type</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>Only return user with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>firstName</p></td>
<td><p>Only return users with the given firstname</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>lastName</p></td>
<td><p>Only return users with the given lastname</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>email</p></td>
<td><p>Only return users with the given email</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>firstNameLike</p></td>
<td><p>Only return users with a firstname like the given value. Use % as wildcard-character.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>lastNameLike</p></td>
<td><p>Only return users with a lastname like the given value. Use % as wildcard-character.</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>emailLike</p></td>
<td><p>Only return users with an email like the given value. Use % as wildcard-character.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>memberOfGroup</p></td>
<td><p>Only return users which are a member of the given group.</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>potentialStarter</p></td>
<td><p>Only return users which are potential starters for a process-definition with the given id.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>Field to sort results on, should be one of id, firstName, lastname or email.</p></td>
<td><p>String</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
          {
             "id":"anotherUser",
             "firstName":"Tijs",
             "lastName":"Barrez",
             "url":"http://localhost:8182/identity/users/anotherUser",
             "email":"no-reply@flowable.org"
          },
          {
             "id":"kermit",
             "firstName":"Kermit",
             "lastName":"the Frog",
             "url":"http://localhost:8182/identity/users/kermit",
             "email":null
          },
          {
             "id":"testuser",
             "firstName":"Fred",
             "lastName":"McDonald",
             "url":"http://localhost:8182/identity/users/testuser",
             "email":"no-reply@flowable.org"
          }
       ],
       "total":3,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":3
    }

<table>
<caption>Get a list of users - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested users were returned.</p></td>
</tr>
</tbody>
</table>

### Update a user

    PUT identity/users/{userId}

**Body JSON:**

    {
      "firstName":"Tijs",
      "lastName":"Barrez",
      "email":"no-reply@flowable.org",
      "password":"pass123"
    }

All request values are optional. For example, you can only include the 'firstName' attribute in the request body JSON-object, only updating the firstName of the user, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the user-value will be updated to null. Example: {"firstName" : null} will clear the firstName of the user).

<table>
<caption>Update a user - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found.</p></td>
</tr>
<tr class="odd">
<td><p>409</p></td>
<td><p>Indicates the requested user was updated simultaneously.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for identity/users/{userId}.

### Create a user

    POST identity/users

**Body JSON:**

    {
      "id":"tijs",
      "firstName":"Tijs",
      "lastName":"Barrez",
      "email":"no-reply@flowable.org",
      "password":"pass123"
    }

<table>
<caption>Create a user - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the user was created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the id of the user was missing.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for identity/users/{userId}.

### Delete a user

    DELETE identity/users/{userId}

<table>
<caption>Delete a user - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a user - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the user was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found.</p></td>
</tr>
</tbody>
</table>

### Get a user’s picture

    GET identity/users/{userId}/picture

<table>
<caption>Get a user’s picture - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to get the picture for.</p></td>
</tr>
</tbody>
</table>

**Response Body:**

The response body contains the raw picture data, representing the user’s picture. The Content-type of the response corresponds to the mimeType that was set when creating the picture.

<table>
<caption>Get a user’s picture - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was found and has a picture, which is returned in the body.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found or the user does not have a profile picture. Status-description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Updating a user’s picture

    GET identity/users/{userId}/picture

<table>
<caption>Updating a user’s picture - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to get the picture for.</p></td>
</tr>
</tbody>
</table>

**Request body:**

The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the picture. On top of that, the following additional form-fields can be present:

-   mimeType: Optional mime-type for the uploaded picture. If omitted, the default of image/jpeg is used as a mime-type for the picture.

<table>
<caption>Updating a user’s picture - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was found and the picture has been updated. The response-body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found.</p></td>
</tr>
</tbody>
</table>

### List a user’s info

    PUT identity/users/{userId}/info

<table>
<caption>List a user’s info - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to get the info for.</p></td>
</tr>
</tbody>
</table>

**Response Body:**

    [
       {
          "key":"key1",
          "url":"http://localhost:8182/identity/users/testuser/info/key1"
       },
       {
          "key":"key2",
          "url":"http://localhost:8182/identity/users/testuser/info/key2"
       }
    ]

<table>
<caption>List a user’s info - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was found and list of info (key and url) is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found.</p></td>
</tr>
</tbody>
</table>

### Get a user’s info

    GET identity/users/{userId}/info/{key}

<table>
<caption>Get a user’s info - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to get the info for.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The key of the user info to get.</p></td>
</tr>
</tbody>
</table>

**Response Body:**

    {
       "key":"key1",
       "value":"Value 1",
       "url":"http://localhost:8182/identity/users/testuser/info/key1"
    }

<table>
<caption>Get a user’s info - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was found and the user has info for the given key..</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found or the user doesn’t have info for the given key. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Update a user’s info

    PUT identity/users/{userId}/info/{key}

<table>
<caption>Update a user’s info - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to update the info for.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The key of the user info to update.</p></td>
</tr>
</tbody>
</table>

**Request Body:**

    {
       "value":"The updated value"
    }

**Response Body:**

    {
       "key":"key1",
       "value":"The updated value",
       "url":"http://localhost:8182/identity/users/testuser/info/key1"
    }

<table>
<caption>Update a user’s info - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the user was found and the info has been updated.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the value was missing from the request body.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found or the user doesn’t have info for the given key. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

### Create a new user’s info entry

    POST identity/users/{userId}/info

<table>
<caption>Create a new user’s info entry - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to create the info for.</p></td>
</tr>
</tbody>
</table>

**Request Body:**

    {
       "key":"key1",
       "value":"The value"
    }

**Response Body:**

    {
       "key":"key1",
       "value":"The value",
       "url":"http://localhost:8182/identity/users/testuser/info/key1"
    }

<table>
<caption>Create a new user’s info entry - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the user was found and the info has been created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the key or value was missing from the request body. Status description contains additional information about the error.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates there is already an info-entry with the given key for the user, update the resource instance (PUT).</p></td>
</tr>
</tbody>
</table>

### Delete a user’s info

    DELETE identity/users/{userId}/info/{key}

<table>
<caption>Delete a user’s info - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to delete the info for.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The key of the user info to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a user’s info - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the user was found and the info for the given key has been deleted. Response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested user was not found or the user doesn’t have info for the given key. Status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

## Groups

### Get a single group

    GET identity/groups/{groupId}

<table>
<caption>Get a single group - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>groupId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the group to get.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "id":"testgroup",
       "url":"http://localhost:8182/identity/groups/testgroup",
       "name":"Test group",
       "type":"Test type"
    }

<table>
<caption>Get a single group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the group exists and is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested group does not exist.</p></td>
</tr>
</tbody>
</table>

### Get a list of groups

    GET identity/groups

<table>
<caption>Get a list of groups - URL query parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Description</th>
<th>Type</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>id</p></td>
<td><p>Only return group with the given id</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>name</p></td>
<td><p>Only return groups with the given name</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>type</p></td>
<td><p>Only return groups with the given type</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>nameLike</p></td>
<td><p>Only return groups with a name like the given value. Use % as wildcard-character.</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>member</p></td>
<td><p>Only return groups which have a member with the given username.</p></td>
<td><p>String</p></td>
</tr>
<tr class="even">
<td><p>potentialStarter</p></td>
<td><p>Only return groups which members are potential starters for a process-definition with the given id.</p></td>
<td><p>String</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>Field to sort results on, should be one of id, name or type.</p></td>
<td><p>String</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
       "data":[
         {
            "id":"testgroup",
            "url":"http://localhost:8182/identity/groups/testgroup",
            "name":"Test group",
            "type":"Test type"
         }
       ],
       "total":3,
       "start":0,
       "sort":"id",
       "order":"asc",
       "size":3
    }

<table>
<caption>Get a list of groups - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the requested groups were returned.</p></td>
</tr>
</tbody>
</table>

### Update a group

    PUT identity/groups/{groupId}

**Body JSON:**

    {
       "name":"Test group",
       "type":"Test type"
    }

All request values are optional. For example, you can only include the 'name' attribute in the request body JSON-object, only updating the name of the group, leaving all other fields unaffected. When an attribute is explicitly included and is set to null, the group-value will be updated to null.

<table>
<caption>Update a group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the group was updated.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested group was not found.</p></td>
</tr>
<tr class="odd">
<td><p>409</p></td>
<td><p>Indicates the requested group was updated simultaneously.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for identity/groups/{groupId}.

### Create a group

    POST identity/groups

**Body JSON:**

    {
       "id":"testgroup",
       "name":"Test group",
       "type":"Test type"
    }

<table>
<caption>Create a group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the group was created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates the id of the group was missing.</p></td>
</tr>
</tbody>
</table>

**Success response body:** see response for identity/groups/{groupId}.

### Delete a group

    DELETE identity/groups/{groupId}

<table>
<caption>Delete a group - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>groupId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the group to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the group was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested group was not found.</p></td>
</tr>
</tbody>
</table>

### Get members in a group

There is no GET allowed on identity/groups/members. Use the identity/users?memberOfGroup=sales URL to get all users that are part of a particular group.

### Add a member to a group

    POST identity/groups/{groupId}/members

<table>
<caption>Add a member to a group - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>groupId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the group to add a member to.</p></td>
</tr>
</tbody>
</table>

**Body JSON:**

    {
       "userId":"kermit"
    }

<table>
<caption>Add a member to a group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the group was found and the member has been added.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the userId was not included in the request body.</p></td>
</tr>
<tr class="odd">
<td><p>404</p></td>
<td><p>Indicates the requested group was not found.</p></td>
</tr>
<tr class="even">
<td><p>409</p></td>
<td><p>Indicates the requested user is already a member of the group.</p></td>
</tr>
</tbody>
</table>

**Response Body:**

    {
       "userId":"kermit",
       "groupId":"sales",
        "url":"http://localhost:8182/identity/groups/sales/members/kermit"
    }

### Delete a member from a group

    DELETE identity/groups/{groupId}/members/{userId}

<table>
<caption>Delete a member from a group - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>groupId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the group to remove a member from.</p></td>
</tr>
<tr class="even">
<td><p>userId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The id of the user to remove.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete a member from a group - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the group was found and the member has been deleted. The response body is left empty intentionally.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested group was not found or that the user is not a member of the group. The status description contains additional information about the error.</p></td>
</tr>
</tbody>
</table>

**Response Body:**

    {
       "userId":"kermit",
       "groupId":"sales",
        "url":"http://localhost:8182/identity/groups/sales/members/kermit"
    }
