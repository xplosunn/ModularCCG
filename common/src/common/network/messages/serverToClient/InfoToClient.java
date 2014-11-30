package common.network.messages.serverToClient;


public class InfoToClient extends MessageToClient {
    public enum TYPE {CHAT_USER_JOINED}
    private TYPE type;
    private String origin;
    private String data;

    public InfoToClient(TYPE type, String origin, String data) {
        this.type = type;
        this.origin = origin;
        this.data = data;
    }

    public TYPE getType() {
        return type;
    }

    public String getOrigin() {
        return origin;
    }

    public String getData() {
        return data;
    }
}
