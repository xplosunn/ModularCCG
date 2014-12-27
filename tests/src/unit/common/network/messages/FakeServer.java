package unit.common.network.messages;

import client.network.ServerHandler;
import common.network.NetworkValues;
import server.ClientHandler;
import server.services.ChatRooms;
import server.services.Games;
import server.services.Users;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import static org.junit.Assert.fail;

/**
 * Created by HugoSousa on 22-10-2014.
 */
public class FakeServer extends Thread{
    private ServerSocket socket = null;
    public final ArrayList<LoggingClientHandler> handlers = new ArrayList<>();
    public boolean running = true;

    public FakeServer(){
        try {
            socket = new ServerSocket(NetworkValues.SERVER_PORT);
        } catch (IOException e) {
            fail();
        }
    }

    @Override
    public void run() {
        while (running) try {
            Socket clientSocket = socket.accept();
            LoggingClientHandler handler = new LoggingClientHandler(clientSocket);
            handlers.add(handler);
            handler.start();
        } catch (IOException e) {

        }
    }

    public void shutdown(){
        running = false;
        Users.loggedClients().forEach((String s, ClientHandler clientHandler) -> ChatRooms.removeFromAllChats(s));
        Users.loggedClients().clear();
        try {
            socket.close();
        } catch (IOException e) {

        }
    }
}
