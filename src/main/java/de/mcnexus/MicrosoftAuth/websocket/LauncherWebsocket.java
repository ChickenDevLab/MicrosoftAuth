package de.mcnexus.MicrosoftAuth.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.mcnexus.MicrosoftAuth.Server;
import kong.unirest.Unirest;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@WebSocket
public class LauncherWebsocket {
    private static final List<Session> pendingSessions = new ArrayList<>();
    private static final Map<String, Session> validatedSessions = new HashMap<>();

    public static void authReceived(String state, String code) {
        if (validatedSessions.containsKey(state)) {
            Session session = validatedSessions.get(state);
            try {
                session.getRemote().sendString(actionWithoutPayload("authreceiv"));
                CompletableFuture<JsonElement> task = CompletableFuture.supplyAsync(() -> JsonParser.parseString(Unirest.post("https://login.live.com/oauth20_token.srf")
                        .field("client_id", Server.clientID)
                        .field("client_secret", Server.clientSecret)
                        .field("code", code)
                        .field("grant_type", "authorization_code")
                        .field("redirect_uri", "https://nexusauth.herokuapp.com/redirect")
                        .contentType("application/x-www-form-urlencoded")
                        .asJson().getBody().toString()));
                task.thenAccept((jsonElement) -> {
                    try {
                        session.getRemote().sendString(new MessageBuilder().code("final").payload("response", jsonElement).toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void denyReceived(String state) throws IOException {
        if(validatedSessions.containsKey(state)){
            Session s = validatedSessions.get(state);
            s.getRemote().sendString(new MessageBuilder().code("authdeny").toString());
            s.close();
            validatedSessions.remove(s);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session){
        try {
            session.getRemote().sendString(actionWithoutPayload("waitforstate"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pendingSessions.add(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String msg){
        JsonObject message = (JsonObject) JsonParser.parseString(msg);
        if (!message.has("code")) {
            try {
                session.getRemote().sendString(actionWithoutPayload("nocode"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String code = message.get("code").getAsString();
        /* Waiting for code = state */
        if (code.equalsIgnoreCase("state")) {
            if (validatedSessions.containsValue(session)) {
                try {
                    session.getRemote().sendString(actionWithoutPayload("alstaterec"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            if (message.has("state")) {
                String state = message.get("state").getAsString();
                if (validatedSessions.containsKey(state)) {
                    try {
                        session.getRemote().sendString(actionWithoutPayload("statedeny"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }

                pendingSessions.remove(session);
                validatedSessions.put(state, session);

                try {
                    session.getRemote().sendString(actionWithoutPayload("stateaccept"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    session.getRemote().sendString(actionWithoutPayload("statedeny"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason){
        if(pendingSessions.contains(session))
            pendingSessions.remove(session);
        if(validatedSessions.containsKey(session))
            validatedSessions.remove(session);
    }

    private static String actionWithoutPayload(String action) {
        return new MessageBuilder().code(action).toString();
    }
}


