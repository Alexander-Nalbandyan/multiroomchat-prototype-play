package controllers;

import play.libs.F;
import play.mvc.*;

import com.fasterxml.jackson.databind.JsonNode;
import play.sockjs.CookieCalculator;
import play.sockjs.SockJS;
import play.sockjs.SockJSRouter;
import views.html.*;

import models.*;

public class Application extends Controller {
  
    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render());
    }
  
    /**
     * Display the chat room.
     */
    public static Result chatRoom(String room, String username) {
        if(room == null || room.trim().equals("")) {
            flash("error", "Please choose a valid room.");
            return redirect(routes.Application.index());
        }
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Application.index());
        }

        response().setCookie("username", username);
        return ok(chatRoom.render(room, username));
    }

    public static Result chatRoomJs(String room,String username) {
        return ok(views.js.chatRoom.render(room, username));
    }
    

    public static SockJSRouter chat = new SockJSRouter() {


        // override sockjs method
        @SockJS.Settings(
                cookies = CookieCalculator.JSESSIONID.class,
                websocket = true,
                heartbeat = 55000 // duration in milliseconds
        )
        public SockJS sockjs() {
            final String username = request().cookie("username").value();
            return new SockJS() {

                // Called when the SockJS Handshake is done.
                public void onReady(SockJS.In in, SockJS.Out out) {


                    // Join the chat room.
                    try {

                        ChatRoomManager.join(username, in, out);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }

            };
        }
    };
  
}
