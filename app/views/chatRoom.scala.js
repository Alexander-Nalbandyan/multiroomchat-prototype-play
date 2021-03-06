@(room: String, username: String)

$(function() {
    var protocols = []; // all protocols
    var sockjs = new SockJS("/room/chat", null, {debug: true, protocols_whitelist: protocols});
    var multiplexer = new WebSocketMultiplex(sockjs);
    var chatSocket  = multiplexer.channel('@room');

    window.chat = chatSocket;

    var sendMessage = function() {
        chatSocket.send($("#talk").val());
        $("#talk").val('');
    };

    var receiveEvent = function(event) {
        var data = event.data;

        // Handle errors
        if(data.error) {
            chatSocket.close();
            $("#onError span").text(data.error);
            $("#onError").show();
            return;
        } else {
            $("#onChat").show();
        }

        // Create the message element
        var el = $('<div class="message"><span></span><p></p></div>');
        $("span", el).text(data.user);
        $("p", el).text(data.message);
        $(el).addClass(data.kind);
        if(data.user == '@username') $(el).addClass('me');
        $('#messages').append(el);

        // Update the members list
        $("#members").html('');
        $(data.members).each(function() {
            var li = document.createElement('li');
            li.textContent = this;
            $("#members").append(li);
        });
    };

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault();
            sendMessage();
        }
    };

    $("#talk").keypress(handleReturnKey);

    chatSocket.onmessage = receiveEvent;

});
