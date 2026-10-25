package com.mygame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class Launcher extends JFrame {

    private String currentLang = "FR";
    private Map<String, Map<String, String>> langMap = new HashMap<>();

    private JLabel studioTitle, gameTitle, gameDesc, statusLabel, pseudoLabel, loadingTitle, gamesLabel;
    private JTextField pseudoField;
    private JButton playButton, langButton, game1Btn, game2Btn;
    private JPanel mainPanel, sidebarPanel, contentPanel, loadingPanel, bottomBar;
    private JProgressBar progressBar;
    private String selectedGame = "Familiars Places";

    public Launcher() {
        initTranslations();

        setTitle("Oblivion Studio Hub");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setBackground(new Color(18, 18, 24));
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        // Sidebar
        sidebarPanel = new JPanel();
        sidebarPanel.setBackground(new Color(12, 12, 16));
        sidebarPanel.setLayout(null);
        mainPanel.add(sidebarPanel);

        studioTitle = new JLabel("OBLIVION HUB", SwingConstants.CENTER);
        studioTitle.setForeground(Color.CYAN);
        studioTitle.setFont(new Font("Arial", Font.BOLD, 20));
        sidebarPanel.add(studioTitle);

        gamesLabel = new JLabel("JEUX");
        gamesLabel.setForeground(Color.GRAY);
        gamesLabel.setFont(new Font("Arial", Font.BOLD, 12));
        sidebarPanel.add(gamesLabel);

        game1Btn = new JButton("Familiars Places");
        styleSidebarButton(game1Btn, true);
        game1Btn.addActionListener(e -> selectGame("Familiars Places"));
        sidebarPanel.add(game1Btn);

        game2Btn = new JButton("Oblivion Quest");
        styleSidebarButton(game2Btn, false);
        game2Btn.addActionListener(e -> selectGame("Oblivion Quest"));
        sidebarPanel.add(game2Btn);

        langButton = new JButton();
        langButton.setBackground(new Color(30, 30, 42));
        langButton.setForeground(Color.WHITE);
        langButton.setFocusPainted(false);
        langButton.addActionListener(e -> toggleLanguage());
        sidebarPanel.add(langButton);

        // Content Area
        contentPanel = new JPanel();
        contentPanel.setBackground(new Color(24, 24, 32));
        contentPanel.setLayout(null);
        mainPanel.add(contentPanel);

        gameTitle = new JLabel("Familiars Places");
        gameTitle.setForeground(Color.WHITE);
        gameTitle.setFont(new Font("Arial", Font.BOLD, 38));
        contentPanel.add(gameTitle);

        gameDesc = new JLabel();
        gameDesc.setForeground(Color.LIGHT_GRAY);
        gameDesc.setFont(new Font("Arial", Font.PLAIN, 15));
        contentPanel.add(gameDesc);

        // Bottom Bar
        bottomBar = new JPanel();
        bottomBar.setBackground(new Color(15, 15, 20));
        bottomBar.setLayout(null);
        mainPanel.add(bottomBar);

        pseudoLabel = new JLabel();
        pseudoLabel.setForeground(Color.LIGHT_GRAY);
        pseudoLabel.setFont(new Font("Arial", Font.BOLD, 13));
        bottomBar.add(pseudoLabel);

        pseudoField = new JTextField("Player1");
        pseudoField.setBackground(new Color(30, 30, 42));
        pseudoField.setForeground(Color.WHITE);
        pseudoField.setCaretColor(Color.WHITE);
        pseudoField.setFont(new Font("Arial", Font.PLAIN, 14));
        bottomBar.add(pseudoField);

        playButton = new JButton();
        playButton.setBackground(new Color(0, 185, 115));
        playButton.setForeground(Color.WHITE);
        playButton.setFont(new Font("Arial", Font.BOLD, 20));
        playButton.setFocusPainted(false);
        playButton.setBorder(BorderFactory.createLineBorder(new Color(0, 220, 130), 2));
        playButton.addActionListener(e -> startLoadingAndLaunch());
        bottomBar.add(playButton);

        // Loading Panel
        loadingPanel = new JPanel();
        loadingPanel.setBackground(new Color(10, 10, 14));
        loadingPanel.setLayout(null);
        loadingPanel.setVisible(false);
        mainPanel.add(loadingPanel);

        loadingTitle = new JLabel("OBLIVION EVENTS - SYNC", SwingConstants.CENTER);
        loadingTitle.setForeground(Color.CYAN);
        loadingTitle.setFont(new Font("Arial", Font.BOLD, 28));
        loadingPanel.add(loadingTitle);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        loadingPanel.add(statusLabel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setForeground(new Color(0, 200, 140));
        progressBar.setBackground(new Color(30, 30, 40));
        progressBar.setStringPainted(true);
        loadingPanel.add(progressBar);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                updateComponentBounds();
            }
        });

        updateTexts();
        setVisible(true);
    }

    private void updateComponentBounds() {
        int w = getWidth();
        int h = getHeight();

        mainPanel.setBounds(0, 0, w, h);

        sidebarPanel.setBounds(0, 0, 260, h);
        studioTitle.setBounds(20, 30, 220, 30);
        gamesLabel.setBounds(25, 90, 200, 20);
        game1Btn.setBounds(15, 120, 230, 45);
        game2Btn.setBounds(15, 175, 230, 45);
        langButton.setBounds(20, h - 90, 220, 40);

        contentPanel.setBounds(260, 0, w - 260, h - 110);
        gameTitle.setBounds(60, 60, 700, 50);
        gameDesc.setBounds(65, 120, 700, 30);

        bottomBar.setBounds(260, h - 110, w - 260, 110);
        pseudoLabel.setBounds(60, 15, 150, 20);
        pseudoField.setBounds(60, 40, 220, 35);
        playButton.setBounds((w - 260) - 300, 22, 240, 65);

        loadingPanel.setBounds(0, 0, w, h);
        loadingTitle.setBounds(w / 2 - 300, h / 2 - 80, 600, 40);
        statusLabel.setBounds(w / 2 - 300, h / 2 - 20, 600, 30);
        progressBar.setBounds(w / 2 - 250, h / 2 + 35, 500, 30);
    }

    private void styleSidebarButton(JButton btn, boolean selected) {
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(selected ? new Color(40, 40, 55) : new Color(18, 18, 24));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
    }

    private void selectGame(String gameName) {
        selectedGame = gameName;
        if (gameName.equals("Familiars Places")) {
            game1Btn.setBackground(new Color(40, 40, 55));
            game2Btn.setBackground(new Color(18, 18, 24));
            gameTitle.setText("Familiars Places");
            gameDesc.setText(currentLang.equals("FR") ? "Jeu principal de survie et d'exploration - Oblivion Studio." : "Main survival and exploration game - Oblivion Studio.");
        } else {
            game2Btn.setBackground(new Color(40, 40, 55));
            game1Btn.setBackground(new Color(18, 18, 24));
            gameTitle.setText("Oblivion Quest");
            gameDesc.setText(currentLang.equals("FR") ? "Projet en cours de développement - Bientôt disponible." : "Project currently in development - Coming soon.");
        }
    }

    private void toggleLanguage() {
        currentLang = currentLang.equals("FR") ? "EN" : "FR";
        updateTexts();
        selectGame(selectedGame);
    }

    private void initTranslations() {
        Map<String, String> fr = new HashMap<>();
        fr.put("pseudo", "Nom de profil :");
        fr.put("play", "JOUER");
        fr.put("lang", "🌐 Langue : Français (FR)");
        fr.put("step1", "Vérification des répertoires AppData...");
        fr.put("step2", "Téléchargement des assets manquants...");
        fr.put("step3", "Synchronisation des serveurs de jeu...");
        fr.put("step4", "Lancement de Familiars Places...");
        langMap.put("FR", fr);

        Map<String, String> en = new HashMap<>();
        en.put("pseudo", "Profile Name:");
        en.put("play", "PLAY");
        en.put("lang", "🌐 Language: English (EN)");
        en.put("step1", "Checking AppData directories...");
        en.put("step2", "Downloading missing assets...");
        en.put("step3", "Synchronizing game servers...");
        en.put("step4", "Launching Familiars Places...");
        langMap.put("EN", en);
    }

    private void updateTexts() {
        Map<String, String> t = langMap.get(currentLang);
        pseudoLabel.setText(t.get("pseudo"));
        playButton.setText(t.get("play"));
        langButton.setText(t.get("lang"));
    }

    private void startLoadingAndLaunch() {
        if (!selectedGame.equals("Familiars Places")) {
            JOptionPane.showMessageDialog(this, currentLang.equals("FR") ? "Ce jeu n'est pas encore disponible !" : "This game is not available yet!");
            return;
        }

        String pseudo = pseudoField.getText().trim();
        if (pseudo.isEmpty()) {
            JOptionPane.showMessageDialog(this, currentLang.equals("FR") ? "Veuillez entrer un pseudo valide !" : "Please enter a valid username!");
            return;
        }

        for (Component c : mainPanel.getComponents()) {
            if (c != loadingPanel) c.setVisible(false);
        }
        loadingPanel.setVisible(true);
        updateComponentBounds();

        Map<String, String> t = langMap.get(currentLang);

        new Thread(() -> {
            try {
                // 1. Vérif AppData
                SwingUtilities.invokeLater(() -> { statusLabel.setText(t.get("step1")); progressBar.setValue(20); });
                Path assetsDir = prepareAppData(pseudo);
                Thread.sleep(600);

                // 2. Téléchargement si absent (Exemple avec un fichier d'asset distant)
                SwingUtilities.invokeLater(() -> { statusLabel.setText(t.get("step2")); progressBar.setValue(55); });
                
                // Remplace cette URL par ton lien direct vers ton fichier (ex: .zip d'assets hébergé sur GitHub, Discord ou ton serveur)
                File targetFile = assetsDir.resolve("game_assets.zip").toFile();
                if (!targetFile.exists()) {
                    // Exemple de téléchargement réel :
                    // downloadFile("https://ton-site.com/game_assets.zip", targetFile);
                }
                Thread.sleep(800);

                // 3. Synchro serveurs
                SwingUtilities.invokeLater(() -> { statusLabel.setText(t.get("step3")); progressBar.setValue(85); });
                Thread.sleep(600);

                // 4. Lancement
                SwingUtilities.invokeLater(() -> { statusLabel.setText(t.get("step4")); progressBar.setValue(100); });
                Thread.sleep(500);

                System.setProperty("game.player.name", pseudo);
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    Main.main(new String[]{pseudo});
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de chargement / Téléchargement !");
                    ex.printStackTrace();
                });
            }
        }).start();
    }

    private Path prepareAppData(String pseudo) {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home") + File.separator + ".appdata";
            }
            
            Path gameDir = Paths.get(appData, "OblivionStudio", "FamiliarsPlaces");
            Path assetsDir = gameDir.resolve("assets");
            Path configDir = gameDir.resolve("config");

            if (!Files.exists(gameDir)) Files.createDirectories(gameDir);
            if (!Files.exists(assetsDir)) Files.createDirectories(assetsDir);
            if (!Files.exists(configDir)) Files.createDirectories(configDir);

            File configFile = configDir.resolve("launcher_profile.cfg").toFile();
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("last_player=" + pseudo + "\n");
                writer.write("lang=" + currentLang + "\n");
            }
            return assetsDir;
        } catch (Exception e) {
            return null;
        }
    }

    private void downloadFile(String fileUrl, File destination) throws Exception {
        URI uri = URI.create(fileUrl);
        try (InputStream in = uri.toURL().openStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }
}