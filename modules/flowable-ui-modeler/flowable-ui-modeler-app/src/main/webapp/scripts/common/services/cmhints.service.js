(function() {
  'use strict';

  flowableModule.service('cmhints', cmhints);

  cmhints.$inject = [ '$http' ];

  function cmhints($http) {
    this.getHints = getHints;

    function getHints() {
      //var modelId = EDITOR.UTIL.getParameterByName('modelId');
      var modelId = window.location.hash.substring(9);
      return $http({
        method : 'GET',
        url : "app/rest/models/" + modelId + "/editorhints"
      });
    }
  }

})();