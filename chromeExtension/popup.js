document.addEventListener('DOMContentLoaded', function() {
  chrome.storage.local.get("name", function(result) {
    $('#name').val(result["name"]);
  });

  $('#change').submit(function(event) {
    event.preventDefault();
    var newname = $("#name").val();
    chrome.storage.local.set({name: newname}, function() {
      console.log("Name changed!");
    });

    chrome.storage.local.get("regId", function(result) {
      var regId = result["regId"];
      var data = {
        "uid": regId,
        "name": newname,
        "device_type": "desktop"
      };

      $.ajax({
          method: "POST",
          url: "http://40.122.208.196:3002/api/connect",
          data: data,
          success: function() { console.log("success"); },
          error: function() { console.log("failure"); },
      });
    });
  });
}, false);
