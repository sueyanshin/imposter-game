package server;

import common.GameClientInterface;
import common.GameInterface;
import common.GameState;
import common.Player;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImposterGameImpl extends UnicastRemoteObject implements GameInterface {
    private List<Player> players;
    private Map<String, GameClientInterface> clientMap;
    private GameState currentState;
    private int currentPlayerIndex;
    private int currentRound;
    private String imposterName;
    private ScheduledExecutorService scheduler;
    private Timer turnTimer;
    private Timer votingTimer;
    private Set<String> votedPlayers;
    private boolean votingComplete;
    private final String[] WORDS = {"tower", "chalk", "school", "ice-cream", "rainbow", "sky", "storm", "carrot", "turtle", "wheel"};
    private final String[] HINTS = {"high", "white", "kids", "cold", "colors", "blue", "dangerous", "bird", "slow", "black"};


    protected ImposterGameImpl() throws RemoteException {
        super();
        this.players = Collections.synchronizedList(new ArrayList<>());
        this.clientMap = new ConcurrentHashMap<>();
        this.currentState = GameState.WAITING_FOR_PLAYERS;
        this.currentRound = 0;
        this.scheduler = Executors.newScheduledThreadPool(0);
        this.votedPlayers = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public synchronized boolean registerPlayer(String playerName, GameClientInterface client) throws RemoteException {
        if (players.size() >= 6 || currentState != GameState.WAITING_FOR_PLAYERS) {
            return false;
        }

        // Check if player name is already exist
        for (Player p : players) {
            if (p.getName().equals(playerName)) {
                return false;
            }
        }

        Player newPlayer = new Player(playerName);
        players.add(newPlayer);
        clientMap.put(playerName, client);
        broadcastGameState();
        return true;
    }

    @Override
    public synchronized void startGame() throws RemoteException {
        if (players.size() < 3) {
            return;
        }
        currentState = GameState.STARTING;
        broadcastGameState();

        // Select imposter
        Random rand = new Random();
        int imposterIndex = rand.nextInt(players.size());
        imposterName = players.get(imposterIndex).getName();

        // Distribute words
        int index = rand.nextInt(WORDS.length);
        String word = WORDS[index];
        String hint = HINTS[index];

        try {
            for (Player p : players) {
                p.setImposter(p.getName().equals(imposterName));
                if (p.isImposter()) {
                    p.setWord(word);
                    p.setHint(hint);
                } else {
                    p.setWord(word);
                    p.setHint(hint);
                }

                GameClientInterface client = clientMap.get(p.getName());
                if (client != null) {
                    client.receiveWord(p.getWord(), p.isImposter(), p.getHint());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentState = GameState.WORD_DISTRIBUTION;
        broadcastGameState();

        // Start first round after a short delay
        scheduler.schedule(this::startNextRound, 3, TimeUnit.SECONDS);

    }

    private synchronized void startNextRound() {
        try {
            currentRound++;
            if (currentRound > 3) {
                startVoting();
                return;
            }

            switch (currentRound) {
                case 1:
                    currentState = GameState.ROUND_1;
                    break;
                case 2:
                    currentState = GameState.ROUND_2;
                    break;
                case 3:
                    currentState = GameState.ROUND_3;
                    break;
            }
            broadcastGameState();
            currentPlayerIndex = 0;
            startPlayerTurn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void startPlayerTurn() {
        if (currentPlayerIndex >= players.size()) {
            // End of round
            scheduler.schedule(this::startNextRound, 2, TimeUnit.SECONDS);
            return;
        }
        try {
            Player currentPlayer = players.get(currentPlayerIndex);
            GameClientInterface client = clientMap.get(currentPlayer.getName());
            if (client != null) {
                client.setYourTurn(true, 30);
            }
            // Set timer for 30 seconds
            if (turnTimer != null) {
                turnTimer.cancel();
            }

            turnTimer = new Timer();
            turnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Auto skip if player didn't respond
                        if (client != null) {
                            client.setYourTurn(false, 0);
                        }
                        currentPlayerIndex++;
                        startPlayerTurn();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, 30000);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(String playerName, String message) throws RemoteException {
        // Verify it's the correct player's turn
        if (currentPlayerIndex < players.size() && players.get(currentPlayerIndex).getName().equals(playerName)) {

            // cancel the turn timer
            if (turnTimer != null) {
                turnTimer.cancel();
            }

            // Broadcast message to all players
            for (GameClientInterface client : clientMap.values()) {
                client.updateChat(playerName, message);
            }

            // move to next player
            GameClientInterface currentClient = clientMap.get(playerName);
            if (currentClient != null) {
                currentClient.setYourTurn(false, 0);
            }
            currentPlayerIndex++;
            startPlayerTurn();
        }

    }

    private synchronized void startVoting() {
        try {
            currentState = GameState.VOTING;
            broadcastGameState();
            votingComplete = false;

            // Reset votes
            for (Player p : players) {
                p.setVotes(0);
            }

            // Start voting timer for 30 seconds
            startVotingTimer(30);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVotingTimer(int seconds) {
        if (votingTimer != null) {
            votingTimer.cancel();
        }

        votingTimer = new Timer();
        votingTimer.schedule(new TimerTask() {
            int timeLeft = seconds;

            @Override
            public void run() {
                try {
                    if (votingComplete) {
                        return;
                    }

                    timeLeft--;

                    // Update timer for all clients
                    for (GameClientInterface client : clientMap.values()) {
                        client.updateVotingTimer(timeLeft);
                    }

                    if (timeLeft <= 0 || votingComplete) {
                        votingTimer.cancel();
                        if (!votingComplete) {
                            calculateResults();
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }

    @Override
    public synchronized void submitVote(String playerName, String votedPlayer) throws RemoteException {
        if (currentState != GameState.VOTING || votingComplete) {
            return;
        }

        // Check if player already voted
        if (votedPlayers.contains(playerName)) {
            return;
        }

        // Record vote
        for (Player p : players) {
            if (p.getName().equals(votedPlayer)) {
                p.addVote();
                break;
            }
        }

        votedPlayers.add(playerName);

        // Notify client that vote was recorded
        GameClientInterface client = clientMap.get(playerName);
        if (client != null) {
            client.voteRecorded();
        }

        // Check if all players have voted
        if (votedPlayers.size() >= players.size()) {
            votingComplete = true;
            if (votingTimer != null) {
                votingTimer.cancel();
            }
            calculateResults();
        }
    }


    private synchronized void calculateResults() {
        try {
            currentState = GameState.RESULT;

            // Find player with most votes
            Player votedPlayer = null;
            int maxVote = -1;

            for (Player p : players) {
                if (p.getVotes() > maxVote) {
                    maxVote = p.getVotes();
                    votedPlayer = p;
                }
            }

            // Handle tie a breaker (if multiple players have same votes, no one is eliminated)
            int voteCount = 0;
            for (Player p : players) {
                if (p.getVotes() == maxVote) {
                    voteCount++;
                }
            }
            boolean imposterCaught = false;
            if (voteCount == 1 && votedPlayer != null) {
                imposterCaught = votedPlayer.isImposter();
            }

            // Send results to each player
            for (Player p : players) {
                GameClientInterface client = clientMap.get(p.getName());
                if (client != null) {
                    String resultMessage;
                    if (voteCount > 1) {
                        resultMessage = "It's a tie! No one was eliminated.\nThe imposter was: " + imposterName;
                    } else {
                        resultMessage = "Most voted: " + (votedPlayer != null ? votedPlayer.getName() : "None") +
                                "\nThe imposter was: " + imposterName;

                    }
                    client.showVotingResult(imposterName, imposterCaught ? !p.isImposter() : p.isImposter(), resultMessage);
                }
            }
            currentState = GameState.GAME_OVER;
            broadcastGameState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void replayGame() throws RemoteException {
        // Reset game state
        currentRound = 0;
        currentState = GameState.WAITING_FOR_PLAYERS;
        votedPlayers.clear();
        votingComplete=false;

        // Reset players
        for (Player p : players) {
            p.setImposter(false);
            p.setVotes(0);
            p.setWord(null);
            p.setHint(null);
        }
        broadcastGameState();
    }

    private void broadcastGameState() {
        try {
            for (GameClientInterface client : clientMap.values()) {
                client.gameStateChanged(currentState);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Player> getPlayers() throws RemoteException {
        return new ArrayList<>(players);
    }

    @Override
    public GameState getGameState() throws RemoteException {
        return currentState;
    }
}
