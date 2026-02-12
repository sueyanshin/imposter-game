package server;

import common.GameClientInterface;

import java.rmi.RemoteException;

public class GameServer extends ImposterGameImpl {
    private GameServerUI ui;

    protected GameServer(GameServerUI ui) throws RemoteException {
        super();
        this.ui = ui;
    }

    @Override
    public synchronized boolean registerPlayer(String playerName, GameClientInterface client) throws RemoteException {
        boolean result = super.registerPlayer(playerName, client);
        if (result) {
            ui.log("Player registered: " + playerName);
        }
        return result;
    }

    @Override
    public synchronized void startGame() throws RemoteException {
        super.startGame();
        ui.log("Game started with " + getPlayers().size() + " players");
    }
}
