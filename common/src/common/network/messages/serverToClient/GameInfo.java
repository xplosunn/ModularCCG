package common.network.messages.serverToClient;

import common.game.GameSteps;
import common.game.RemoteCard;
import common.card.ability.change.GameChange;
import scala.Tuple2;

public class GameInfo extends MessageToClient{
    public static enum TYPE {
        GAME_STARTED,
        HAND_PRE_MULLIGAN,
        HAND,
        GAME_CHANGES, //used at the start of the turn and in battle
        NEXT_TURN,
        NEXT_STEP,
        CARD_PLAYED,
        ATTACKERS,
        DEFENDERS,
        ON_COMBAT_ABILITY,
        ON_DEATH_ABILITY,
        PLAYER_WON,
        TIE
    }
    public final int gameID;
    public final TYPE type;
    private final Object data;

    private GameInfo(int gameID, TYPE type, Object data){
        this.gameID = gameID;
        this.type = type;
        this.data = data;
    }

    // Game started

    /**
     * Game started message. Must contain the username of each player, ordered by turn sequence.
     * @param gameID the id of the game
     * @param playerNames the usernames of each of the players ordered by turn sequence
     */
    public static GameInfo gameStarted(int gameID, String[] playerNames){
        return new GameInfo(gameID, TYPE.GAME_STARTED, playerNames);
    }

    public String[] playerNames() {
        return type == TYPE.GAME_STARTED ? (String[]) data : null;
    }

    // Hand pre mulligan

    public static GameInfo handPreMulligan(int gameID, RemoteCard[] cards){
        return new GameInfo(gameID, TYPE.HAND_PRE_MULLIGAN, cards);

    }

    public RemoteCard[] getCards(){
        return type == TYPE.HAND_PRE_MULLIGAN ? (RemoteCard[]) data : null;
    }

    // Hand

    public static GameInfo hand(int gameID, int[] cardIDs){
        return new GameInfo(gameID, TYPE.HAND, cardIDs);
    }

    public int[] getCardIDs(){
        return type == TYPE.HAND ? (int[]) data : null;
    }

    // Next Step

    /**
     * Next step message. Sent when a step a new step starts.
     * @param gameID the id of the game
     * @param step the step starting
     */
    public static GameInfo nextStep(int gameID, GameSteps step){
        return new GameInfo(gameID, TYPE.NEXT_STEP, step);
    }

    public GameSteps step(){
        return type == TYPE.NEXT_STEP ? (GameSteps) data : null;
    }

    // Card played

    /**
     * Card played message.
     * @param gameID
     * @param remoteCard
     * @param changes
     */
    public static GameInfo cardPlayed(int gameID, RemoteCard remoteCard, GameChange[] changes ){
        return new GameInfo(gameID, TYPE.CARD_PLAYED, new Tuple2<RemoteCard,GameChange[]>(remoteCard,changes));
    }

    public RemoteCard card(){
        return type == TYPE.CARD_PLAYED ? ((Tuple2<RemoteCard,GameChange[]>)data)._1() : null;
    }

    // Next Turn

    /**
     * Next turn message.
     * @param gameID
     * @param player
     * @param changes
     */
    public static GameInfo nextTurn(int gameID, String player, GameChange[] changes){
        return new GameInfo(gameID, TYPE.NEXT_TURN, new Tuple2<String, GameChange[]>(player, changes));
    }

    //Player Won

    /**
     * Player won message.
     * @param gameID
     * @param winner
     */
    public static GameInfo playerWon(int gameID, String winner){
        return new GameInfo(gameID, TYPE.PLAYER_WON, winner);
    }

    //Next turn && Player Won
    public String player(){
        switch(type){
            case NEXT_TURN: return ((Tuple2<String, GameChange[]>) data)._1();
            case PLAYER_WON: return (String) data;
            default: return null;
        }
    }

    //Tie

    /**
     * Player won message.
     * @param gameID
     */
    public static GameInfo gameTied(int gameID){
        return new GameInfo(gameID, TYPE.TIE, null);
    }

    // Game change

    /**
     * Game change message.
     * @param gameID
     * @param changes
     */
    public static GameInfo gameChanges(int gameID, GameChange[] changes){
        return new GameInfo(gameID, TYPE.GAME_CHANGES, changes);
    }


