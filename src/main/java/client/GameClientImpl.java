package client;

import common.GameClientInterface;
import common.GameState;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class GameClientImpl extends UnicastRemoteObject implements GameClientInterface {
    private String playerName;
    private GameClientUI ui;
    private String assignedWord;
    private String hint;
    private boolean isImposter;
    private boolean isMyTurn;

    public GameClientImpl (String playerName,GameClientUI ui) throws RemoteException{
        super();
        this.playerName=playerName;
        this.ui=ui;
        this.isMyTurn=false;
    }

    @Override
    public void updateChat(String playerName, String message) throws RemoteException {
        ui.appendChat(playerName+ ": "+message);
    }

    @Override
    public void setYourTurn(boolean isYourTurn, int timeLimit) throws RemoteException {
        this.isMyTurn = isYourTurn;
        ui.setTurnStatus(isYourTurn, timeLimit);
    }

    @Override
    public void receiveWord(String word, boolean isImposter, String hint) throws RemoteException {
        this.assignedWord=word;
        this.isImposter = isImposter;
        this.hint=hint;

        if(isImposter){
            ui.showWord("You are the IMPOSTER!\nHint: "+hint);
        }else {
            ui.showWord("Your word is: "+word);
        }
    }

    @Override
    public void showVotingResult(String imposter, boolean won) throws RemoteException {
        ui.showVotingResult(imposter,won);
    }

    @Override
    public void gameStateChanged(GameState state) throws RemoteException {
        ui.updateGameState(state);
    }

    @Override
    public String getPlayerName() throws RemoteException {
        return playerName;
    }
    public boolean isMyTurn() {
        return isMyTurn;
    }

    public String getAssignedWord() {
        return assignedWord;
    }

    public boolean isImposter() {
        return isImposter;
    }
}
