window.equo = window.equo || {};

(function (equo) {

    let webSocket;
    let userEventCallbacks = {};

    let receiveMessage = function(event){
        if(event === undefined) {
            return;
        }
        try {
            let parsedPayload = JSON.parse(event);
            let actionId = parsedPayload.action;
            if (actionId in userEventCallbacks) {
                let params = parsedPayload.params;
                if (parsedPayload.params) {
                    userEventCallbacks[actionId](params);
                } else {
                    userEventCallbacks[actionId]();
                }
            }
        } catch(err) {

        }
    };

    const openSocket = function() {
        // Ensures only one connection is open at a time
        if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED){
            //equo.logDebug('WebSocket is already opened.');
            return;
        }
        let wsPort = '%d';
        // Create a new instance of the websocket
        webSocket = new WebSocket('ws://127.0.0.1:' + wsPort);
        /**
         * Binds functions to the listeners for the websocket.
         */
        webSocket.onopen = function(event){
            // For reasons I can't determine, onopen gets called twice
            // and the first time event.data is undefined.
            // Leave a comment if you know the answer.
            if(event.data === undefined)
                return;
        };

        webSocket.onmessage = function(event){
            receiveMessage(event.data);
        };

        webSocket.onclose = function(event){
        };
    }();

    // Expose the below methods via the equo interface while
    // hiding the implementation of the method within the 
    // function() block

    equo.sendToWebSocketServer = function(action, browserParams) {
        // Wait until the state of the socket is not ready and send the message when it is...
        waitForSocketConnection(webSocket, function(){
            let event = JSON.stringify({
                action: action,
                params: browserParams
            });
            webSocket.send(event);
            receiveMessage(event);
        });
    };

    equo.send = function(actionId) {
        equo.sendToWebSocketServer(actionId);
    };

    equo.send = function(actionId, payload) {
        equo.sendToWebSocketServer(actionId, payload);
    };

    equo.on = function(userEvent, callback) {
        userEventCallbacks[userEvent] = callback;
    };

    // Make the function wait until the connection is made...
    let waitForSocketConnection = function(socket, callback){
        setTimeout(
            function () {
                if (socket.readyState === 1) {
                    if(callback != null){
                        callback();
                    }
                    return;

                } else {
		            try{
		                openSocket();
                    }catch(err){}
                    waitForSocketConnection(socket, callback);
                }
            }, 5); // wait 5 milisecond for the connection...
    };

}(equo));