    //Attackers

    /**
     * Attackers message.
     * @param gameID
     * @param attackerIDs
     */
    public static GameInfo attackers(int gameID, int[] attackerIDs){
        return new GameInfo(gameID, TYPE.ATTACKERS, attackerIDs);
    }

    public int[] attackerIDs(){
        return type == TYPE.ATTACKERS ? (int[]) data : null;
    }

    //Defenders

    /**
     * Defenders message.
     * @param gameID
     * @param defenseIDs
     */
    public static GameInfo defenders(int gameID, int[][] defenseIDs){
        return new GameInfo(gameID, TYPE.DEFENDERS, defenseIDs);
    }

    public int[][] defenseIDs(){
        return type == TYPE.DEFENDERS ? (int[][]) data : null;
    }

    //On death and on combat ability

    /**
     * On death and on combat ability messages
     * @param gameID
     * @param summonID
     * @param changes
     */
    public static GameInfo onDeathAbility(int gameID, int summonID, GameChange[] changes){
        return new GameInfo(gameID, TYPE.ON_DEATH_ABILITY, new Tuple2<Integer, GameChange[]>(summonID, changes));
    }
    public static GameInfo onCombatAbility(int gameID, int summonID, GameChange[] changes){
        return new GameInfo(gameID, TYPE.ON_COMBAT_ABILITY, new Tuple2<Integer, GameChange[]>(summonID, changes));
    }

    public Integer summonID(){
        return (type == TYPE.ON_DEATH_ABILITY || type == TYPE.ON_COMBAT_ABILITY) ? ((Tuple2<Integer, GameChange[]>) data)._1() : null;
    }

    //Multiple
    public GameChange[] changes(){
        switch(type){
            case CARD_PLAYED: return ((Tuple2<RemoteCard,GameChange[]>)data)._2();
            case GAME_CHANGES: return (GameChange[]) data;
            case NEXT_TURN: return ((Tuple2<String, GameChange[]>) data)._2();
            case ON_COMBAT_ABILITY: return ((Tuple2<Integer, GameChange[]>) data)._2();
            case ON_DEATH_ABILITY: return ((Tuple2<Integer, GameChange[]>) data)._2();
            default: return null;
        }
    }

    @Override
    public String toString() {
        String output = "GameInfo:{ " + type + "|";
        switch (type) {
            case GAME_STARTED:
                for(int i = 0; i < playerNames().length; i++)
                    output += playerNames()[i] + (i < playerNames().length - 1 ? " , " : "");
                break;

            case HAND_PRE_MULLIGAN:
                for(int i = 0; i < getCards().length; i++)
                    output += getCards()[i] + (i < getCards().length - 1 ? " , " : "");
                break;

            case HAND:
                for(int i = 0; i < getCardIDs().length; i++)
                    output += getCardIDs()[i] + (i < getCardIDs().length - 1 ? " , " : "");
                break;

            case NEXT_TURN:
                output += player() + "|";
            case GAME_CHANGES:
                for(int i = 0; i < changes().length; i++)
                    output += changes()[i] + (i < changes().length - 1 ? " , " : "");
                break;

            case NEXT_STEP:
                output += step();
                break;

            case CARD_PLAYED:
                output += card() + "|";
                for(int i = 0; i < changes().length; i++)
                    output += changes()[i] + (i < changes().length - 1 ? " , " : "");
                break;

            case ATTACKERS:
                for(int i = 0; i < attackerIDs().length; i++)
                    output += attackerIDs()[i] + (i < attackerIDs().length - 1 ? " , " : "");
                break;

            case DEFENDERS:
                for(int i = 0; i < defenseIDs().length; i++){
                    output += defenseIDs()[i][0] + "-";
                    for (int j = 1; j < defenseIDs()[i].length; j++)
                        output += defenseIDs()[i][j] + (j < defenseIDs()[i].length ? "," : "");
                    output += "|";
                }
                break;

            case ON_COMBAT_ABILITY:
            case ON_DEATH_ABILITY:
                output += summonID() + "|";
                for(int i = 0; i < changes().length; i++)
                    output += changes()[i] + " , ";
                break;

            case PLAYER_WON:
                output += player();
                break;
        }
        return  output + "}";
    }
}