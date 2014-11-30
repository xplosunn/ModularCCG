package unit.common.network.messages;

import common.network.messages.NetworkMessage;
import common.network.messages.clientToServer.MessageToServer;
import server.ClientHandler;

import java.net.Socket;
import java.util.Vector;

/**
 * Created by HugoSousa on 24-10-2014.
 */
public class LoggingClientHandler extends ClientHandler {
    public final Vector<NetworkMessage> recievedMessages = new Vector<NetworkMessage>();

    public LoggingClientHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void handleMessage(MessageToServer message) {
        recievedMessages.add(message);
        super.handleMessage(message);
    }
}