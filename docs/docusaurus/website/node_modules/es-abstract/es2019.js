'use strict';

var trimStart = require('string.prototype.trimleft');
var trimEnd = require('string.prototype.trimright');
var inspect = require('object-inspect');

var ES2018 = require('./es2018');
var assign = require('./helpers/assign');

var GetIntrinsic = require('./GetIntrinsic');

var $TypeError = GetIntrinsic('%TypeError%');
var $Number = GetIntrinsic('%Number%');
var MAX_SAFE_INTEGER = $Number.MAX_SAFE_INTEGER || Math.pow(2, 53) - 1;

var ES2019 = assign(assign({}, ES2018), {
	// https://tc39.es/ecma262/#sec-add-entries-from-iterable
	AddEntriesFromIterable: function AddEntriesFromIterable(target, iterable, adder) {
		if (!this.IsCallable(adder)) {
			throw new $TypeError('Assertion failed: `adder` is not callable');
		}
		if (iterable == null) {
			throw new $TypeError('Assertion failed: `iterable` is present, and not nullish');
		}
		var iteratorRecord = this.GetIterator(iterable);
		while (true) { // eslint-disable-line no-constant-condition
			var next = this.IteratorStep(iteratorRecord);
			if (!next) {
				return target;
			}
			var nextItem = this.IteratorValue(next);
			if (this.Type(nextItem) !== 'Object') {
				var error = new $TypeError('iterator next must return an Object, got ' + inspect(nextItem));
				return this.IteratorClose(
					iteratorRecord,
					function () { throw error; } // eslint-disable-line no-loop-func
				);
			}
			try {
				var k = this.Get(nextItem, '0');
				var v = this.Get(nextItem, '1');
				this.Call(adder, target, [k, v]);
			} catch (e) {
				return this.IteratorClose(
					iteratorRecord,
					function () { throw e; } // eslint-disable-line no-loop-func
				);
			}
		}
	},

	// https://ecma-international.org/ecma-262/10.0/#sec-flattenintoarray
	// eslint-disable-next-line max-params, max-statements
	FlattenIntoArray: function FlattenIntoArray(target, source, sourceLen, start, depth) {
		var mapperFunction;
		if (arguments.length > 5) {
			mapperFunction = arguments[5];
		}

		var targetIndex = start;
		var sourceIndex = 0;
		while (sourceIndex < sourceLen) {
			var P = this.ToString(sourceIndex);
			var exists = this.HasProperty(source, P);
			if (exists === true) {
				var element = this.Get(source, P);
				if (typeof mapperFunction !== 'undefined') {
					if (arguments.length <= 6) {
						throw new $TypeError('Assertion failed: thisArg is required when mapperFunction is provided');
					}
					element = this.Call(mapperFunction, arguments[6], [element, sourceIndex, source]);
				}
				var shouldFlatten = false;
				if (depth > 0) {
					shouldFlatten = this.IsArray(element);
				}
				if (shouldFlatten) {
					var elementLen = this.ToLength(this.Get(element, 'length'));
					targetIndex = this.FlattenIntoArray(target, element, elementLen, targetIndex, depth - 1);
				} else {
					if (targetIndex >= MAX_SAFE_INTEGER) {
						throw new $TypeError('index too large');
					}
					this.CreateDataPropertyOrThrow(target, this.ToString(targetIndex), element);
					targetIndex += 1;
				}
			}
			sourceIndex += 1;
		}

		return targetIndex;
	},

	// https://ecma-international.org/ecma-262/10.0/#sec-trimstring
	TrimString: function TrimString(string, where) {
		var str = this.RequireObjectCoercible(string);
		var S = this.ToString(str);
		var T;
		if (where === 'start') {
			T = trimStart(S);
		} else if (where === 'end') {
			T = trimEnd(S);
		} else if (where === 'start+end') {
			T = trimStart(trimEnd(S));
		} else {
			throw new $TypeError('Assertion failed: invalid `where` value; must be "start", "end", or "start+end"');
		}
		return T;
	}
});

module.exports = ES2019;
