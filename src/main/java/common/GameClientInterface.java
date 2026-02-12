package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameClientInterface extends Remote {
    void updateChat(String playerName, String message) throws RemoteException;

    void setYourTurn(boolean isYourTurn, int timeLimit) throws RemoteException;

    void receiveWord(String word, boolean isImposter, String hint) throws RemoteException;

    void showVotingResult(String imposter, boolean won) throws RemoteException;

    void gameStateChanged(GameState state) throws RemoteException;

    String getPlayerName() throws RemoteException;
}
