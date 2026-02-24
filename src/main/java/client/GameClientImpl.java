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
    private boolean hasVoted;

    public GameClientImpl(String playerName, GameClientUI ui) throws RemoteException {
        super();
        this.playerName = playerName;
        this.ui = ui;
        this.isMyTurn = false;
        this.hasVoted = false;
    }

    @Override
    public void updateChat(String playerName, String message) throws RemoteException {
        ui.appendChat(playerName + ": " + message);
    }

    @Override
    public void setYourTurn(boolean isYourTurn, int timeLimit) throws RemoteException {
        this.isMyTurn = isYourTurn;
        ui.setTurnStatus(isYourTurn, timeLimit);
    }

    @Override
    public void receiveWord(String word, boolean isImposter, String hint) throws RemoteException {
        this.assignedWord = word;
        this.isImposter = isImposter;
        this.hint = hint;

        if (isImposter) {
            ui.showWord("You are the IMPOSTER!\nHint: " + hint);
        } else {
            ui.showWord("Your word is: " + word);
        }
    }

    @Override
    public void showVotingResult(String imposter, boolean won, String resultMessage) throws RemoteException {
        ui.showVotingResult(imposter, won);
    }

    @Override
    public void gameStateChanged(GameState state) throws RemoteException {
        ui.updateGameState(state);
        if (state == GameState.VOTING) {
            hasVoted = false;
        }
    }

    @Override
    public void updateVotingTimer(int timeLeft) throws RemoteException {
        ui.updateVotingTimer(timeLeft);
    }

    @Override
    public void voteRecorded() throws RemoteException {
        this.hasVoted = true;
        ui.voteRecorded();
    }


    @Override
    public String getPlayerName() throws RemoteException {
        return playerName;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public boolean hasVoted() {
        return hasVoted;
    }
    public String getAssignedWord() {
        return assignedWord;
    }

    public boolean isImposter() {
        return isImposter;
    }
}
