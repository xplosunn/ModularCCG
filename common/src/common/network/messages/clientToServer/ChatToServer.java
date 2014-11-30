package common.network.messages.clientToServer;

public class ChatToServer extends MessageToServer {

    public static enum TARGET {ROOM, PRIVATE}

    private TARGET targetType;
    private String target;
    private String message;

    public ChatToServer(TARGET targetType, String target, String message) {
        this.targetType = targetType;
        this.target = target;
        this.message = message;
    }

    public TARGET getTargetType() {
        return targetType;
    }

    public String getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }
}
