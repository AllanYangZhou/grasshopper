var express = require('express');
var router = express.Router();

var querystring = require('querystring');
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
      list_of_uids.push({uid: connecteduser.uid});
    });
    // get the data for the connected people and send it as a response
    var connected_data = [];
    User.find(list_of_uids, function(err, users) {
      if (err)
        console.log("Failed to look up connected user in Users");

      users.forEach(function(user){
        if (user.uid !== uid) {
          connected_data.push({
            uid: user.uid,
            name: user.name,
            device_type: user.device_type
          });
        }
      });

      res.status(200).json({
        connected_data: connected_data
      });
    });
  });
});

// when someone shares content
router.post('/share', function(req, res) {
  var src_uid = req.body.src_uid;
  var target_uids = req.body.target_uids;
  var content_type = req.body.content_type;
  var content = {
    message: req.body.content,
    date: req.body.timestamp
  }

  var list_of_uids = [];
  target_uids.forEach(function(target_uid) {
    list_of_uids.push({uid: target_uid});
  });
  User.find(list_of_uids, function(err, users) {
    if (err)
      console.log("Error finding targets to share with");
    // separate recipients
    var mobile_recipients = [];
    var desktop_recipients = [];
    users.forEach(function(user) {
      if (user.device_type === "mobile")
        mobile_recipients.append(user.name);
      else
        desktop_recipients.append(user.name);
    });

    // see http://stackoverflow.com/a/6158966
    // send magnet requests
    var magnet_post_data = querystring.stringify({
      'recipient_usernames': mobile_recipients,
      'content': content,
      'receipt': false
    });

    var magnet_post_options = {
      host: '',
      port: '',
      path: '',
      method: 'POST',
      headers: {
        'X-mmx-app-id': '',
        'X-mmx-api-key': '',
        'Content-Type': 'application/json',
        'Content-Length': magnet_post_data.length
      }
    };

    var magnet_post_req = http.request(magnet_post_options, function(res) {
      res.setEncoding('utf8');
      res.on('data', function(chunk) {
        console.log('Response: ' + chunk);
      });
    });

    magnet_post_req.write(magnet_post_data);
    magnet_post_req.end();
  });
});

module.exports = router;
