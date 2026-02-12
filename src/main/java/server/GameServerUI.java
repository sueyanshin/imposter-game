package server;

import common.GameState;
import common.Player;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class GameServerUI extends JFrame {
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JLabel playerCountLabel;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private GameServer server;
    private Registry registry;

    public GameServerUI() {
        initComponents();
        startRMIServer();
    }

    private void initComponents() {
        setTitle("Imposter Game Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Top Panel - Server Control
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(statusLabel, gbc);

        playerCountLabel = new JLabel("Players: 0/6");
        playerCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        topPanel.add(playerCountLabel, gbc);

        startButton = new JButton("Start Game");
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startGame());
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 0, 0, 5);
        topPanel.add(startButton, gbc);

        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(true);
        stopButton.addActionListener(e -> stopServer());
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 0, 0);
        topPanel.add(stopButton, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Player list and log
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Player List Panel
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Connected Players",
                TitledBorder.LEFT, TitledBorder.TOP));
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane playerScroll = new JScrollPane(playerList);
        playerScroll.setPreferredSize(new Dimension(200, 300));
        playerPanel.add(playerScroll, BorderLayout.CENTER);

        // Log Panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Server Log",
                TitledBorder.LEFT, TitledBorder.TOP));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(playerPanel);
        splitPane.setRightComponent(logPanel);
        splitPane.setDividerLocation(250);

        add(splitPane, BorderLayout.CENTER);

        // Bottom Panel - Info
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel infoLabel = new JLabel("Server running on port 1099");
        bottomPanel.add(infoLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(900, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
    }

    private void startRMIServer() {
        try {
            registry = LocateRegistry.createRegistry(1099);
            server = new GameServer(this);
            registry.bind("ImposterGame", server);

            statusLabel.setText("Status: Running");
            statusLabel.setForeground(new Color(0, 150, 0));
            startButton.setEnabled(false); // Will be enabled when enough players join
            stopButton.setEnabled(true);

            log("RMI Registry started on port 1099");
            log("server is ready");

            // Start server status update thread
            startServerStatusUpdater();
        } catch (Exception e) {
            log("Error starting server: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to start server:" + e.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startServerStatusUpdater() {
        Thread updater = new Thread(() -> {
            while (server != null) {
                try {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            int playerCount = server.getPlayers().size();
                            playerCountLabel.setText("Players: " + playerCount + "/6");

                            // Update player list
                            playerListModel.clear();
                            List<Player> players = server.getPlayers();
                            for (Player p : players) {
                                String status = p.isImposter() ? " (Imposter)" : "";
                                playerListModel.addElement(p.getName() + status);
                            }

                            // Enable start button if enough players and game not started
                            GameState state = server.getGameState();
                            if (playerCount >= 3 && state == GameState.WAITING_FOR_PLAYERS) {
                                startButton.setEnabled(true);
                            } else {
                                startButton.setEnabled(false);
                            }
                        } catch (RemoteException e) {
                            log("Error updating status: " + e);
                        }
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updater.setDaemon(true);
        updater.start();
    }

    private void startGame() {
        try {
            server.startGame();
            log("Game Started");
            startButton.setEnabled(false);
        } catch (RemoteException e) {
            log("Error starting game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopServer() {
        try {
            if (registry != null) {
                registry.unbind("ImposterGame");
                Registry registry = LocateRegistry.getRegistry(1099);
                registry.unbind("ImposterGame");
            }
            if (server != null) {
                server = null;
            }

            statusLabel.setText("Status: Stopped");
            statusLabel.setForeground(Color.RED);
            startButton.setEnabled(false);
            stopButton.setEnabled(false);

            log("Server stopped");
        } catch (Exception e) {
            log("Error stopping server: " + e.getMessage());
        }
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new GameServerUI().setVisible(true);
        });

    }
}
