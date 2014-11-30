package common.network.messages.clientToServer;

/**
 * Created by Hugo on 25-05-2014.
 */
public class LoginRequest extends MessageToServer{
    private String user;
    public LoginRequest(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }
}
