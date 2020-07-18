/**
 * angular-drag-and-drop-lists v1.2.0
 *
 * Copyright (c) 2014 Marcel Juenemann mail@marcel-junemann.de
 * https://github.com/marceljuenemann/angular-drag-and-drop-lists
 *
 * License: MIT
 */
angular.module('dndLists', [])

  /**
   * Use the dnd-draggable attribute to make your element draggable
   *
   * Attributes:
   * - dnd-draggable      Required attribute. The value has to be an object that represents the data
   *                      of the element. In case of a drag and drop operation the object will be
   *                      serialized and unserialized on the receiving end.
   * - dnd-selected       Callback that is invoked when the element was clicked but not dragged.
   *                      The original click event will be provided in the local event variable.
   * - dnd-effect-allowed Use this attribute to limit the operations that can be performed. Options:
   *                      - "move": The drag operation will move the element. This is the default.
   *                      - "copy": The drag operation will copy the element. Shows a copy cursor.
   *                      - "copyMove": The user can choose between copy and move by pressing the
   *                        ctrl or shift key. *Not supported in IE:* In Internet Explorer this
   *                        option will be the same as "copy". *Not fully supported in Chrome on
   *                        Windows:* In the Windows version of Chrome the cursor will always be the
   *                        move cursor. However, when the user drops an element and has the ctrl
   *                        key pressed, we will perform a copy anyways.
   *                      - HTML5 also specifies the "link" option, but this library does not
   *                        actively support it yet, so use it at your own risk.
   * - dnd-moved          Callback that is invoked when the element was moved. Usually you will
   *                      remove your element from the original list in this callback, since the
   *                      directive is not doing that for you automatically. The original dragend
   *                      event will be provided in the local event variable.
   * - dnd-copied         Same as dnd-moved, just that it is called when the element was copied
   *                      instead of moved. The original dragend event will be provided in the local
   *                      event variable.
   * - dnd-dragstart      Callback that is invoked when the element was dragged. The original
   *                      dragstart event will be provided in the local event variable.
   * - dnd-type           Use this attribute if you have different kinds of items in your
   *                      application and you want to limit which items can be dropped into which
   *                      lists. Combine with dnd-allowed-types on the dnd-list(s). This attribute
   *                      should evaluate to a string, although this restriction is not enforced.
   * - dnd-disable-if     You can use this attribute to dynamically disable the draggability of the
   *                      element. This is useful if you have certain list items that you don't want
   *                      to be draggable, or if you want to disable drag & drop completely without
   *                      having two different code branches (e.g. only allow for admins).
   *                      **Note**: If your element is not draggable, the user is probably able to
   *                      select text or images inside of it. Since a selection is always draggable,
   *                      this breaks your UI. You most likely want to disable user selection via
   *                      CSS (see user-select).
   *
   * CSS classes:
   * - dndDragging        This class will be added to the element while the element is being
   *                      dragged. It will affect both the element you see while dragging and the
   *                      source element that stays at it's position. Do not try to hide the source
   *                      element with this class, because that will abort the drag operation.
   * - dndDraggingSource  This class will be added to the element after the drag operation was
   *                      started, meaning it only affects the original element that is still at
   *                      it's source position, and not the "element" that the user is dragging with
   *                      his mouse pointer.
   */
  .directive('dndDraggable', ['$parse', '$timeout', 'dndDropEffectWorkaround', 'dndDragTypeWorkaround',
                      function($parse,   $timeout,   dndDropEffectWorkaround,   dndDragTypeWorkaround) {
    return function(scope, element, attr) {
      // Set the HTML5 draggable attribute on the element
      element.attr("draggable", "true");

      // If the dnd-disable-if attribute is set, we have to watch that
      if (attr.dndDisableIf) {
        scope.$watch(attr.dndDisableIf, function(disabled) {
          element.attr("draggable", !disabled);
        });
      }

      /**
       * When the drag operation is started we have to prepare the dataTransfer object,
       * which is the primary way we communicate with the target element
       */
      element.on('dragstart', function(event) {
        event = event.originalEvent || event;

        // Serialize the data associated with this element. IE only supports the Text drag type
        event.dataTransfer.setData("Text", angular.toJson(scope.$eval(attr.dndDraggable)));

        // Only allow actions specified in dnd-effect-allowed attribute
        event.dataTransfer.effectAllowed = attr.dndEffectAllowed || "move";

        // Add CSS classes. See documentation above
        element.addClass("dndDragging");

        // Workarounds for stupid browsers, see description below
        dndDropEffectWorkaround.dropEffect = "none";
        dndDragTypeWorkaround.isDragging = true;

        // <FLOWABLE>
        // This code was originally placed after element.addClass.
        // This timeout is invoked after the 'dragend' event in IE9 (at leats on slooow virtual box)
        // and since this class is used to hide elements it seems like the element is gone,
        // therefor make sure the dragging still is happening when adding this class
        $timeout(function() {
          // Flowable This code
          if (dndDragTypeWorkaround.isDragging) {
            element.addClass("dndDraggingSource");
          }
        }, 0);
        // </FLOWABLE>

        // Save type of item in global state. Usually, this would go into the dataTransfer
        // typename, but we have to use "Text" there to support IE
        dndDragTypeWorkaround.dragType = attr.dndType ? scope.$eval(attr.dndType) : undefined;

        // Invoke callback
        $parse(attr.dndDragstart)(scope, {event: event});

        event.stopPropagation();
      });

      /**
       * The dragend event is triggered when the element was dropped or when the drag
       * operation was aborted (e.g. hit escape button). Depending on the executed action
       * we will invoke the callbacks specified with the dnd-moved or dnd-copied attribute.
       */
      element.on('dragend', function(event) {
        event = event.originalEvent || event;

        // Invoke callbacks. Usually we would use event.dataTransfer.dropEffect to determine
        // the used effect, but Chrome has not implemented that field correctly. On Windows
        // it always sets it to 'none', while Chrome on Linux sometimes sets it to something
        // else when it's supposed to send 'none' (drag operation aborted).
        var dropEffect = dndDropEffectWorkaround.dropEffect;
        scope.$apply(function() {
          switch (dropEffect) {
            case "move":
              $parse(attr.dndMoved)(scope, {event: event});
              break;

            case "copy":
              $parse(attr.dndCopied)(scope, {event: event});
              break;
          }
        });

        // Clean up
        element.removeClass("dndDragging");
        element.removeClass("dndDraggingSource");
        dndDragTypeWorkaround.isDragging = false;
        event.stopPropagation();
      });

      /**
       * When the element is clicked we invoke the callback function
       * specified with the dnd-selected attribute.
       */
      element.on('click', function(event) {
        event = event.originalEvent || event;

        scope.$apply(function() {
          $parse(attr.dndSelected)(scope, {event: event});
        });

        event.stopPropagation();
      });

      /**
       * Workaround to make element draggable in IE9
       */
      element.on('selectstart', function() {
        if (this.dragDrop) this.dragDrop();
        return false;
      });
    };
  }])

  /**
   * Use the dnd-list attribute to make your list element a dropzone. Usually you will add a single
   * li element as child with the ng-repeat directive. If you don't do that, we will not be able to
   * position the dropped element correctly. If you want your list to be sortable, also add the
   * dnd-draggable directive to your li element(s). Both the dnd-list and it's direct children must
   * have position: relative CSS style, otherwise the positioning algorithm will not be able to
   * determine the correct placeholder position in all browsers.
   *
   * Attributes:
   * - dnd-list             Required attribute. The value has to be the array in which the data of
   *                        the dropped element should be inserted.
   * - dnd-allowed-types    Optional array of allowed item types. When used, only items that had a
   *                        matching dnd-type attribute will be dropable.
   * - dnd-disable-if       Optional boolean expresssion. When it evaluates to true, no dropping
   *                        into the list is possible. Note that this also disables rearranging
   *                        items inside the list.
   * - dnd-horizontal-list  Optional boolean expresssion. When it evaluates to true, the positioning
   *                        algorithm will use the left and right halfs of the list items instead of
   *                        the upper and lower halfs.
   * - dnd-dragover         Optional expression that is invoked when an element is dragged over the
   *                        list. If the expression is set, but does not return true, the element is
   *                        not allowed to be dropped. The following variables will be available:
   *                        - event: The original dragover event sent by the browser.
   *                        - index: The position in the list at which the element would be dropped.
   *                        - type: The dnd-type set on the dnd-draggable, or undefined if unset.
   * - dnd-drop             Optional expression that is invoked when an element is dropped over the
   *                        list. If the expression is set, it must return the object that will be
   *                        inserted into the list. If it returns false, the drop will be aborted
   *                        and the event is propagated. The following variables will be available:
   *                        - event: The original drop event sent by the browser.
   *                        - index: The position in the list at which the element would be dropped.
   *                        - item: The transferred object.
   *                        - type: The dnd-type set on the dnd-draggable, or undefined if unset.
   * - dnd-external-sources Optional boolean expression. When it evaluates to true, the list accepts
   *                        drops from sources outside of the current browser tab. This allows to
   *                        drag and drop accross different browser tabs. Note that this will allow
   *                        to drop arbitrary text into the list, thus it is highly recommended to
   *                        implement the dnd-drop callback to check the incoming element for
   *                        sanity. Furthermore, the dnd-type of external sources can not be
   *                        determined, therefore do not rely on restrictions of dnd-allowed-type.
   *
   * CSS classes:
   * - dndPlaceholder       When an element is dragged over the list, a new placeholder child
   *                        element will be added. This element is of type li and has the class
   *                        dndPlaceholder set.
   * - dndDragover          Will be added to the list while an element is dragged over the list.
   */
  .directive('dndList', ['$parse', '$timeout', 'dndDropEffectWorkaround', 'dndDragTypeWorkaround',
                 function($parse,   $timeout,   dndDropEffectWorkaround,   dndDragTypeWorkaround) {
    return function(scope, element, attr) {
      // While an element is dragged over the list, this placeholder element is inserted
      // at the location where the element would be inserted after dropping
      var placeholder = angular.element("<li class='dndPlaceholder'></li>");
      var placeholderNode = placeholder[0];
      var listNode = element[0];

      var horizontal = attr.dndHorizontalList && scope.$eval(attr.dndHorizontalList);
      var externalSources = attr.dndExternalSources && scope.$eval(attr.dndExternalSources);

      /**
       * The dragover event is triggered "every few hundred milliseconds" while an element
       * is being dragged over our list, or over an child element.
       */
      element.on('dragover', function(event) {
        event = event.originalEvent || event;

        if (!isDropAllowed(event)) {
            return true;
        }

        // First of all, make sure that the placeholder is shown
        // This is especially important if the list is empty
        if (placeholderNode.parentNode != listNode) {
          element.append(placeholder);
        }

        if (event.target !== listNode) {
          // Try to find the node direct directly below the list node.
          var listItemNode = event.target;
          while (listItemNode.parentNode !== listNode && listItemNode.parentNode) {
            listItemNode = listItemNode.parentNode;
          }

          if (listItemNode.parentNode === listNode && listItemNode !== placeholderNode) {
            // If the mouse pointer is in the upper half of the child element,
            // we place it before the child element, otherwise below it.
            if (isMouseInFirstHalf(event, listItemNode)) {
              listNode.insertBefore(placeholderNode, listItemNode);
            } else {
              listNode.insertBefore(placeholderNode, listItemNode.nextSibling);
            }
          }
        } else {
          // This branch is reached when we are dragging directly over the list element.
          // Usually we wouldn't need to do anything here, but the IE does not fire it's
          // events for the child element, only for the list directly. Therefore we repeat
          // the positioning algorithm for IE here.
          if (isMouseInFirstHalf(event, placeholderNode, true)) {
            // Check if we should move the placeholder element one spot towards the top.
            // Note that display none elements will have offsetTop and offsetHeight set to
            // zero, therefore we need a special check for them.
            while (placeholderNode.previousElementSibling
                 && (isMouseInFirstHalf(event, placeholderNode.previousElementSibling, true)
                 || placeholderNode.previousElementSibling.offsetHeight === 0)) {
              listNode.insertBefore(placeholderNode, placeholderNode.previousElementSibling);
            }
          } else {
            // Check if we should move the placeholder element one spot towards the bottom
            while (placeholderNode.nextElementSibling &&
                 !isMouseInFirstHalf(event, placeholderNode.nextElementSibling, true)) {
              listNode.insertBefore(placeholderNode,
                  placeholderNode.nextElementSibling.nextElementSibling);
            }
          }
        }

        // At this point we invoke the callback, which still can disallow the drop.
        // We can't do this earlier because we want to pass the index of the placeholder.
        if (attr.dndDragover && !invokeCallback(attr.dndDragover, event)) {
          return stopDragover();
        }

        element.addClass("dndDragover");
        event.preventDefault();
        event.stopPropagation();
        return false;
      });

      /**
       * When the element is dropped, we use the position of the placeholder element as the
       * position where we insert the transferred data. This assumes that the list has exactly
       * one child element per array element.
       */
      element.on('drop', function(event) {
        event = event.originalEvent || event;

        if (!isDropAllowed(event)) return true;

        // The default behavior in Firefox is to interpret the dropped element as URL and
        // forward to it. We want to prevent that even if our drop is aborted.
        event.preventDefault();

        // Unserialize the data that was serialized in dragstart. According to the HTML5 specs,
        // the "Text" drag type will be converted to text/plain, but IE does not do that.
        var data = event.dataTransfer.getData("Text") || event.dataTransfer.getData("text/plain");
        var transferredObject;
        try {
          transferredObject = JSON.parse(data);
        } catch(e) {
          return stopDragover();
        }

        // Invoke the callback, which can transform the transferredObject and even abort the drop.
        if (attr.dndDrop) {
          transferredObject = invokeCallback(attr.dndDrop, event, transferredObject);
          if (!transferredObject) {
            return stopDragover();
          }
        }

        // Retrieve the JSON array and insert the transferred object into it.
        var targetArray = scope.$eval(attr.dndList);
        scope.$apply(function() {
          targetArray.splice(getPlaceholderIndex(), 0, transferredObject);
        });

        // Invoke the callback, after the transfered objrct is added to the new container.
        if (attr.dndAfterDrop) {
           invokeCallback(attr.dndAfterDrop, event, transferredObject);
        }
        // In Chrome on Windows the dropEffect will always be none...
        // We have to determine the actual effect manually from the allowed effects
        if (event.dataTransfer.dropEffect === "none") {
          if (event.dataTransfer.effectAllowed === "copy" ||
              event.dataTransfer.effectAllowed === "move") {
            dndDropEffectWorkaround.dropEffect = event.dataTransfer.effectAllowed;
          } else {
            dndDropEffectWorkaround.dropEffect = event.ctrlKey ? "copy" : "move";
          }
        } else {
          dndDropEffectWorkaround.dropEffect = event.dataTransfer.dropEffect;
        }

        // Clean up
        stopDragover();
        event.stopPropagation();
        return false;
      });

      /**
       * We have to remove the placeholder when the element is no longer dragged over our list. The
       * problem is that the dragleave event is not only fired when the element leaves our list,
       * but also when it leaves a child element -- so practically it's fired all the time. As a
       * workaround we wait a few milliseconds and then check if the dndDragover class was added
       * again. If it is there, dragover must have been called in the meantime, i.e. the element
       * is still dragging over the list. If you know a better way of doing this, please tell me!
       */
      element.on('dragleave', function(event) {
        event = event.originalEvent || event;

        element.removeClass("dndDragover");
        $timeout(function() {
          if (!element.hasClass("dndDragover")) {
            placeholder.remove();
          }
        }, 100);
      });

      /**
       * Checks whether the mouse pointer is in the first half of the given target element.
       *
       * In Chrome we can just use offsetY, but in Firefox we have to use layerY, which only
       * works if the child element has position relative. In IE the events are only triggered
       * on the listNode instead of the listNodeItem, therefore the mouse positions are
       * relative to the parent element of targetNode.
       */
      function isMouseInFirstHalf(event, targetNode, relativeToParent) {
        var mousePointer = horizontal ? (event.offsetX || event.layerX)
                                      : (event.offsetY || event.layerY);
        var targetSize = horizontal ? targetNode.offsetWidth : targetNode.offsetHeight;
        var targetPosition = horizontal ? targetNode.offsetLeft : targetNode.offsetTop;
        targetPosition = relativeToParent ? targetPosition : 0;
        return mousePointer < targetPosition + targetSize / 2;
      }

        /**
         * Flowable-patched version of isMouseInFirstHalf that uses page and bounding client rect
         * instead of the offsetX and layerX properties to determine which half of target the mouse pointer is hovering
         * this is more natural since th method now actually works like above, but it also adds some mild
         * flickering when sorting inside the list, why its still not in use.
         */
        function isMouseInFirstHalf_new(event, targetNode, relativeToParent) {
            var targetNodeRect = targetNode.getBoundingClientRect();
            if (horizontal) {
                return (event.pageX - targetNodeRect.left) < (targetNodeRect.width / 2);
            }
            else {
                return (event.pageY - targetNodeRect.top) < (targetNodeRect.height / 2);
            }
        }
      /**
       * We use the position of the placeholder node to determine at which position of the array the
       * object needs to be inserted
       */
      function getPlaceholderIndex() {
        return Array.prototype.indexOf.call(listNode.children, placeholderNode);
      }

      /**
       * Checks various conditions that must be fulfilled for a drop to be allowed
       */
      function isDropAllowed(event) {
        // Disallow drop from external source unless it's allowed explicitly.
        if (!dndDragTypeWorkaround.isDragging && !externalSources) return false;

        // Check mimetype. Usually we would use a custom drag type instead of Text, but IE doesn't
        // support that.
        if (!hasTextMimetype(event.dataTransfer.types)) return false;

        // Now check the dnd-allowed-types against the type of the incoming element. For drops from
        // external sources we don't know the type, so it will need to be checked via dnd-drop.
        if (attr.dndAllowedTypes && dndDragTypeWorkaround.isDragging) {
          var allowed = scope.$eval(attr.dndAllowedTypes);
          if (angular.isArray(allowed) && allowed.indexOf(dndDragTypeWorkaround.dragType) === -1) {
            return false;
          }
        }

        // Check whether droping is disabled completely
        if (attr.dndDisableIf && scope.$eval(attr.dndDisableIf)) return false;

        return true;
      }

      /**
       * Small helper function that cleans up if we aborted a drop.
       */
      function stopDragover() {
        placeholder.remove();
        element.removeClass("dndDragover");
        return true;
      }

      /**
       * Invokes a callback with some interesting parameters and returns the callbacks return value.
       */
      function invokeCallback(expression, event, item) {
        return $parse(expression)(scope, {
          event: event,
          index: getPlaceholderIndex(),
          item: item || undefined,
          external: !dndDragTypeWorkaround.isDragging,
          type: dndDragTypeWorkaround.isDragging ? dndDragTypeWorkaround.dragType : undefined
        });
      }

      /**
       * Check if the dataTransfer object contains a drag type that we can handle. In old versions
       * of IE the types collection will not even be there, so we just assume a drop is possible.
       */
      function hasTextMimetype(types) {
        if (!types) return true;
        for (var i = 0; i < types.length; i++) {
          if (types[i] === "Text" || types[i] === "text/plain") return true;
        }

        return false;
      }
    };
  }])

  /**
   * This workaround handles the fact that Internet Explorer does not support drag types other than
   * "Text" and "URL". That means we can not know whether the data comes from one of our elements or
   * is just some other data like a text selection. As a workaround we save the isDragging flag in
   * here. When a dropover event occurs, we only allow the drop if we are already dragging, because
   * that means the element is ours.
   */
  .factory('dndDragTypeWorkaround', function(){ return {} })

  /**
   * Chrome on Windows does not set the dropEffect field, which we need in dragend to determine
   * whether a drag operation was successful. Therefore we have to maintain it in this global
   * variable. The bug report for that has been open for years:
   * https://code.google.com/p/chromium/issues/detail?id=39399
   */
  .factory('dndDropEffectWorkaround', function(){ return {} });
