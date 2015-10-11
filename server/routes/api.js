var express = require('express');
var router = express.Router();

var request = require('request');
var http = require('http');

var mongoose = require('mongoose');
mongoose.connect('mongodb://localhost/grasshopper');

var User = mongoose.model('User', {
  uid: String,
  name: String,
  device_type: String,
});

var ConnectedUsers = mongoose.model('ConnectedUsers', {
  uid: String
});

// when someone connects to the server
router.post('/connect', function(req, res) {
  var uid = req.body.uid;
  var name = req.body.name
  var device_type = req.body.device_type;

  // check if user exists, if not create them
  User.count({uid: uid}, function(err, count) {
    if (err)
      console.log("error counting users");
    if (!count) {
      var user = new User({
        uid: uid,
        name: name,
        device_type: device_type
      });
      user.save(function(err) {
        if (err)
          console.log("Failed to create new user");
      });
    } else {
      var conditions = { uid: uid};
      var update = {
        name: name,
        device_type: device_type
      };
      User.update(conditions, update, {}, function(err, numAffected) {
        if (err)
          console.log(err);
      });
    }
  });

  // check if user is in ConnectedUsers, if not add them
  ConnectedUsers.count({uid: uid}, function(err, count) {
    if (!count) {
      var connecteduser = new ConnectedUsers({
        uid: uid
      });
      connecteduser.save(function(err) {
        if (err)
          console.log("Failed to save to connected users");
      });
    }
  });

  // send list of connected users back to requester
  ConnectedUsers.find({}, function(err, connected) {
    if (err)
      console.log("Error trying to query connected users");
    // construct list to query ConnectedUsers
    var list_of_uids = [];
    connected.forEach(function(connecteduser) {
      if (connecteduser.uid !== uid)
        list_of_uids.push({uid: connecteduser.uid});
    });
    // get the data for the connected people and send it as a response
    var connected_data = [];
    User.find({"$or": list_of_uids}, function(err, users) {
      if (err)
        console.log("Failed to look up connected user in Users");

      if (users) {
        users.forEach(function(user){
          connected_data.push({
            uid: user.uid,
            name: user.name,
            device_type: user.device_type
          });
        });
      }

      res.status(200).json({
        connected_data: connected_data
      });
    });
  });
});

// when someone shares content
router.post('/share', function(req, res) {
  var src_uid = req.body.src_uid;
  var target_uids = eval(req.body.target_uids); //total hack
  var content_type = req.body.content_type;
  var content = {
    message: req.body.content,
    date: Date.now()
  }

  var list_of_uids = [];
  target_uids.forEach(function(target_uid) {
    list_of_uids.push({uid: target_uid});
  });

  User.find({"$or": list_of_uids}, function(err, users) {
    if (err)
      console.log("Error finding targets to share with");
    // separate recipients
    var mobile_recipients = [];
    var desktop_recipients = [];
    users.forEach(function(user) {
      if (user.uid != src_uid) {
        if (user.device_type === "mobile")
          mobile_recipients.push(user.name);
        else
          desktop_recipients.push(user.uid);
      }
    });

    // send magnet requests
    console.log(mobile_recipients);
    if (mobile_recipients.length) {
      request({
        method: 'POST',
        url: 'http://40.122.208.196:5220/mmxmgmt/api/v1/send_message',
        headers: {
          'X-mmx-app-id': 'j7giflogogp',
          'X-mmx-api-key': '32e7d37e-2205-4782-81f4-1e05a124dff8'
        },
        json: true,
        body: {
          recipientUsernames: mobile_recipients,
          content: content,
          receipt: false
        }
      }, function(err, response, body) {
        console.log(JSON.stringify(response));
        if (err) {
          console.log("Failed to make request to magnet" + err);
          res.status(500).end();
        } else {
          res.status(200).end();
        }
      });
    }

    // send gcm requests
    if (desktop_recipients.length) {
      for (recipient in desktop_recipients) {
        request({
          method: 'POST',
          url: 'https://gcm-http.googleapis.com/gcm/send',
          headers: {'Authorization': 'key=AIzaSyDPo_ZESJy9y6oOB8abtyya5lgZsOsk7yU'},
          json: true,
          body: {
            data: {
              content: content
            },
            to: desktop_recipients[recipient]
          }
        }, function(err, response, body) {
          if (err) {
            console.log("Failed to make request to gcm" + err);
            res.status(500).end();
          } else {
            res.status(200).end();
          }
        });
      }
    }
  });
});

module.exports = router;
