package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import jme3tools.optimize.GeometryBatchFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class Main extends SimpleApplication implements ActionListener {

    private enum GameState {
        EPILEPSY_WARNING,
        MAIN_MENU,
        MULTIPARTY_LOBBY,
        PLAYING
    }
    private GameState currentState = GameState.EPILEPSY_WARNING;

    private Node warningNode = new Node("WarningNode");
    private Node menuNode = new Node("MenuNode");
    private Node lobbyNode = new Node("LobbyNode");

    // Menu Navigation & Selection
    private int selectedMenuIndex = 0; 
    private BitmapText menuOpt1;
    private BitmapText menuOpt2;
    private BitmapText menuOpt3;

    // Multiplayer Client & Notification System
    private String playerName = System.getProperty("player.name", "Player1");
    private StringBuilder targetHostInput = new StringBuilder();
    private BitmapText lobbyStatusText;
    private BitmapText joinBoxText;
    private BitmapText ingameMultiHud;

    // Chat / Command System (/give key)
    private boolean isChatting = false;
    private Node chatNode = new Node("ChatNode");
    private StringBuilder chatInput = new StringBuilder();
    private BitmapText chatText;

    // Mode Saisie Invitation In-Game (F3 -> 4)
    private boolean isIngameInviting = false;
    private Node ingameInviteNode = new Node("IngameInviteNode");
    private BitmapText ingameInviteText;

    private Node invitationDialogNode = new Node("InvitationDialogNode");
    private BitmapText invitationText;
    private boolean hasActiveInvite = false;
    private String incomingSenderName = "";
    private boolean shiftPressed = false;

    private List<String> playerInventory = new ArrayList<>();
    private BitmapText inventoryText;
    private BitmapText orbStatusHudText;

    private Geometry playerBodyGeo;
    private Node playerAvatarNode = new Node("PlayerAvatarNode");

    // --- COMPAS D'ÂME (SPIRIT ORB) ---
    private Geometry spiritOrbGeo;
    private PointLight spiritOrbLight;
    private float orbFloatTimer = 0f;

    public static void main(String[] args) {
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            System.setProperty("player.name", args[0]);
        }

        Main app = new Main();
        app.setShowSettings(false);

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Familiars Places - Oblivion Studio");
        settings.setFullscreen(true);
        settings.setWidth(1920);
        settings.setHeight(1080);
        settings.setFrequency(-1);
        settings.setVSync(true);

        app.setSettings(settings);
        app.start();
    }

    private final int chunkSize = 16;
    private final int renderDistance = 2;
    private final Map<String, Node> loadedChunks = new HashMap<>();
    private final Map<String, List<PointLight>> chunkLights = new HashMap<>();
    private final Set<String> globalWalls = new HashSet<>();

    private long worldSeed = 1337;

    private Vector3f playerPos = new Vector3f(2f, 1f, 2f);
    private float moveSpeed = 4f;

    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean qPressed = false;
    private boolean dPressed = false;

    private boolean isPaused = false;
    private float sensitivity = 1.0f;
    private String graphicsQuality = "Medium";
    private float masterVolume = 1.0f;

    private AudioNode hubAudio;
    private AudioNode cityAudio;
    private boolean isMouseDown = false;

    private Geometry keyGeo;
    private Vector3f keyPos;
    private boolean hasKey = false;
    private boolean hasWon = false;
    private boolean isGameOver = false;
    private boolean isNoclip = false;

    private Geometry exitGeo;
    private Vector3f exitPos;
    private boolean nearPortal = false;
    private boolean inDungeon = false;
    private boolean inCity = false;

    private Geometry swordGeo;
    private boolean hasSword = false;
    private Geometry swordPickupGeo;
    private Vector3f swordPos;

    private Geometry giantMonsterGeo;
    private Vector3f giantMonsterPos;
    private float giantMonsterSpeed = 1.5f;
    private float cityTimer = 0f;

    private static class GoblinData {
        Node node;
        float hp;
        Geometry healthBar;
    }
    private List<GoblinData> goblins = new ArrayList<>();
    private Node dungeonRoomNode = new Node("DungeonRoomNode");
    private Node cityNode = new Node("CityNode");

    private Node deathScreenNode = new Node("DeathScreenNode");
    private Node winScreenNode = new Node("WinScreenNode");
    private BitmapText gameOverText;
    private BitmapText respawnBtnText;
    private BitmapText winText;
    private BitmapText replayWinBtnText;

    private Node dungeonNode = new Node("DungeonNode");

    private BitmapText fpsText;
    private BitmapText questText;
    private BitmapText interactText;
    private BitmapText pauseMenuText;
    private float fpsUpdateTimer = 0f;

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);

        flyCam.setMoveSpeed(0f);
        flyCam.setRotationSpeed(sensitivity * 2f);

        cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 100f);
        rootNode.attachChild(dungeonNode);
        rootNode.attachChild(dungeonRoomNode);
        rootNode.attachChild(cityNode);
        dungeonRoomNode.setCullHint(Node.CullHint.Never);
        cityNode.setCullHint(Node.CullHint.Never);

        Box bodyMesh = new Box(0.4f, 0.9f, 0.2f);
        playerBodyGeo = new Geometry("PlayerBody", bodyMesh);
        Material bodyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bodyMat.setColor("Color", ColorRGBA.Blue);
        playerBodyGeo.setMaterial(bodyMat);
        playerBodyGeo.setLocalTranslation(0, 0.9f, 0);
        playerAvatarNode.attachChild(playerBodyGeo);
        playerAvatarNode.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(playerAvatarNode);

        // --- COMPAS D'ÂME (SPIRIT ORB) ---
        Sphere orbMesh = new Sphere(16, 16, 0.25f);
        spiritOrbGeo = new Geometry("SpiritOrb", orbMesh);
        Material orbMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        orbMat.setColor("Color", ColorRGBA.White);
        spiritOrbGeo.setMaterial(orbMat);
        spiritOrbGeo.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(spiritOrbGeo);

        spiritOrbLight = new PointLight();
        spiritOrbLight.setColor(ColorRGBA.Black);
        spiritOrbLight.setRadius(8f);
        rootNode.addLight(spiritOrbLight);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.01f));
        rootNode.addLight(ambient);

        BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        initEpilepsyWarningScreen(guiFont);
        initMainMenuScreen(guiFont);
        initLobbyScreen(guiFont);
        initInvitationDialog(guiFont);
        initIngameInviteDialog(guiFont);
        initChatDialog(guiFont);
        guiNode.attachChild(warningNode);

        inputManager.setCursorVisible(true);
        flyCam.setEnabled(false);

        fpsText = new BitmapText(guiFont);
        fpsText.setSize(guiFont.getCharSet().getRenderedSize());
        fpsText.setColor(ColorRGBA.Green);
        fpsText.setLocalTranslation(10, cam.getHeight() - 10, 0);
        guiNode.attachChild(fpsText);

        questText = new BitmapText(guiFont);
        questText.setSize(guiFont.getCharSet().getRenderedSize());
        questText.setColor(ColorRGBA.Yellow);
        questText.setLocalTranslation(10, cam.getHeight() - 35, 0);
        guiNode.attachChild(questText);

        ingameMultiHud = new BitmapText(guiFont);
        ingameMultiHud.setSize(guiFont.getCharSet().getRenderedSize() * 0.9f);
        ingameMultiHud.setColor(ColorRGBA.Cyan);
        ingameMultiHud.setLocalTranslation(10, cam.getHeight() - 65, 0);
        ingameMultiHud.setText("Multiplayer Target: None (Press F3 -> 4 to invite)");
        guiNode.attachChild(ingameMultiHud);

        inventoryText = new BitmapText(guiFont);
        inventoryText.setSize(guiFont.getCharSet().getRenderedSize());
        inventoryText.setColor(ColorRGBA.Cyan);
        inventoryText.setLocalTranslation(cam.getWidth() - 300, cam.getHeight() - 30, 0);
        inventoryText.setCullHint(BitmapText.CullHint.Always);
        guiNode.attachChild(inventoryText);

        orbStatusHudText = new BitmapText(guiFont);
        orbStatusHudText.setSize(guiFont.getCharSet().getRenderedSize() * 0.9f);
        orbStatusHudText.setColor(ColorRGBA.White);
        orbStatusHudText.setLocalTranslation(cam.getWidth() - 300, cam.getHeight() - 150, 0);
        orbStatusHudText.setCullHint(BitmapText.CullHint.Always);
        guiNode.attachChild(orbStatusHudText);

        updateInventoryUI();

        interactText = new BitmapText(guiFont);
        interactText.setSize(guiFont.getCharSet().getRenderedSize() * 1.2f);
        interactText.setColor(ColorRGBA.Green);
        interactText.setLocalTranslation((cam.getWidth() / 2f) - 180, 100, 0);
        interactText.setCullHint(BitmapText.CullHint.Always);
        guiNode.attachChild(interactText);

        pauseMenuText = new BitmapText(guiFont);
        pauseMenuText.setSize(guiFont.getCharSet().getRenderedSize() * 1.2f);
        pauseMenuText.setColor(ColorRGBA.White);
        pauseMenuText.setLocalTranslation((cam.getWidth() / 2f) - 220, (cam.getHeight() / 2f) + 180, 0);
        pauseMenuText.setCullHint(BitmapText.CullHint.Always);
        guiNode.attachChild(pauseMenuText);

        Quad vignetteMesh = new Quad(cam.getWidth(), cam.getHeight());
        Geometry vignetteGeo = new Geometry("Vignette", vignetteMesh);
        Material vignetteMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        vignetteMat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.85f));
        vignetteGeo.setMaterial(vignetteMat);
        deathScreenNode.attachChild(vignetteGeo);

        gameOverText = new BitmapText(guiFont);
        gameOverText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        gameOverText.setColor(ColorRGBA.Red);
        gameOverText.setText("GAME OVER");
        gameOverText.setLocalTranslation((cam.getWidth() / 2f) - 140, (cam.getHeight() / 2f) + 50, 0);
        deathScreenNode.attachChild(gameOverText);

        respawnBtnText = new BitmapText(guiFont);
        respawnBtnText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        respawnBtnText.setColor(ColorRGBA.White);
        respawnBtnText.setText("Press [R] to Respawn");
        respawnBtnText.setLocalTranslation((cam.getWidth() / 2f) - 130, (cam.getHeight() / 2f) - 40, 0);
        deathScreenNode.attachChild(respawnBtnText);
        deathScreenNode.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(deathScreenNode);

        Geometry winVignetteGeo = new Geometry("WinVignette", vignetteMesh);
        Material winVignetteMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        winVignetteMat.setColor("Color", new ColorRGBA(0f, 0.1f, 0.2f, 0.85f));
        winVignetteGeo.setMaterial(winVignetteMat);
        winScreenNode.attachChild(winVignetteGeo);

        winText = new BitmapText(guiFont);
        winText.setSize(guiFont.getCharSet().getRenderedSize() * 2.5f);
        winText.setColor(ColorRGBA.Cyan);
        winText.setText("YOU WIN");
        winText.setLocalTranslation((cam.getWidth() / 2f) - 110, (cam.getHeight() / 2f) + 50, 0);
        winScreenNode.attachChild(winText);

        replayWinBtnText = new BitmapText(guiFont);
        replayWinBtnText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        replayWinBtnText.setColor(ColorRGBA.White);
        replayWinBtnText.setText("Press [R] to Replay");
        replayWinBtnText.setLocalTranslation((cam.getWidth() / 2f) - 120, (cam.getHeight() / 2f) - 40, 0);
        winScreenNode.attachChild(replayWinBtnText);
        winScreenNode.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(winScreenNode);

        Box swordMesh = new Box(0.1f, 0.4f, 0.05f);
        swordGeo = new Geometry("PlayerSword", swordMesh);
        Material swordMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        swordMat.setColor("Color", ColorRGBA.Cyan);
        swordGeo.setMaterial(swordMat);
        swordGeo.setLocalTranslation(cam.getWidth() - 100, 80, 0);
        swordGeo.setCullHint(Geometry.CullHint.Always);
        guiNode.attachChild(swordGeo);

        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Attack", new MouseButtonTrigger(0));
        inputManager.addMapping("ToggleFullscreen", new KeyTrigger(KeyInput.KEY_F11));
        inputManager.addMapping("ResetGame", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("TogglePause", new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addMapping("OpenChatSlash", new KeyTrigger(KeyInput.KEY_SLASH));
        inputManager.addMapping("OpenChatT", new KeyTrigger(KeyInput.KEY_T));

        inputManager.addMapping("OptResume", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("OptSens", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("OptGraph", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("OptJoinRequest", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("VolUp", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("VolDown", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping("OptMainMenu", new KeyTrigger(KeyInput.KEY_TAB));
        inputManager.addMapping("OptQuit", new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping("MultiplayerToggle", new KeyTrigger(KeyInput.KEY_M), new KeyTrigger(KeyInput.KEY_TAB));

        inputManager.addMapping("MenuUp", new KeyTrigger(KeyInput.KEY_UP), new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MenuDown", new KeyTrigger(KeyInput.KEY_DOWN), new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("MenuAction", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addMapping("KeyYes", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("KeyNo", new KeyTrigger(KeyInput.KEY_N));

        inputManager.addMapping("ShiftLeft", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("ShiftRight", new KeyTrigger(KeyInput.KEY_RSHIFT));

        inputManager.addMapping("KeyBackspace", new KeyTrigger(KeyInput.KEY_BACK));
        inputManager.addMapping("KeyEnter", new KeyTrigger(KeyInput.KEY_RETURN));

        int[] azertyKeys = {
            KeyInput.KEY_A, KeyInput.KEY_B, KeyInput.KEY_C, KeyInput.KEY_D, KeyInput.KEY_E,
            KeyInput.KEY_F, KeyInput.KEY_G, KeyInput.KEY_H, KeyInput.KEY_I, KeyInput.KEY_J,
            KeyInput.KEY_K, KeyInput.KEY_L, KeyInput.KEY_M, KeyInput.KEY_N, KeyInput.KEY_O,
            KeyInput.KEY_P, KeyInput.KEY_Q, KeyInput.KEY_R, KeyInput.KEY_S, KeyInput.KEY_T,
            KeyInput.KEY_U, KeyInput.KEY_V, KeyInput.KEY_W, KeyInput.KEY_X, KeyInput.KEY_Y,
            KeyInput.KEY_Z,
            KeyInput.KEY_1, KeyInput.KEY_2, KeyInput.KEY_3, KeyInput.KEY_4, KeyInput.KEY_5,
            KeyInput.KEY_6, KeyInput.KEY_7, KeyInput.KEY_8, KeyInput.KEY_9, KeyInput.KEY_0,
            KeyInput.KEY_SPACE, KeyInput.KEY_MINUS, KeyInput.KEY_SLASH
        };

        for (int key : azertyKeys) {
            inputManager.addMapping("TypeKey_" + key, new KeyTrigger(key));
            inputManager.addListener(this, "TypeKey_" + key);
        }

        inputManager.addListener(this, "Forward", "Backward", "Left", "Right", "Interact", "Attack", "ToggleFullscreen", "ResetGame", "TogglePause", "OpenChatSlash", "OpenChatT", "OptResume", "OptSens", "OptGraph", "OptJoinRequest", "VolUp", "VolDown", "OptMainMenu", "OptQuit", "MultiplayerToggle", "MenuUp", "MenuDown", "MenuAction", "KeyYes", "KeyNo", "ShiftLeft", "ShiftRight", "KeyBackspace", "KeyEnter");
    }

    private char getAzertyCharForCode(int keyId) {
        char c = 0;
        switch (keyId) {
            case KeyInput.KEY_Q: c = 'a'; break;
            case KeyInput.KEY_W: c = 'z'; break;
            case KeyInput.KEY_E: c = 'e'; break;
            case KeyInput.KEY_R: c = 'r'; break;
            case KeyInput.KEY_T: c = 't'; break;
            case KeyInput.KEY_Y: c = 'y'; break;
            case KeyInput.KEY_U: c = 'u'; break;
            case KeyInput.KEY_I: c = 'i'; break;
            case KeyInput.KEY_O: c = 'o'; break;
            case KeyInput.KEY_P: c = 'p'; break;

            case KeyInput.KEY_A: c = 'q'; break;
            case KeyInput.KEY_S: c = 's'; break;
            case KeyInput.KEY_D: c = 'd'; break;
            case KeyInput.KEY_F: c = 'f'; break;
            case KeyInput.KEY_G: c = 'g'; break;
            case KeyInput.KEY_H: c = 'h'; break;
            case KeyInput.KEY_J: c = 'j'; break;
            case KeyInput.KEY_K: c = 'k'; break;
            case KeyInput.KEY_L: c = 'l'; break;
            case KeyInput.KEY_SEMICOLON:
            case KeyInput.KEY_COLON:
            case KeyInput.KEY_M: c = 'm'; break;

            case KeyInput.KEY_Z: c = 'w'; break;
            case KeyInput.KEY_X: c = 'x'; break;
            case KeyInput.KEY_C: c = 'c'; break;
            case KeyInput.KEY_V: c = 'v'; break;
            case KeyInput.KEY_B: c = 'b'; break;
            case KeyInput.KEY_N: c = 'n'; break;
            case KeyInput.KEY_COMMA: c = ','; break;
            case KeyInput.KEY_SLASH: c = '/'; break;

            case KeyInput.KEY_1: c = '1'; break;
            case KeyInput.KEY_2: c = '2'; break;
            case KeyInput.KEY_3: c = '3'; break;
            case KeyInput.KEY_4: c = '4'; break;
            case KeyInput.KEY_5: c = '5'; break;
            case KeyInput.KEY_6: c = '6'; break;
            case KeyInput.KEY_7: c = '7'; break;
            case KeyInput.KEY_8: c = '8'; break;
            case KeyInput.KEY_9: c = '9'; break;
            case KeyInput.KEY_0: c = '0'; break;

            case KeyInput.KEY_SPACE: c = ' '; break;
            case KeyInput.KEY_MINUS: c = '-'; break;
            default: c = 0; break;
        }
        if (shiftPressed && c != 0) {
            c = Character.toUpperCase(c);
        }
        return c;
    }

    private void initChatDialog(BitmapFont font) {
        Quad bgQuad = new Quad(600f, 50f);
        Geometry dialogBg = new Geometry("ChatBg", bgQuad);
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.15f, 0.95f));
        dialogBg.setMaterial(bgMat);
        dialogBg.setLocalTranslation(20f, 40f, 10f);
        chatNode.attachChild(dialogBg);

        chatText = new BitmapText(font);
        chatText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        chatText.setColor(ColorRGBA.White);
        chatText.setLocalTranslation(30f, 70f, 11f);
        chatNode.attachChild(chatText);

        chatNode.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(chatNode);
    }

    private void updateChatDisplay() {
        if (chatText != null) {
            chatText.setText("Chat / Cmd: " + chatInput.toString() + "_");
        }
    }

    private void initEpilepsyWarningScreen(BitmapFont font) {
        BitmapText devText = new BitmapText(font);
        devText.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        devText.setColor(ColorRGBA.Cyan);
        devText.setText("DEV BY");
        devText.setLocalTranslation(cam.getWidth() / 2f - devText.getLineWidth() / 2f, cam.getHeight() / 2f + 250f, 0);
        warningNode.attachChild(devText);

        float logoWidth = 750f;
        float logoHeight = 250f;
        Quad logoQuad = new Quad(logoWidth, logoHeight);
        Geometry logoGeo = new Geometry("StudioLogo", logoQuad);
        Material logoMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture logoTex = assetManager.loadTexture("Textures/oblivionstudio.png");
            logoMat.setTexture("ColorMap", logoTex);
            logoMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        } catch (Exception e) {
            logoMat.setColor("Color", ColorRGBA.Magenta);
        }
        logoGeo.setMaterial(logoMat);
        logoGeo.setLocalTranslation(cam.getWidth() / 2f - logoWidth / 2f, cam.getHeight() / 2f - 30f, 0);
        warningNode.attachChild(logoGeo);

        BitmapText warningText = new BitmapText(font);
        warningText.setSize(font.getCharSet().getRenderedSize() * 1.1f);
        warningText.setColor(ColorRGBA.White);
        warningText.setText("EPILEPSY WARNING\n" +
                "A small percentage of individuals may experience epileptic seizures\n" +
                "when exposed to certain light patterns or flashing lights.\n\n" +
                "Press SPACE, ENTER or CLICK to continue.");
        warningText.setLocalTranslation(cam.getWidth() / 2f - warningText.getLineWidth() / 2f, cam.getHeight() / 2f - 180f, 0);
        warningNode.attachChild(warningText);
    }

    private void initMainMenuScreen(BitmapFont font) {
        Quad bgQuad = new Quad(cam.getWidth(), cam.getHeight());
        Geometry bgGeo = new Geometry("MenuBackground", bgQuad);
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture bgTex = assetManager.loadTexture("Textures/menu_bg.png");
            bgMat.setTexture("ColorMap", bgTex);
        } catch (Exception e) {
            bgMat.setColor("Color", ColorRGBA.DarkGray);
        }
        bgGeo.setMaterial(bgMat);
        bgGeo.setLocalTranslation(0, 0, -1f);
        menuNode.attachChild(bgGeo);

        float titleWidth = 850f;
        float titleHeight = 160f;
        Quad titleQuad = new Quad(titleWidth, titleHeight);
        Geometry titleGeo = new Geometry("GameTitleLogo", titleQuad);
        Material titleMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture titleTex = assetManager.loadTexture("Textures/familliar_places.png");
            titleMat.setTexture("ColorMap", titleTex);
            titleMat.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        } catch (Exception e) {
            titleMat.setColor("Color", ColorRGBA.Cyan);
        }
        titleGeo.setMaterial(titleMat);
        titleGeo.setLocalTranslation(cam.getWidth() / 2f - titleWidth / 2f, cam.getHeight() - 180f, 0);
        menuNode.attachChild(titleGeo);

        BitmapText subTitleText = new BitmapText(font);
        subTitleText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        subTitleText.setColor(ColorRGBA.White);
        subTitleText.setText("Dev by Oblivion | Profile: " + playerName);
        subTitleText.setLocalTranslation(cam.getWidth() / 2f - subTitleText.getLineWidth() / 2f, cam.getHeight() - 240f, 0);
        menuNode.attachChild(subTitleText);

        menuOpt1 = new BitmapText(font);
        menuOpt1.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        menuNode.attachChild(menuOpt1);

        menuOpt2 = new BitmapText(font);
        menuOpt2.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        menuNode.attachChild(menuOpt2);

        menuOpt3 = new BitmapText(font);
        menuOpt3.setSize(font.getCharSet().getRenderedSize() * 1.8f);
        menuNode.attachChild(menuOpt3);

        updateMainMenuDisplay();
    }

    private void updateMainMenuDisplay() {
        if (menuOpt1 == null || menuOpt2 == null || menuOpt3 == null) return;

        String opt1Text = (selectedMenuIndex == 0) ? "> [ SOLO PLAY ] <" : "  SOLO PLAY  ";
        String opt2Text = (selectedMenuIndex == 1) ? "> [ MULTIPLAYER LOBBY ] <" : "  MULTIPLAYER LOBBY  ";
        String opt3Text = (selectedMenuIndex == 2) ? "> [ SETTINGS ] <" : "  SETTINGS  ";

        menuOpt1.setText(opt1Text);
        menuOpt1.setColor(selectedMenuIndex == 0 ? ColorRGBA.Green : ColorRGBA.LightGray);
        menuOpt1.setLocalTranslation(cam.getWidth() / 2f - menuOpt1.getLineWidth() / 2f, cam.getHeight() / 2f + 60f, 0);

        menuOpt2.setText(opt2Text);
        menuOpt2.setColor(selectedMenuIndex == 1 ? ColorRGBA.Yellow : ColorRGBA.LightGray);
        menuOpt2.setLocalTranslation(cam.getWidth() / 2f - menuOpt2.getLineWidth() / 2f, cam.getHeight() / 2f, 0);

        menuOpt3.setText(opt3Text);
        menuOpt3.setColor(selectedMenuIndex == 2 ? ColorRGBA.Cyan : ColorRGBA.LightGray);
        menuOpt3.setLocalTranslation(cam.getWidth() / 2f - menuOpt3.getLineWidth() / 2f, cam.getHeight() / 2f - 60f, 0);
    }

    private void initLobbyScreen(BitmapFont font) {
        BitmapText lobbyTitle = new BitmapText(font);
        lobbyTitle.setSize(font.getCharSet().getRenderedSize() * 2f);
        lobbyTitle.setColor(ColorRGBA.Yellow);
        lobbyTitle.setText("MULTIPLAYER JOIN REQUEST LOBBY");
        lobbyTitle.setLocalTranslation(cam.getWidth() / 2f - lobbyTitle.getLineWidth() / 2f, cam.getHeight() - 180f, 0);
        lobbyNode.attachChild(lobbyTitle);

        lobbyStatusText = new BitmapText(font);
        lobbyStatusText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        lobbyStatusText.setColor(ColorRGBA.Green);
        lobbyStatusText.setText("Status: Ready to request joining a host session.");
        lobbyStatusText.setLocalTranslation(cam.getWidth() / 2f - lobbyStatusText.getLineWidth() / 2f, cam.getHeight() - 240f, 0);
        lobbyNode.attachChild(lobbyStatusText);

        Quad boxBg = new Quad(500f, 80f);
        Geometry boxGeo = new Geometry("JoinBoxBg", boxBg);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.15f, 0.9f));
        boxGeo.setMaterial(boxMat);
        boxGeo.setLocalTranslation(cam.getWidth() / 2f - 250f, cam.getHeight() / 2f - 40f, 0);
        lobbyNode.attachChild(boxGeo);

        joinBoxText = new BitmapText(font);
        joinBoxText.setSize(font.getCharSet().getRenderedSize() * 1.3f);
        joinBoxText.setColor(ColorRGBA.White);
        updateJoinBoxDisplay();
        joinBoxText.setLocalTranslation(cam.getWidth() / 2f - 230f, cam.getHeight() / 2f + 15f, 1f);
        lobbyNode.attachChild(joinBoxText);

        BitmapText lobbyDesc = new BitmapText(font);
        lobbyDesc.setSize(font.getCharSet().getRenderedSize() * 1.1f);
        lobbyDesc.setColor(ColorRGBA.LightGray);
        lobbyDesc.setText("Type host's pseudo inside the box and press [ENTER] to send join request.\n" +
                "Press [TAB] to return to Main Menu.");
        lobbyDesc.setLocalTranslation(cam.getWidth() / 2f - lobbyDesc.getLineWidth() / 2f, cam.getHeight() / 2f - 100f, 0);
        lobbyNode.attachChild(lobbyDesc);
    }

    private void initIngameInviteDialog(BitmapFont font) {
        Quad bgQuad = new Quad(500f, 120f);
        Geometry dialogBg = new Geometry("IngameInviteBg", bgQuad);
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.15f, 0.95f));
        dialogBg.setMaterial(bgMat);
        dialogBg.setLocalTranslation(cam.getWidth() / 2f - 250f, cam.getHeight() / 2f - 60f, 10f);
        ingameInviteNode.attachChild(dialogBg);

        ingameInviteText = new BitmapText(font);
        ingameInviteText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
        ingameInviteText.setColor(ColorRGBA.Yellow);
        ingameInviteText.setLocalTranslation(cam.getWidth() / 2f - 230f, cam.getHeight() / 2f + 30f, 11f);
        ingameInviteNode.attachChild(ingameInviteText);

        ingameInviteNode.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(ingameInviteNode);
    }

    private void updateIngameInviteDisplay() {
        if (ingameInviteText != null) {
            String displayVal = targetHostInput.length() == 0 ? "_" : targetHostInput.toString() + "_";
            ingameInviteText.setText("SEND JOIN REQUEST IN-GAME\n\nTarget Pseudo: " + displayVal + "\n[ENTER] Send  |  [F3] Cancel");
        }
    }

    private void initInvitationDialog(BitmapFont font) {
        Quad bgQuad = new Quad(600f, 160f);
        Geometry dialogBg = new Geometry("DialogBg", bgQuad);
        Material bgMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.1f, 0.95f));
        dialogBg.setMaterial(bgMat);
        dialogBg.setLocalTranslation(cam.getWidth() / 2f - 300f, cam.getHeight() / 2f - 80f, 5f);
        invitationDialogNode.attachChild(dialogBg);

        invitationText = new BitmapText(font);
        invitationText.setSize(font.getCharSet().getRenderedSize() * 1.3f);
        invitationText.setColor(ColorRGBA.White);
        invitationText.setLocalTranslation(cam.getWidth() / 2f - 280f, cam.getHeight() / 2f + 50f, 6f);
        invitationDialogNode.attachChild(invitationText);

        invitationDialogNode.setCullHint(Node.CullHint.Always);
        guiNode.attachChild(invitationDialogNode);
    }

    private void showInvitationDialog(String senderName) {
        incomingSenderName = senderName;
        hasActiveInvite = true;
        if (invitationText != null) {
            invitationText.setText("INVITATION RECEIVED!\n\n" +
                    "[" + senderName + "] wants to join your session.\n\n" +
                    "Press [Y] Yes  |  [N] No");
        }
        invitationDialogNode.setCullHint(Node.CullHint.Inherit);
    }

    private void updateJoinBoxDisplay() {
        if (joinBoxText != null) {
            String displayVal = targetHostInput.length() == 0 ? "_ (Type host pseudo...)" : targetHostInput.toString() + "_";
            joinBoxText.setText("Target Host: " + displayVal);
        }
        if (ingameMultiHud != null) {
            String val = targetHostInput.length() == 0 ? "None" : targetHostInput.toString();
            ingameMultiHud.setText("Multiplayer Target Host: " + val + " (Press F3 -> 4 to invite)");
        }
    }

    private void updateInventoryUI() {
        if (inventoryText == null) return;
        StringBuilder sb = new StringBuilder("=== INVENTORY ===\n");
        if (playerInventory.isEmpty()) {
            sb.append("- (Empty)");
        } else {
            for (String item : playerInventory) {
                sb.append("- ").append(item).append("\n");
            }
        }
        inventoryText.setText(sb.toString());
    }

    private void applyVolumeToAudios() {
        float appliedVol = masterVolume * 5.0f;
        if (hubAudio != null) hubAudio.setVolume(appliedVol);
        if (cityAudio != null) cityAudio.setVolume(appliedVol);
    }

    private void initAudioAndStartGameplay() {
        try {
            hubAudio = new AudioNode(assetManager, "Sounds/Hubsound.ogg", AudioData.DataType.Buffer);
            hubAudio.setLooping(true);
            hubAudio.setPositional(false);
            rootNode.attachChild(hubAudio);
            hubAudio.play();
        } catch (Exception e) {
            System.err.println("AUDIO ERROR: " + e.getMessage());
        }

        try {
            cityAudio = new AudioNode(assetManager, "Sounds/citysound.ogg", AudioData.DataType.Buffer);
            cityAudio.setLooping(true);
            cityAudio.setPositional(false);
            rootNode.attachChild(cityAudio);
        } catch (Exception e) {
            System.err.println("CITY AUDIO ERROR: " + e.getMessage());
        }

        applyVolumeToAudios();
        resetGame();
    }

    private Material createMaterialForCurrentQuality() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        if (graphicsQuality.equals("Low")) {
            Texture procTex = createProceduralStoneTexture();
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.White);
            mat.setColor("Ambient", ColorRGBA.DarkGray);
            mat.setTexture("DiffuseMap", procTex);
        } else {
            try {
                Texture diffTex = assetManager.loadTexture("Textures/rock_wall_16_diff_4k.jpg");
                diffTex.setWrap(Texture.WrapMode.Repeat);
                mat.setTexture("DiffuseMap", diffTex);
            } catch (Exception e) {
                Texture procTex = createProceduralStoneTexture();
                mat.setBoolean("UseMaterialColors", true);
                mat.setColor("Diffuse", ColorRGBA.LightGray);
                mat.setTexture("DiffuseMap", procTex);
            }
        }

        mat.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Off);
        return mat;
    }

    private void updateWorldMaterials() {
        Material newMat = createMaterialForCurrentQuality();
        for (Node chunk : loadedChunks.values()) {
            applyMaterialRecursive(chunk, newMat);
        }
        if (inDungeon) {
            applyMaterialRecursive(dungeonRoomNode, newMat);
        }
    }

    private void applyMaterialRecursive(Spatial spatial, Material mat) {
        if (spatial instanceof Geometry) {
            Geometry geom = (Geometry) spatial;
            if (!geom.getName().equals("Torch") && !geom.getName().equals("PlayerBody") && !geom.getName().equals("SpiritOrb") && !geom.getName().equals("SwordPickup")) {
                geom.setMaterial(mat);
            }
        } else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                applyMaterialRecursive(child, mat);
            }
        }
    }

    private void resetGame() {
        dungeonNode.detachAllChildren();
        dungeonRoomNode.detachAllChildren();
        cityNode.detachAllChildren();
        dungeonRoomNode.setCullHint(Node.CullHint.Never);
        cityNode.setCullHint(Node.CullHint.Never);
        dungeonNode.setCullHint(Node.CullHint.Inherit);

        for (List<PointLight> lights : chunkLights.values()) {
            for (PointLight light : lights) {
                rootNode.removeLight(light);
            }
        }
        chunkLights.clear();
        loadedChunks.clear();
        globalWalls.clear();

        worldSeed = new Random().nextLong();

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.01f));
        rootNode.addLight(ambient);

        if (keyGeo != null) rootNode.detachChild(keyGeo);
        if (exitGeo != null) rootNode.detachChild(exitGeo);
        if (swordPickupGeo != null) rootNode.detachChild(swordPickupGeo);
        if (giantMonsterGeo != null && giantMonsterGeo.getParent() != null) rootNode.detachChild(giantMonsterGeo.getParent());

        hasKey = false;
        hasSword = false;
        hasWon = false;
        isGameOver = false;
        isNoclip = false;
        inDungeon = false;
        inCity = false;
        nearPortal = false;
        cityTimer = 0f;

        playerInventory.clear();
        updateInventoryUI();

        deathScreenNode.setCullHint(Node.CullHint.Always);
        winScreenNode.setCullHint(Node.CullHint.Always);
        interactText.setCullHint(BitmapText.CullHint.Always);
        swordGeo.setCullHint(Geometry.CullHint.Always);

        playerPos.set(2f, 1f, 2f);
        cam.setLocation(playerPos);
        playerAvatarNode.setLocalTranslation(playerPos.clone().subtract(0, 1f, 0));

        updateChunksAroundPlayer();
        spawnGameElements();

        viewPort.setBackgroundColor(ColorRGBA.Black);

        if (hubAudio != null) {
            if (cityAudio != null) cityAudio.stop();
            hubAudio.play();
        }
        applyVolumeToAudios();
        updateTextUI();
    }

    private void updateChunksAroundPlayer() {
        if (inDungeon || inCity) return;

        int pChunkX = Math.floorDiv((int) playerPos.x, chunkSize * 2);
        int pChunkZ = Math.floorDiv((int) playerPos.z, chunkSize * 2);

        Set<String> activeKeys = new HashSet<>();

        for (int x = pChunkX - renderDistance; x <= pChunkX + renderDistance; x++) {
            for (int z = pChunkZ - renderDistance; z <= pChunkZ + renderDistance; z++) {
                String chunkKey = x + "," + z;
                activeKeys.add(chunkKey);

                if (!loadedChunks.containsKey(chunkKey)) {
                    Node chunkNode = generateChunk(x, z);
                    loadedChunks.put(chunkKey, chunkNode);
                    dungeonNode.attachChild(chunkNode);
                }
            }
        }

        List<String> keysToRemove = new ArrayList<>();
        for (String key : loadedChunks.keySet()) {
            if (!activeKeys.contains(key)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            Node chunkNode = loadedChunks.remove(key);
            if (chunkNode != null) {
                dungeonNode.detachChild(chunkNode);
            }

            List<PointLight> lights = chunkLights.remove(key);
            if (lights != null) {
                for (PointLight light : lights) {
                    rootNode.removeLight(light);
                }
            }
        }
    }

    private Node generateChunk(int cx, int cz) {
        Node chunkNode = new Node("Chunk_" + cx + "_" + cz);
        Node wallsBatchNode = new Node("WallsBatch");

        Material currentDungeonMat = createMaterialForCurrentQuality();
        Box wallMesh = new Box(1f, 1f, 1f);

        Random chunkRand = new Random(Objects.hash(cx, cz, worldSeed));
        boolean[][] wallGrid = new boolean[chunkSize][chunkSize];

        for (int i = 0; i < 5; i++) {
            int wx = chunkRand.nextInt(chunkSize - 4) + 2;
            int wz = chunkRand.nextInt(chunkSize - 4) + 2;
            int length = 4 + chunkRand.nextInt(6);
            boolean horizontal = chunkRand.nextBoolean();

            for (int l = 0; l < length; l++) {
                int px = wx + (horizontal ? l : 0);
                int pz = wz + (horizontal ? 0 : l);
                if (px < chunkSize && pz < chunkSize) {
                    wallGrid[px][pz] = true;
                }
            }
        }

        List<PointLight> lightsList = new ArrayList<>();

        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                int worldX = (cx * chunkSize) + x;
                int worldZ = (cz * chunkSize) + z;

                if (Math.abs(worldX) <= 2 && Math.abs(worldZ) <= 2) continue;

                if (wallGrid[x][z] || chunkRand.nextFloat() < 0.15f) {
                    Vector3f wallPos = new Vector3f(worldX * 2f, 1f, worldZ * 2f);
                    Geometry wall = new Geometry("Wall", wallMesh);
                    wall.setMaterial(currentDungeonMat);
                    wall.setLocalTranslation(wallPos);
                    wallsBatchNode.attachChild(wall);

                    globalWalls.add(worldX + "," + worldZ);

                    // --- TORCHES 3D AVEC TEXTURES D'ORIGINE ---
                    if (chunkRand.nextFloat() < 0.2f) {
                        Vector3f torchPos = wallPos.clone().add(0, 0f, 1.15f);

                        try {
                            Spatial torchModel = assetManager.loadModel("Models/torch.glb");
                            torchModel.setName("Torch");
                            torchModel.setLocalTranslation(torchPos);
                            torchModel.setLocalRotation(new Quaternion());
                            
                            chunkNode.attachChild(torchModel);
                        } catch (Exception e) {
                            Box fallbackTorchBox = new Box(0.05f, 0.3f, 0.05f);
                            Material fallbackTorchMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                            fallbackTorchMat.setColor("Color", ColorRGBA.Orange);
                            Geometry torch = new Geometry("Torch", fallbackTorchBox);
                            torch.setMaterial(fallbackTorchMat);
                            torch.setLocalTranslation(torchPos);
                            chunkNode.attachChild(torch);
                        }

                        PointLight light = new PointLight();
                        light.setColor(new ColorRGBA(2.5f, 1.3f, 0.5f, 1.0f));
                        light.setRadius(12f);
                        light.setPosition(torchPos);
                        rootNode.addLight(light);
                        lightsList.add(light);
                    }
                }
            }
        }

        if (wallsBatchNode.getQuantity() > 0) {
            GeometryBatchFactory.optimize(wallsBatchNode);
            chunkNode.attachChild(wallsBatchNode);
        }

        chunkLights.put(cx + "," + cz, lightsList);

        Box floorMesh = new Box(chunkSize, 0.1f, chunkSize);
        Geometry floor = new Geometry("Floor", floorMesh);
        floor.setMaterial(currentDungeonMat);
        floor.setLocalTranslation((cx * chunkSize + chunkSize / 2f) * 2f, 0f, (cz * chunkSize + chunkSize / 2f) * 2f);
        chunkNode.attachChild(floor);

        Geometry ceiling = new Geometry("Ceiling", floorMesh);
        ceiling.setMaterial(currentDungeonMat);
        ceiling.setLocalTranslation((cx * chunkSize + chunkSize / 2f) * 2f, 2f, (cz * chunkSize + chunkSize / 2f) * 2f);
        chunkNode.attachChild(ceiling);

        return chunkNode;
    }

    private void spawnGiantCityMonster() {
        Sphere monsterMesh = new Sphere(32, 32, 3.5f);
        giantMonsterGeo = new Geometry("GiantMonster", monsterMesh);

        Material monsterMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture monsterTex = assetManager.loadTexture("Textures/monster.png");
            monsterMat.setTexture("ColorMap", monsterTex);
        } catch (Exception e) {
            monsterMat.setColor("Color", ColorRGBA.Red);
        }

        giantMonsterGeo.setMaterial(monsterMat);
        giantMonsterGeo.setCullHint(Spatial.CullHint.Never);

        Node monsterNode = new Node("GiantMonsterNode");
        monsterNode.attachChild(giantMonsterGeo);
        monsterNode.setCullHint(Spatial.CullHint.Never);

        giantMonsterPos = new Vector3f(50f, 3.5f, 50f);
        monsterNode.setLocalTranslation(giantMonsterPos);
        cityNode.attachChild(monsterNode);
    }

    private void fallIntoCity() {
        inDungeon = false;
        inCity = true;
        cityTimer = 0f;

        playerInventory.clear();
        playerInventory.add("City Survival Kit");
        updateInventoryUI();

        for (GoblinData goblin : goblins) {
            goblin.node.setCullHint(Spatial.CullHint.Always);
        }

        dungeonRoomNode.setCullHint(Node.CullHint.Always);
        dungeonNode.setCullHint(Node.CullHint.Always);
        cityNode.detachAllChildren();
        cityNode.setCullHint(Node.CullHint.Never);

        viewPort.setBackgroundColor(new ColorRGBA(0.28f, 0.28f, 0.35f, 1f));

        if (hubAudio != null) hubAudio.stop();
        if (cityAudio != null) cityAudio.play();
        applyVolumeToAudios();

        Material roadMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        roadMat.setColor("Color", new ColorRGBA(0.35f, 0.35f, 0.35f, 1f));

        Box cityFloorMesh = new Box(100f, 0.1f, 100f);
        Geometry cityFloor = new Geometry("CityFloor", cityFloorMesh);
        cityFloor.setMaterial(roadMat);
        cityFloor.setLocalTranslation(0f, 0f, 0f);
        cityNode.attachChild(cityFloor);

        Material buildingWhiteMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        buildingWhiteMat.setColor("Color", ColorRGBA.White);

        Material bandMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bandMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));

        Material shopBrownMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        shopBrownMat.setColor("Color", new ColorRGBA(0.55f, 0.27f, 0.07f, 1f));

        Material shopShadowMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        shopShadowMat.setColor("Color", new ColorRGBA(0.15f, 0.1f, 0.05f, 1f));

        List<int[]> shopCoords = new ArrayList<>();
        for (int x = -60; x <= 60; x += 30) {
            for (int z = -60; z <= 60; z += 30) {
                if (Math.abs(x) > 10 || Math.abs(z) > 10) {
                    shopCoords.add(new int[]{x, z});
                }
            }
        }
        Random cityRand = new Random(worldSeed);
        int[] chosenShop = shopCoords.get(cityRand.nextInt(shopCoords.size()));
        int targetShopX = chosenShop[0];
        int targetShopZ = chosenShop[1];

        for (int[] coord : shopCoords) {
            int x = coord[0];
            int z = coord[1];
            Node buildingNode = new Node("Building");

            Box shopFloorMesh = new Box(3.8f, 0.1f, 3.8f);
            Geometry sFloor = new Geometry("ShopFloor", shopFloorMesh);
            sFloor.setMaterial(shopShadowMat);
            sFloor.setLocalTranslation(x, 0.1f, z);
            buildingNode.attachChild(sFloor);

            Box shopRoofMesh = new Box(4f, 0.1f, 4f);
            Geometry sRoof = new Geometry("ShopRoof", shopRoofMesh);
            sRoof.setMaterial(shopBrownMat);
            sRoof.setLocalTranslation(x, 6.0f, z);
            buildingNode.attachChild(sRoof);

            Box backWall = new Box(4f, 3f, 0.2f);
            Geometry bWallGeo = new Geometry("BackWall", backWall);
            bWallGeo.setMaterial(shopShadowMat);
            bWallGeo.setLocalTranslation(x, 3f, z + 3.8f);
            buildingNode.attachChild(bWallGeo);

            Box leftWall = new Box(0.2f, 3f, 3.8f);
            Geometry lWallGeo = new Geometry("LeftWall", leftWall);
            lWallGeo.setMaterial(shopBrownMat);
            lWallGeo.setLocalTranslation(x - 3.8f, 3f, z);
            buildingNode.attachChild(lWallGeo);

            Box rightWall = new Box(0.2f, 3f, 3.8f);
            Geometry rWallGeo = new Geometry("RightWall", rightWall);
            rWallGeo.setMaterial(shopBrownMat);
            rWallGeo.setLocalTranslation(x + 3.8f, 3f, z);
            buildingNode.attachChild(rWallGeo);

            Box frontLeftWall = new Box(1.4f, 3f, 0.2f);
            Geometry flWallGeo = new Geometry("FrontLeftWall", frontLeftWall);
            flWallGeo.setMaterial(shopBrownMat);
            flWallGeo.setLocalTranslation(x - 2.6f, 3f, z - 3.8f);
            buildingNode.attachChild(flWallGeo);

            Box frontRightWall = new Box(1.4f, 3f, 0.2f);
            Geometry frWallGeo = new Geometry("FrontRightWall", frontRightWall);
            frWallGeo.setMaterial(shopBrownMat);
            frWallGeo.setLocalTranslation(x + 2.6f, 3f, z - 3.8f);
            buildingNode.attachChild(frWallGeo);

            Box mainBox = new Box(4f, 22f, 4f);
            Geometry bGeo = new Geometry("Body", mainBox);
            bGeo.setMaterial(buildingWhiteMat);
            bGeo.setLocalTranslation(x, 28f, z);
            buildingNode.attachChild(bGeo);

            for (float y = 8f; y < 50f; y += 4f) {
                Box bandBox = new Box(4.1f, 0.8f, 4.1f);
                Geometry bandGeo = new Geometry("Band", bandBox);
                bandGeo.setMaterial(bandMat);
                bandGeo.setLocalTranslation(x, y, z);
                buildingNode.attachChild(bandGeo);
            }

            if (x == targetShopX && z == targetShopZ) {
                Sphere shopKeyMesh = new Sphere(8, 8, 0.4f);
                Geometry shopKeyGeo = new Geometry("CityKey", shopKeyMesh);
                Material skMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                skMat.setColor("Color", ColorRGBA.Yellow);
                shopKeyGeo.setMaterial(skMat);
                shopKeyGeo.setLocalTranslation(x, 1.5f, z);
                cityNode.attachChild(shopKeyGeo);
                keyGeo = shopKeyGeo;
                keyPos = new Vector3f(x, 1.5f, z);
                hasKey = false;

                Box cityExitMesh = new Box(0.6f, 0.8f, 0.05f);
                Geometry cityExitGeo = new Geometry("CityExitDoor", cityExitMesh);
                Material cityExitMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                cityExitMat.setColor("Color", ColorRGBA.Green);
                cityExitGeo.setMaterial(cityExitMat);
                cityExitGeo.setLocalTranslation(x, 1.5f, z - 2f);
                cityNode.attachChild(cityExitGeo);
                exitGeo = cityExitGeo;
                exitPos = new Vector3f(x, 1.5f, z - 2f);
            }

            buildingNode.setCullHint(Spatial.CullHint.Never);
            cityNode.attachChild(buildingNode);
        }

        cityNode.setCullHint(Spatial.CullHint.Never);

        playerPos.set(0f, 60f, 0f);
        cam.setLocation(playerPos);

        spawnGiantCityMonster();
        updateTextUI();
    }

    private void enterDungeonInstance() {
        inDungeon = true;
        inCity = false;
        dungeonNode.setCullHint(Node.CullHint.Always);
        dungeonRoomNode.detachAllChildren();

        playerInventory.clear();
        playerInventory.add("Dungeon Gear");
        if (hasSword) playerInventory.add("Iron Sword");
        updateInventoryUI();

        viewPort.setBackgroundColor(ColorRGBA.Black);

        if (cityAudio != null) cityAudio.stop();
        if (hubAudio != null) hubAudio.play();
        applyVolumeToAudios();

        Material currentMat = createMaterialForCurrentQuality();
        Box wallMesh = new Box(1f, 1f, 1f);

        int dungeonSize = 12;
        for (int x = -dungeonSize; x <= dungeonSize; x++) {
            for (int z = -dungeonSize; z <= dungeonSize; z++) {
                if (x == -dungeonSize || x == dungeonSize || z == -dungeonSize || z == dungeonSize) {
                    Geometry wall = new Geometry("DungeonWall", wallMesh);
                    wall.setMaterial(currentMat);
                    wall.setLocalTranslation(x * 2f, 1f, z * 2f);
                    dungeonRoomNode.attachChild(wall);
                }
            }
        }

        Box roomFloorMesh = new Box(dungeonSize * 2f, 0.1f, dungeonSize * 2f);
        Geometry roomFloor = new Geometry("DungeonFloor", roomFloorMesh);
        roomFloor.setMaterial(currentMat);
        roomFloor.setLocalTranslation(0f, 0f, 0f);
        dungeonRoomNode.attachChild(roomFloor);

        Geometry roomCeiling = new Geometry("Ceiling", roomFloorMesh);
        roomCeiling.setMaterial(currentMat);
        roomCeiling.setLocalTranslation(0f, 2f, 0f);
        dungeonRoomNode.attachChild(roomCeiling);

        dungeonRoomNode.setCullHint(Node.CullHint.Never);

        playerPos.set(0f, 1f, 0f);
        cam.setLocation(playerPos);

        for (GoblinData goblin : goblins) {
            goblin.node.setCullHint(Spatial.CullHint.Inherit);
            rootNode.attachChild(goblin.node);
        }

        Vector3f[] goblinSpawns = {
            new Vector3f(-16f, 0.5f, -16f),
            new Vector3f(16f, 0.5f, -16f),
            new Vector3f(-16f, 0.5f, 16f),
            new Vector3f(16f, 0.5f, 16f)
        };

        if (goblins.isEmpty()) {
            for (int i = 0; i < goblinSpawns.length; i++) {
                Node goblinNode = new Node("GoblinNode_" + i);

                Spatial goblinModel;
                try {
                    goblinModel = assetManager.loadModel("Models/goblin_ghoul.glb");
                    goblinModel.scale(0.12f);
                } catch (Exception e) {
                    Quad goblinMesh = new Quad(0.5f, 0.5f);
                    Geometry goblinGeo = new Geometry("GoblinMesh", goblinMesh);
                    Material goblinMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    goblinMat.setColor("Color", ColorRGBA.Green);
                    goblinGeo.setMaterial(goblinMat);
                    goblinGeo.setLocalTranslation(-0.25f, -0.25f, 0f);
                    goblinModel = goblinGeo;
                }
                goblinNode.attachChild(goblinModel);

                Quad hbMesh = new Quad(0.8f, 0.12f);
                Geometry healthBar = new Geometry("HealthBar", hbMesh);
                Material hbMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                hbMat.setColor("Color", ColorRGBA.Green);
                healthBar.setMaterial(hbMat);
                healthBar.setLocalTranslation(-0.4f, 0.9f, 0f);
                goblinNode.attachChild(healthBar);

                goblinNode.setLocalTranslation(goblinSpawns[i]);
                rootNode.attachChild(goblinNode);

                GoblinData data = new GoblinData();
                data.node = goblinNode;
                data.hp = 100f;
                data.healthBar = healthBar;
                goblins.add(data);
            }
        }

        updateTextUI();
    }

    private void updateTextUI() {
        if (isGameOver) questText.setText("GAME OVER! (" + playerName + ")");
        else if (hasWon) questText.setText("VICTORY! (" + playerName + ") Escaped! Press [R] to replay.");
        else if (inCity) questText.setText("Objective: Find the key and portal inside one of the shops!");
        else if (!hasKey) questText.setText("Objective: Find the golden key further away!");
        else if (!hasSword) questText.setText("Objective: Pick up the sword easily near the portal!");
        else if (!inDungeon) questText.setText("Objective: Go to the portal and press [E]!");
        else questText.setText("Objective: Clear all Goblins using left click!");
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            updatePauseMenuText();
            pauseMenuText.setCullHint(BitmapText.CullHint.Inherit);
            flyCam.setEnabled(false);
            inputManager.setCursorVisible(true);
        } else {
            pauseMenuText.setCullHint(BitmapText.CullHint.Always);
            ingameInviteNode.setCullHint(Node.CullHint.Always);
            isIngameInviting = false;
            flyCam.setEnabled(true);
            inputManager.setCursorVisible(false);
            wPressed = sPressed = qPressed = dPressed = false;
        }
    }

    private void updatePauseMenuText() {
        int volPercent = (int)(masterVolume * 100f);
        pauseMenuText.setText(
                "=== OPTIONS MENU (" + playerName + ") ===\n\n" +
                        "[1] Resume\n" +
                        "[2] Mouse Sensitivity: " + sensitivity + "\n" +
                        "[3] Graphics: " + graphicsQuality + " (Press to change)\n" +
                        "[4] Send Join Request / Invite Player\n" +
                        "[5] Volume + (Current: " + volPercent + "%)\n" +
                        "[6] Volume -\n" +
                        "[TAB] Return to Main Menu\n" +
                        "[7] Quit Game\n\n" +
                        "Press F3 to close menu"
        );
    }

    private void spawnGameElements() {
        keyPos = new Vector3f(18f, 1f, 18f);
        Sphere keyMesh = new Sphere(8, 8, 0.3f);
        keyGeo = new Geometry("Key", keyMesh);
        Material keyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        keyMat.setColor("Color", ColorRGBA.Yellow);
        keyGeo.setMaterial(keyMat);
        keyGeo.setLocalTranslation(keyPos);
        rootNode.attachChild(keyGeo);

        exitPos = new Vector3f(4f, 1f, 4f);
        Box exitMesh = new Box(0.8f, 1f, 0.1f);
        exitGeo = new Geometry("ExitDoor", exitMesh);
        Material exitMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        exitMat.setColor("Color", ColorRGBA.Red);
        exitGeo.setMaterial(exitMat);
        exitGeo.setLocalTranslation(exitPos);
        rootNode.attachChild(exitGeo);

        swordPos = new Vector3f(4f, 1f, 5f);
        Box swordPickupMesh = new Box(0.2f, 0.6f, 0.05f);
        swordPickupGeo = new Geometry("SwordPickup", swordPickupMesh);
        Material swordPickupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        swordPickupMat.setColor("Color", ColorRGBA.Cyan);
        swordPickupGeo.setMaterial(swordPickupMat);
        swordPickupGeo.setLocalTranslation(swordPos);
        swordPickupGeo.setCullHint(Spatial.CullHint.Inherit);
        rootNode.attachChild(swordPickupGeo);
    }

    private boolean isWall(float x, float z) {
        if (isNoclip) return false;

        if (inDungeon) {
            float limit = 23.5f;
            return Math.abs(x) > limit || Math.abs(z) > limit;
        } else if (inCity) {
            for (int bx = -60; bx <= 60; bx += 30) {
                for (int bz = -60; bz <= 60; bz += 30) {
                    if (Math.abs(bx) > 10 || Math.abs(bz) > 10) {
                        float halfSize = 4.0f;
                        boolean insideX = (x >= bx - halfSize && x <= bx + halfSize);
                        boolean insideZ = (z >= bz - halfSize && z <= bz + halfSize);

                        if (insideX && insideZ) {
                            boolean isFrontEntrance = (Math.abs(x - bx) < 1.2f && Math.abs(z - (bz - halfSize)) < 0.6f);
                            if (isFrontEntrance) {
                                continue;
                            }
                            float wallThickness = 0.4f;
                            boolean hitBack = (Math.abs(z - (bz + halfSize)) < wallThickness);
                            boolean hitLeft = (Math.abs(x - (bx - halfSize)) < wallThickness);
                            boolean hitRight = (Math.abs(x - (bx + halfSize)) < wallThickness);
                            boolean hitFront = (Math.abs(z - (bz - halfSize)) < wallThickness && Math.abs(x - bx) >= 1.2f);

                            if (hitBack || hitLeft || hitRight || hitFront) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        } else {
            int gridX = Math.round(x / 2f);
            int gridZ = Math.round(z / 2f);
            return globalWalls.contains(gridX + "," + gridZ);
        }
    }

    private Texture createProceduralStoneTexture() {
        int width = 128;
        int height = 128;
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        Random noiseGen = new Random(1337);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int brickRow = y / 24;
                int offsetX = (brickRow % 2 == 0) ? 0 : 32;
                int localX = (x + offsetX) % 64;
                int localY = y % 24;

                boolean isMortar = (localX < 3 || localX > 61 || localY < 3 || localY > 21);

                int baseColor;
                if (isMortar) {
                    baseColor = 35;
                } else {
                    int stoneBase = 100 + (int)((x * 5 + y * 11) % 40);
                    baseColor = Math.min(255, Math.max(0, stoneBase + (int)((noiseGen.nextFloat() - 0.5f) * 35f)));
                }

                data.put((byte) baseColor);
                data.put((byte) baseColor);
                data.put((byte) baseColor);
                data.put((byte) 255);
            }
        }
        data.flip();
        Image img = new Image(Image.Format.RGBA8, width, height, data, ColorSpace.sRGB);
        return new Texture2D(img);
    }

    @Override
    public void stop() {
        try {
            String javaBin = System.getProperty("java.home") + "/bin/java";
            String classpath = System.getProperty("java.class.path");
            String className = "com.mygame.Launcher";

            ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
            builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.stop();
    }

    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("ShiftLeft") || name.equals("ShiftRight")) {
            shiftPressed = keyPressed;
            return;
        }

        if (isChatting && keyPressed) {
            if (name.equals("KeyBackspace") && chatInput.length() > 0) {
                chatInput.deleteCharAt(chatInput.length() - 1);
                updateChatDisplay();
                return;
            }
            if (name.equals("KeyEnter")) {
                isChatting = false;
                chatNode.setCullHint(Node.CullHint.Always);
                flyCam.setEnabled(true);
                inputManager.setCursorVisible(false);

                String cmd = chatInput.toString().trim();
                if (cmd.equalsIgnoreCase("/give key")) {
                    if (!hasKey) {
                        hasKey = true;
                        if (keyGeo != null) rootNode.detachChild(keyGeo);
                        if (!playerInventory.contains("Golden Key")) {
                            playerInventory.add("Golden Key");
                        }
                        updateInventoryUI();
                        updateTextUI();
                        questText.setText("Clé obtenue par commande !");
                        questText.setColor(ColorRGBA.Yellow);
                    }
                }
                chatInput.setLength(0);
                return;
            }
            if (name.startsWith("TypeKey_")) {
                try {
                    int keyId = Integer.parseInt(name.substring(8));
                    char c = getAzertyCharForCode(keyId);
                    if (c != 0 && chatInput.length() < 30) {
                        chatInput.append(c);
                        updateChatDisplay();
                    }
                } catch (Exception ignored) {}
                return;
            }
        }

        if ((name.equals("OpenChatSlash") || name.equals("OpenChatT")) && keyPressed && currentState == GameState.PLAYING && !isPaused) {
            if (!isChatting) {
                isChatting = true;
                chatInput.setLength(0);
                chatInput.append("/");
                updateChatDisplay();
                chatNode.setCullHint(Node.CullHint.Inherit);
                flyCam.setEnabled(false);
                inputManager.setCursorVisible(true);
                return;
            }
        }

        if (hasActiveInvite && keyPressed) {
            if (name.equals("KeyYes")) {
                hasActiveInvite = false;
                invitationDialogNode.setCullHint(Node.CullHint.Always);
                if (lobbyStatusText != null) {
                    lobbyStatusText.setText("Accepted [" + incomingSenderName + "]! Starting Multiplayer Session...");
                    lobbyStatusText.setColor(ColorRGBA.Green);
                }
                return;
            } else if (name.equals("KeyNo")) {
                hasActiveInvite = false;
                invitationDialogNode.setCullHint(Node.CullHint.Always);
                if (lobbyStatusText != null) {
                    lobbyStatusText.setText("Declined invitation from [" + incomingSenderName + "].");
                    lobbyStatusText.setColor(ColorRGBA.Red);
                }
                return;
            }
        }

        if (currentState == GameState.MAIN_MENU && keyPressed) {
            if (name.equals("MenuUp")) {
                selectedMenuIndex--;
                if (selectedMenuIndex < 0) selectedMenuIndex = 2;
                updateMainMenuDisplay();
                return;
            } else if (name.equals("MenuDown")) {
                selectedMenuIndex++;
                if (selectedMenuIndex > 2) selectedMenuIndex = 0;
                updateMainMenuDisplay();
                return;
            } else if (name.equals("MenuAction")) {
                if (selectedMenuIndex == 0) {
                    guiNode.detachChild(menuNode);
                    currentState = GameState.PLAYING;
                    playerAvatarNode.setCullHint(Spatial.CullHint.Inherit);
                    inventoryText.setCullHint(BitmapText.CullHint.Inherit);
                    if (orbStatusHudText != null) orbStatusHudText.setCullHint(BitmapText.CullHint.Inherit);
                    if (spiritOrbGeo != null) spiritOrbGeo.setCullHint(Spatial.CullHint.Inherit);
                    inputManager.setCursorVisible(false);
                    flyCam.setEnabled(true);
                    initAudioAndStartGameplay();
                } else if (selectedMenuIndex == 1) {
                    guiNode.detachChild(menuNode);
                    guiNode.attachChild(lobbyNode);
                    currentState = GameState.MULTIPARTY_LOBBY;
                } else if (selectedMenuIndex == 2) {
                    questText.setText("Settings coming soon!");
                }
                return;
            }
        }

        if (isIngameInviting && keyPressed) {
            if (name.equals("KeyBackspace") && targetHostInput.length() > 0) {
                targetHostInput.deleteCharAt(targetHostInput.length() - 1);
                updateIngameInviteDisplay();
                updateJoinBoxDisplay();
                return;
            }
            if (name.equals("KeyEnter")) {
                isIngameInviting = false;
                ingameInviteNode.setCullHint(Node.CullHint.Always);
                String targetName = targetHostInput.toString();

                if (ingameMultiHud != null) {
                    ingameMultiHud.setText("Sending invite to [" + targetName + "]...");
                }

                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        java.awt.EventQueue.invokeLater(() -> showInvitationDialog(targetName.isEmpty() ? "Player2" : targetName));
                    }
                }, 1500);
                return;
            }
            if (name.startsWith("TypeKey_")) {
                try {
                    int keyId = Integer.parseInt(name.substring(8));
                    char c = getAzertyCharForCode(keyId);
                    if (c != 0 && targetHostInput.length() < 15) {
                        targetHostInput.append(c);
                        updateIngameInviteDisplay();
                        updateJoinBoxDisplay();
                    }
                } catch (Exception ignored) {}
                return;
            }
        }

        if (currentState == GameState.MULTIPARTY_LOBBY && keyPressed) {
            if (name.equals("MultiplayerToggle") || name.equals("OptMainMenu")) {
                guiNode.detachChild(lobbyNode);
                if (menuNode.getParent() == null) {
                    guiNode.attachChild(menuNode);
                }
                currentState = GameState.MAIN_MENU;
                updateMainMenuDisplay();
                return;
            }
            if (name.equals("KeyBackspace") && targetHostInput.length() > 0) {
                targetHostInput.deleteCharAt(targetHostInput.length() - 1);
                updateJoinBoxDisplay();
                return;
            }
            if (name.equals("KeyEnter")) {
                if (lobbyStatusText != null) {
                    String targetName = targetHostInput.toString();
                    lobbyStatusText.setText("Invitation sent to [" + targetName + "]. Waiting for response...");
                    lobbyStatusText.setColor(ColorRGBA.Yellow);

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            java.awt.EventQueue.invokeLater(() -> showInvitationDialog(targetName.isEmpty() ? "Player2" : targetName));
                        }
                    }, 2000);
                }
                return;
            }
            if (name.startsWith("TypeKey_")) {
                try {
                    int keyId = Integer.parseInt(name.substring(8));
                    char c = getAzertyCharForCode(keyId);
                    if (c != 0 && targetHostInput.length() < 15) {
                        targetHostInput.append(c);
                        updateJoinBoxDisplay();
                    }
                } catch (Exception ignored) {}
                return;
            }
        }

        if (name.equals("MultiplayerToggle") && keyPressed) {
            if (currentState == GameState.MULTIPARTY_LOBBY) {
                guiNode.detachChild(lobbyNode);
                if (menuNode.getParent() == null) {
                    guiNode.attachChild(menuNode);
                }
                currentState = GameState.MAIN_MENU;
                updateMainMenuDisplay();
                return;
            }
        }

        if (name.equals("MenuAction") && keyPressed) {
            if (currentState == GameState.EPILEPSY_WARNING) {
                guiNode.detachChild(warningNode);
                if (menuNode.getParent() == null) {
                    guiNode.attachChild(menuNode);
                }
                currentState = GameState.MAIN_MENU;
                updateMainMenuDisplay();
                return;
            }
        }

        if (currentState != GameState.PLAYING) return;

        if (name.equals("Attack")) {
            isMouseDown = keyPressed;
        }

        if (keyPressed) {
            if (name.equals("TogglePause")) {
                togglePause();
                return;
            }

            if (isPaused) {
                if (name.equals("OptResume")) {
                    togglePause();
                } else if (name.equals("OptSens")) {
                    sensitivity += 0.5f;
                    if (sensitivity > 3.0f) sensitivity = 0.5f;
                    flyCam.setRotationSpeed(sensitivity * 2f);
                    updatePauseMenuText();
                } else if (name.equals("OptGraph")) {
                    if (graphicsQuality.equals("Medium")) graphicsQuality = "Low";
                    else if (graphicsQuality.equals("Low")) graphicsQuality = "High";
                    else graphicsQuality = "Medium";
                    updatePauseMenuText();
                    updateWorldMaterials();
                } else if (name.equals("VolUp")) {
                    masterVolume = Math.min(2.0f, masterVolume + 0.2f);
                    applyVolumeToAudios();
                    updatePauseMenuText();
                } else if (name.equals("VolDown")) {
                    masterVolume = Math.max(0.0f, masterVolume - 0.2f);
                    applyVolumeToAudios();
                    updatePauseMenuText();
                } else if (name.equals("OptJoinRequest")) {
                    isIngameInviting = true;
                    updateIngameInviteDisplay();
                    ingameInviteNode.setCullHint(Node.CullHint.Inherit);
                } else if (name.equals("OptMainMenu")) {
                    togglePause();
                    if (hubAudio != null) hubAudio.stop();
                    if (cityAudio != null) cityAudio.stop();
                    dungeonNode.detachAllChildren();
                    dungeonRoomNode.detachAllChildren();
                    cityNode.detachAllChildren();
                    deathScreenNode.setCullHint(Node.CullHint.Always);
                    winScreenNode.setCullHint(Node.CullHint.Always);
                    playerAvatarNode.setCullHint(Spatial.CullHint.Always);
                    if (orbStatusHudText != null) orbStatusHudText.setCullHint(BitmapText.CullHint.Always);
                    if (spiritOrbGeo != null) spiritOrbGeo.setCullHint(Spatial.CullHint.Always);

                    viewPort.setBackgroundColor(ColorRGBA.Black);
                    if (menuNode.getParent() == null) {
                        guiNode.attachChild(menuNode);
                    }
                    currentState = GameState.MAIN_MENU;
                    inputManager.setCursorVisible(true);
                    flyCam.setEnabled(false);
                    updateMainMenuDisplay();
                    return;
                } else if (name.equals("OptQuit")) {
                    stop();
                }
                return;
            }

            if (name.equals("Attack") && inDungeon && hasSword) {
                List<GoblinData> defeated = new ArrayList<>();
                for (GoblinData goblin : goblins) {
                    if (playerPos.distance(goblin.node.getLocalTranslation()) < 3.0f) {
                        goblin.hp -= 35f;
                        if (goblin.hp <= 0) {
                            defeated.add(goblin);
                            rootNode.detachChild(goblin.node);
                        } else {
                            goblin.healthBar.setLocalScale(goblin.hp / 100f, 1f, 1f);
                        }
                    }
                }
                goblins.removeAll(defeated);

                if (goblins.isEmpty()) {
                    hasWon = true;
                    winScreenNode.setCullHint(Node.CullHint.Inherit);
                    updateTextUI();
                    questText.setColor(ColorRGBA.Cyan);
                }
            }

            if (name.equals("Interact") && nearPortal && hasKey && hasSword && !inDungeon && !inCity) {
                enterDungeonInstance();
                interactText.setCullHint(BitmapText.CullHint.Always);
            }

            if (name.equals("Forward")) wPressed = true;
            if (name.equals("Backward")) sPressed = true;
            if (name.equals("Left")) qPressed = true;
            if (name.equals("Right")) dPressed = true;

            if (name.equals("ToggleFullscreen")) {
                boolean currentFullscreen = settings.isFullscreen();
                settings.setFullscreen(!currentFullscreen);
                restart();
            }

            if (name.equals("ResetGame")) {
                if ("ItzItx".equals(playerName)) {
                    resetGame();
                } else {
                    questText.setText("Action interdite : Seul ItzItx peut reset le jeu !");
                    questText.setColor(ColorRGBA.Red);
                }
            }
        } else {
            if (!isPaused) {
                if (name.equals("Forward")) wPressed = false;
                if (name.equals("Backward")) sPressed = false;
                if (name.equals("Left")) qPressed = false;
                if (name.equals("Right")) dPressed = false;
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (currentState != GameState.PLAYING) return;

        Quaternion rot = cam.getRotation();
        float[] angles = new float[3];
        rot.toAngles(angles);
        float maxPitch = FastMath.HALF_PI - 0.05f;
        if (angles[0] > maxPitch) {
            angles[0] = maxPitch;
            rot.fromAngles(angles);
            cam.setRotation(rot);
        } else if (angles[0] < -maxPitch) {
            angles[0] = -maxPitch;
            rot.fromAngles(angles);
            cam.setRotation(rot);
        }

        fpsUpdateTimer += tpf;
        if (fpsUpdateTimer >= 0.5f) {
            if (fpsText != null) {
                fpsText.setText("FPS: " + (int)(1f / tpf));
            }
            fpsUpdateTimer = 0f;
        }

        if (isPaused || hasWon || isChatting) return;

        // --- MISE À JOUR DU COMPAS D'ÂME ---
        if (spiritOrbGeo != null) {
            orbFloatTimer += tpf * 3f;
            Vector3f targetOrbPos = playerPos.clone()
                    .add(cam.getLeft().mult(-1.2f))
                    .add(0, (float)Math.sin(orbFloatTimer) * 0.15f, 0)
                    .add(cam.getDirection().mult(1.8f));
            spiritOrbGeo.setLocalTranslation(targetOrbPos);
            spiritOrbLight.setPosition(targetOrbPos);

            Material orbMat = spiritOrbGeo.getMaterial();
            boolean dangerClose = false;

            if (inDungeon) {
                for (GoblinData goblin : goblins) {
                    if (playerPos.distance(goblin.node.getLocalTranslation()) < 8.0f) {
                        dangerClose = true;
                        break;
                    }
                }
            } else if (inCity && giantMonsterGeo != null && giantMonsterGeo.getParent() != null) {
                if (playerPos.distance(giantMonsterGeo.getParent().getLocalTranslation()) < 15.0f) {
                    dangerClose = true;
                }
            }

            if (dangerClose) {
                float pulse = (float)(Math.sin(orbFloatTimer * 4f) * 0.5f + 0.5f);
                orbMat.setColor("Color", new ColorRGBA(1f, pulse * 0.2f, 0.2f, 1f));
                spiritOrbLight.setColor(new ColorRGBA(3f, 0.5f, 0.5f, 1f));
                if (orbStatusHudText != null) {
                    orbStatusHudText.setText("Spirit Orb:\n> DANGER (Red)");
                    orbStatusHudText.setColor(ColorRGBA.Red);
                }
            } else if (!hasKey && keyGeo != null && playerPos.distance(keyPos) < 15.0f) {
                orbMat.setColor("Color", ColorRGBA.Yellow);
                spiritOrbLight.setColor(new ColorRGBA(2.5f, 2.5f, 0.5f, 1f));
                if (orbStatusHudText != null) {
                    orbStatusHudText.setText("Spirit Orb:\n> KEY NEAR (Yellow)");
                    orbStatusHudText.setColor(ColorRGBA.Yellow);
                }
            } else if (!hasSword && swordPickupGeo != null && playerPos.distance(swordPos) < 15.0f) {
                orbMat.setColor("Color", ColorRGBA.Cyan);
                spiritOrbLight.setColor(new ColorRGBA(0.5f, 2.5f, 2.5f, 1f));
                if (orbStatusHudText != null) {
                    orbStatusHudText.setText("Spirit Orb:\n> SWORD NEAR (Cyan)");
                    orbStatusHudText.setColor(ColorRGBA.Cyan);
                }
            } else {
                orbMat.setColor("Color", new ColorRGBA(0.8f, 0.9f, 1.0f, 1f));
                spiritOrbLight.setColor(new ColorRGBA(1.5f, 1.8f, 2.5f, 1f));
                if (orbStatusHudText != null) {
                    orbStatusHudText.setText("Spirit Orb:\n> SEARCHING (White)");
                    orbStatusHudText.setColor(ColorRGBA.White);
                }
            }
        }

        if (inCity && playerPos.y > 1.0f) {
            playerPos.y -= 9.81f * tpf;
            if (playerPos.y < 1.0f) playerPos.y = 1.0f;
            cam.setLocation(playerPos);
        }

        if (inCity) {
            cityTimer += tpf;
        }

        if (inCity && giantMonsterGeo != null && giantMonsterGeo.getParent() != null && cityTimer > 10.0f) {
            Node monsterNode = (Node) giantMonsterGeo.getParent();
            Vector3f dirToPlayer = playerPos.subtract(monsterNode.getLocalTranslation());
            dirToPlayer.y = 0;

            float distanceToPlayer = dirToPlayer.length();
            Random aiRand = new Random();
            boolean notNoticed = aiRand.nextFloat() < 0.85f;

            if (!notNoticed && distanceToPlayer < 70.0f) {
                if (distanceToPlayer > 0.1f) {
                    dirToPlayer.normalizeLocal();
                    Vector3f nextMonsterPos = monsterNode.getLocalTranslation().clone().add(dirToPlayer.mult(giantMonsterSpeed * tpf));
                    
                    if (!isWall(nextMonsterPos.x, nextMonsterPos.z)) {
                        monsterNode.setLocalTranslation(nextMonsterPos);
                    }
                    monsterNode.lookAt(new Vector3f(cam.getLocation().x, monsterNode.getLocalTranslation().y, cam.getLocation().z), Vector3f.UNIT_Y);
                }
            }

            if (distanceToPlayer < 4.0f && !isGameOver) {
                isGameOver = true;
                isNoclip = true;
                deathScreenNode.setCullHint(Node.CullHint.Inherit);
                updateTextUI();
            }
        }

        if (inDungeon) {
            for (GoblinData goblin : goblins) {
                Vector3f dir = playerPos.subtract(goblin.node.getLocalTranslation());
                dir.y = 0;
                if (dir.length() > 0.2f) {
                    dir.normalizeLocal();
                    goblin.node.getLocalTranslation().addLocal(dir.mult(1.5f * tpf));
                    goblin.node.lookAt(new Vector3f(cam.getLocation().x, goblin.node.getLocalTranslation().y, goblin.node.getLocalTranslation().z), Vector3f.UNIT_Y);
                }

                if (playerPos.distance(goblin.node.getLocalTranslation()) < 1.0f) {
                    fallIntoCity();
                    return;
                }
            }
        }

        Vector3f camDir = cam.getDirection().clone();
        Vector3f camLeft = cam.getLeft().clone();

        camDir.y = 0;
        camDir.normalizeLocal();
        camLeft.y = 0;
        camLeft.normalizeLocal();

        Vector3f movement = new Vector3f(0, 0, 0);

        if (wPressed) movement.addLocal(camDir);
        if (sPressed) movement.subtractLocal(camDir);
        if (qPressed) movement.addLocal(camLeft);
        if (dPressed) movement.subtractLocal(camLeft);

        if (movement.lengthSquared() > 0) {
            movement.normalizeLocal().multLocal(moveSpeed * tpf);

            float rad = 0.3f;

            Vector3f nextXPos = playerPos.clone();
            nextXPos.x += movement.x;
            if (!isWall(nextXPos.x + Math.signum(movement.x) * rad, playerPos.z)) {
                playerPos.x = nextXPos.x;
            }

            Vector3f nextZPos = playerPos.clone();
            nextZPos.z += movement.z;
            if (!isWall(playerPos.x, nextZPos.z + Math.signum(movement.z) * rad)) {
                playerPos.z = nextZPos.z;
            }

            cam.setLocation(playerPos);
            playerAvatarNode.setLocalTranslation(playerPos.clone().subtract(0, 1f, 0));

            if (!inDungeon && !inCity) {
                updateChunksAroundPlayer();
            }
        }

        if (!hasKey && keyGeo != null) {
            if (playerPos.distance(keyPos) < 1.2f) {
                hasKey = true;
                rootNode.detachChild(keyGeo);
                playerInventory.add("Golden Key");
                updateInventoryUI();
                updateTextUI();
                questText.setColor(ColorRGBA.Yellow);
            }
        }

        if (!hasSword && swordPickupGeo != null) {
            if (playerPos.distance(swordPos) < 1.2f) {
                hasSword = true;
                rootNode.detachChild(swordPickupGeo);
                swordGeo.setCullHint(Geometry.CullHint.Inherit);
                playerInventory.add("Iron Sword");
                updateInventoryUI();
                updateTextUI();
                questText.setColor(ColorRGBA.Green);
            }
        }

        if (inCity && exitGeo != null) {
            if (playerPos.distance(exitPos) < 2.0f && hasKey) {
                hasWon = true;
                winScreenNode.setCullHint(Node.CullHint.Inherit);
                updateTextUI();
                    questText.setColor(ColorRGBA.Cyan);
            }
        }

        if (!inDungeon && !inCity && exitGeo != null) {
            if (playerPos.distance(exitPos) < 2.0f) {
                nearPortal = true;
                if (hasKey && hasSword) {
                    interactText.setText("Press [E] to enter the Dungeon");
                    interactText.setCullHint(BitmapText.CullHint.Inherit);
                } else if (!hasKey) {
                    interactText.setText("Locked! Find the golden key.");
                    interactText.setCullHint(BitmapText.CullHint.Inherit);
                } else if (!hasSword) {
                    interactText.setText("Locked! Pick up the sword first.");
                    interactText.setCullHint(BitmapText.CullHint.Inherit);
                }
            } else {
                nearPortal = false;
                interactText.setCullHint(BitmapText.CullHint.Always);
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {}
}