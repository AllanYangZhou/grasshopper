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
  var data = {
    "uid": regId,
    "name": "BOB",
    "device_type": "desktop"
  };

  $.ajax({
      method: "POST",
      url: "http://40.122.208.196:3002/api/connect",
      data: data,
      success: function() { callback(true); },
      error: function() { callback(false); },
  });
}

function startRegistration() {
  chrome.storage.local.get("registered", function(result) {
    if (result["registered"]) {
      return;
    }

    var senderIds = ["990221519663"];
    chrome.gcm.register(senderIds, registerCallback);
  })
}

chrome.runtime.onInstalled.addListener(startRegistration);
chrome.runtime.onStartup.addListener(startRegistration);

chrome.gcm.onMessage.addListener(function (message) {
  // load link
  var url = JSON.parse(message["data"]["content"])["message"];
  chrome.tabs.create({ url: url });
});
