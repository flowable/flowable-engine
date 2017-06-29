(function (window) {
    var baseUrl = '/designer/';
    window.__sfbEnv = window.__sfbEnv || {};
    // window.__sfbEnv.apiUrl = 'http://localhost:8080';
    window.__sfbEnv.baseUrl = baseUrl;
    window.__sfbEnv.formRest = "app/sf/forms";
    window.__sfbEnv.customComponentRest = "sf/components/custom";
    window.__sfbEnv.enableDebug = true;
    window.__sfbEnv.usedInFlowablUi = true;
    window.__sfbEnv.previewUrl = baseUrl + 'rdsFormPreviewer.jsp?formKey={{formKey}}&theme={{theme}}';
}(this));