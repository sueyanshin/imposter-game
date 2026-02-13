package client;

import common.GameInterface;
import common.GameState;
import common.Player;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

public class GameClientUI extends JFrame {
    private JTextField nameField;
    private JButton connectButton;
    private JButton registerButton;
    private JButton startGameButton;
    private JButton sendButton;
    private JButton voteButton;
    private JButton replayButton;
    private JTextArea chatArea;
    private JTextField messageField;
    private JLabel statusLabel;
    private JLabel turnLabel;
    private JLabel timerLabel;
    private JLabel wordLabel;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private JComboBox<String> voteComboBox;

    private GameInterface server;
    private GameClientImpl client;
    private String playerName;
    private GameState currentState;
    private java.util.Timer turnTimer;
    private int timeLeft;

    public GameClientUI() {
        initComponents();
        connectToServer();
    }


    private void initComponents() {
        setTitle("Imposter Game Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Top Panel - Connection
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topPanel.add(new JLabel("Player Name"));
        nameField = new JTextField(15);
        topPanel.add(nameField);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        topPanel.add(connectButton);

        registerButton = new JButton("Register");
        registerButton.setEnabled(false);
        registerButton.addActionListener(e -> registerPlayer());
        topPanel.add(registerButton);

        statusLabel = new JLabel("Status: Not Connected");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Game Area
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left Panel - Player info
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Game Info",
                TitledBorder.LEFT, TitledBorder.TOP));
        // Word Display
        wordLabel = new JLabel("", SwingConstants.CENTER);
        wordLabel.setFont(new Font("Arial", Font.BOLD, 16));
        wordLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        leftPanel.add(wordLabel, BorderLayout.NORTH);

        // Player List
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane playerScroll = new JScrollPane(playerList);
        playerScroll.setBorder(BorderFactory.createTitledBorder("Players"));
        leftPanel.add(playerScroll, BorderLayout.CENTER);

