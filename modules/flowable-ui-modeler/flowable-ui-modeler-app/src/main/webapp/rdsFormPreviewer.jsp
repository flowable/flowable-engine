<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="en-AU" version="2.0">

<head>
    <meta content="text/html; charset=UTF-8" http-equiv="Content-Type" />
    <meta content="no-cache" http-equiv="Cache-Control" />
    <meta content="no-cache" http-equiv="Pragma" />
    <meta content="0" http-equiv="Expires" />
    <title>RDS Designer</title>
    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport" />
    <base href="<%=request.getContextPath()%>/" />
    <link href="assets/styles/bootstrap-grid.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/bootstrap-${param['theme']}.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/bootstrap-decorator.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/select.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/bootstrap.vertical-tabs.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/schema-form-file.css" media="all" type="text/css" rel="stylesheet" />
    <link href="assets/styles/rds-schemaform-runtime-vendor.css" media="all" type="text/css" rel="stylesheet" />
</head>

<body>
<div class="designer-frame">
    <div class="page-body">
        <div version="2.0">
            <div ng-app="rds-schemaform-runtime">
                <form class="form">
                    <form-runtime form-design-id="${param['formKey']}" />
                </form>
            </div>
        </div>
    </div>
    <div class="footer-container">

    </div>
</div>
</body>
<script src="libs/html2canvas_0.4.1/html2canvas.js"></script>
<script src="js/sfb-config.js"></script>
<script src="rds-schemaform-runtime-vendor.js"></script>
<script src="rds-schemaform-runtime.js"></script>

</html>
