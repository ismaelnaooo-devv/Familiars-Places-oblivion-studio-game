package com.mygame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Launcher extends JFrame {

    public static class UserAccount {
        public String username;
        public String email;
        public String password;
    }

    private static final Path APPDATA_DIR = Paths.get(System.getenv("APPDATA"), "OblivionStudio", "FamiliarsPlaces");
    private static final Path ASSETS_DIR = APPDATA_DIR.resolve("assets");
    private static final File CONFIG_FILE = APPDATA_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String GITHUB_ASSETS_URL = "https://github.com/ismaelnaooo-devv/Familiars-Places-oblivion-studio-game/releases/download/v1.0.0/game_assets.zip";

    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private BackgroundPanel homePanel;
    private BackgroundPanel otherGamesPanel;

    public Launcher() {
        setTitle("Familiars Places - Launcher");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            Files.createDirectories(ASSETS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        initUI();
        loadSavedAccount();
        checkBackgroundSilently();
    }

    class BackgroundPanel extends JPanel {
        private Image bgImage;

        public BackgroundPanel(LayoutManager layout) {
            super(layout);
            loadBackground();
        }

        public void loadBackground() {
            File bgFile = ASSETS_DIR.resolve("Textures").resolve("menu_bg.png").toFile();
            if (bgFile.exists()) {
                bgImage = new ImageIcon(bgFile.getAbsolutePath()).getImage();
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bgImage != null) {
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(new Color(30, 30, 30));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private void initUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        homePanel = createHomePanel();
        JPanel profilePanel = createProfilePanel();
        otherGamesPanel = createOtherGamesPanel();

        mainPanel.add(homePanel, "HOME");
        mainPanel.add(profilePanel, "PROFILE");
        mainPanel.add(otherGamesPanel, "OTHER_GAMES");

        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(20, 20, 20, 220));
        sidebar.setLayout(new GridLayout(6, 1, 10, 10));
        sidebar.setPreferredSize(new Dimension(220, getHeight()));

        JButton btnPlayNav = new JButton("Home / Play");
        JButton btnProfileNav = new JButton("My Account");
        JButton btnOtherGamesNav = new JButton("Other Games");

        styleNavButton(btnPlayNav);
        styleNavButton(btnProfileNav);
        styleNavButton(btnOtherGamesNav);

        btnPlayNav.addActionListener(e -> cardLayout.show(mainPanel, "HOME"));
        btnProfileNav.addActionListener(e -> cardLayout.show(mainPanel, "PROFILE"));
        btnOtherGamesNav.addActionListener(e -> cardLayout.show(mainPanel, "OTHER_GAMES"));

        sidebar.add(btnPlayNav);
        sidebar.add(btnProfileNav);
        sidebar.add(btnOtherGamesNav);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void styleNavButton(JButton btn) {
        btn.setBackground(new Color(40, 40, 40));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
    }

    private BackgroundPanel createHomePanel() {
        BackgroundPanel panel = new BackgroundPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        JLabel titleLabel = new JLabel("FAMILIARS PLACES");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JButton playButton = new JButton(" LAUNCH GAME ");
        playButton.setFont(new Font("Arial", Font.BOLD, 20));
        playButton.setBackground(new Color(0, 150, 100));
        playButton.setForeground(Color.WHITE);
        playButton.setFocusPainted(false);
        gbc.gridy = 1;
        panel.add(playButton, gbc);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(400, 25));
        progressBar.setVisible(false);
        gbc.gridy = 2;
        panel.add(progressBar, gbc);

        statusLabel = new JLabel("");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridy = 3;
        panel.add(statusLabel, gbc);

        playButton.addActionListener(e -> {
            saveAccountFromFields();
            playButton.setEnabled(false);
            progressBar.setVisible(true);
            startLoadingAndLaunch();
        });

        return panel;
    }

    private BackgroundPanel createOtherGamesPanel() {
        BackgroundPanel panel = new BackgroundPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);

        JLabel titleLabel = new JLabel("Other games coming soon");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblTitle = new JLabel("Account Settings");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        JLabel lblUser = new JLabel("Username :");
        lblUser.setForeground(Color.LIGHT_GRAY);
        gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(lblUser, gbc);

        usernameField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        JLabel lblEmail = new JLabel("E-mail :");
        lblEmail.setForeground(Color.LIGHT_GRAY);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblEmail, gbc);

        emailField = new JTextField(18);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        JLabel lblPass = new JLabel("Password :");
        lblPass.setForeground(Color.LIGHT_GRAY);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(lblPass, gbc);

        passwordField = new JPasswordField(18);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JButton saveButton = new JButton("Save");
        saveButton.setBackground(new Color(70, 130, 180));
        saveButton.setForeground(Color.WHITE);
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            saveAccountFromFields();
            JOptionPane.showMessageDialog(this, "Account saved successfully!");
        });

        return panel;
    }

    private void saveAccountFromFields() {
        try {
            Files.createDirectories(APPDATA_DIR);
            UserAccount account = new UserAccount();
            account.username = usernameField.getText().trim();
            account.email = emailField.getText().trim();
            account.password = new String(passwordField.getPassword());

            String json = GSON.toJson(account);
            Files.writeString(CONFIG_FILE.toPath(), json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSavedAccount() {
        if (!CONFIG_FILE.exists()) return;
        try {
            String json = Files.readString(CONFIG_FILE.toPath());
            UserAccount account = GSON.fromJson(json, UserAccount.class);
            if (account != null) {
                if (account.username != null) usernameField.setText(account.username);
                if (account.email != null) emailField.setText(account.email);
                if (account.password != null) passwordField.setText(account.password);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkBackgroundSilently() {
        SwingUtilities.invokeLater(() -> {
            if (homePanel != null) homePanel.loadBackground();
            if (otherGamesPanel != null) otherGamesPanel.loadBackground();
        });
    }

    private void startLoadingAndLaunch() {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> { statusLabel.setText("Checking files..."); progressBar.setValue(50); });
                
                File texturesDir = ASSETS_DIR.resolve("Textures").toFile();
                File modelsDir = ASSETS_DIR.resolve("Models").toFile();

                // Si les dossiers n'existent pas encore, on télécharge et extrait
                if (!texturesDir.exists() || !modelsDir.exists()) {
                    File targetZip = ASSETS_DIR.resolve("game_assets.zip").toFile();
                    if (!targetZip.exists()) {
                        SwingUtilities.invokeLater(() -> statusLabel.setText("Downloading assets..."));
                        downloadFile(GITHUB_ASSETS_URL, targetZip);
                    }
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Extracting files..."));
                    unzipFile(targetZip, ASSETS_DIR.toFile());
                }

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Launching game...");
                    progressBar.setValue(100);
                });
                Thread.sleep(300);

                String playerName = usernameField.getText().trim();
                if (playerName.isEmpty()) playerName = "Player";
                System.setProperty("player.name", playerName);

                com.mygame.Main.main(new String[0]);
                dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    progressBar.setValue(0);
                });
            }
        }).start();
    }

    private void downloadFile(String fileURL, File saveFile) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setInstanceFollowRedirects(true);
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        httpConn.setConnectTimeout(30000);
        httpConn.setReadTimeout(60000);
        
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
            responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
            responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = httpConn.getHeaderField("Location");
            url = new URL(newUrl);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            httpConn.setConnectTimeout(30000);
            httpConn.setReadTimeout(60000);
            responseCode = httpConn.getResponseCode();
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(saveFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            throw new IOException("Server error code: " + responseCode);
        }
        httpConn.disconnect();
    }

    private void unzipFile(File zipFile, File targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(targetDir, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new RuntimeException("Cannot create directory: " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new RuntimeException("Cannot create parent directory: " + parent);
                    }
                    Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }
}