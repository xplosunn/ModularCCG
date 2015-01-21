package clientfx.network;

import clientfx.lobby.Lobby;
import common.network.NetworkValues;
import common.network.messages.clientToServer.ChatToServer;
import common.network.messages.clientToServer.LoginRequest;
import common.network.messages.clientToServer.RequestToServer;
import common.network.messages.serverToClient.LoginResponse;
import common.network.messages.serverToClient.MessageToClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
            //ClientSession.disconnect()
        } catch (ClassNotFoundException e) {

        }
    }

    private void requestJoinChat(String room){
        //objOut.writeObject();
    }

    private void handleMessage(MessageToClient message){
        if(lobby != null){

        }
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }
}
