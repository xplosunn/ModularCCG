package common.game;

import common.card.Card;

import java.io.Serializable;

/**
 * Created by xs on 26-09-2014.
 */
public class RemoteCard implements Serializable{
    public final int id;
    public final String owner;
    public final Card card;
    
    public RemoteCard(int id, String owner, Card card){
        if(owner == null || owner.length() == 0 || card == null)
            throw new IllegalArgumentException(id + " " + owner + " " + card);
        this.id = id;
        this.owner = owner;
        this.card = card;
    }
}
