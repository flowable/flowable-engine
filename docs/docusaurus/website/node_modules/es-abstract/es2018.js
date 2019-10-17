'use strict';

var GetIntrinsic = require('./GetIntrinsic');

var keys = require('object-keys');
var inspect = require('object-inspect');

var ES2017 = require('./es2017');
var assign = require('./helpers/assign');
var forEach = require('./helpers/forEach');
var callBind = require('./helpers/callBind');
var every = require('./helpers/every');

var $String = GetIntrinsic('%String%');
var $Object = GetIntrinsic('%Object%');
var $TypeError = GetIntrinsic('%TypeError%');
var $RegExp = GetIntrinsic('%RegExp%');

var $SymbolProto = GetIntrinsic('%SymbolPrototype%', true);
var $SymbolValueOf = $SymbolProto ? callBind($SymbolProto.valueOf) : null;
var $StringProto = GetIntrinsic('%StringPrototype%');
var $charAt = callBind($StringProto.charAt);
var strSlice = callBind($StringProto.slice);
var $indexOf = callBind($StringProto.indexOf);
var $parseInt = parseInt;
var isDigit = callBind($RegExp.prototype.test, /^[0-9]$/);

var $PromiseResolveOrig = GetIntrinsic('%Promise_resolve%', true);
var $PromiseResolve = $PromiseResolveOrig ? callBind($PromiseResolveOrig) : null;

var $isEnumerable = callBind(GetIntrinsic('%ObjectPrototype%').propertyIsEnumerable);
var $pushApply = callBind.apply(GetIntrinsic('%ArrayPrototype%').push);
var $gOPS = $SymbolValueOf ? $Object.getOwnPropertySymbols : null;

var OwnPropertyKeys = function OwnPropertyKeys(ES, source) {
	var ownKeys = keys(source);
	if ($gOPS) {
		$pushApply(ownKeys, $gOPS(source));
	}
	return ownKeys;
};

