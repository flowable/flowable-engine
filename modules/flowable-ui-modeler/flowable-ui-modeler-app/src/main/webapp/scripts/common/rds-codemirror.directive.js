(function() {
  'use strict';

  flowableModule.directive("rdsCodeMirror", rdsCodeMirror);

  rdsCodeMirror.$inject = [ '$http', '$timeout', '$log', 'cmhints' ];

  function rdsCodeMirror($http, $timeout, $log, cmhints) {
    var hints;

    cmhints.getHints().then(function(success) {
      hints = success.data;
    }, function(error) {
      $log.info(error);
      hints = {};
    });

    var wrapper = {
      restrict : 'A',
      link : link,
    };
    return wrapper;

    function link(scope, element, attrs) {

      $timeout(function() {

        var editor = CodeMirror.fromTextArea(element[0], {
          lineNumbers : true,
          extraKeys : {
            "Ctrl-Space" : "autocomplete"
          },
          hintOptions : {
            hint : rdsConditionHint
          }
        });

        editor.on('change', onChange);

        scope.$on('$destroy', function() {
          editor.off('change', onChange);
        });

        function onChange(cMirror) {
          scope.expression.staticValue = cMirror.getValue();
        }

      });
    }

    function rdsConditionHint(cm, option) {

      var cursor = cm.getCursor();
      var line = cm.getLine(cursor.line);
      var start = cursor.ch;
      var end = cursor.ch;
      while (start && /\w/.test(line.charAt(start - 1))) {
        --start;
      }
      while (end < line.length && /\w/.test(line.charAt(end))) {
        ++end;
      }

      var parentEnd = start;
      var parentStart = start;
      var stack = [];
      while (line.charAt(parentStart - 1) == '.') {
        parentEnd = parentStart - 1;
        parentStart = parentEnd;
        while (parentStart && /\w/.test(line.charAt(parentStart - 1))) {
          --parentStart;
        }
        var parent = line.slice(parentStart, parentEnd);
        stack.push(parent);
      }

      if (stack.length > 0) {
        var parentHints = hints;
        while (parent = stack.pop()) {
          parentHints = parentHints[parent];
        }

        return getResultHints(start, end, line, parentHints, cursor);

      } else {
        return getResultHints(start, end, line, hints, cursor);
      }
    }

    function getResultHints(start, end, line, narrowedHints, cursor) {
      var word = line.slice(start, end);
      var resultHints;
      if (!word) {
        resultHints = Object.keys(narrowedHints);
      } else {
        resultHints = Object.keys(narrowedHints).map(function(key) {
          if (key.startsWith(word))
            return key;
        }).filter(function(element) {
          return element !== undefined;
        });
      }

      return {
        list : resultHints,
        from : CodeMirror.Pos(cursor.line, start),
        to : CodeMirror.Pos(cursor.line, end)
      };
    }
  }

})();