var ws;
var inputbox;

function change_pano() {
    console.log("change pano");
    var msg = { 'type' : 'pano_viewsync', 'data' : { 'src' : 'control panel', 'extra': {
        'type' : 'pano', 'fileurl' : inputbox.value
    }}};

    ws.send(JSON.stringify(msg));
}

function init() {
	ws = new WebSocket( "ws://" + window.location.host + "/websocket" ); // arg?
    ws.onmessage = function ( evt ) {
        console.log("Control panel got websocket message: " + JSON.stringify(evt));
    }
	ws.onopen = function () {
		console.log("websocket connected");
	}
	ws.onclose = function () {
        console.log("websocket disconnected");
    }

    inputbox = document.getElementById('file_url');
}
