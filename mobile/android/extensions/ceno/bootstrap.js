ChromeUtils.import("resource://gre/modules/Services.jsm");

this.install = function() {};
this.uninstall = function() {};

this.startup = function({webExtension}) {
  webExtension.startup().then((api) => {
    return Promise.resolve();
  }).catch((ex) => {
    console.error(ex);
  });
};

this.shutdown = function() {};
