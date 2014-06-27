package models;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import play.mvc.WebSocket;
import play.sockjs.SockJS;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lst on 06.02.14.
 */
public class ChatRoom extends UntypedActor {

    public ChatRoom(String roomName) {
       this.roomName = roomName;
    }

    //Room name
    String roomName;

    // Members of this room.
    Map<String, SockJS.Out> members = new HashMap<String, SockJS.Out>();

    public void onReceive(Object message) throws Exception {

        if(message instanceof Join) {

            // Received a Join message
            Join join = (Join)message;

            // Check if this username is free.
            if(members.containsKey(join.username)) {
                getSender().tell("This username is already used", getSelf());
            } else {
                members.put(join.username, join.channel);
                notifyAll("join", join.username, "has entered the room");
                getSender().tell("OK", getSelf());
            }

        } else if(message instanceof Talk)  {

            // Received a Talk message
            Talk talk = (Talk)message;

            notifyAll("talk", talk.username, talk.text);

        } else if(message instanceof Quit)  {

            // Received a Quit message
            Quit quit = (Quit)message;

            members.remove(quit.username);

            notifyAll("quit", quit.username, "has left the room");

        } else {
            unhandled(message);
        }

    }

    // Send a Json event to all members
    public void notifyAll(String kind, String user, String text) {
        for(SockJS.Out channel: members.values()) {

            ObjectNode event = Json.newObject();
            event.put("type", "msg");
            event.put("topic", roomName);
            ObjectNode payload = event.putObject("payload");
            payload.put("kind", kind);
            payload.put("user", user);
            payload.put("message", text);

            ArrayNode m = payload.putArray("members");
            for(String u: members.keySet()) {
                m.add(u);
            }

            channel.write(event.toString());
        }
    }

    public static class Join {

        final String username;
        final SockJS.Out channel;

        public Join(String username, SockJS.Out channel) {
            this.username = username;
            this.channel = channel;
        }

    }

    public static class Talk {

        final String username;
        final String text;

        public Talk(String username, String text) {
            this.username = username;
            this.text = text;
        }

    }

    public static class Quit {

        final String username;

        public Quit(String username) {
            this.username = username;
        }

    }
}
