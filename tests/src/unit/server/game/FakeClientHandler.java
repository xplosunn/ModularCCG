package unit.server.game;

import common.network.messages.clientToServer.MessageToServer;
import common.network.messages.serverToClient.MessageToClient;
import server.ClientHandler;
import server.services.Users;

import java.util.ArrayList;

/**
 * Created by xs on 02-10-2014.
 */
public class FakeClientHandler extends ClientHandler{

    public FakeClientHandler(String username) {
        super(null);
        super.userName_$eq(username);
        if(!Users.login(username, this))
            throw new IllegalArgumentException("Could not login this fake client.");
    }

    public void recieveMessage(MessageToServer message){
        handleMessage(message);
    }

    @Override
    public void handleMessage(MessageToServer message) {
        super.handleMessage(message);
    }

    @Override
    public boolean sendMessageToClient(MessageToClient message) {
        return true;
    }

    public void logout(){
        Users.removeLoggedClient(this);
    }
}