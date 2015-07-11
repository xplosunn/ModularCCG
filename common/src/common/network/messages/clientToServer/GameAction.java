package common.network.messages.clientToServer;

import common.game.GameSteps;
import scala.Int;
import scala.Tuple2;

public class GameAction extends MessageToServer {
    public enum ACTIONS{
        PLAY_CARD,
        MULLIGAN,
        SET_ATTACKERS,
        SET_DEFENDERS,
        NEXT_STEP,
        END_TURN
    }

    private int gameID;
    private ACTIONS action;
    private Object data;

    private GameAction(int gameID, ACTIONS action, Object data) {
        this.gameID = gameID;
        this.action = action;
        this.data = data;
    }

    //Play Card
    public static GameAction playCard(int gameID, int cardID){
       return new GameAction(gameID, ACTIONS.PLAY_CARD, cardID);
    }

    public int getCardID(){
        return action == ACTIONS.PLAY_CARD ? (Integer) data : -1;
    }

    //Mulligan
    public static GameAction mulligan(int gameID, int[] cardIDs){
        return new GameAction(gameID, ACTIONS.MULLIGAN, cardIDs);
    }

    public int[] getCardIDs(){
        return action == ACTIONS.MULLIGAN ? (int[]) data : null;
    }

    //Set Attackers
    public static GameAction setAttackers(int gameID, int[] attackerIDs){
        return new GameAction(gameID, ACTIONS.SET_ATTACKERS, attackerIDs);
    }

    public int[] getAttackerIDs(){
        return action == ACTIONS.SET_ATTACKERS ? (int[]) data : null;
    }

    //Set Defenders
    public static GameAction setDefenders(int gameID, Tuple2<Int, Int>[] defenses){
        return new GameAction(gameID, ACTIONS.SET_DEFENDERS, defenses);
    }

    public Tuple2<Int,Int>[] getDefenses(){
        return (action == ACTIONS.SET_DEFENDERS) ? (Tuple2<Int,Int>[]) data : null;
    }

    //End turn
    public static GameAction endTurn(int gameID){
        return new GameAction(gameID, ACTIONS.END_TURN, null);
    }

    //Next Step
    public static GameAction nextStep(int gameID, GameSteps currentStep){
        return new GameAction(gameID, ACTIONS.NEXT_STEP, currentStep);
    }

    public GameSteps getCurrentStep(){
        return (action == ACTIONS.NEXT_STEP) ? (GameSteps) data : null;
    }

    //Multiple
    public ACTIONS getAction() {
        return action;
    }

    public int getGameID() {
        return gameID;
    }
}
