package de.mcnexus.MicrosoftAuth.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.mcnexus.MicrosoftAuth.Server;
import kong.unirest.Unirest;
import org.eclipse.jetty.websocket.api.Session;
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
    private static List<Session> pendingSessions = new ArrayList<>();
    private static Map<String, Session> validatedSessions = new HashMap<>();

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
                        .field("redirect_uri", "http://localhost/redirect")
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

    @OnWebSocketConnect
    private void onConnect(Session session) throws IOException {
        System.out.println("connect");
        String ip = session.getRemoteAddress().getAddress().getHostAddress();
        for (Session s : pendingSessions)
            if (checkIfFromSelfIp(s, ip)) {
                s.getRemote().sendString(actionWithoutPayload("ipadralus"));
                s.close();
                return;
            }
        for (Session s : validatedSessions.values())
            if (checkIfFromSelfIp(s, ip)) {
                s.getRemote().sendString(actionWithoutPayload("ipadralus"));
                s.close();
                return;
            }
        session.getRemote().sendString(actionWithoutPayload("waitforstate"));
        pendingSessions.add(session);
    }

    @OnWebSocketMessage
    private void onMessage(Session session, String msg) throws IOException {
        JsonObject message = (JsonObject) JsonParser.parseString(msg);
        if (!message.has("code")) {
            session.getRemote().sendString(actionWithoutPayload("nocode"));
            return;
        }
        String code = message.get("code").toString();


    /* Waiting for code = state */

        if (code.equalsIgnoreCase("state")) {
            if (validatedSessions.containsValue(session)) {
                session.getRemote().sendString(actionWithoutPayload("alstaterec"));
                return;
            }
            if (message.has("state")) {
                String state = message.get("state").toString();
                if (validatedSessions.containsKey(state)) {
                    session.getRemote().sendString(actionWithoutPayload("statedeny"));
                    return;
                }
                pendingSessions.remove(session);
                validatedSessions.put(state, session);
                session.getRemote().sendString(actionWithoutPayload("stateaccept"));
                return;
            } else
                session.getRemote().sendString(actionWithoutPayload("statedeny"));
            return;
        }

    }

    private static boolean checkIfFromSelfIp(Session s, String ip) {
        return s.getRemoteAddress().getAddress().getHostAddress().equalsIgnoreCase(ip);
    }

    private static String actionWithoutPayload(String action) {
        return new MessageBuilder().code(action).toString();
    }
}

