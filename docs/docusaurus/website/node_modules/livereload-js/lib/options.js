(function() {
  var Options;

  exports.Options = Options = (function() {
    function Options() {
      this.https = false;
      this.host = null;
      this.port = 35729;
      this.snipver = null;
      this.ext = null;
      this.extver = null;
      this.mindelay = 1000;
      this.maxdelay = 60000;
      this.handshake_timeout = 5000;
    }

    Options.prototype.set = function(name, value) {
      if (typeof value === 'undefined') {
        return;
      }
      if (!isNaN(+value)) {
        value = +value;
      }
      return this[name] = value;
    };

    return Options;

  })();

  Options.extract = function(document) {
    var element, i, j, keyAndValue, len, len1, m, mm, options, pair, ref, ref1, src;
    ref = document.getElementsByTagName('script');
    for (i = 0, len = ref.length; i < len; i++) {
      element = ref[i];
      if ((src = element.src) && (m = src.match(/^[^:]+:\/\/(.*)\/z?livereload\.js(?:\?(.*))?$/))) {
        options = new Options();
        options.https = src.indexOf("https") === 0;
        if (mm = m[1].match(/^([^\/:]+)(?::(\d+))?(\/+.*)?$/)) {
          options.host = mm[1];
          if (mm[2]) {
            options.port = parseInt(mm[2], 10);
          }
        }
        if (m[2]) {
          ref1 = m[2].split('&');
          for (j = 0, len1 = ref1.length; j < len1; j++) {
            pair = ref1[j];
            if ((keyAndValue = pair.split('=')).length > 1) {
              options.set(keyAndValue[0].replace(/-/g, '_'), keyAndValue.slice(1).join('='));
            }
          }
        }
        return options;
      }
    }
    return null;
  };

}).call(this);
