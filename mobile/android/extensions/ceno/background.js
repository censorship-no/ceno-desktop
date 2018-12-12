// Useful links:
// https://github.com/mdn/webextensions-examples/tree/master/http-response
// https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/webRequest/onHeadersReceived
//
// Chrome's webRequest doc is a bit better ATM
// https://developer.chrome.com/extensions/webRequest

var versionError = false;

function setCookie(e) {
  if (!versionError) {
    for (var i in e.responseHeaders) {
        var h = e.responseHeaders[i];
        if (h.name == "X-Ouinet-Error") {
            var ec = h.value.substring(0, 2);
            if (ec === "0 " || ec === "1 ") {
                versionError = true;
            } 
        }
    }
  }

  if (versionError) {
    return {
      redirectUrl: browser.extension.getURL("update-page/index.html"),
    };
  }
}

// Listen for onHeaderReceived for the target page.
browser.webRequest.onHeadersReceived.addListener(
  setCookie,
  {urls: ["<all_urls>"]},
  ["blocking", "responseHeaders"]
);
