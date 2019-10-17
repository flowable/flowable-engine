'use strict';

var ES = require('../').ES5;
var test = require('tape');

var forEach = require('foreach');
var is = require('object-is');
var debug = require('object-inspect');

var v = require('./helpers/values');

test('ToPrimitive', function (t) {
	t.test('primitives', function (st) {
		var testPrimitive = function (primitive) {
			st.ok(is(ES.ToPrimitive(primitive), primitive), debug(primitive) + ' is returned correctly');
		};
		forEach(v.primitives, testPrimitive);
		st.end();
	});

	t.test('objects', function (st) {
		st.equal(ES.ToPrimitive(v.coercibleObject), v.coercibleObject.valueOf(), 'coercibleObject coerces to valueOf');
		st.equal(ES.ToPrimitive(v.coercibleObject, Number), v.coercibleObject.valueOf(), 'coercibleObject with hint Number coerces to valueOf');
		st.equal(ES.ToPrimitive(v.coercibleObject, String), v.coercibleObject.toString(), 'coercibleObject with hint String coerces to toString');
		st.equal(ES.ToPrimitive(v.coercibleFnObject), v.coercibleFnObject.toString(), 'coercibleFnObject coerces to toString');
		st.equal(ES.ToPrimitive(v.toStringOnlyObject), v.toStringOnlyObject.toString(), 'toStringOnlyObject returns toString');
		st.equal(ES.ToPrimitive(v.valueOfOnlyObject), v.valueOfOnlyObject.valueOf(), 'valueOfOnlyObject returns valueOf');
		st.equal(ES.ToPrimitive({}), '[object Object]', '{} with no hint coerces to Object#toString');
		st.equal(ES.ToPrimitive({}, String), '[object Object]', '{} with hint String coerces to Object#toString');
		st.equal(ES.ToPrimitive({}, Number), '[object Object]', '{} with hint Number coerces to Object#toString');
		st['throws'](function () { return ES.ToPrimitive(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws a TypeError');
		st['throws'](function () { return ES.ToPrimitive(v.uncoercibleFnObject); }, TypeError, 'uncoercibleFnObject throws a TypeError');
		st.end();
	});

	t.end();
});

test('ToBoolean', function (t) {
	t.equal(false, ES.ToBoolean(undefined), 'undefined coerces to false');
	t.equal(false, ES.ToBoolean(null), 'null coerces to false');
	t.equal(false, ES.ToBoolean(false), 'false returns false');
	t.equal(true, ES.ToBoolean(true), 'true returns true');
	forEach([0, -0, NaN], function (falsyNumber) {
		t.equal(false, ES.ToBoolean(falsyNumber), 'falsy number ' + falsyNumber + ' coerces to false');
	});
	forEach([Infinity, 42, 1, -Infinity], function (truthyNumber) {
		t.equal(true, ES.ToBoolean(truthyNumber), 'truthy number ' + truthyNumber + ' coerces to true');
	});
	t.equal(false, ES.ToBoolean(''), 'empty string coerces to false');
	t.equal(true, ES.ToBoolean('foo'), 'nonempty string coerces to true');
	forEach(v.objects, function (obj) {
		t.equal(true, ES.ToBoolean(obj), 'object coerces to true');
	});
	t.equal(true, ES.ToBoolean(v.uncoercibleObject), 'uncoercibleObject coerces to true');
	t.end();
});

test('ToNumber', function (t) {
	t.ok(is(NaN, ES.ToNumber(undefined)), 'undefined coerces to NaN');
	t.ok(is(ES.ToNumber(null), 0), 'null coerces to +0');
	t.ok(is(ES.ToNumber(false), 0), 'false coerces to +0');
	t.equal(1, ES.ToNumber(true), 'true coerces to 1');
	t.ok(is(NaN, ES.ToNumber(NaN)), 'NaN returns itself');
	forEach([0, -0, 42, Infinity, -Infinity], function (num) {
		t.equal(num, ES.ToNumber(num), num + ' returns itself');
	});
	forEach(['foo', '0', '4a', '2.0', 'Infinity', '-Infinity'], function (numString) {
		t.ok(is(+numString, ES.ToNumber(numString)), '"' + numString + '" coerces to ' + Number(numString));
	});
	forEach(v.objects, function (object) {
		t.ok(is(ES.ToNumber(object), ES.ToNumber(ES.ToPrimitive(object))), 'object ' + object + ' coerces to same as ToPrimitive of object does');
	});
	t['throws'](function () { return ES.ToNumber(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.end();
});

test('ToInteger', function (t) {
	t.ok(is(0, ES.ToInteger(NaN)), 'NaN coerces to +0');
	forEach([0, Infinity, 42], function (num) {
		t.ok(is(num, ES.ToInteger(num)), num + ' returns itself');
		t.ok(is(-num, ES.ToInteger(-num)), '-' + num + ' returns itself');
	});
	t.equal(3, ES.ToInteger(Math.PI), 'pi returns 3');
	t['throws'](function () { return ES.ToInteger(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.end();
});

test('ToInt32', function (t) {
	t.ok(is(0, ES.ToInt32(NaN)), 'NaN coerces to +0');
	forEach([0, Infinity], function (num) {
		t.ok(is(0, ES.ToInt32(num)), num + ' returns +0');
		t.ok(is(0, ES.ToInt32(-num)), '-' + num + ' returns +0');
	});
	t['throws'](function () { return ES.ToInt32(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.ok(is(ES.ToInt32(0x100000000), 0), '2^32 returns +0');
	t.ok(is(ES.ToInt32(0x100000000 - 1), -1), '2^32 - 1 returns -1');
	t.ok(is(ES.ToInt32(0x80000000), -0x80000000), '2^31 returns -2^31');
	t.ok(is(ES.ToInt32(0x80000000 - 1), 0x80000000 - 1), '2^31 - 1 returns 2^31 - 1');
	forEach([0, Infinity, NaN, 0x100000000, 0x80000000, 0x10000, 0x42], function (num) {
		t.ok(is(ES.ToInt32(num), ES.ToInt32(ES.ToUint32(num))), 'ToInt32(x) === ToInt32(ToUint32(x)) for 0x' + num.toString(16));
		t.ok(is(ES.ToInt32(-num), ES.ToInt32(ES.ToUint32(-num))), 'ToInt32(x) === ToInt32(ToUint32(x)) for -0x' + num.toString(16));
	});
	t.end();
});

test('ToUint32', function (t) {
	t.ok(is(0, ES.ToUint32(NaN)), 'NaN coerces to +0');
	forEach([0, Infinity], function (num) {
		t.ok(is(0, ES.ToUint32(num)), num + ' returns +0');
		t.ok(is(0, ES.ToUint32(-num)), '-' + num + ' returns +0');
	});
	t['throws'](function () { return ES.ToUint32(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.ok(is(ES.ToUint32(0x100000000), 0), '2^32 returns +0');
	t.ok(is(ES.ToUint32(0x100000000 - 1), 0x100000000 - 1), '2^32 - 1 returns 2^32 - 1');
	t.ok(is(ES.ToUint32(0x80000000), 0x80000000), '2^31 returns 2^31');
	t.ok(is(ES.ToUint32(0x80000000 - 1), 0x80000000 - 1), '2^31 - 1 returns 2^31 - 1');
	forEach([0, Infinity, NaN, 0x100000000, 0x80000000, 0x10000, 0x42], function (num) {
		t.ok(is(ES.ToUint32(num), ES.ToUint32(ES.ToInt32(num))), 'ToUint32(x) === ToUint32(ToInt32(x)) for 0x' + num.toString(16));
		t.ok(is(ES.ToUint32(-num), ES.ToUint32(ES.ToInt32(-num))), 'ToUint32(x) === ToUint32(ToInt32(x)) for -0x' + num.toString(16));
	});
	t.end();
});

test('ToUint16', function (t) {
	t.ok(is(0, ES.ToUint16(NaN)), 'NaN coerces to +0');
	forEach([0, Infinity], function (num) {
		t.ok(is(0, ES.ToUint16(num)), num + ' returns +0');
		t.ok(is(0, ES.ToUint16(-num)), '-' + num + ' returns +0');
	});
	t['throws'](function () { return ES.ToUint16(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.ok(is(ES.ToUint16(0x100000000), 0), '2^32 returns +0');
	t.ok(is(ES.ToUint16(0x100000000 - 1), 0x10000 - 1), '2^32 - 1 returns 2^16 - 1');
	t.ok(is(ES.ToUint16(0x80000000), 0), '2^31 returns +0');
	t.ok(is(ES.ToUint16(0x80000000 - 1), 0x10000 - 1), '2^31 - 1 returns 2^16 - 1');
	t.ok(is(ES.ToUint16(0x10000), 0), '2^16 returns +0');
	t.ok(is(ES.ToUint16(0x10000 - 1), 0x10000 - 1), '2^16 - 1 returns 2^16 - 1');
	t.end();
});

test('ToString', function (t) {
	t['throws'](function () { return ES.ToString(v.uncoercibleObject); }, TypeError, 'uncoercibleObject throws');
	t.end();
});

test('ToObject', function (t) {
	t['throws'](function () { return ES.ToObject(undefined); }, TypeError, 'undefined throws');
	t['throws'](function () { return ES.ToObject(null); }, TypeError, 'null throws');
	forEach(v.numbers, function (number) {
		var obj = ES.ToObject(number);
		t.equal(typeof obj, 'object', 'number ' + number + ' coerces to object');
		t.equal(true, obj instanceof Number, 'object of ' + number + ' is Number object');
		t.ok(is(obj.valueOf(), number), 'object of ' + number + ' coerces to ' + number);
	});
	t.end();
});

test('CheckObjectCoercible', function (t) {
	t['throws'](function () { return ES.CheckObjectCoercible(undefined); }, TypeError, 'undefined throws');
	t['throws'](function () { return ES.CheckObjectCoercible(null); }, TypeError, 'null throws');
	var checkCoercible = function (value) {
		t.doesNotThrow(function () { return ES.CheckObjectCoercible(value); }, debug(value) + ' does not throw');
	};
	forEach(v.objects.concat(v.nonNullPrimitives), checkCoercible);
	t.end();
});

test('IsCallable', function (t) {
	t.equal(true, ES.IsCallable(function () {}), 'function is callable');
	var nonCallables = [/a/g, {}, Object.prototype, NaN].concat(v.primitives);
	forEach(nonCallables, function (nonCallable) {
		t.equal(false, ES.IsCallable(nonCallable), debug(nonCallable) + ' is not callable');
	});
	t.end();
});

test('SameValue', function (t) {
	t.equal(true, ES.SameValue(NaN, NaN), 'NaN is SameValue as NaN');
	t.equal(false, ES.SameValue(0, -0), '+0 is not SameValue as -0');
	forEach(v.objects.concat(v.primitives), function (val) {
		t.equal(val === val, ES.SameValue(val, val), debug(val) + ' is SameValue to itself');
	});
	t.end();
});

test('Type', function (t) {
	t.equal(ES.Type(), 'Undefined', 'Type() is Undefined');
	t.equal(ES.Type(undefined), 'Undefined', 'Type(undefined) is Undefined');
	t.equal(ES.Type(null), 'Null', 'Type(null) is Null');
	t.equal(ES.Type(true), 'Boolean', 'Type(true) is Boolean');
	t.equal(ES.Type(false), 'Boolean', 'Type(false) is Boolean');
	t.equal(ES.Type(0), 'Number', 'Type(0) is Number');
	t.equal(ES.Type(NaN), 'Number', 'Type(NaN) is Number');
	t.equal(ES.Type('abc'), 'String', 'Type("abc") is String');
	t.equal(ES.Type(function () {}), 'Object', 'Type(function () {}) is Object');
	t.equal(ES.Type({}), 'Object', 'Type({}) is Object');
	t.end();
});

test('IsPropertyDescriptor', function (t) {
	forEach(v.primitives, function (primitive) {
		t.equal(ES.IsPropertyDescriptor(primitive), false, debug(primitive) + ' is not a Property Descriptor');
	});

	t.equal(ES.IsPropertyDescriptor({ invalid: true }), false, 'invalid keys not allowed on a Property Descriptor');

	t.equal(ES.IsPropertyDescriptor({}), true, 'empty object is an incomplete Property Descriptor');

	t.equal(ES.IsPropertyDescriptor(v.accessorDescriptor()), true, 'accessor descriptor is a Property Descriptor');
	t.equal(ES.IsPropertyDescriptor(v.mutatorDescriptor()), true, 'mutator descriptor is a Property Descriptor');
	t.equal(ES.IsPropertyDescriptor(v.dataDescriptor()), true, 'data descriptor is a Property Descriptor');
	t.equal(ES.IsPropertyDescriptor(v.genericDescriptor()), true, 'generic descriptor is a Property Descriptor');

	t['throws'](
		function () { ES.IsPropertyDescriptor(v.bothDescriptor()); },
		TypeError,
		'a Property Descriptor can not be both a Data and an Accessor Descriptor'
	);

	t['throws'](
		function () { ES.IsPropertyDescriptor(v.bothDescriptorWritable()); },
		TypeError,
		'a Property Descriptor can not be both a Data and an Accessor Descriptor'
	);

	t.end();
});

test('IsAccessorDescriptor', function (t) {
	forEach(v.nonNullPrimitives.concat(null), function (primitive) {
		t['throws'](function () { ES.IsAccessorDescriptor(primitive); }, TypeError, debug(primitive) + ' is not a Property Descriptor');
	});

	t.equal(ES.IsAccessorDescriptor(), false, 'no value is not an Accessor Descriptor');
	t.equal(ES.IsAccessorDescriptor(undefined), false, 'undefined value is not an Accessor Descriptor');

	t.equal(ES.IsAccessorDescriptor(v.accessorDescriptor()), true, 'accessor descriptor is an Accessor Descriptor');
	t.equal(ES.IsAccessorDescriptor(v.mutatorDescriptor()), true, 'mutator descriptor is an Accessor Descriptor');
	t.equal(ES.IsAccessorDescriptor(v.dataDescriptor()), false, 'data descriptor is not an Accessor Descriptor');
	t.equal(ES.IsAccessorDescriptor(v.genericDescriptor()), false, 'generic descriptor is not an Accessor Descriptor');

	t.end();
});

test('IsDataDescriptor', function (t) {
	forEach(v.nonNullPrimitives.concat(null), function (primitive) {
		t['throws'](function () { ES.IsDataDescriptor(primitive); }, TypeError, debug(primitive) + ' is not a Property Descriptor');
	});

	t.equal(ES.IsDataDescriptor(), false, 'no value is not a Data Descriptor');
	t.equal(ES.IsDataDescriptor(undefined), false, 'undefined value is not a Data Descriptor');

	t.equal(ES.IsDataDescriptor(v.accessorDescriptor()), false, 'accessor descriptor is not a Data Descriptor');
	t.equal(ES.IsDataDescriptor(v.mutatorDescriptor()), false, 'mutator descriptor is not a Data Descriptor');
	t.equal(ES.IsDataDescriptor(v.dataDescriptor()), true, 'data descriptor is a Data Descriptor');
	t.equal(ES.IsDataDescriptor(v.genericDescriptor()), false, 'generic descriptor is not a Data Descriptor');

	t.end();
});

test('IsGenericDescriptor', function (t) {
	forEach(v.nonNullPrimitives.concat(null), function (primitive) {
		t['throws'](
			function () { ES.IsGenericDescriptor(primitive); },
			TypeError,
			debug(primitive) + ' is not a Property Descriptor'
		);
	});

	t.equal(ES.IsGenericDescriptor(), false, 'no value is not a Data Descriptor');
	t.equal(ES.IsGenericDescriptor(undefined), false, 'undefined value is not a Data Descriptor');

	t.equal(ES.IsGenericDescriptor(v.accessorDescriptor()), false, 'accessor descriptor is not a generic Descriptor');
	t.equal(ES.IsGenericDescriptor(v.mutatorDescriptor()), false, 'mutator descriptor is not a generic Descriptor');
	t.equal(ES.IsGenericDescriptor(v.dataDescriptor()), false, 'data descriptor is not a generic Descriptor');

	t.equal(ES.IsGenericDescriptor(v.genericDescriptor()), true, 'generic descriptor is a generic Descriptor');

	t.end();
});

test('FromPropertyDescriptor', function (t) {
	t.equal(ES.FromPropertyDescriptor(), undefined, 'no value begets undefined');
	t.equal(ES.FromPropertyDescriptor(undefined), undefined, 'undefined value begets undefined');

	forEach(v.nonNullPrimitives.concat(null), function (primitive) {
		t['throws'](
			function () { ES.FromPropertyDescriptor(primitive); },
			TypeError,
			debug(primitive) + ' is not a Property Descriptor'
		);
	});

	var accessor = v.accessorDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(accessor), {
		get: accessor['[[Get]]'],
		set: accessor['[[Set]]'],
		enumerable: !!accessor['[[Enumerable]]'],
		configurable: !!accessor['[[Configurable]]']
	});

	var mutator = v.mutatorDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(mutator), {
		get: mutator['[[Get]]'],
		set: mutator['[[Set]]'],
		enumerable: !!mutator['[[Enumerable]]'],
		configurable: !!mutator['[[Configurable]]']
	});
	var data = v.dataDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(data), {
		value: data['[[Value]]'],
		writable: data['[[Writable]]'],
		enumerable: !!data['[[Enumerable]]'],
		configurable: !!data['[[Configurable]]']
	});

	t['throws'](
		function () { ES.FromPropertyDescriptor(v.genericDescriptor()); },
		TypeError,
		'a complete Property Descriptor is required'
	);

	t.end();
});

test('ToPropertyDescriptor', function (t) {
	forEach(v.nonNullPrimitives.concat(null), function (primitive) {
		t['throws'](
			function () { ES.ToPropertyDescriptor(primitive); },
			TypeError,
			debug(primitive) + ' is not an Object'
		);
	});

	var accessor = v.accessorDescriptor();
	t.deepEqual(ES.ToPropertyDescriptor({
		get: accessor['[[Get]]'],
		enumerable: !!accessor['[[Enumerable]]'],
		configurable: !!accessor['[[Configurable]]']
	}), accessor);

	var mutator = v.mutatorDescriptor();
	t.deepEqual(ES.ToPropertyDescriptor({
		set: mutator['[[Set]]'],
		enumerable: !!mutator['[[Enumerable]]'],
		configurable: !!mutator['[[Configurable]]']
	}), mutator);

	var data = v.descriptors.nonConfigurable(v.dataDescriptor());
	t.deepEqual(ES.ToPropertyDescriptor({
		value: data['[[Value]]'],
		writable: data['[[Writable]]'],
		configurable: !!data['[[Configurable]]']
	}), data);

	var both = v.bothDescriptor();
	t['throws'](
		function () {
			ES.ToPropertyDescriptor({ get: both['[[Get]]'], value: both['[[Value]]'] });
		},
		TypeError,
		'data and accessor descriptors are mutually exclusive'
	);

	t['throws'](
		function () { ES.ToPropertyDescriptor({ get: 'not callable' }); },
		TypeError,
		'"get" must be undefined or callable'
	);

	t['throws'](
		function () { ES.ToPropertyDescriptor({ set: 'not callable' }); },
		TypeError,
		'"set" must be undefined or callable'
	);

	t.end();
});

test('Abstract Equality Comparison', function (t) {
	t.test('same types use ===', function (st) {
		forEach(v.primitives.concat(v.objects), function (value) {
			st.equal(ES['Abstract Equality Comparison'](value, value), value === value, debug(value) + ' is abstractly equal to itself');
		});
		st.end();
	});

	t.test('different types coerce', function (st) {
		var pairs = [
			[null, undefined],
			[3, '3'],
			[true, '3'],
			[true, 3],
			[false, 0],
			[false, '0'],
			[3, [3]],
			['3', [3]],
			[true, [1]],
			[false, [0]],
			[String(v.coercibleObject), v.coercibleObject],
			[Number(String(v.coercibleObject)), v.coercibleObject],
			[Number(v.coercibleObject), v.coercibleObject],
			[String(Number(v.coercibleObject)), v.coercibleObject]
		];
		forEach(pairs, function (pair) {
			var a = pair[0];
			var b = pair[1];
			// eslint-disable-next-line eqeqeq
			st.equal(ES['Abstract Equality Comparison'](a, b), a == b, debug(a) + ' == ' + debug(b));
			// eslint-disable-next-line eqeqeq
			st.equal(ES['Abstract Equality Comparison'](b, a), b == a, debug(b) + ' == ' + debug(a));
		});
		st.end();
	});

	t.end();
});

test('Strict Equality Comparison', function (t) {
	t.test('same types use ===', function (st) {
		forEach(v.primitives.concat(v.objects), function (value) {
			st.equal(ES['Strict Equality Comparison'](value, value), value === value, debug(value) + ' is strictly equal to itself');
		});
		st.end();
	});

	t.test('different types are not ===', function (st) {
		var pairs = [
			[null, undefined],
			[3, '3'],
			[true, '3'],
			[true, 3],
			[false, 0],
			[false, '0'],
			[3, [3]],
			['3', [3]],
			[true, [1]],
			[false, [0]],
			[String(v.coercibleObject), v.coercibleObject],
			[Number(String(v.coercibleObject)), v.coercibleObject],
			[Number(v.coercibleObject), v.coercibleObject],
			[String(Number(v.coercibleObject)), v.coercibleObject]
		];
		forEach(pairs, function (pair) {
			var a = pair[0];
			var b = pair[1];
			st.equal(ES['Strict Equality Comparison'](a, b), a === b, debug(a) + ' === ' + debug(b));
			st.equal(ES['Strict Equality Comparison'](b, a), b === a, debug(b) + ' === ' + debug(a));
		});
		st.end();
	});

	t.end();
});

test('Abstract Relational Comparison', function (t) {
	t.test('at least one operand is NaN', function (st) {
		st.equal(ES['Abstract Relational Comparison'](NaN, {}, true), undefined, 'LeftFirst: first is NaN, returns undefined');
		st.equal(ES['Abstract Relational Comparison']({}, NaN, true), undefined, 'LeftFirst: second is NaN, returns undefined');
		st.equal(ES['Abstract Relational Comparison'](NaN, {}, false), undefined, '!LeftFirst: first is NaN, returns undefined');
		st.equal(ES['Abstract Relational Comparison']({}, NaN, false), undefined, '!LeftFirst: second is NaN, returns undefined');
		st.end();
	});

	t.equal(ES['Abstract Relational Comparison'](3, 4, true), true, 'LeftFirst: 3 is less than 4');
	t.equal(ES['Abstract Relational Comparison'](4, 3, true), false, 'LeftFirst: 3 is not less than 4');
	t.equal(ES['Abstract Relational Comparison'](3, 4, false), true, '!LeftFirst: 3 is less than 4');
	t.equal(ES['Abstract Relational Comparison'](4, 3, false), false, '!LeftFirst: 3 is not less than 4');

	t.equal(ES['Abstract Relational Comparison']('3', '4', true), true, 'LeftFirst: "3" is less than "4"');
	t.equal(ES['Abstract Relational Comparison']('4', '3', true), false, 'LeftFirst: "3" is not less than "4"');
	t.equal(ES['Abstract Relational Comparison']('3', '4', false), true, '!LeftFirst: "3" is less than "4"');
	t.equal(ES['Abstract Relational Comparison']('4', '3', false), false, '!LeftFirst: "3" is not less than "4"');

	t.equal(ES['Abstract Relational Comparison'](v.coercibleObject, 42, true), true, 'LeftFirst: coercible object is less than 42');
	t.equal(ES['Abstract Relational Comparison'](42, v.coercibleObject, true), false, 'LeftFirst: 42 is not less than coercible object');
	t.equal(ES['Abstract Relational Comparison'](v.coercibleObject, 42, false), true, '!LeftFirst: coercible object is less than 42');
	t.equal(ES['Abstract Relational Comparison'](42, v.coercibleObject, false), false, '!LeftFirst: 42 is not less than coercible object');

	t.equal(ES['Abstract Relational Comparison'](v.coercibleObject, '3', true), false, 'LeftFirst: coercible object is not less than "3"');
	t.equal(ES['Abstract Relational Comparison']('3', v.coercibleObject, true), false, 'LeftFirst: "3" is not less than coercible object');
	t.equal(ES['Abstract Relational Comparison'](v.coercibleObject, '3', false), false, '!LeftFirst: coercible object is not less than "3"');
	t.equal(ES['Abstract Relational Comparison']('3', v.coercibleObject, false), false, '!LeftFirst: "3" is not less than coercible object');

	t.end();
});

test('FromPropertyDescriptor', function (t) {
	t.equal(ES.FromPropertyDescriptor(), undefined, 'no value begets undefined');
	t.equal(ES.FromPropertyDescriptor(undefined), undefined, 'undefined value begets undefined');

	forEach(v.nonUndefinedPrimitives, function (primitive) {
		t['throws'](
			function () { ES.FromPropertyDescriptor(primitive); },
			TypeError,
			debug(primitive) + ' is not a Property Descriptor'
		);
	});

	var accessor = v.accessorDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(accessor), {
		get: accessor['[[Get]]'],
		set: accessor['[[Set]]'],
		enumerable: !!accessor['[[Enumerable]]'],
		configurable: !!accessor['[[Configurable]]']
	});

	var mutator = v.mutatorDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(mutator), {
		get: mutator['[[Get]]'],
		set: mutator['[[Set]]'],
		enumerable: !!mutator['[[Enumerable]]'],
		configurable: !!mutator['[[Configurable]]']
	});
	var data = v.dataDescriptor();
	t.deepEqual(ES.FromPropertyDescriptor(data), {
		value: data['[[Value]]'],
		writable: data['[[Writable]]'],
		enumerable: !!data['[[Enumerable]]'],
		configurable: !!data['[[Configurable]]']
	});

	t['throws'](
		function () { ES.FromPropertyDescriptor(v.genericDescriptor()); },
		TypeError,
		'a complete Property Descriptor is required'
	);

	t.end();
});
