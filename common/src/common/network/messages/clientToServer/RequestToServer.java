package common.network.messages.clientToServer;

import common.card.Deck;


public class RequestToServer extends MessageToServer {
    public enum REQUEST{
        JOIN_CHAT,
        JOIN_GAME_QUEUE
    }

    private REQUEST request;
    private String requestTarget;
    private int requestID;
    private Object data;

    public RequestToServer(int requestID, REQUEST request, String requestTarget) {
        this.requestID = requestID;
        this.request = request;
        this.requestTarget = requestTarget;
    }

    public RequestToServer(int requestID, Deck deck) {
        this.requestID = requestID;
        request = REQUEST.JOIN_GAME_QUEUE;
        data = deck;
    }

    public int getRequestID() {
        return requestID;
    }

    public REQUEST getRequest() {
        return request;
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public Deck getDeck(){
        if (request == REQUEST.JOIN_GAME_QUEUE && data instanceof Deck){
            return (Deck) data;
        }
        return null;
    }
}
