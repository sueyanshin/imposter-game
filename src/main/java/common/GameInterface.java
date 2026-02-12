package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameInterface extends Remote {
    // server methods
    boolean registerPlayer(String playerName, GameClientInterface client) throws RemoteException;

    void startGame() throws RemoteException;

    void sendMessage(String playerName, String message) throws RemoteException;

    void submitVote(String playerName, String votedPlayer) throws RemoteException;

    void replayGame() throws RemoteException;

    List<Player> getPlayers() throws RemoteException;

    GameState getGameState() throws RemoteException;
}