var ES2018 = assign(assign({}, ES2017), {
	EnumerableOwnPropertyNames: ES2017.EnumerableOwnProperties,

	// https://ecma-international.org/ecma-262/9.0/#sec-thissymbolvalue
	thisSymbolValue: function thisSymbolValue(value) {
		if (!$SymbolValueOf) {
			throw new SyntaxError('Symbols are not supported; thisSymbolValue requires that `value` be a Symbol or a Symbol object');
		}
		if (this.Type(value) === 'Symbol') {
			return value;
		}
		return $SymbolValueOf(value);
	},

	// https://www.ecma-international.org/ecma-262/9.0/#sec-isstringprefix
	IsStringPrefix: function IsStringPrefix(p, q) {
		if (this.Type(p) !== 'String') {
			throw new TypeError('Assertion failed: "p" must be a String');
		}

		if (this.Type(q) !== 'String') {
			throw new TypeError('Assertion failed: "q" must be a String');
		}

		if (p === q || p === '') {
			return true;
		}

		var pLength = p.length;
		var qLength = q.length;
		if (pLength >= qLength) {
			return false;
		}

		// assert: pLength < qLength

		for (var i = 0; i < pLength; i += 1) {
			if ($charAt(p, i) !== $charAt(q, i)) {
				return false;
			}
		}
		return true;
	},

	// https://www.ecma-international.org/ecma-262/9.0/#sec-tostring-applied-to-the-number-type
	NumberToString: function NumberToString(m) {
		if (this.Type(m) !== 'Number') {
			throw new TypeError('Assertion failed: "m" must be a String');
		}

		return $String(m);
	},

	// https://www.ecma-international.org/ecma-262/9.0/#sec-copydataproperties
	CopyDataProperties: function CopyDataProperties(target, source, excludedItems) {
		if (this.Type(target) !== 'Object') {
			throw new TypeError('Assertion failed: "target" must be an Object');
		}

		if (!this.IsArray(excludedItems)) {
			throw new TypeError('Assertion failed: "excludedItems" must be a List of Property Keys');
		}
		for (var i = 0; i < excludedItems.length; i += 1) {
			if (!this.IsPropertyKey(excludedItems[i])) {
				throw new TypeError('Assertion failed: "excludedItems" must be a List of Property Keys');
			}
		}

		if (typeof source === 'undefined' || source === null) {
			return target;
		}

		var ES = this;

		var fromObj = ES.ToObject(source);

		var sourceKeys = OwnPropertyKeys(ES, fromObj);
		forEach(sourceKeys, function (nextKey) {
			var excluded = false;

			forEach(excludedItems, function (e) {
				if (ES.SameValue(e, nextKey) === true) {
					excluded = true;
				}
			});

			var enumerable = $isEnumerable(fromObj, nextKey) || (
				// this is to handle string keys being non-enumerable in older engines
				typeof source === 'string'
				&& nextKey >= 0
				&& ES.IsInteger(ES.ToNumber(nextKey))
			);
			if (excluded === false && enumerable) {
				var propValue = ES.Get(fromObj, nextKey);
				ES.CreateDataProperty(target, nextKey, propValue);
			}
		});

		return target;
	},

	// https://ecma-international.org/ecma-262/9.0/#sec-promise-resolve
	PromiseResolve: function PromiseResolve(C, x) {
		if (!$PromiseResolve) {
			throw new SyntaxError('This environment does not support Promises.');
		}
		return $PromiseResolve(C, x);
	},

	// http://www.ecma-international.org/ecma-262/9.0/#sec-getsubstitution
	// eslint-disable-next-line max-statements, max-params, max-lines-per-function
	GetSubstitution: function GetSubstitution(matched, str, position, captures, namedCaptures, replacement) {
		if (this.Type(matched) !== 'String') {
			throw new $TypeError('Assertion failed: `matched` must be a String');
		}
		var matchLength = matched.length;

		if (this.Type(str) !== 'String') {
			throw new $TypeError('Assertion failed: `str` must be a String');
		}
		var stringLength = str.length;

		if (!this.IsInteger(position) || position < 0 || position > stringLength) {
			throw new $TypeError('Assertion failed: `position` must be a nonnegative integer, and less than or equal to the length of `string`, got ' + inspect(position));
		}

		var ES = this;
		var isStringOrHole = function (capture, index, arr) { return ES.Type(capture) === 'String' || !(index in arr); };
		if (!this.IsArray(captures) || !every(captures, isStringOrHole)) {
			throw new $TypeError('Assertion failed: `captures` must be a List of Strings, got ' + inspect(captures));
		}

		if (this.Type(replacement) !== 'String') {
			throw new $TypeError('Assertion failed: `replacement` must be a String');
		}

		var tailPos = position + matchLength;
		var m = captures.length;
		if (this.Type(namedCaptures) !== 'Undefined') {
			namedCaptures = this.ToObject(namedCaptures); // eslint-disable-line no-param-reassign
		}

		var result = '';
		for (var i = 0; i < replacement.length; i += 1) {
			// if this is a $, and it's not the end of the replacement
			var current = replacement[i];
			var isLast = (i + 1) >= replacement.length;
			var nextIsLast = (i + 2) >= replacement.length;
			if (current === '$' && !isLast) {
				var next = replacement[i + 1];
				if (next === '$') {
					result += '$';
					i += 1;
				} else if (next === '&') {
					result += matched;
					i += 1;
				} else if (next === '`') {
					result += position === 0 ? '' : strSlice(str, 0, position - 1);
					i += 1;
				} else if (next === "'") {
					result += tailPos >= stringLength ? '' : strSlice(str, tailPos);
					i += 1;
				} else {
					var nextNext = nextIsLast ? null : replacement[i + 2];
					if (isDigit(next) && next !== '0' && (nextIsLast || !isDigit(nextNext))) {
						// $1 through $9, and not followed by a digit
						var n = $parseInt(next, 10);
						// if (n > m, impl-defined)
						result += (n <= m && this.Type(captures[n - 1]) === 'Undefined') ? '' : captures[n - 1];
						i += 1;
					} else if (isDigit(next) && (nextIsLast || isDigit(nextNext))) {
						// $00 through $99
						var nn = next + nextNext;
						var nnI = $parseInt(nn, 10) - 1;
						// if nn === '00' or nn > m, impl-defined
						result += (nn <= m && this.Type(captures[nnI]) === 'Undefined') ? '' : captures[nnI];
						i += 2;
					} else if (next === '<') {
						// eslint-disable-next-line max-depth
						if (this.Type(namedCaptures) === 'Undefined') {
							result += '$<';
							i += 2;
						} else {
							var endIndex = $indexOf(replacement, '>', i);
							// eslint-disable-next-line max-depth
							if (endIndex > -1) {
								var groupName = strSlice(replacement, i, endIndex);
								var capture = this.Get(namedCaptures, groupName);
								// eslint-disable-next-line max-depth
								if (this.Type(capture) !== 'Undefined') {
									result += this.ToString(capture);
								}
								i += '$<' + groupName + '>'.length;
							}
						}
					} else {
						result += '$';
					}
				}
			} else {
				// the final $, or else not a $
				result += replacement[i];
			}
		}
		return result;
	}
});

delete ES2018.EnumerableOwnProperties; // replaced with EnumerableOwnPropertyNames

delete ES2018.IsPropertyDescriptor; // not an actual abstract operation

module.exports = ES2018;
