package clientfx.network;

import common.network.NetworkValues;
import common.network.messages.clientToServer.LoginRequest;
import common.network.messages.serverToClient.LoginResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by xs on 19-01-2015.
 */
public class ServerHandler {
    private final String username;
    private Socket socket;
    private ObjectOutputStream objOut;
    private ObjectInputStream objIn;
    private String errorMessage;

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
                case SUCCESS: return true;
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
}
