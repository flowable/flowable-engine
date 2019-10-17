'use strict';
const path = require('path');
const binBuild = require('bin-build');
const log = require('logalot');
const bin = require('.');

const args = [
	'-copy',
	'none',
	'-optimize',
	'-outfile',
	path.join(__dirname, '../test/fixtures/test-optimized.jpg'),
	path.join(__dirname, '../test/fixtures/test.jpg')
];

bin.run(args).then(() => {
	log.success('jpegtran pre-build test passed successfully');
}).catch(error => {
	log.warn(error.message);
	log.warn('jpegtran pre-build test failed');
	log.info('compiling from source');

	const cfg = [
		'./configure --disable-shared',
		`--prefix="${bin.dest()}" --bindir="${bin.dest()}"`
	].join(' ');

	binBuild.url('https://downloads.sourceforge.net/project/libjpeg-turbo/1.5.1/libjpeg-turbo-1.5.1.tar.gz', [
		'touch configure.ac aclocal.m4 configure Makefile.am Makefile.in',
		cfg,
		'make install'
	]).then(() => {
		log.success('jpegtran built successfully');
	}).catch(error => {
		log.error(error.stack);
	});
});
