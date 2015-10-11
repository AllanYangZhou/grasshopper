function registerCallback(registrationId) {
  // le bad hack
  regId = registrationId;
  if (chrome.runtime.lastError) {
    return;
  }

  sendRegistrationId(function(succeed) {
    if (succeed)
      chrome.storage.local.set({registered: true});
  });
}

function sendRegistrationId(callback) {
  // send registration to your application server
  // in a secure way.
  alert(regId);
  callback(true);
}

function startRegistration() {
  chrome.storage.local.get("registered", function(result) {
    if (result["registered"])
      return;

    var senderIds = ["990221519663"];
    chrome.gcm.register(senderIds, registerCallback);
  })
}

chrome.runtime.onInstalled.addListener(startRegistration);
chrome.runtime.onStartup.addListener(startRegistration);
