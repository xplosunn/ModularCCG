package common.network.messages.serverToClient;

/**
 * Created by Hugo on 25-05-2014.
 */
public class ChatToClient extends MessageToClient{
    private String sender;
    private boolean roomMessage;
    private String room;
    private String message;

    public ChatToClient(String sender, String message){
        this.sender = sender;
        roomMessage = false;
        this.message = message;
    }

    public ChatToClient(String sender, String room, String message){
        this.sender = sender;
        roomMessage = true;
        this.room = room;
        this.message = message;
    }

    public boolean isRoomMessage() {
        return roomMessage;
    }

    public String getMessage() {
        return message;
    }

    public String getRoom() {
        return room;
    }

    public String getSender() {
        return sender;
    }
}
