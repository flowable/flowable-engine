---
id: header-rest
title: General Flowable REST principles
---

## Installation and Authentication

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

## Configuration

The Flowable REST web application uses Spring Java Configuration for starting the Flowable Form engine, defining the basic authentication security using Spring security, and to define the variable converters for specific variable handling.
A small number of properties can be defined by changing the flowable-app.properties file you can find in the WEB-INF/META-INF/flowable-app/flowable-app.properties folder.
If you need more advanced configuration options there’s the possibility to override the default Spring beans in XML in the flowable-custom-context.xml file you can also find in the WEB-INF/classes folder.
An example configuration is already in comments in this file. This is also the place to override the default RestResponseFactory by defining a new Spring bean with the name restResponsefactory and use your custom implementation class for it.

## Usage in Tomcat

Due to [default security properties on Tomcat](http://tomcat.apache.org/tomcat-8.0-doc/security-howto.html), **escaped forward slashes (%2F and %5C) are not allowed by default (400-result is returned).** This may have an impact on the deployment resources and their data-URL, as the URL can potentially contain escaped forward slashes.

When issues are experienced with unexpected 400-results, set the following system-property:

*-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW\_ENCODED\_SLASH=true*

It’s best practice to always set the **Accept** and **Content-Type** (in case of posting/putting JSON) headers to **application/json** on the HTTP requests described below.

## Methods and return-codes

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

## Error response body

When an error occurs (both client and server, 4XX and 5XX status-codes) the response body contains an object describing the error that occurred. An example for a 404-status when a task is not found:

    {
      "statusCode" : 404,
      "errorMessage" : "Could not find a task with id '444'."
    }

## Request parameters

### URL fragments

Parameters that are part of the URL (for example, the deploymentId parameter in http://host/flowable-rest/form-api/form-repository/deployments/{deploymentId})
need to be properly escaped (see [URL-encoding or Percent-encoding](https://en.wikipedia.org/wiki/Percent-encoding)) in case the segment contains special characters. Most frameworks have this functionality built in, but it should be taken into account. Especially for segments that can contain forward-slashes (for example, deployment resource), this is required.

### Rest URL query parameters

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

### JSON body parameters

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

### Paging and sorting

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

### JSON query variable format

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

### Variable representation

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
