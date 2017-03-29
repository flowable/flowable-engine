(function() {
  'use strict';

  flowableModule.factory('Base64', Base64);

  Base64.$inject = [ '$window' ];

  function Base64($window) {
    return {
      encode : function(input) {
        return $window.btoa(input);
      }
    };
  }
})();