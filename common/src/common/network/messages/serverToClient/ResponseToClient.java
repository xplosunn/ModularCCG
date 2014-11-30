package common.network.messages.serverToClient;

import scala.collection.mutable.ArrayBuffer;

public class ResponseToClient extends MessageToClient {
    public enum TYPE{OK, DENIED}

    private int ID;
    private TYPE type;
    private ArrayBuffer<String> data;

    public ResponseToClient(TYPE type, int ID, ArrayBuffer<String> data){
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

    public ArrayBuffer<String> getData() {
        return data;
    }
}
