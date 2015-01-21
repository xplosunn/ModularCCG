package clientfx.network;

import clientfx.lobby.Lobby;
import common.network.NetworkValues;
import common.network.messages.clientToServer.ChatToServer;
import common.network.messages.clientToServer.LoginRequest;
import common.network.messages.clientToServer.RequestToServer;
import common.network.messages.serverToClient.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

import static common.network.messages.clientToServer.RequestToServer.REQUEST.*;

/**
 * Created by xs on 19-01-2015.
 */
public class ServerHandler implements Runnable{
    private final String username;
    private Socket socket;
    private ObjectOutputStream objOut;
    private ObjectInputStream objIn;
    private String errorMessage;
    private boolean connected = false;
    private Lobby lobby;

    private final Map<Integer, RequestToServer> responsesWaiting = new LinkedHashMap<>();

    public ServerHandler(String username){
        this.username = username;
    }

    public boolean connect(){
        try {
            socket = new Socket(NetworkValues.SERVER_IP, NetworkValues.SERVER_PORT);
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objIn = new ObjectInputStream(socket.getInputStream());
            objOut.writeObject(new LoginRequest(username));
            LoginResponse loginResponse = (LoginResponse) objIn.readObject();
            switch(loginResponse.getType()){
                case SUCCESS:
                    connected = true;
                    new Thread(this).start();
                    return true;
                case ALREADY_LOGGED:
                    errorMessage = "User already logged";
                    return false;
            }
        } catch (IOException e) {
            errorMessage = "Could not reach server";
        } catch (ClassNotFoundException e) {
            errorMessage = "Invalid response from server";
        }
        return false;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void run() {
        try {
            requestJoinChat("main");
            while (connected) {
                MessageToClient msg = (MessageToClient) objIn.readObject();
                handleMessage(msg);
            }
        }catch (IOException e){
            connected = false;
            disconnect();
        } catch (ClassNotFoundException e) {
            disconnect();
        }
    }

    private void requestJoinChat(String room){
        int reqId = 0;
        for(int i :responsesWaiting.keySet())
            if (i > reqId)
                reqId = i+1;
        RequestToServer req = new RequestToServer(reqId, JOIN_CHAT, room);
        try {
            objOut.writeObject(req);
            responsesWaiting.put(reqId, req);
        } catch (IOException e) {
            disconnect();
        }
    }

    private void disconnect(){
        if(lobby != null){
            lobby.disconnect();
        }
    }

    public void chat(String msg){
        try {
            objOut.writeObject(new ChatToServer(ChatToServer.TARGET.ROOM, "main", msg));
        } catch (IOException e) {
            disconnect();
        }
    }

    private void handleMessage(MessageToClient message){
        if(lobby != null){
            if(message instanceof ResponseToClient){
                ResponseToClient resp = (ResponseToClient) message;
                if(resp.getType().equals(ResponseToClient.TYPE.OK)) {
                    RequestToServer req = responsesWaiting.get(resp.getID());
                    switch(req.getRequest()){
                        case JOIN_CHAT:
                            lobby.chat.join(req.getRequestTarget(), resp.getData());
                            break;
                        case JOIN_GAME_QUEUE:
                            //TODO
                            break;
                    }
                }
            } else if(message instanceof InfoToClient){
                InfoToClient info = (InfoToClient) message;
                switch(info.getType()){
                    case CHAT_USER_JOINED:
                        lobby.chat.addUser(info.getData());
                        break;
                    case CHAT_USER_LEFT:
                        lobby.chat.removeUser(info.getData());
                        break;
                }
            } else if(message instanceof ChatToClient){
                ChatToClient chatMsg = (ChatToClient) message;
                if(chatMsg.isRoomMessage())
                    lobby.chat.roomMessage(chatMsg.getRoom(), chatMsg.getSender(), chatMsg.getMessage());
            }
        }
    }

    public void setLobby(Lobby lobby) {
        assert lobby != null;
        this.lobby = lobby;
    }

    public void endConnection() {
        connected = false;
        try {
            objIn.close();
        } catch (IOException e) {

        }
        try {
            objOut.close();
        } catch (IOException e) {

        }
    }
}
