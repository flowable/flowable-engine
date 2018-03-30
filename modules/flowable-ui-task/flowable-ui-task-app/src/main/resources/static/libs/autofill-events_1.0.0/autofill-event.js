/**
 * Autofill event polyfill ##version:1.0.0##
 * (c) 2014 Google, Inc.
 * License: MIT
 */
(function(window) {
  var $ = window.jQuery || window.angular.element;
  var rootElement = window.document.documentElement,
    $rootElement = $(rootElement);

  addGlobalEventListener('change', markValue);
  addValueChangeByJsListener(markValue);

  $.prototype.checkAndTriggerAutoFillEvent = jqCheckAndTriggerAutoFillEvent;

  // Need to use blur and not change event
  // as Chrome does not fire change events in all cases an input is changed
  // (e.g. when starting to type and then finish the input by auto filling a username)
  addGlobalEventListener('blur', function(target) {
    // setTimeout needed for Chrome as it fills other
    // form fields a little later...
    window.setTimeout(function() {
      findParentForm(target).find('input').checkAndTriggerAutoFillEvent();
    }, 20);
  });

  function DOMContentLoadedListener() {
      // mark all values that are present when the DOM is ready.
      // We don't need to trigger a change event here,
      // as js libs start with those values already being set!
      forEach(document.getElementsByTagName('input'), markValue);

      // The timeout is needed for Chrome as it auto fills
      // login forms some time after DOMContentLoaded!
      window.setTimeout(function() {
        $rootElement.find('input').checkAndTriggerAutoFillEvent();
      }, 200);
  }

  //IE8 compatibility issue
  if(!window.document.addEventListener){
    window.document.attachEvent('DOMContentLoaded', DOMContentLoadedListener);    
  }else{
    window.document.addEventListener('DOMContentLoaded', DOMContentLoadedListener, false);
  }

  return;

  // ----------

  function jqCheckAndTriggerAutoFillEvent() {
    var i, el;
    for (i=0; i<this.length; i++) {
      el = this[i];
      if (!valueMarked(el)) {
        markValue(el);
        triggerChangeEvent(el);
      }
    }
  }

  function valueMarked(el) {
    var val = el.value,
         $$currentValue = el.$$currentValue;
    if (!val && !$$currentValue) {
      return true;
    }
    return val === $$currentValue;
  }

  function markValue(el) {
    el.$$currentValue = el.value;
  }

  function addValueChangeByJsListener(listener) {
    var jq = window.jQuery || window.angular.element,
        jqProto = jq.prototype;
    var _val = jqProto.val;
    jqProto.val = function(newValue) {
      var res = _val.apply(this, arguments);
      if (arguments.length > 0) {
        forEach(this, function(el) {
          listener(el, newValue);
        });
      }
      return res;
    }
  }

  function addGlobalEventListener(eventName, listener) {
      // Use a capturing event listener so that
      // we also get the event when it's stopped!
      // Also, the blur event does not bubble.
      if(!rootElement.addEventListener){
          rootElement.attachEvent(eventName, onEvent);      
      }else{
          rootElement.addEventListener(eventName, onEvent, true);
      }

      function onEvent(event) {
          var target = event.target;
          listener(target);
      }
  }

  function findParentForm(el) {
    while (el) {
      if (el.nodeName === 'FORM') {
        return $(el);
      }
      el = el.parentNode;
    }
    return $();
  }

  function forEach(arr, listener) {
    if (arr.forEach) {
      return arr.forEach(listener);
    }
    var i;
    for (i=0; i<arr.length; i++) {
      listener(arr[i]);
    }
  }

  function triggerChangeEvent(element) {
    var doc = window.document;
    var event = doc.createEvent("HTMLEvents");
    event.initEvent("change", true, true);
    element.dispatchEvent(event);
  }

})(window);
