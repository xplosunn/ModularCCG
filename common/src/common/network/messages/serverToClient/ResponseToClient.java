package common.network.messages.serverToClient;


public class ResponseToClient extends MessageToClient {
    public enum TYPE{OK, DENIED}

    private int ID;
    private TYPE type;
    private String[] data;

    public ResponseToClient(TYPE type, int ID, String[] data){
        this.type = type;
        this.ID = ID;
        this.data = data;
    }

    public int getID() {
        return ID;
    }

    public TYPE getType() {
        return type;
    }

    public String[] getData() {
        return data;
    }
}
