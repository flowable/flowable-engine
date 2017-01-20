(function(angular) {

    'use strict';

    angular.module('gridster', [])

        .constant('gridsterConfig', {
            columns: 6, // number of columns in the grid
            pushing: true, // whether to push other items out of the way
            floating: true, // whether to automatically float items up so they stack
            swapping: false, // whether or not to have items switch places instead of push down if they are the same size
            width: 'auto', // width of the grid. "auto" will expand the grid to its parent container
            colWidth: 'auto', // width of grid columns. "auto" will divide the width of the grid evenly among the columns
            rowHeight: 'match', // height of grid rows. 'match' will make it the same as the column width, a numeric value will be interpreted as pixels, '/2' is half the column width, '*5' is five times the column width, etc.
            margins: [10, 10], // margins in between grid items
            outerMargin: true,
            isMobile: false, // toggle mobile view
            mobileBreakPoint: 600, // width threshold to toggle mobile mode
            mobileModeEnabled: true, // whether or not to toggle mobile mode when screen width is less than mobileBreakPoint
            minColumns: 1, // minimum amount of columns the grid can scale down to
            minRows: 1, // minimum amount of rows to show if the grid is empty
            maxRows: 100, // maximum amount of rows in the grid
            defaultSizeX: 2, // default width of an item in columns
            defaultSizeY: 1, // default height of an item in rows
            minSizeX: 1, // minimum column width of an item
            maxSizeX: null, // maximum column width of an item
            minSizeY: 1, // minumum row height of an item
            maxSizeY: null, // maximum row height of an item
            saveGridItemCalculatedHeightInMobile: false, // grid item height in mobile display. true- to use the calculated height by sizeY given
            resizable: { // options to pass to resizable handler
                enabled: true,
                handles: ['s', 'e', 'n', 'w', 'se', 'ne', 'sw', 'nw']
            },
            draggable: { // options to pass to draggable handler
                enabled: true,
                scrollSensitivity: 20, // Distance in pixels from the edge of the viewport after which the viewport should scroll, relative to pointer
                scrollSpeed: 15 // Speed at which the window should scroll once the mouse pointer gets within scrollSensitivity distance
            }
        })

        .controller('GridsterCtrl', ['gridsterConfig', '$timeout',
            function(gridsterConfig, $timeout) {

                var gridster = this;

                /**
                 * Create options from gridsterConfig constant
                 */
                angular.extend(this, gridsterConfig);

                this.resizable = angular.extend({}, gridsterConfig.resizable || {});
                this.draggable = angular.extend({}, gridsterConfig.draggable || {});

                var flag = false;
                this.layoutChanged = function() {
                    if (flag) {
                        return;
                    }
                    flag = true;
                    $timeout(function() {
                        flag = false;
                        if (gridster.loaded) {
                            gridster.floatItemsUp();
                        }
                        gridster.updateHeight(gridster.movingItem ? gridster.movingItem.sizeY : 0);
                    });
                };

                /**
                 * A positional array of the items in the grid
                 */
                this.grid = [];

                /**
                 * Clean up after yourself
                 */
                this.destroy = function() {
                    if (this.grid) {
                        this.grid.length = 0;
                        this.grid = null;
                    }
                };

                /**
                 * Overrides default options
                 *
                 * @param {object} options The options to override
                 */
                this.setOptions = function(options) {
                    if (!options) {
                        return;
                    }

                    options = angular.extend({}, options);

                    // all this to avoid using jQuery...
                    if (options.draggable) {
                        angular.extend(this.draggable, options.draggable);
                        delete(options.draggable);
                    }
                    if (options.resizable) {
                        angular.extend(this.resizable, options.resizable);
                        delete(options.resizable);
                    }

                    angular.extend(this, options);

                    if (!this.margins || this.margins.length !== 2) {
                        this.margins = [0, 0];
                    } else {
                        for (var x = 0, l = this.margins.length; x < l; ++x) {
                            this.margins[x] = parseInt(this.margins[x], 10);
                            if (isNaN(this.margins[x])) {
                                this.margins[x] = 0;
                            }
                        }
                    }
                };

                /**
                 * Check if item can occupy a specified position in the grid
                 *
                 * @param {object} item The item in question
                 * @param {number} row The row index
                 * @param {number} column The column index
                 * @returns {boolean} True if if item fits
                 */
                this.canItemOccupy = function(item, row, column) {
                    return row > -1 && column > -1 && item.sizeX + column <= this.columns && item.sizeY + row <= this.maxRows;
                };

                /**
                 * Set the item in the first suitable position
                 *
                 * @param {object} item The item to insert
                 */
                this.autoSetItemPosition = function(item) {
                    // walk through each row and column looking for a place it will fit
                    for (var rowIndex = 0; rowIndex < this.maxRows; ++rowIndex) {
                        for (var colIndex = 0; colIndex < this.columns; ++colIndex) {
                            // only insert if position is not already taken and it can fit
                            var items = this.getItems(rowIndex, colIndex, item.sizeX, item.sizeY, item);
                            if (items.length === 0 && this.canItemOccupy(item, rowIndex, colIndex)) {
                                this.putItem(item, rowIndex, colIndex);
                                return;
                            }
                        }
                    }
                    throw new Error('Unable to place item!');
                };

                /**
                 * Gets items at a specific coordinate
                 *
                 * @param {number} row
                 * @param {number} column
                 * @param {number} sizeX
                 * @param {number} sizeY
                 * @param {array} excludeItems An array of items to exclude from selection
                 * @returns {array} Items that match the criteria
                 */
                this.getItems = function(row, column, sizeX, sizeY, excludeItems) {
                    var items = [];
                    if (!sizeX || !sizeY) {
                        sizeX = sizeY = 1;
                    }
                    if (excludeItems && !(excludeItems instanceof Array)) {
                        excludeItems = [excludeItems];
                    }
                    for (var h = 0; h < sizeY; ++h) {
                        for (var w = 0; w < sizeX; ++w) {
                            var item = this.getItem(row + h, column + w, excludeItems);
                            if (item && (!excludeItems || excludeItems.indexOf(item) === -1) && items.indexOf(item) === -1) {
                                items.push(item);
                            }
                        }
                    }
                    return items;
                };

                this.getBoundingBox = function(items) {

                    if (items.length === 0) {
                        return null;
                    }
                    if (items.length === 1) {
                        return {
                            row: items[0].row,
                            col: items[0].col,
                            sizeY: items[0].sizeY,
                            sizeX: items[0].sizeX
                        };
                    }

                    var maxRow = 0;
                    var maxCol = 0;
                    var minRow = 9999;
                    var minCol = 9999;

                    for (var i = 0, l = items.length; i < l; ++i) {
                        var item = items[i];
                        minRow = Math.min(item.row, minRow);
                        minCol = Math.min(item.col, minCol);
                        maxRow = Math.max(item.row + item.sizeY, maxRow);
                        maxCol = Math.max(item.col + item.sizeX, maxCol);
                    }

                    return {
                        row: minRow,
                        col: minCol,
                        sizeY: maxRow - minRow,
                        sizeX: maxCol - minCol
                    };
                };


                /**
                 * Removes an item from the grid
                 *
                 * @param {object} item
                 */
                this.removeItem = function(item) {
                    for (var rowIndex = 0, l = this.grid.length; rowIndex < l; ++rowIndex) {
                        var columns = this.grid[rowIndex];
                        if (!columns) {
                            continue;
                        }
                        var index = columns.indexOf(item);
                        if (index !== -1) {
                            columns[index] = null;
                            break;
                        }
                    }
                    this.layoutChanged();
                };

                /**
                 * Returns the item at a specified coordinate
                 *
                 * @param {number} row
                 * @param {number} column
                 * @param {array} excludeitems Items to exclude from selection
                 * @returns {object} The matched item or null
                 */
                this.getItem = function(row, column, excludeItems) {
                    if (excludeItems && !(excludeItems instanceof Array)) {
                        excludeItems = [excludeItems];
                    }
                    var sizeY = 1;
                    while (row > -1) {
                        var sizeX = 1,
                            col = column;
                        while (col > -1) {
                            var items = this.grid[row];
                            if (items) {
                                var item = items[col];
                                if (item && (!excludeItems || excludeItems.indexOf(item) === -1) && item.sizeX >= sizeX && item.sizeY >= sizeY) {
                                    return item;
                                }
                            }
                            ++sizeX;
                            --col;
                        }
                        --row;
                        ++sizeY;
                    }
                    return null;
                };

                /**
                 * Insert an array of items into the grid
                 *
                 * @param {array} items An array of items to insert
                 */
                this.putItems = function(items) {
                    for (var i = 0, l = items.length; i < l; ++i) {
                        this.putItem(items[i]);
                    }
                };

                /**
                 * Insert a single item into the grid
                 *
                 * @param {object} item The item to insert
                 * @param {number} row (Optional) Specifies the items row index
                 * @param {number} column (Optional) Specifies the items column index
                 * @param {array} ignoreItems
                 */
                this.putItem = function(item, row, column, ignoreItems) {
                    if (typeof row === 'undefined' || row === null) {
                        row = item.row;
                        column = item.col;
                        if (typeof row === 'undefined' || row === null) {
                            this.autoSetItemPosition(item);
                            return;
                        }
                    }
                    if (!this.canItemOccupy(item, row, column)) {
                        column = Math.min(this.columns - item.sizeX, Math.max(0, column));
                        row = Math.min(this.maxRows - item.sizeY, Math.max(0, row));
                    }

                    if (item.oldRow !== null && typeof item.oldRow !== 'undefined') {
                        var samePosition = item.oldRow === row && item.oldColumn === column;
                        var inGrid = this.grid[row] && this.grid[row][column] === item;
                        if (samePosition && inGrid) {
                            item.row = row;
                            item.col = column;
                            return;
                        } else {
                            // remove from old position
                            var oldRow = this.grid[item.oldRow];
                            if (oldRow && oldRow[item.oldColumn] === item) {
                                delete oldRow[item.oldColumn];
                            }
                        }
                    }

                    item.oldRow = item.row = row;
                    item.oldColumn = item.col = column;

                    this.moveOverlappingItems(item, ignoreItems);

                    if (!this.grid[row]) {
                        this.grid[row] = [];
                    }
                    this.grid[row][column] = item;

                    if (this.movingItem === item) {
                        this.floatItemUp(item);
                    }
                    this.layoutChanged();
                };

                /**
                 * Trade row and column if item1 with item2
                 *
                 * @param {object} item1
                 * @param {object} item2
                 */
                this.swapItems = function(item1, item2) {
                    this.grid[item1.row][item1.col] = item2;
                    this.grid[item2.row][item2.col] = item1;

                    var item1Row = item1.row;
                    var item1Col = item1.col;
                    item1.row = item2.row;
                    item1.col = item2.col;
                    item2.row = item1Row;
                    item2.col = item1Col;
                };

                /**
                 * Prevents items from being overlapped
                 *
                 * @param {object} item The item that should remain
                 * @param {array} ignoreItems
                 */
                this.moveOverlappingItems = function(item, ignoreItems) {
                    if (ignoreItems) {
                        if (ignoreItems.indexOf(item) === -1) {
                            ignoreItems = ignoreItems.slice(0);
                            ignoreItems.push(item);
                        }
                    } else {
                        ignoreItems = [item];
                    }
                    var overlappingItems = this.getItems(
                        item.row,
                        item.col,
                        item.sizeX,
                        item.sizeY,
                        ignoreItems
                    );
                    this.moveItemsDown(overlappingItems, item.row + item.sizeY, ignoreItems);
                };

                /**
                 * Moves an array of items to a specified row
                 *
                 * @param {array} items The items to move
                 * @param {number} newRow The target row
                 * @param {array} ignoreItems
                 */
                this.moveItemsDown = function(items, newRow, ignoreItems) {
                    if (!items || items.length === 0) {
                        return;
                    }
                    items.sort(function(a, b) {
                        return a.row - b.row;
                    });
                    ignoreItems = ignoreItems ? ignoreItems.slice(0) : [];
                    var topRows = {},
                        item, i, l;
                    // calculate the top rows in each column
                    for (i = 0, l = items.length; i < l; ++i) {
                        item = items[i];
                        var topRow = topRows[item.col];
                        if (typeof topRow === 'undefined' || item.row < topRow) {
                            topRows[item.col] = item.row;
                        }
                    }
                    // move each item down from the top row in its column to the row
                    for (i = 0, l = items.length; i < l; ++i) {
                        item = items[i];
                        var rowsToMove = newRow - topRows[item.col];
                        this.moveItemDown(item, item.row + rowsToMove, ignoreItems);
                        ignoreItems.push(item);
                    }
                };

                this.moveItemDown = function(item, newRow, ignoreItems) {
                    if (item.row >= newRow) {
                        return;
                    }
                    while (item.row < newRow) {
                        ++item.row;
                        this.moveOverlappingItems(item, ignoreItems);
                    }
                    this.putItem(item, item.row, item.col, ignoreItems);
                };

                /**
                 * Moves all items up as much as possible
                 */
                this.floatItemsUp = function() {
                    if (this.floating === false) {
                        return;
                    }
                    for (var rowIndex = 0, l = this.grid.length; rowIndex < l; ++rowIndex) {
                        var columns = this.grid[rowIndex];
                        if (!columns) {
                            continue;
                        }
                        for (var colIndex = 0, len = columns.length; colIndex < len; ++colIndex) {
                            var item = columns[colIndex];
                            if (item) {
                                this.floatItemUp(item);
                            }
                        }
                    }
                };

                /**
                 * Float an item up to the most suitable row
                 *
                 * @param {object} item The item to move
                 */
                this.floatItemUp = function(item) {
                    if (this.floating === false) {
                        return;
                    }
                    var colIndex = item.col,
                        sizeY = item.sizeY,
                        sizeX = item.sizeX,
                        bestRow = null,
                        bestColumn = null,
                        rowIndex = item.row - 1;

                    while (rowIndex > -1) {
                        var items = this.getItems(rowIndex, colIndex, sizeX, sizeY, item);
                        if (items.length !== 0) {
                            break;
                        }
                        bestRow = rowIndex;
                        bestColumn = colIndex;
                        --rowIndex;
                    }
                    if (bestRow !== null) {
                        this.putItem(item, bestRow, bestColumn);
                    }
                };

                /**
                 * Update gridsters height
                 *
                 * @param {number} plus (Optional) Additional height to add
                 */
                this.updateHeight = function(plus) {
                    var maxHeight = this.minRows;
                    plus = plus || 0;
                    for (var rowIndex = this.grid.length; rowIndex >= 0; --rowIndex) {
                        var columns = this.grid[rowIndex];
                        if (!columns) {
                            continue;
                        }
                        for (var colIndex = 0, len = columns.length; colIndex < len; ++colIndex) {
                            if (columns[colIndex]) {
                                maxHeight = Math.max(maxHeight, rowIndex + plus + columns[colIndex].sizeY);
                            }
                        }
                    }
                    this.gridHeight = this.maxRows - maxHeight > 0 ? Math.min(this.maxRows, maxHeight) : Math.max(this.maxRows, maxHeight);
                };

                /**
                 * Returns the number of rows that will fit in given amount of pixels
                 *
                 * @param {number} pixels
                 * @param {boolean} ceilOrFloor (Optional) Determines rounding method
                 */
                this.pixelsToRows = function(pixels, ceilOrFloor) {
                    if (ceilOrFloor === true) {
                        return Math.ceil(pixels / this.curRowHeight);
                    } else if (ceilOrFloor === false) {
                        return Math.floor(pixels / this.curRowHeight);
                    }

                    return Math.round(pixels / this.curRowHeight);
                };

                /**
                 * Returns the number of columns that will fit in a given amount of pixels
                 *
                 * @param {number} pixels
                 * @param {boolean} ceilOrFloor (Optional) Determines rounding method
                 * @returns {number} The number of columns
                 */
                this.pixelsToColumns = function(pixels, ceilOrFloor) {
                    if (ceilOrFloor === true) {
                        return Math.ceil(pixels / this.curColWidth);
                    } else if (ceilOrFloor === false) {
                        return Math.floor(pixels / this.curColWidth);
                    }

                    return Math.round(pixels / this.curColWidth);
                };

                // unified input handling
                // adopted from a msdn blogs sample
                this.unifiedInput = function(target, startEvent, moveEvent, endEvent) {
                    var lastXYById = {};

                    //  Opera doesn't have Object.keys so we use this wrapper
                    var numberOfKeys = function(theObject) {
                        if (Object.keys) {
                            return Object.keys(theObject).length;
                        }

                        var n = 0,
                            key;
                        for (key in theObject) {
                            ++n;
                        }

                        return n;
                    };

                    //  this calculates the delta needed to convert pageX/Y to offsetX/Y because offsetX/Y don't exist in the TouchEvent object or in Firefox's MouseEvent object
                    var computeDocumentToElementDelta = function(theElement) {
                        var elementLeft = 0;
                        var elementTop = 0;
                        var oldIEUserAgent = navigator.userAgent.match(/\bMSIE\b/);

                        for (var offsetElement = theElement; offsetElement != null; offsetElement = offsetElement.offsetParent) {
                            //  the following is a major hack for versions of IE less than 8 to avoid an apparent problem on the IEBlog with double-counting the offsets
                            //  this may not be a general solution to IE7's problem with offsetLeft/offsetParent
                            if (oldIEUserAgent &&
                                (!document.documentMode || document.documentMode < 8) &&
                                offsetElement.currentStyle.position === 'relative' && offsetElement.offsetParent && offsetElement.offsetParent.currentStyle.position === 'relative' && offsetElement.offsetLeft === offsetElement.offsetParent.offsetLeft) {
                                // add only the top
                                elementTop += offsetElement.offsetTop;
                            } else {
                                elementLeft += offsetElement.offsetLeft;
                                elementTop += offsetElement.offsetTop;
                            }
                        }

                        return {
                            x: elementLeft,
                            y: elementTop
                        };
                    };

                    //  cache the delta from the document to our event target (reinitialized each mousedown/MSPointerDown/touchstart)
                    var documentToTargetDelta = computeDocumentToElementDelta(target);

                    //  common event handler for the mouse/pointer/touch models and their down/start, move, up/end, and cancel events
                    var doEvent = function(theEvtObj) {

                        if (theEvtObj.type === 'mousemove' && numberOfKeys(lastXYById) === 0) {
                            return;
                        }

                        var prevent = true;

                        var pointerList = theEvtObj.changedTouches ? theEvtObj.changedTouches : [theEvtObj];
                        for (var i = 0; i < pointerList.length; ++i) {
                            var pointerObj = pointerList[i];
                            var pointerId = (typeof pointerObj.identifier !== 'undefined') ? pointerObj.identifier : (typeof pointerObj.pointerId !== 'undefined') ? pointerObj.pointerId : 1;

                            //  use the pageX/Y coordinates to compute target-relative coordinates when we have them (in ie < 9, we need to do a little work to put them there)
                            if (typeof pointerObj.pageX === 'undefined') {
                                //  initialize assuming our source element is our target
                                pointerObj.pageX = pointerObj.offsetX + documentToTargetDelta.x;
                                pointerObj.pageY = pointerObj.offsetY + documentToTargetDelta.y;

                                if (pointerObj.srcElement.offsetParent === target && document.documentMode && document.documentMode === 8 && pointerObj.type === 'mousedown') {
                                    //  source element is a child piece of VML, we're in IE8, and we've not called setCapture yet - add the origin of the source element
                                    pointerObj.pageX += pointerObj.srcElement.offsetLeft;
                                    pointerObj.pageY += pointerObj.srcElement.offsetTop;
                                } else if (pointerObj.srcElement !== target && !document.documentMode || document.documentMode < 8) {
                                    //  source element isn't the target (most likely it's a child piece of VML) and we're in a version of IE before IE8 -
                                    //  the offsetX/Y values are unpredictable so use the clientX/Y values and adjust by the scroll offsets of its parents
                                    //  to get the document-relative coordinates (the same as pageX/Y)
                                    var sx = -2,
                                        sy = -2; // adjust for old IE's 2-pixel border
                                    for (var scrollElement = pointerObj.srcElement; scrollElement !== null; scrollElement = scrollElement.parentNode) {
                                        sx += scrollElement.scrollLeft ? scrollElement.scrollLeft : 0;
                                        sy += scrollElement.scrollTop ? scrollElement.scrollTop : 0;
                                    }

                                    pointerObj.pageX = pointerObj.clientX + sx;
                                    pointerObj.pageY = pointerObj.clientY + sy;
                                }
                            }


                            var pageX = pointerObj.pageX;
                            var pageY = pointerObj.pageY;

                            if (theEvtObj.type.match(/(start|down)$/i)) {
                                //  clause for processing MSPointerDown, touchstart, and mousedown

                                //  refresh the document-to-target delta on start in case the target has moved relative to document
                                documentToTargetDelta = computeDocumentToElementDelta(target);

                                //  protect against failing to get an up or end on this pointerId
                                if (lastXYById[pointerId]) {
                                    if (endEvent) {
                                        endEvent({
                                            target: theEvtObj.target,
                                            which: theEvtObj.which,
                                            pointerId: pointerId,
                                            pageX: pageX,
                                            pageY: pageY
                                        });
                                    }

                                    delete lastXYById[pointerId];
                                }

                                if (startEvent) {
                                    if (prevent) {
                                        prevent = startEvent({
                                            target: theEvtObj.target,
                                            which: theEvtObj.which,
                                            pointerId: pointerId,
                                            pageX: pageX,
                                            pageY: pageY
                                        });
                                    }
                                }

                                //  init last page positions for this pointer
                                lastXYById[pointerId] = {
                                    x: pageX,
                                    y: pageY
                                };

                                // IE pointer model
                                if (target.msSetPointerCapture) {
                                    target.msSetPointerCapture(pointerId);
                                } else if (theEvtObj.type === 'mousedown' && numberOfKeys(lastXYById) === 1) {
                                    if (useSetReleaseCapture) {
                                        target.setCapture(true);
                                    } else {
                                        document.addEventListener('mousemove', doEvent, false);
                                        document.addEventListener('mouseup', doEvent, false);
                                    }
                                }
                            } else if (theEvtObj.type.match(/move$/i)) {
                                //  clause handles mousemove, MSPointerMove, and touchmove

                                if (lastXYById[pointerId] && !(lastXYById[pointerId].x === pageX && lastXYById[pointerId].y === pageY)) {
                                    //  only extend if the pointer is down and it's not the same as the last point

                                    if (moveEvent && prevent) {
                                        prevent = moveEvent({
                                            target: theEvtObj.target,
                                            which: theEvtObj.which,
                                            pointerId: pointerId,
                                            pageX: pageX,
                                            pageY: pageY
                                        });
                                    }

                                    //  update last page positions for this pointer
                                    lastXYById[pointerId].x = pageX;
                                    lastXYById[pointerId].y = pageY;
                                }
                            } else if (lastXYById[pointerId] && theEvtObj.type.match(/(up|end|cancel)$/i)) {
                                //  clause handles up/end/cancel

                                if (endEvent && prevent) {
                                    prevent = endEvent({
                                        target: theEvtObj.target,
                                        which: theEvtObj.which,
                                        pointerId: pointerId,
                                        pageX: pageX,
                                        pageY: pageY
                                    });
                                }

                                //  delete last page positions for this pointer
                                delete lastXYById[pointerId];

                                //  in the Microsoft pointer model, release the capture for this pointer
                                //  in the mouse model, release the capture or remove document-level event handlers if there are no down points
                                //  nothing is required for the iOS touch model because capture is implied on touchstart
                                if (target.msReleasePointerCapture) {
                                    target.msReleasePointerCapture(pointerId);
                                } else if (theEvtObj.type === 'mouseup' && numberOfKeys(lastXYById) === 0) {
                                    if (useSetReleaseCapture) {
                                        target.releaseCapture();
                                    } else {
                                        document.removeEventListener('mousemove', doEvent, false);
                                        document.removeEventListener('mouseup', doEvent, false);
                                    }
                                }
                            }
                        }

                        if (prevent) {
                            if (theEvtObj.preventDefault) {
                                theEvtObj.preventDefault();
                            }

                            if (theEvtObj.preventManipulation) {
                                theEvtObj.preventManipulation();
                            }

                            if (theEvtObj.preventMouseEvent) {
                                theEvtObj.preventMouseEvent();
                            }
                        }
                    };

                    var useSetReleaseCapture = false;
                    // saving the settings for contentZooming and touchaction before activation
                    var contentZooming, msTouchAction;

                    this.enable = function() {

                        if (window.navigator.msPointerEnabled) {
                            //  Microsoft pointer model
                            target.addEventListener('MSPointerDown', doEvent, false);
                            target.addEventListener('MSPointerMove', doEvent, false);
                            target.addEventListener('MSPointerUp', doEvent, false);
                            target.addEventListener('MSPointerCancel', doEvent, false);

                            //  css way to prevent panning in our target area
                            if (typeof target.style.msContentZooming !== 'undefined') {
                                contentZooming = target.style.msContentZooming;
                                target.style.msContentZooming = 'none';
                            }

                            //  new in Windows Consumer Preview: css way to prevent all built-in touch actions on our target
                            //  without this, you cannot touch draw on the element because IE will intercept the touch events
                            if (typeof target.style.msTouchAction !== 'undefined') {
                                msTouchAction = target.style.msTouchAction;
                                target.style.msTouchAction = 'none';
                            }
                        } else if (target.addEventListener) {
                            //  iOS touch model
                            target.addEventListener('touchstart', doEvent, false);
                            target.addEventListener('touchmove', doEvent, false);
                            target.addEventListener('touchend', doEvent, false);
                            target.addEventListener('touchcancel', doEvent, false);

                            //  mouse model
                            target.addEventListener('mousedown', doEvent, false);

                            //  mouse model with capture
                            //  rejecting gecko because, unlike ie, firefox does not send events to target when the mouse is outside target
                            if (target.setCapture && !window.navigator.userAgent.match(/\bGecko\b/)) {
                                useSetReleaseCapture = true;

                                target.addEventListener('mousemove', doEvent, false);
                                target.addEventListener('mouseup', doEvent, false);
                            }
                        } else if (target.attachEvent && target.setCapture) {
                            //  legacy IE mode - mouse with capture
                            useSetReleaseCapture = true;
                            target.attachEvent('onmousedown', function() {
                                doEvent(window.event);
                                window.event.returnValue = false;
                                return false;
                            });
                            target.attachEvent('onmousemove', function() {
                                doEvent(window.event);
                                window.event.returnValue = false;
                                return false;
                            });
                            target.attachEvent('onmouseup', function() {
                                doEvent(window.event);
                                window.event.returnValue = false;
                                return false;
                            });
                        }
                    };

                    this.disable = function() {
                        if (window.navigator.msPointerEnabled) {
                            //  Microsoft pointer model
                            target.removeEventListener('MSPointerDown', doEvent, false);
                            target.removeEventListener('MSPointerMove', doEvent, false);
                            target.removeEventListener('MSPointerUp', doEvent, false);
                            target.removeEventListener('MSPointerCancel', doEvent, false);

                            //  reset zooming to saved value
                            if (contentZooming) {
                                target.style.msContentZooming = contentZooming;
                            }

                            // reset touch action setting
                            if (msTouchAction) {
                                target.style.msTouchAction = msTouchAction;
                            }
                        } else if (target.removeEventListener) {
                            //  iOS touch model
                            target.removeEventListener('touchstart', doEvent, false);
                            target.removeEventListener('touchmove', doEvent, false);
                            target.removeEventListener('touchend', doEvent, false);
                            target.removeEventListener('touchcancel', doEvent, false);

                            //  mouse model
                            target.removeEventListener('mousedown', doEvent, false);

                            //  mouse model with capture
                            //  rejecting gecko because, unlike ie, firefox does not send events to target when the mouse is outside target
                            if (target.setCapture && !window.navigator.userAgent.match(/\bGecko\b/)) {
                                useSetReleaseCapture = true;

                                target.removeEventListener('mousemove', doEvent, false);
                                target.removeEventListener('mouseup', doEvent, false);
                            }
                        } else if (target.detachEvent && target.setCapture) {
                            //  legacy IE mode - mouse with capture
                            useSetReleaseCapture = true;
                            target.detachEvent('onmousedown');
                            target.detachEvent('onmousemove');
                            target.detachEvent('onmouseup');
                        }
                    };

                    return this;
                };

            }
        ])

    /**
     * The gridster directive
     *
     * @param {object} $parse
     * @param {object} $timeout
     */
        .directive('gridster', ['$timeout', '$rootScope', '$window',
            function($timeout, $rootScope, $window) {
                return {
                    restrict: 'EAC',
                    // without transclude, some child items may lose their parent scope
                    transclude: true,
                    replace: true,
                    template: '<div ng-class="gridsterClass()"><div ng-style="previewStyle()" class="gridster-item gridster-preview-holder"></div><div class="gridster-content" ng-transclude></div></div>',
                    controller: 'GridsterCtrl',
                    controllerAs: 'gridster',
                    scope: {
                        config: '=?gridster'
                    },
                    compile: function() {

                        return function(scope, $elem, attrs, gridster) {
                            gridster.loaded = false;

                            scope.gridsterClass = function() {
                                return {
                                    gridster: true,
                                    'gridster-desktop': !gridster.isMobile,
                                    'gridster-mobile': gridster.isMobile,
                                    'gridster-loaded': gridster.loaded
                                };
                            };

                            /**
                             * @returns {Object} style object for preview element
                             */
                            scope.previewStyle = function() {
                                if (!gridster.movingItem) {
                                    return {
                                        display: 'none'
                                    };
                                }

                                return {
                                    display: 'block',
                                    height: (gridster.movingItem.sizeY * gridster.curRowHeight - gridster.margins[0]) + 'px',
                                    width: (gridster.movingItem.sizeX * gridster.curColWidth - gridster.margins[1]) + 'px',
                                    top: (gridster.movingItem.row * gridster.curRowHeight + (gridster.outerMargin ? gridster.margins[0] : 0)) + 'px',
                                    left: (gridster.movingItem.col * gridster.curColWidth + (gridster.outerMargin ? gridster.margins[1] : 0)) + 'px'
                                };
                            };

                            var refresh = function() {
                                gridster.setOptions(scope.config);

                                // resolve "auto" & "match" values
                                if (gridster.width === 'auto') {
                                    gridster.curWidth = $elem[0].offsetWidth || parseInt($elem.css('width'), 10);
                                } else {
                                    gridster.curWidth = gridster.width;
                                }

                                if (gridster.colWidth === 'auto') {
                                    gridster.curColWidth = (gridster.curWidth + (gridster.outerMargin ? -gridster.margins[1] : gridster.margins[1])) / gridster.columns;
                                } else {
                                    gridster.curColWidth = gridster.colWidth;
                                }

                                gridster.curRowHeight = gridster.rowHeight;
                                if (typeof gridster.rowHeight === 'string') {
                                    if (gridster.rowHeight === 'match') {
                                        gridster.curRowHeight = Math.round(gridster.curColWidth);
                                    } else if (gridster.rowHeight.indexOf('*') !== -1) {
                                        gridster.curRowHeight = Math.round(gridster.curColWidth * gridster.rowHeight.replace('*', '').replace(' ', ''));
                                    } else if (gridster.rowHeight.indexOf('/') !== -1) {
                                        gridster.curRowHeight = Math.round(gridster.curColWidth / gridster.rowHeight.replace('/', '').replace(' ', ''));
                                    }
                                }

                                gridster.isMobile = gridster.mobileModeEnabled && gridster.curWidth <= gridster.mobileBreakPoint;

                                // loop through all items and reset their CSS
                                for (var rowIndex = 0, l = gridster.grid.length; rowIndex < l; ++rowIndex) {
                                    var columns = gridster.grid[rowIndex];
                                    if (!columns) {
                                        continue;
                                    }

                                    for (var colIndex = 0, len = columns.length; colIndex < len; ++colIndex) {
                                        if (columns[colIndex]) {
                                            var item = columns[colIndex];
                                            item.setElementPosition();
                                            item.setElementSizeY();
                                            item.setElementSizeX();
                                        }
                                    }
                                }

                                updateHeight();
                            };

                            // update grid items on config changes
                            scope.$watch('config', refresh, true);

                            scope.$watch('config.draggable', function() {
                                $rootScope.$broadcast('gridster-draggable-changed');
                            }, true);

                            scope.$watch('config.resizable', function() {
                                $rootScope.$broadcast('gridster-resizable-changed');
                            }, true);

                            var updateHeight = function() {
                                $elem.css('height', (gridster.gridHeight * gridster.curRowHeight) + (gridster.outerMargin ? gridster.margins[0] : -gridster.margins[0]) + 'px');
                            };

                            scope.$watch('gridster.gridHeight', updateHeight);

                            scope.$watch('gridster.movingItem', function() {
                                gridster.updateHeight(gridster.movingItem ? gridster.movingItem.sizeY : 0);
                            });

                            var prevWidth = $elem[0].offsetWidth || parseInt($elem.css('width'), 10);

                            function resize() {
                                var width = $elem[0].offsetWidth || parseInt($elem.css('width'), 10);

                                if (!width || width === prevWidth || gridster.movingItem) {
                                    return;
                                }
                                prevWidth = width;

                                if (gridster.loaded) {
                                    $elem.removeClass('gridster-loaded');
                                }

                                refresh();

                                if (gridster.loaded) {
                                    $elem.addClass('gridster-loaded');
                                }

                                scope.$parent.$broadcast('gridster-resized', [width, $elem.offsetHeight]);
                            }

                            // track element width changes any way we can
                            function onResize() {
                                resize();
                                $timeout(function() {
                                    scope.$apply();
                                });
                            }
                            if (typeof $elem.resize === 'function') {
                                $elem.resize(onResize);
                            }
                            var $win = angular.element($window);
                            $win.on('resize', onResize);

                            scope.$watch(function() {
                                return $elem[0].offsetWidth || parseInt($elem.css('width'), 10);
                            }, resize);

                            // be sure to cleanup
                            scope.$on('$destroy', function() {
                                gridster.destroy();
                                $win.off('resize', onResize);
                            });

                            // allow a little time to place items before floating up
                            $timeout(function() {
                                scope.$watch('gridster.floating', function() {
                                    gridster.floatItemsUp();
                                });
                                gridster.loaded = true;
                            }, 100);



                            // Custom code

                            /**
                             * Callback called when starting to drag from palette to canvas.
                             */
                            $rootScope.startDragCallback = function (event, ui) {
                                $timeout(function () {
                                    var fieldId = event.target.id;
                                    var fieldType;

                                    var colspan = 1;
                                    if (fieldId === 'group' || fieldId === 'dynamic-table') {
                                        colspan = 2;
                                    }

                                    var numberOfRows = 1;
                                    if (fieldId === 'multi-line-text') {
                                        numberOfRows = 2;
                                    } else if (fieldId === 'dynamic-table') {
                                        numberOfRows = 2;
                                    }

                                    scope.startDragItem = {
                                        type: fieldId,
                                        fieldType: fieldType,
                                        name: 'Label',
                                        required: false,
                                        readOnly: false,
                                        sizeX: colspan,
                                        sizeY: numberOfRows,
                                        row: -1,
                                        col: -1
                                    };

                                    if (fieldId === 'radio-buttons' || fieldId === 'dropdown') {
                                        scope.startDragItem.options = [
                                            {name: 'Option 1'}
                                        ];
                                    }

                                }, 0);
                            };

                            /**
                             * Callback called when stopping to drag from palette to canvas.
                             */
                            $rootScope.dropCallback = function (event, ui) {
                                $timeout(function () {
                                    gridster.movingItem = null;
                                    var offset = jQuery($elem[0]).offset();
                                    var row = gridster.pixelsToRows(event.clientY - offset.top, true) - 1;
                                    var column = gridster.pixelsToColumns(event.clientX - offset.left, true) - 1;
                                    if (row >= 0 && column >= 0) {

                                        scope.startDragItem.row = row;
                                        scope.startDragItem.col = column;
                                    }
                                }, 0);
                            };

                            /**
                             * Callback called during dragging from palette to canvas.
                             */
                            $rootScope.dragCallback = function (event, ui) {

                                    var offset = jQuery($elem[0]).offset();
                                    var row = gridster.pixelsToRows(event.clientY - offset.top, true) - 1;
                                    var column = gridster.pixelsToColumns(event.clientX - offset.left, true) - 1;

                                    if (!scope.startDragItem) {
                                        return;
                                    }

                                    if (scope.startDragItem.row === row && scope.startDragItem.col === column) {
                                        return;
                                    }

                                    if ( ((scope.startDragItem.row === -1 || scope.startDragItem.col === -1) && row >= 0 && column >= 0)
                                        || (scope.startDragItem.row >= 0 && scope.startDragItem.col >= 0 && (row < 0 || column < 0)) ) {
                                        $timeout(function () {
                                            if ((scope.startDragItem.row === -1 || scope.startDragItem.col === -1) && row >= 0 && column >= 0) {
                                                scope.$parent.formItems.push(scope.startDragItem);

                                                gridster.movingItem = scope.startDragItem;
                                                gridster.updateHeight(1);
                                            }

                                            if (scope.startDragItem.row >= 0 && scope.startDragItem.col >= 0 && (row < 0 || column < 0)) {
                                                scope.$parent.formItems.pop();
                                                gridster.movingItem = null;
                                            }


                                        }, 0);
                                    }

                                $timeout(function () {
                                    scope.startDragItem.row = row;
                                    scope.startDragItem.col = column;
                                }, 0);

                            };

                            // END custom code


                        };
                    }
                };
            }
        ])

        .controller('GridsterItemCtrl', function() {
            this.$element = null;
            this.gridster = null;
            this.row = null;
            this.col = null;
            this.sizeX = null;
            this.sizeY = null;
            this.minSizeX = 0;
            this.minSizeY = 0;
            this.maxSizeX = null;
            this.maxSizeY = null;

            this.init = function($element, gridster) {
                this.$element = $element;
                this.gridster = gridster;
                this.sizeX = gridster.defaultSizeX;
                this.sizeY = gridster.defaultSizeY;
            };

            this.destroy = function() {
                this.gridster = null;
                this.$element = null;
            };

            /**
             * Returns the items most important attributes
             */
            this.toJSON = function() {
                return {
                    row: this.row,
                    col: this.col,
                    sizeY: this.sizeY,
                    sizeX: this.sizeX
                };
            };

            this.isMoving = function() {
                return this.gridster.movingItem === this;
            };

            /**
             * Set the items position
             *
             * @param {number} row
             * @param {number} column
             */
            this.setPosition = function(row, column) {
                this.gridster.putItem(this, row, column);

                if (!this.isMoving()) {
                    this.setElementPosition();
                }
            };

            /**
             * Sets a specified size property
             *
             * @param {string} key Can be either "x" or "y"
             * @param {number} value The size amount
             */
            this.setSize = function(key, value, preventMove) {
                key = key.toUpperCase();
                var camelCase = 'size' + key,
                    titleCase = 'Size' + key;
                if (value === '') {
                    return;
                }
                value = parseInt(value, 10);
                if (isNaN(value) || value === 0) {
                    value = this.gridster['default' + titleCase];
                }
                var max = key === 'X' ? this.gridster.columns : this.gridster.maxRows;
                if (this['max' + titleCase]) {
                    max = Math.min(this['max' + titleCase], max);
                }
                if (this.gridster['max' + titleCase]) {
                    max = Math.min(this.gridster['max' + titleCase], max);
                }
                if (key === 'X' && this.cols) {
                    max -= this.cols;
                } else if (key === 'Y' && this.rows) {
                    max -= this.rows;
                }

                var min = 0;
                if (this['min' + titleCase]) {
                    min = Math.max(this['min' + titleCase], min);
                }
                if (this.gridster['min' + titleCase]) {
                    min = Math.max(this.gridster['min' + titleCase], min);
                }

                value = Math.max(Math.min(value, max), min);

                var changed = (this[camelCase] !== value || (this['old' + titleCase] && this['old' + titleCase] !== value));
                this['old' + titleCase] = this[camelCase] = value;

                if (!this.isMoving()) {
                    this['setElement' + titleCase]();
                }
                if (!preventMove && changed) {
                    this.gridster.moveOverlappingItems(this);
                    this.gridster.layoutChanged();
                }

                return changed;
            };

            /**
             * Sets the items sizeY property
             *
             * @param {number} rows
             */
            this.setSizeY = function(rows, preventMove) {
                return this.setSize('Y', rows, preventMove);
            };

            /**
             * Sets the items sizeX property
             *
             * @param {number} rows
             */
            this.setSizeX = function(columns, preventMove) {
                return this.setSize('X', columns, preventMove);
            };

            /**
             * Sets an elements position on the page
             *
             * @param {number} row
             * @param {number} column
             */
            this.setElementPosition = function() {
                if (this.gridster.isMobile) {
                    this.$element.css({
                        marginLeft: this.gridster.margins[0] + 'px',
                        marginRight: this.gridster.margins[0] + 'px',
                        marginTop: this.gridster.margins[1] + 'px',
                        marginBottom: this.gridster.margins[1] + 'px',
                        top: '',
                        left: ''
                    });
                } else {
                    this.$element.css({
                        margin: 0,
                        top: (this.row * this.gridster.curRowHeight + (this.gridster.outerMargin ? this.gridster.margins[0] : 0)) + 'px',
                        left: (this.col * this.gridster.curColWidth + (this.gridster.outerMargin ? this.gridster.margins[1] : 0)) + 'px'
                    });
                }
            };

            /**
             * Sets an elements height
             */
            this.setElementSizeY = function() {
                if (this.gridster.isMobile && !this.gridster.saveGridItemCalculatedHeightInMobile) {
                    this.$element.css('height', '');
                } else {
                    this.$element.css('height', (this.sizeY * this.gridster.curRowHeight - this.gridster.margins[0]) + 'px');
                }
            };

            /**
             * Sets an elements width
             */
            this.setElementSizeX = function() {
                if (this.gridster.isMobile) {
                    this.$element.css('width', '');
                } else {
                    this.$element.css('width', (this.sizeX * this.gridster.curColWidth - this.gridster.margins[1]) + 'px');
                }
            };

            /**
             * Gets an element's width
             */
            this.getElementSizeX = function() {
                return (this.sizeX * this.gridster.curColWidth - this.gridster.margins[1]);
            };

            /**
             * Gets an element's height
             */
            this.getElementSizeY = function() {
                return (this.sizeY * this.gridster.curRowHeight - this.gridster.margins[0]);
            };

        })

        .factory('GridsterDraggable', ['$document', '$timeout', '$window',
            function($document, $timeout, $window) {
                function GridsterDraggable($el, scope, gridster, item, itemOptions) {

                    var elmX, elmY, elmW, elmH,

                        mouseX = 0,
                        mouseY = 0,
                        lastMouseX = 0,
                        lastMouseY = 0,
                        mOffX = 0,
                        mOffY = 0,

                        minTop = 0,
                        maxTop = 9999,
                        minLeft = 0,
                        realdocument = $document[0];

                    var originalCol, originalRow;
                    var inputTags = ['select', 'input', 'textarea', 'button'];

                    function mouseDown(e) {
                        if (inputTags.indexOf(e.target.nodeName.toLowerCase()) !== -1) {
                            return false;
                        }

                        // exit, if a resize handle was hit
                        if (angular.element(e.target).hasClass('gridster-item-resizable-handler')) {
                            return false;
                        }

                        // exit, if the target has it's own click event
                        if (angular.element(e.target).attr('onclick') || angular.element(e.target).attr('ng-click')) {
                            return false;
                        }

                        switch (e.which) {
                            case 1:
                                // left mouse button
                                break;
                            case 2:
                            case 3:
                                // right or middle mouse button
                                return;
                        }

                        lastMouseX = e.pageX;
                        lastMouseY = e.pageY;

                        elmX = parseInt($el.css('left'), 10);
                        elmY = parseInt($el.css('top'), 10);
                        elmW = $el[0].offsetWidth;
                        elmH = $el[0].offsetHeight;

                        originalCol = item.col;
                        originalRow = item.row;

                        dragStart(e);

                        return true;
                    }

                    function mouseMove(e) {
                        if (!$el.hasClass('gridster-item-moving') || $el.hasClass('gridster-item-resizing')) {
                            return false;
                        }

                        var maxLeft = gridster.curWidth - 1;

                        // Get the current mouse position.
                        mouseX = e.pageX;
                        mouseY = e.pageY;

                        // Get the deltas
                        var diffX = mouseX - lastMouseX + mOffX;
                        var diffY = mouseY - lastMouseY + mOffY;
                        mOffX = mOffY = 0;

                        // Update last processed mouse positions.
                        lastMouseX = mouseX;
                        lastMouseY = mouseY;

                        var dX = diffX,
                            dY = diffY;
                        if (elmX + dX < minLeft) {
                            diffX = minLeft - elmX;
                            mOffX = dX - diffX;
                        } else if (elmX + elmW + dX > maxLeft) {
                            diffX = maxLeft - elmX - elmW;
                            mOffX = dX - diffX;
                        }

                        if (elmY + dY < minTop) {
                            diffY = minTop - elmY;
                            mOffY = dY - diffY;
                        } else if (elmY + elmH + dY > maxTop) {
                            diffY = maxTop - elmY - elmH;
                            mOffY = dY - diffY;
                        }
                        elmX += diffX;
                        elmY += diffY;

                        // set new position
                        $el.css({
                            'top': elmY + 'px',
                            'left': elmX + 'px'
                        });

                        drag(e);

                        return true;
                    }

                    function mouseUp(e) {
                        if (!$el.hasClass('gridster-item-moving') || $el.hasClass('gridster-item-resizing')) {
                            return false;
                        }

                        mOffX = mOffY = 0;

                        dragStop(e);

                        return true;
                    }

                    function dragStart(event) {
                        $el.addClass('gridster-item-moving');
                        gridster.movingItem = item;

                        gridster.updateHeight(item.sizeY);
                        scope.$apply(function() {
                            if (gridster.draggable && gridster.draggable.start) {
                                gridster.draggable.start(event, $el, itemOptions);
                            }
                        });
                    }

                    function drag(event) {
                        var oldRow = item.row,
                            oldCol = item.col,
                            hasCallback = gridster.draggable && gridster.draggable.drag,
                            scrollSensitivity = gridster.draggable.scrollSensitivity,
                            scrollSpeed = gridster.draggable.scrollSpeed;

                        var row = gridster.pixelsToRows(elmY);
                        var col = gridster.pixelsToColumns(elmX);

                        var itemsInTheWay = gridster.getItems(row, col, item.sizeX, item.sizeY, item);
                        var hasItemsInTheWay = itemsInTheWay.length !== 0;

                        if (gridster.swapping === true && hasItemsInTheWay) {
                            var boundingBoxItem = gridster.getBoundingBox(itemsInTheWay);
                            var sameSize = boundingBoxItem.sizeX === item.sizeX && boundingBoxItem.sizeY === item.sizeY;
                            var sameRow = boundingBoxItem.row === row;
                            var sameCol = boundingBoxItem.col === col;
                            var samePosition = sameRow && sameCol;
                            var inline = sameRow || sameCol;

                            if (sameSize && itemsInTheWay.length === 1) {
                                if (samePosition) {
                                    gridster.swapItems(item, itemsInTheWay[0]);
                                } else if (inline) {
                                    return;
                                }
                            } else if (boundingBoxItem.sizeX <= item.sizeX && boundingBoxItem.sizeY <= item.sizeY && inline) {
                                var emptyRow = item.row <= row ? item.row : row + item.sizeY;
                                var emptyCol = item.col <= col ? item.col : col + item.sizeX;
                                var rowOffset = emptyRow - boundingBoxItem.row;
                                var colOffset = emptyCol - boundingBoxItem.col;

                                for (var i = 0, l = itemsInTheWay.length; i < l; ++i) {
                                    var itemInTheWay = itemsInTheWay[i];

                                    var itemsInFreeSpace = gridster.getItems(
                                            itemInTheWay.row + rowOffset,
                                            itemInTheWay.col + colOffset,
                                        itemInTheWay.sizeX,
                                        itemInTheWay.sizeY,
                                        item
                                    );

                                    if (itemsInFreeSpace.length === 0) {
                                        gridster.putItem(itemInTheWay, itemInTheWay.row + rowOffset, itemInTheWay.col + colOffset);
                                    }
                                }
                            }
                        }

                        if (gridster.pushing !== false || !hasItemsInTheWay) {
                            item.row = row;
                            item.col = col;
                        }

                        if (event.pageY - realdocument.body.scrollTop < scrollSensitivity) {
                            realdocument.body.scrollTop = realdocument.body.scrollTop - scrollSpeed;
                        } else if ($window.innerHeight - (event.pageY - realdocument.body.scrollTop) < scrollSensitivity) {
                            realdocument.body.scrollTop = realdocument.body.scrollTop + scrollSpeed;
                        }

                        if (event.pageX - realdocument.body.scrollLeft < scrollSensitivity) {
                            realdocument.body.scrollLeft = realdocument.body.scrollLeft - scrollSpeed;
                        } else if ($window.innerWidth - (event.pageX - realdocument.body.scrollLeft) < scrollSensitivity) {
                            realdocument.body.scrollLeft = realdocument.body.scrollLeft + scrollSpeed;
                        }

                        if (hasCallback || oldRow !== item.row || oldCol !== item.col) {
                            scope.$apply(function() {
                                if (hasCallback) {
                                    gridster.draggable.drag(event, $el, itemOptions);
                                }
                            });
                        }
                    }

                    function dragStop(event) {
                        $el.removeClass('gridster-item-moving');
                        var row = gridster.pixelsToRows(elmY);
                        var col = gridster.pixelsToColumns(elmX);
                        if (gridster.pushing !== false || gridster.getItems(row, col, item.sizeX, item.sizeY, item).length === 0) {
                            item.row = row;
                            item.col = col;
                        }
                        gridster.movingItem = null;
                        item.setPosition(item.row, item.col);

                        scope.$apply(function() {
                            if (gridster.draggable && gridster.draggable.stop) {
                                gridster.draggable.stop(event, $el, itemOptions);
                            }
                        });
                    }

                    var enabled = false;
                    var $dragHandle = null;
                    var unifiedInput;

                    this.enable = function() {
                        var self = this;
                        // disable and timeout required for some template rendering
                        $timeout(function() {
                            self.disable();

                            if (gridster.draggable && gridster.draggable.handle) {
                                $dragHandle = angular.element($el[0].querySelector(gridster.draggable.handle));
                                if ($dragHandle.length === 0) {
                                    // fall back to element if handle not found...
                                    $dragHandle = $el;
                                }
                            } else {
                                $dragHandle = $el;
                            }

                            unifiedInput = new gridster.unifiedInput($dragHandle[0], mouseDown, mouseMove, mouseUp);
                            unifiedInput.enable();

                            enabled = true;
                        });
                    };

                    this.disable = function() {
                        if (!enabled) {
                            return;
                        }

                        unifiedInput.disable();
                        unifiedInput = undefined;
                        enabled = false;
                    };

                    this.toggle = function(enabled) {
                        if (enabled) {
                            this.enable();
                        } else {
                            this.disable();
                        }
                    };

                    this.destroy = function() {
                        this.disable();
                    };
                }

                return GridsterDraggable;
            }
        ])

        .factory('GridsterResizable', [
            function() {
                function GridsterResizable($el, scope, gridster, item, itemOptions) {

                    function ResizeHandle(handleClass) {

                        var hClass = handleClass;

                        var elmX, elmY, elmW, elmH,

                            mouseX = 0,
                            mouseY = 0,
                            lastMouseX = 0,
                            lastMouseY = 0,
                            mOffX = 0,
                            mOffY = 0,

                            minTop = 0,
                            maxTop = 9999,
                            minLeft = 0;

                        var getMinHeight = function() {
                            return gridster.curRowHeight - gridster.margins[0];
                        };
                        var getMinWidth = function() {
                            return gridster.curColWidth - gridster.margins[1];
                        };

                        var originalWidth, originalHeight;
                        var savedDraggable;

                        function mouseDown(e) {
                            switch (e.which) {
                                case 1:
                                    // left mouse button
                                    break;
                                case 2:
                                case 3:
                                    // right or middle mouse button
                                    return;
                            }

                            // save the draggable setting to restore after resize
                            savedDraggable = gridster.draggable.enabled;
                            if (savedDraggable) {
                                gridster.draggable.enabled = false;
                                scope.$broadcast('gridster-draggable-changed');
                            }

                            // Get the current mouse position.
                            lastMouseX = e.pageX;
                            lastMouseY = e.pageY;

                            // Record current widget dimensions
                            elmX = parseInt($el.css('left'), 10);
                            elmY = parseInt($el.css('top'), 10);
                            elmW = $el[0].offsetWidth;
                            elmH = $el[0].offsetHeight;

                            originalWidth = item.sizeX;
                            originalHeight = item.sizeY;

                            resizeStart(e);

                            return true;
                        }

                        function resizeStart(e) {
                            $el.addClass('gridster-item-moving');
                            $el.addClass('gridster-item-resizing');

                            gridster.movingItem = item;

                            item.setElementSizeX();
                            item.setElementSizeY();
                            item.setElementPosition();
                            gridster.updateHeight(1);

                            scope.$apply(function() {
                                // callback
                                if (gridster.resizable && gridster.resizable.start) {
                                    gridster.resizable.start(e, $el, itemOptions); // options is the item model
                                }
                            });
                        }

                        function mouseMove(e) {
                            var maxLeft = gridster.curWidth - 1;

                            // Get the current mouse position.
                            mouseX = e.pageX;
                            mouseY = e.pageY;

                            // Get the deltas
                            var diffX = mouseX - lastMouseX + mOffX;
                            var diffY = mouseY - lastMouseY + mOffY;
                            mOffX = mOffY = 0;

                            // Update last processed mouse positions.
                            lastMouseX = mouseX;
                            lastMouseY = mouseY;

                            var dY = diffY,
                                dX = diffX;

                            if (hClass.indexOf('n') >= 0) {
                                if (elmH - dY < getMinHeight()) {
                                    diffY = elmH - getMinHeight();
                                    mOffY = dY - diffY;
                                } else if (elmY + dY < minTop) {
                                    diffY = minTop - elmY;
                                    mOffY = dY - diffY;
                                }
                                elmY += diffY;
                                elmH -= diffY;
                            }
                            if (hClass.indexOf('s') >= 0) {
                                if (elmH + dY < getMinHeight()) {
                                    diffY = getMinHeight() - elmH;
                                    mOffY = dY - diffY;
                                } else if (elmY + elmH + dY > maxTop) {
                                    diffY = maxTop - elmY - elmH;
                                    mOffY = dY - diffY;
                                }
                                elmH += diffY;
                            }
                            if (hClass.indexOf('w') >= 0) {
                                if (elmW - dX < getMinWidth()) {
                                    diffX = elmW - getMinWidth();
                                    mOffX = dX - diffX;
                                } else if (elmX + dX < minLeft) {
                                    diffX = minLeft - elmX;
                                    mOffX = dX - diffX;
                                }
                                elmX += diffX;
                                elmW -= diffX;
                            }
                            if (hClass.indexOf('e') >= 0) {
                                if (elmW + dX < getMinWidth()) {
                                    diffX = getMinWidth() - elmW;
                                    mOffX = dX - diffX;
                                } else if (elmX + elmW + dX > maxLeft) {
                                    diffX = maxLeft - elmX - elmW;
                                    mOffX = dX - diffX;
                                }
                                elmW += diffX;
                            }

                            // set new position
                            $el.css({
                                'top': elmY + 'px',
                                'left': elmX + 'px',
                                'width': elmW + 'px',
                                'height': elmH + 'px'
                            });

                            resize(e);

                            return true;
                        }

                        function mouseUp(e) {
                            // restore draggable setting to its original state
                            if (gridster.draggable.enabled !== savedDraggable) {
                                gridster.draggable.enabled = savedDraggable;
                                scope.$broadcast('gridster-draggable-changed');
                            }

                            mOffX = mOffY = 0;

                            resizeStop(e);

                            return true;
                        }

                        function resize(e) {
                            var oldRow = item.row,
                                oldCol = item.col,
                                oldSizeX = item.sizeX,
                                oldSizeY = item.sizeY,
                                hasCallback = gridster.resizable && gridster.resizable.resize;

                            var col = item.col;
                            // only change column if grabbing left edge
                            if (['w', 'nw', 'sw'].indexOf(handleClass) !== -1) {
                                col = gridster.pixelsToColumns(elmX, false);
                            }

                            var row = item.row;
                            // only change row if grabbing top edge
                            if (['n', 'ne', 'nw'].indexOf(handleClass) !== -1) {
                                row = gridster.pixelsToRows(elmY, false);
                            }

                            var sizeX = item.sizeX;
                            // only change row if grabbing left or right edge
                            if (['n', 's'].indexOf(handleClass) === -1) {
                                sizeX = gridster.pixelsToColumns(elmW, true);
                            }

                            var sizeY = item.sizeY;
                            // only change row if grabbing top or bottom edge
                            if (['e', 'w'].indexOf(handleClass) === -1) {
                                sizeY = gridster.pixelsToRows(elmH, true);
                            }

                            if (gridster.pushing !== false || gridster.getItems(row, col, sizeX, sizeY, item).length === 0) {
                                item.row = row;
                                item.col = col;
                                item.sizeX = sizeX;
                                item.sizeY = sizeY;
                            }
                            var isChanged = item.row !== oldRow || item.col !== oldCol || item.sizeX !== oldSizeX || item.sizeY !== oldSizeY;

                            if (hasCallback || isChanged) {
                                scope.$apply(function() {
                                    if (hasCallback) {
                                        gridster.resizable.resize(e, $el, itemOptions); // options is the item model
                                    }
                                });
                            }
                        }

                        function resizeStop(e) {
                            $el.removeClass('gridster-item-moving');
                            $el.removeClass('gridster-item-resizing');

                            gridster.movingItem = null;

                            item.setPosition(item.row, item.col);
                            item.setSizeY(item.sizeY);
                            item.setSizeX(item.sizeX);

                            scope.$apply(function() {
                                if (gridster.resizable && gridster.resizable.stop) {
                                    gridster.resizable.stop(e, $el, itemOptions); // options is the item model
                                }
                            });
                        }

                        var $dragHandle = null;
                        var unifiedInput;

                        this.enable = function() {
                            if (!$dragHandle) {
                                $dragHandle = angular.element('<div class="gridster-item-resizable-handler handle-' + hClass + '"></div>');
                                $el.append($dragHandle);
                            }

                            unifiedInput = new gridster.unifiedInput($dragHandle[0], mouseDown, mouseMove, mouseUp);
                            unifiedInput.enable();
                        };

                        this.disable = function() {
                            if ($dragHandle) {
                                $dragHandle.remove();
                                $dragHandle = null;
                            }

                            unifiedInput.disable();
                            unifiedInput = undefined;
                        };

                        this.destroy = function() {
                            this.disable();
                        };
                    }

                    var handles = [];
                    var handlesOpts = gridster.resizable.handles;
                    if (typeof handlesOpts === 'string') {
                        handlesOpts = gridster.resizable.handles.split(',');
                    }
                    var enabled = false;

                    for (var c = 0, l = handlesOpts.length; c < l; c++) {
                        handles.push(new ResizeHandle(handlesOpts[c]));
                    }

                    this.enable = function() {
                        if (enabled) {
                            return;
                        }
                        for (var c = 0, l = handles.length; c < l; c++) {
                            handles[c].enable();
                        }
                        enabled = true;
                    };

                    this.disable = function() {
                        if (!enabled) {
                            return;
                        }
                        for (var c = 0, l = handles.length; c < l; c++) {
                            handles[c].disable();
                        }
                        enabled = false;
                    };

                    this.toggle = function(enabled) {
                        if (enabled) {
                            this.enable();
                        } else {
                            this.disable();
                        }
                    };

                    this.destroy = function() {
                        for (var c = 0, l = handles.length; c < l; c++) {
                            handles[c].destroy();
                        }
                    };
                }
                return GridsterResizable;
            }
        ])

    /**
     * GridsterItem directive
     */
        .directive('gridsterItem', ['$parse', 'GridsterDraggable', 'GridsterResizable',
            function($parse, GridsterDraggable, GridsterResizable) {
                return {
                    restrict: 'EA',
                    controller: 'GridsterItemCtrl',
                    require: ['^gridster', 'gridsterItem'],
                    link: function(scope, $el, attrs, controllers) {
                        var optionsKey = attrs.gridsterItem,
                            options;

                        var gridster = controllers[0],
                            item = controllers[1];

                        // bind the item's position properties
                        if (optionsKey) {
                            var $optionsGetter = $parse(optionsKey);
                            options = $optionsGetter(scope) || {};
                            if (!options && $optionsGetter.assign) {
                                options = {
                                    row: item.row,
                                    col: item.col,
                                    sizeX: item.sizeX,
                                    sizeY: item.sizeY,
                                    minSizeX: 0,
                                    minSizeY: 0,
                                    maxSizeX: null,
                                    maxSizeY: null
                                };
                                $optionsGetter.assign(scope, options);
                            }
                        } else {
                            options = attrs;
                        }

                        item.init($el, gridster);

                        $el.addClass('gridster-item');

                        var aspects = ['minSizeX', 'maxSizeX', 'minSizeY', 'maxSizeY', 'sizeX', 'sizeY', 'row', 'col'],
                            $getters = {};

                        var aspectFn = function(aspect) {
                            var key;
                            if (typeof options[aspect] === 'string') {
                                key = options[aspect];
                            } else if (typeof options[aspect.toLowerCase()] === 'string') {
                                key = options[aspect.toLowerCase()];
                            } else if (optionsKey) {
                                key = $parse(optionsKey + '.' + aspect);
                            } else {
                                return;
                            }
                            $getters[aspect] = $parse(key);

                            // when the value changes externally, update the internal item object
                            scope.$watch(key, function(newVal) {
                                newVal = parseInt(newVal, 10);
                                if (!isNaN(newVal)) {
                                    item[aspect] = newVal;
                                }
                            });

                            // initial set
                            var val = $getters[aspect](scope);
                            if (typeof val === 'number') {
                                item[aspect] = val;
                            }
                        };

                        for (var i = 0, l = aspects.length; i < l; ++i) {
                            aspectFn(aspects[i]);
                        }

                        scope.$broadcast('gridster-item-initialized', [item.sizeY, item.sizeX, item.getElementSizeY(), item.getElementSizeX()]);

                        function positionChanged() {
                            // call setPosition so the element and gridster controller are updated
                            item.setPosition(item.row, item.col);

                            // when internal item position changes, update externally bound values
                            if ($getters.row && $getters.row.assign) {
                                $getters.row.assign(scope, item.row);
                            }
                            if ($getters.col && $getters.col.assign) {
                                $getters.col.assign(scope, item.col);
                            }
                        }
                        scope.$watch(function() {
                            return item.row + ',' + item.col;
                        }, positionChanged);

                        function sizeChanged() {
                            var changedX = item.setSizeX(item.sizeX, true);
                            if (changedX && $getters.sizeX && $getters.sizeX.assign) {
                                $getters.sizeX.assign(scope, item.sizeX);
                            }
                            var changedY = item.setSizeY(item.sizeY, true);
                            if (changedY && $getters.sizeY && $getters.sizeY.assign) {
                                $getters.sizeY.assign(scope, item.sizeY);
                            }

                            if (changedX || changedY) {
                                item.gridster.moveOverlappingItems(item);
                                gridster.layoutChanged();
                            }
                        }
                        scope.$watch(function() {
                            return item.sizeY + ',' + item.sizeX + '|' + item.minSizeX + ',' + item.maxSizeX + ',' + item.minSizeY + ',' + item.maxSizeY;
                        }, sizeChanged);

                        var draggable = new GridsterDraggable($el, scope, gridster, item, options);
                        var resizable = new GridsterResizable($el, scope, gridster, item, options);

                        scope.$on('gridster-draggable-changed', function() {
                            draggable.toggle(!gridster.isMobile && gridster.draggable && gridster.draggable.enabled);
                        });
                        scope.$on('gridster-resizable-changed', function() {
                            resizable.toggle(!gridster.isMobile && gridster.resizable && gridster.resizable.enabled);
                        });
                        scope.$on('gridster-resized', function() {
                            resizable.toggle(!gridster.isMobile && gridster.resizable && gridster.resizable.enabled);
                        });
                        scope.$watch(function() {
                            return gridster.isMobile;
                        }, function() {
                            resizable.toggle(!gridster.isMobile && gridster.resizable && gridster.resizable.enabled);
                            draggable.toggle(!gridster.isMobile && gridster.draggable && gridster.draggable.enabled);
                        });

                        function whichTransitionEvent() {
                            var el = document.createElement('div');
                            var transitions = {
                                'transition': 'transitionend',
                                'OTransition': 'oTransitionEnd',
                                'MozTransition': 'transitionend',
                                'WebkitTransition': 'webkitTransitionEnd'
                            };
                            for (var t in transitions) {
                                if (el.style[t] !== undefined) {
                                    return transitions[t];
                                }
                            }
                        }

                        $el.on(whichTransitionEvent(), function() {
                            scope.$apply(function() {
                                scope.$broadcast('gridster-item-transition-end');
                            });
                        });

                        return scope.$on('$destroy', function() {
                            try {
                                resizable.destroy();
                                draggable.destroy();
                            } catch (e) {}

                            try {
                                gridster.removeItem(item);
                            } catch (e) {}

                            try {
                                item.destroy();
                            } catch (e) {}
                        });
                    }
                };
            }
        ])

    ;

})(angular);