package com.mygame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserManager {
    
    public static class UserAccount {
        public String username;
        public String email;
        public String password; // Note: Pour un vrai projet sécurisé, évite de stocker le mdp en clair, mais pour ton launcher local, ça fonctionne.
    }

    private static final Path CONFIG_DIR = Paths.get(System.getenv("APPDATA"), "OblivionStudio", "FamiliarsPlaces");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Sauvegarder les infos du compte
    public static void saveAccount(String username, String email, String password) {
        try {
            Files.createDirectories(CONFIG_DIR);
            UserAccount account = new UserAccount();
            account.username = username;
            account.email = email;
            account.password = password;

            String json = GSON.toJson(account);
            Files.writeString(CONFIG_FILE.toPath(), json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Charger les infos du compte enregistrées
    public static UserAccount loadAccount() {
        if (!CONFIG_FILE.exists()) {
            return null;
        }
        try {
            String json = Files.readString(CONFIG_FILE.toPath());
            return GSON.fromJson(json, UserAccount.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}