        // Turn and Time info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        turnLabel = new JLabel("Not your turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 12));
        timerLabel = new JLabel("", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(turnLabel);
        infoPanel.add(timerLabel);
        leftPanel.add(infoPanel, BorderLayout.SOUTH);

        // Right Panel - Chatting and Voting
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Chat & Voting",
                TitledBorder.LEFT, TitledBorder.TOP));

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setPreferredSize(new Dimension(400, 300));
        rightPanel.add(chatScroll, BorderLayout.CENTER);

        // Message Input
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        rightPanel.add(messagePanel, BorderLayout.SOUTH);

        // Voting Panel
        JPanel votingPanel = new JPanel(new FlowLayout());
        votingPanel.setBorder(BorderFactory.createTitledBorder("Voting"));
        voteComboBox = new JComboBox<>();
        voteComboBox.setEnabled(false);
        voteButton = new JButton("Vote");
        voteButton.setEnabled(false);
        voteButton.addActionListener(e -> submitVote());
        votingPanel.add(voteComboBox);
        votingPanel.add(voteButton);
        rightPanel.add(votingPanel, BorderLayout.NORTH);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(300);

        add(splitPane, BorderLayout.CENTER);

        // Bottom Panel - Game Control
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());

        startGameButton = new JButton("Start Game");
        startGameButton.setEnabled(false);
        startGameButton.addActionListener(e -> startGame());

        replayButton = new JButton("Play Again");
        replayButton.setEnabled(false);
        replayButton.addActionListener(e -> replayGame());

        bottomPanel.add(startGameButton);
        bottomPanel.add(replayButton);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(900, 700);
        setLocationRelativeTo(null);
    }

    private void connectToServer() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            server = (GameInterface) registry.lookup("ImposterGame");

            statusLabel.setText("Status: Connected to Server");
            statusLabel.setForeground(new Color(0, 150, 0));
            registerButton.setEnabled(true);
            connectButton.setEnabled(false);

            appendChat("System: Connected to game server");
            appendChat("System: Enter your name and click Register");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to serverL: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void registerPlayer() {
        playerName = nameField.getText().trim();
        if (playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a player name",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            client = new GameClientImpl(playerName, this);
            boolean registered = server.registerPlayer(playerName, client);
            if (registered) {
                statusLabel.setText("Status: Registered as " + playerName);
                registerButton.setEnabled(false);
                nameField.setEnabled(false);
                startGameButton.setEnabled(true);
                appendChat("System: Successfully registered as " + playerName);
                updatePlayerList();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to register. Name might be taken or game already started.",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to register" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startGame() {
        try {
            server.startGame();
            startGameButton.setEnabled(false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && client != null && client.isMyTurn()) {
            try {
                server.sendMessage(playerName, message);
                messageField.setText("");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void submitVote() {
        String votedPlayer = (String) voteComboBox.getSelectedItem();
        if (votedPlayer != null && !votedPlayer.equals(playerName)) {
            try {
                server.submitVote(playerName, votedPlayer);
                voteButton.setEnabled(false);
                voteComboBox.setEnabled(false);
                appendChat("System: You voted for " + votedPlayer);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void replayGame() {
        try {
            server.replayGame();
            restartGameUI();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void appendChat(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void setTurnStatus(boolean isMyTurn, int timeLimit) {
        SwingUtilities.invokeLater(() -> {
            if (isMyTurn) {
                turnLabel.setText("YOUR TURN!");
                turnLabel.setForeground(Color.RED);
                messageField.setEnabled(true);
                sendButton.setEnabled(true);
                messageField.requestFocus();
                startTimer(timeLimit);
            } else {
                turnLabel.setText("Not your Turn");
                turnLabel.setForeground(Color.black);
                messageField.setEnabled(false);
                sendButton.setEnabled(false);
                stopTimer();
            }
        });
    }

    public void showWord(String word) {
        SwingUtilities.invokeLater(() -> {
            wordLabel.setText("<html><center>" + word.replace("\n", "<br>") + "</center></html>");
        });
    }

    public void showVotingResult(String imposter, boolean won) {
        SwingUtilities.invokeLater(() -> {
            String message;
            if (won) {
                message = "Congratulations! " + (client.isImposter() ?
                        "You fooled everyone!" : "You caught the imposter!");
            } else {
                message = "Game Over! " + (client.isImposter() ?
                        "You were caught!" : "The imposter escaped!");
            }
            message += "\nThe imposter was: " + imposter;

            JOptionPane.showMessageDialog(this, message, "Game Result", JOptionPane.INFORMATION_MESSAGE);

            replayButton.setEnabled(true);
        });
    }

    public void updateGameState(GameState state) {
        this.currentState = state;
        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case WAITING_FOR_PLAYERS:
                    setTitle("Imposter Game - Waiting for Players");
                    break;
                case WORD_DISTRIBUTION:
                    setTitle("Imposter Game - Game Starting...");
                    break;
                case ROUND_1:
                    setTitle("Imposter Game - Round 1");
                    break;
                case ROUND_2:
                    setTitle("Imposter Game - Round 2");
                    break;
                case ROUND_3:
                    setTitle("Imposter Game - Round 3");
                    break;
                case VOTING:
                    setTitle("Imposter Game - Voting");
                    enableVoting();
                    break;
                case GAME_OVER:
                    setTitle("Imposter Game - Game Over");
                    break;
            }

            updatePlayerList();
        });
    }

    private void enableVoting() {
        SwingUtilities.invokeLater(() -> {
            voteComboBox.removeAllItems();
            try {
                java.util.List<Player> players = server.getPlayers();
                for (Player p : players) {
                    if (!p.getName().equals(playerName)) {
                        voteComboBox.addItem(p.getName());
                    }
                }
                voteComboBox.setEnabled(true);
                voteButton.setEnabled(true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

    }

    private void updatePlayerList() {
        try {
            java.util.List<Player> players = server.getPlayers();
            SwingUtilities.invokeLater(() -> {
                playerListModel.clear();
                for (Player p : players) {
                    playerListModel.addElement(p.getName());
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startTimer(int seconds) {
        stopTimer();
        timeLeft = seconds;
        turnTimer = new Timer();
        turnTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Time left: " + timeLeft + "s");
                    timeLeft--;
                    if (timeLeft < 0) {
                        stopTimer();
                        timerLabel.setText("Time's up");
                    }
                });

            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;

        }
        timerLabel.setText("");
    }

    private void restartGameUI() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            wordLabel.setText("");
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            voteComboBox.setEnabled(false);
            voteButton.setEnabled(false);
            replayButton.setEnabled(false);
            startGameButton.setEnabled(true);
            turnLabel.setText("Not your turn");
            timerLabel.setText("");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new GameClientUI().setVisible(true);
        });
    }
}