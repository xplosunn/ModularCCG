package common.network.messages.serverToClient;

import common.network.messages.NetworkMessage;

/**
 * Created by Hugo on 25-05-2014.
 */
public class LoginResponse implements NetworkMessage{
    public static enum TYPE {SUCCESS, ALREADY_LOGGED}

    private TYPE type;

    public LoginResponse(TYPE type){
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

}
