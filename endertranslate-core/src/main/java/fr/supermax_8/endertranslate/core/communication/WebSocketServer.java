package fr.supermax_8.endertranslate.core.communication;

import com.google.gson.JsonSyntaxException;
import fr.supermax_8.endertranslate.core.EnderTranslate;
import io.javalin.Javalin;
import lombok.Getter;
import org.eclipse.jetty.websocket.api.Session;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServer {

    @Getter
    private static WebSocketServer instance;

    @Getter
    private final ConcurrentHashMap<Session, Optional<WsSession>> sessions = new ConcurrentHashMap<>();
    private final Javalin server;

    public WebSocketServer(int port) {
        Javalin app = Javalin.create(config -> {
            config.jetty.modifyWebSocketServletFactory(wsConfig -> {
                wsConfig.setIdleTimeout(Duration.ZERO);
            });
        }).start(port);

        instance = this;
        EnderTranslate.log("Starting websocket server...");
        server = app.ws("/", wsConfig -> {
            wsConfig.onConnect(ctx -> {
                sessions.put(ctx.session, Optional.empty());
                System.out.println("New session connected, total: " + sessions.size());
            });
            wsConfig.onClose(ctx -> {
                sessions.remove(ctx.session);
                System.out.println("Session disconnected, total: " + sessions.size());
            });
            wsConfig.onMessage((ctx) -> {
                try {
                    WsPacketWrapper packet = EnderTranslate.getGson().fromJson(ctx.message(), WsPacketWrapper.class);
                    packet.getPacket().receiveFromClient(ctx.session, this);
                } catch (JsonSyntaxException e) {
                }
            });
        });
        EnderTranslate.log("Websocket server started on port: " + port);
    }

    public void stop() {
        server.stop();
    }

}