package models;

import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import play.sockjs.SockJS;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import akka.actor.*;
import static akka.pattern.Patterns.ask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


import java.util.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * A chat room is an Actor.
 */
public class ChatRoomManager extends ChatRoom {

    // Default room.
    //static ActorRef defaultRoom = Akka.system().actorOf(Props.create(ChatRoom.class));
    static Map<String, ActorRef> rooms = new HashMap<String, ActorRef>();
    static Map<String, List<ActorRef>> userRooms = new HashMap<String, List<ActorRef>>();

    public ChatRoomManager(String roomName) {
        super(roomName);
    }

    public static void join(final String username, final SockJS.In in, final SockJS.Out out) throws Exception{


        // For each event received on the socket,
        in.onMessage(new Callback<String>() {
            public void invoke(String event) {

                JsonNode eventJson = Json.parse(event);
                String type = eventJson.get("type").asText();
                String roomName = eventJson.get("topic").asText();
                final ActorRef room = populateRoom(roomName);

                if (type.equals("sub")) {

                    // Send the Join message to the room
                    try {
                        String result = (String)Await.result(ask(room,new Join(username, out), 1000), Duration.create(1, SECONDS));

                        if ("OK".equals(result)) {
                            List<ActorRef> userJoinedRooms = userRooms.get(username);
                            if (userJoinedRooms == null) {
                                userJoinedRooms = new ArrayList<ActorRef>();
                                userRooms.put(username, userJoinedRooms);
                            }
                            userJoinedRooms.add(room);
                        } else {
                            // Cannot connect, create a Json error.
                            ObjectNode error = Json.newObject();
                            error.put("error", result);

                            // Send the error to the socket.
                            out.write(error.toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if(type.equals("msg")) {

                    room.tell(new Talk(username, eventJson.get("payload").get("message").asText()), null);

                } else if(type.equals("uns")) {

                    room.tell(new Quit(username), null);

                }



                // Send a Talk message to the room.


            }
        });

        // When the socket is closed.
        in.onClose(new Callback0() {
            public void invoke() {

                List<ActorRef> userJoinedRooms = userRooms.get(username);
                for (ActorRef userJoinedRoom : userJoinedRooms) {
                    userJoinedRoom.tell(new Quit(username), null);
                }
            }
        });







    }

    private static ActorRef populateRoom(final String roomName) {
        ActorRef room;
        if (rooms.containsKey(roomName)){
            room = rooms.get(roomName);
        } else {
            room = Akka.system().actorOf(
                    new Props(new UntypedActorFactory() {
                        public UntypedActor create() {
                            return new ChatRoomManager(roomName);
                        }
                    })
            );
            rooms.put(roomName, room);
//            new Robot(room, roomName);
        }
        return room;
    }

    // -- Messages

}
