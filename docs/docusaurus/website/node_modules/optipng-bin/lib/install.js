'use strict';
const binBuild = require('bin-build');
const log = require('logalot');
const bin = require('.');

bin.run(['--version']).then(() => {
	log.success('optipng pre-build test passed successfully');
}).catch(error => {
	log.warn(error.message);
	log.warn('optipng pre-build test failed');
	log.info('compiling from source');

	binBuild.url('https://downloads.sourceforge.net/project/optipng/OptiPNG/optipng-0.7.7/optipng-0.7.7.tar.gz', [
		`./configure --with-system-zlib --prefix="${bin.dest()}" --bindir="${bin.dest()}"`,
		'make install'
	]).then(() => {
		log.success('optipng built successfully');
	}).catch(error => {
		log.error(error.stack);
	});
});
