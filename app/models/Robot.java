package models;

import play.*;
import play.mvc.*;
import play.libs.*;

import play.sockjs.SockJS;
import scala.concurrent.duration.*;
import akka.actor.*;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.concurrent.TimeUnit.*;

public class Robot {


    public Robot(ActorRef chatRoom, String roomName) {
        
        // Create a Fake socket out for the robot that log events to the console.
        SockJS.Out robotChannel = new SockJS.Out() {
            
            public void write(String frame) {
                Logger.of("robot").info(frame);
            }
            
            public void close() {}
            
        };
        
        // Join the room

        chatRoom.tell(new ChatRoomManager.Join("Robot", robotChannel), null);
        
        // Make the robot talk every 30 seconds
        Akka.system().scheduler().schedule(
            Duration.create(30, SECONDS),
            Duration.create(30, SECONDS),
            chatRoom,
            new ChatRoomManager.Talk("Robot", "I'm still alive"),
            Akka.system().dispatcher(),
            /** sender **/ null
        );
        
    }
    
}
