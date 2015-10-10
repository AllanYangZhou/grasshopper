function showIndex() {
      
     //this takes the current URL of the tab and creates another tab with the same URL 
    chrome.tabs.query({'active': true, 'lastFocusedWindow': true}, function (tabs) {
        var hurl = tabs[0].url;
        chrome.tabs.create({ url: hurl });
    });

    //this takes a URL and downloads it 
    chrome.downloads.download( {url: "http://static.giantbomb.com/uploads/original/3/34821/2577499-cat.jpg"}) 

}


document.addEventListener('DOMContentLoaded', function() { 

       document.getElementById('index').addEventListener("click", showIndex);

}, false);