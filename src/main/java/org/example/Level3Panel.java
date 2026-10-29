package org.example;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Level3Panel extends JPanel implements Runnable, Restartable {

    // ===== COSTANTI DI GIOCO =====
    public final int originalTileSize = 16;
    public final int scale = 3;
    public final int tileSize = originalTileSize * scale; // 48x48 px

    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol; // 768 px
    public final int screenHeight = tileSize * maxScreenRow; // 576 px

    public final int worldWidth = screenWidth * 4; // Mappa lunga 3072 pixel

    private final int FPS = 60;
    private Thread gameThread;

    public KeyHandler keyH;
    public Cat cat;
    public ArrayList<Platform> platforms = new ArrayList<>();

    public Rectangle homeGoal;
    public Rectangle floorHazard;
    public int cameraX = 0;

    public boolean gameWon = false;
    public boolean gameOver = false;

    // ===== FRASI CASUALI PER GAME OVER =====
    private String currentGameOverPhrase = "";
    private final String[] gameOverPhrases = {
            "I gatti cadono sempre sui piedi... quasi sempre!",
            "Un piccolo riposino e ci riproviamo!",
            "La casa è vicina, ritenta!",
            "Anche i migliori cacciatori ogni tanto scivolano!",
            "Niente paura, i gatti hanno 7 vite!"
    };

    // ===== BOTTONI GAME OVER =====
    private Rectangle retryButton;
    private boolean mouseOverRetry = false;
    private boolean mousePressedRetry = false;

    private Rectangle gameOverExitButton;
    private boolean mouseOverGameOverExit = false;
    private boolean mousePressedGameOverExit = false;

    // ===== BOTTONI VITTORIA =====
    private Rectangle restartGameFromBeginningButton;
    private boolean mouseOverRestartGame = false;
    private boolean mousePressedRestartGame = false;

    private Rectangle restartLevelButton;
    private boolean mouseOverRestartLevel = false;
    private boolean mousePressedRestartLevel = false;

    private Rectangle winExitButton;
    private boolean mouseOverWinExit = false;
    private boolean mousePressedWinExit = false;

    // ===== CROCCANTINI =====
    public ArrayList<FallingTreat> treats = new ArrayList<>();
    public int treatsCollected = 0;

    public Level3Panel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(15, 18, 28));
        this.setDoubleBuffered(true);

        // Movement mouse per interazione bottoni
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                mouseOverRetry = retryButton != null && retryButton.contains(p);
                mouseOverGameOverExit = gameOverExitButton != null && gameOverExitButton.contains(p);
                mouseOverRestartGame = restartGameFromBeginningButton != null && restartGameFromBeginningButton.contains(p);
                mouseOverRestartLevel = restartLevelButton != null && restartLevelButton.contains(p);
                mouseOverWinExit = winExitButton != null && winExitButton.contains(p);
                repaint();
            }
        });

        // Click mouse per bottoni
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (gameOver) {
                    if (retryButton != null && retryButton.contains(p)) mousePressedRetry = true;
                    if (gameOverExitButton != null && gameOverExitButton.contains(p)) mousePressedGameOverExit = true;
                }
                if (gameWon) {
                    if (restartGameFromBeginningButton != null && restartGameFromBeginningButton.contains(p)) mousePressedRestartGame = true;
                    if (restartLevelButton != null && restartLevelButton.contains(p)) mousePressedRestartLevel = true;
                    if (winExitButton != null && winExitButton.contains(p)) mousePressedWinExit = true;
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();

                if (mousePressedRetry && retryButton != null && retryButton.contains(p)) {
                    restartGame();
                }
                if (mousePressedGameOverExit && gameOverExitButton != null && gameOverExitButton.contains(p)) {
                    System.exit(0);
                }

                if (mousePressedRestartGame && restartGameFromBeginningButton != null && restartGameFromBeginningButton.contains(p)) {
                    restartWholeGame();
                }
                if (mousePressedRestartLevel && restartLevelButton != null && restartLevelButton.contains(p)) {
                    restartGame();
                }
                if (mousePressedWinExit && winExitButton != null && winExitButton.contains(p)) {
                    System.exit(0);
                }

                mousePressedRestartGame = false;
                mousePressedRestartLevel = false;
                mousePressedRetry = false;
                mousePressedGameOverExit = false;
                mousePressedWinExit = false;
                repaint();
            }
        });

        keyH = new KeyHandler(this);
        this.addKeyListener(keyH);

        this.setFocusable(true);
        SwingUtilities.invokeLater(this::requestFocusInWindow);

        setupLevel();
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    @Override
    public boolean isGameWon() {
        return gameWon;
    }

    public void setupLevel() {
        Color roofTuscan = new Color(165, 70, 45);
        Color roofTuscanDark = new Color(105, 40, 25);

        // Strada inferiore
        floorHazard = new Rectangle(0, screenHeight - 25, worldWidth, 25);

        // PIATTAFORME E CASE DEL LIVELLO 3
        platforms.clear();
        platforms.add(new Platform(0, 430, 230, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(360, 370, 190, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(700, 280, 250, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(1100, 390, 180, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(1440, 250, 270, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(1880, 350, 200, 30, roofTuscan, roofTuscanDark));
        platforms.add(new Platform(2240, 210, 280, 30, roofTuscan, roofTuscanDark));

        // PIATTAFORMA FINALE APPOGGIATA ALLA STRADA (Y = 520)
        platforms.add(new Platform(2720, 520, 250, 31, roofTuscan, roofTuscanDark));

        // PORTA D'INGRESSO ESATTAMENTE SULLA STRADA (Y = 520 - 60 = 460)
        homeGoal = new Rectangle(2745, 460, 50, 60);

        cat = new Cat(this, keyH);
        cat.x = 50;
        cat.y = 430 - cat.height;
    }

    public void restartGame() {
        cat.x = 50;
        cat.y = 430 - cat.height;
        cat.velY = 0;
        cameraX = 0;
        gameOver = false;
        gameWon = false;

        treats.clear();
        treatsCollected = 0;
    }

    // ===== RICOMINCIA IL GIOCO DAL PRIMO LIVELLO (GamePanel) =====
    public void restartWholeGame() {
        if (gameThread != null) {
            gameThread = null; // Interrompe il thread del Livello 3
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            frame.getContentPane().removeAll();

            GamePanel level1 = new GamePanel();
            frame.add(level1);

            frame.revalidate();
            frame.repaint();

            level1.requestFocusInWindow();
            level1.startGameThread();
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        if (gameWon || gameOver) return;

        cat.update(platforms);

        cameraX = (int) cat.x - (screenWidth / 2) + (cat.width / 2);
        if (cameraX < 0) cameraX = 0;
        if (cameraX > worldWidth - screenWidth) cameraX = worldWidth - screenWidth;

        if (cat.getBounds().intersects(floorHazard) || cat.y + cat.height >= screenHeight - 20) {
            if (!gameOver) {
                int randomIndex = (int) (Math.random() * gameOverPhrases.length);
                currentGameOverPhrase = gameOverPhrases[randomIndex];
            }
            gameOver = true;
        }

        if (cat.getBounds().intersects(homeGoal)) {
            gameWon = true;
        }

        // Genera croccantini casuali
        if (Math.random() < 0.02) {
            float spawnX = cameraX + (float) (Math.random() * screenWidth);
            treats.add(new FallingTreat(spawnX, -40));
        }

        // Aggiorna croccantini
        for (int i = 0; i < treats.size(); i++) {
            FallingTreat t = treats.get(i);
            t.update();

            if (t.y > screenHeight + 100) {
                treats.remove(i);
                i--;
                continue;
            }

            if (cat.getBounds().intersects(t.getBounds())) {
                treatsCollected++;
                treats.remove(i);
                i--;
            }
        }
    }

    // =======================================================
    // ===== SFONDO NOTTURNO CON LUNA E STELLE =================
    // =======================================================
    private void drawNightSky(Graphics2D g2) {
        if (gameOver) {
            GradientPaint stormGrad = new GradientPaint(
                    0, 0, new Color(20, 25, 40),
                    0, screenHeight, new Color(50, 60, 85)
            );
            g2.setPaint(stormGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawSoftCloud(g2, 60 - (int) (cameraX * 0.08), 45, 1.3f, new Color(40, 45, 60, 230));
            drawSoftCloud(g2, 320 - (int) (cameraX * 0.08), 60, 1.1f, new Color(35, 40, 55, 220));
            drawSoftCloud(g2, 620 - (int) (cameraX * 0.08), 35, 1.4f, new Color(45, 50, 65, 240));
            drawSoftCloud(g2, 1000 - (int) (cameraX * 0.08), 50, 1.2f, new Color(35, 40, 55, 230));
        } else {
            GradientPaint nightGrad = new GradientPaint(
                    0, 0, new Color(10, 15, 32),
                    0, screenHeight, new Color(30, 40, 70)
            );
            g2.setPaint(nightGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            g2.setColor(new Color(255, 255, 255, 180));
            for (int i = 0; i < 70; i++) {
                int starX = (i * 97 + i * i * 13) % worldWidth - (int) (cameraX * 0.05);
                int starY = (i * 37) % (screenHeight - 200);
                int starSize = (i % 3 == 0) ? 3 : 2;
                g2.fillRect(starX, starY, starSize, starSize);
            }

            int moonX = 580 - (int) (cameraX * 0.04);
            int moonY = 50;

            float[] dists = {0.0f, 0.4f, 1.0f};
            Color[] colors = {
                    new Color(255, 255, 230, 220),
                    new Color(200, 220, 255, 80),
                    new Color(150, 180, 240, 0)
            };
            RadialGradientPaint moonGlow = new RadialGradientPaint(
                    moonX + 40, moonY + 40, 90, dists, colors
            );
            g2.setPaint(moonGlow);
            g2.fillOval(moonX - 50, moonY - 50, 180, 180);

            g2.setColor(new Color(245, 245, 230));
            g2.fillOval(moonX, moonY, 60, 60);

            g2.setColor(new Color(220, 220, 200));
            g2.fillOval(moonX + 12, moonY + 15, 12, 10);
            g2.fillOval(moonX + 32, moonY + 28, 14, 14);

            Color nightCloud = new Color(50, 65, 95, 120);
            drawSoftCloud(g2, 100 - (int) (cameraX * 0.08), 70, 1.0f, nightCloud);
            drawSoftCloud(g2, 480 - (int) (cameraX * 0.08), 95, 0.85f, nightCloud);
            drawSoftCloud(g2, 920 - (int) (cameraX * 0.08), 60, 1.1f, nightCloud);
        }
    }

    private void drawSoftCloud(Graphics2D g2, int x, int y, float scaleFactor, Color mainColor) {
        int w = (int) (120 * scaleFactor);
        int h = (int) (40 * scaleFactor);

        g2.setColor(mainColor);
        g2.fillOval(x, y, w, h);
        g2.fillOval(x + (int) (20 * scaleFactor), y - (int) (20 * scaleFactor), (int) (55 * scaleFactor), (int) (55 * scaleFactor));
        g2.fillOval(x + (int) (50 * scaleFactor), y - (int) (30 * scaleFactor), (int) (65 * scaleFactor), (int) (65 * scaleFactor));
        g2.fillOval(x + (int) (80 * scaleFactor), y - (int) (15 * scaleFactor), (int) (45 * scaleFactor), (int) (45 * scaleFactor));
    }

    // =======================================================
    // ===== SFONDO COLLINE TOSCANE E CIPRESSI ===============
    // =======================================================
    private void drawTuscanHillsAndCypresses(Graphics2D g2) {
        g2.setColor(gameOver ? new Color(30, 35, 50, 180) : new Color(20, 32, 52, 180));

        int[] hillX = {0, 350, 750, 1200, 1600, 2050, 2500, 2950, 3300};
        int[] hillH = {170, 210, 150, 230, 180, 220, 160, 200, 170};

        for (int i = 0; i < hillX.length - 1; i++) {
            int hx = hillX[i] - (int) (cameraX * 0.2);
            int hw = hillX[i + 1] - hillX[i] + 40;
            int hy = screenHeight - hillH[i];
            g2.fillOval(hx, hy - 40, hw, 180);
        }

        g2.setColor(gameOver ? new Color(15, 20, 30) : new Color(10, 20, 35));
        int[] cypX = {120, 420, 800, 1150, 1520, 1900, 2300, 2750};
        for (int cx : cypX) {
            int x = cx - (int) (cameraX * 0.25);
            int y = screenHeight - 190;

            g2.fillOval(x, y, 16, 80);
            g2.fillOval(x + 2, y - 10, 12, 60);

            g2.fillRect(x + 6, y + 60, 4, 30);
        }
    }

    // =======================================================
    // ===== CASE RUSTICHE TOSCANE CON PORTE, LUCI E FIORI ===
    // =======================================================
    private void drawRusticTuscanHouse(Graphics2D g2, int worldX, int roofY, int width, int houseType) {
        int x = worldX - cameraX;
        int floorY = screenHeight - 25;
        int wallY = roofY + 30;
        int height = floorY - wallY;

        if (x + width < -50 || x > screenWidth + 50) return;

        Color[][] palette = {
                {new Color(185, 140, 100), new Color(145, 105, 70), new Color(100, 70, 45)},
                {new Color(160, 120, 90), new Color(125, 90, 65), new Color(85, 55, 38)},
                {new Color(195, 160, 125), new Color(155, 125, 95), new Color(110, 85, 60)},
                {new Color(170, 130, 110), new Color(135, 95, 80), new Color(90, 60, 50)}
        };
        Color[] chosenColors = palette[houseType % palette.length];

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(x + width, wallY + 10, 12, height - 10);

        GradientPaint wallGrad = new GradientPaint(
                x, wallY, chosenColors[0],
                x, floorY, chosenColors[1]
        );
        g2.setPaint(wallGrad);
        g2.fillRect(x, wallY, width, height);

        g2.setColor(new Color(0, 0, 0, 30));
        for (int lineY = wallY + 15; lineY < floorY; lineY += 18) {
            g2.drawLine(x, lineY, x + width - 1, lineY);
        }

        g2.setColor(new Color(145, 55, 35));
        int[] px = {x - 8, x + width / 2, x + width + 8};
        int[] py = {wallY - 4, roofY - 22, wallY - 4};
        g2.fillPolygon(px, py, 3);

        g2.setColor(new Color(90, 30, 20));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawPolygon(px, py, 3);

        // FINESTRE CON FIORI SUI DAVANZALI
        int windowWidth = 26;
        int windowHeight = 40;
        int cols = Math.max(2, (width - 25) / (windowWidth + 20));
        int spacingX = (width - (cols * windowWidth)) / (cols + 1);

        for (int winY = wallY + 30; winY < floorY - 80; winY += windowHeight + 22) {
            for (int c = 0; c < cols; c++) {
                int winX = x + spacingX + c * (windowWidth + spacingX);

                g2.setColor(new Color(255, 210, 120));
                g2.fillRect(winX, winY, windowWidth, windowHeight);

                g2.setColor(chosenColors[2]);
                g2.drawRect(winX - 2, winY - 2, windowWidth + 4, windowHeight + 4);

                g2.setColor(new Color(40, 80, 50));
                g2.fillRect(winX - 9, winY, 7, windowHeight);
                g2.fillRect(winX + windowWidth + 2, winY, 7, windowHeight);

                g2.setColor(new Color(70, 50, 30));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(winX + windowWidth / 2, winY, winX + windowWidth / 2, winY + windowHeight);
                g2.drawLine(winX, winY + windowHeight / 2, winX + windowWidth, winY + windowHeight / 2);

                // Fiorellini sui davanzali
                g2.setColor(new Color(110, 50, 30));
                g2.fillRect(winX - 4, winY + windowHeight, windowWidth + 8, 5);
                g2.setColor(new Color(255, 90, 110));
                g2.fillOval(winX - 2, winY + windowHeight - 3, 5, 5);
                g2.fillOval(winX + 8, winY + windowHeight - 4, 6, 6);
                g2.fillOval(winX + 18, winY + windowHeight - 3, 5, 5);
            }
        }

        // PORTONCINO RUSTICO A LIVELLO STRADA
        int doorWidth = 32;
        int doorHeight = 56;
        int doorX = x + (width - doorWidth) / 2;
        int doorY = floorY - doorHeight;

        g2.setColor(new Color(85, 45, 25));
        g2.fillRoundRect(doorX, doorY, doorWidth, doorHeight, 10, 10);
        g2.setColor(new Color(55, 28, 15));
        g2.drawRoundRect(doorX, doorY, doorWidth, doorHeight, 10, 10);

        g2.setColor(new Color(220, 180, 50));
        g2.fillOval(doorX + doorWidth - 8, doorY + doorHeight / 2, 4, 4);

        // LAMPIONCINO ACCESO VICINO ALLA PORTA
        int lampX = doorX - 16;
        int lampY = doorY + 12;

        g2.setColor(new Color(255, 200, 80, 70));
        g2.fillOval(lampX - 12, lampY - 12, 32, 32);

        g2.setColor(new Color(40, 35, 30));
        g2.fillRect(lampX, lampY, 8, 10);
        g2.setColor(new Color(255, 220, 120));
        g2.fillRect(lampX + 2, lampY + 2, 4, 6);
    }

    // =======================================================
    // ===== CASINA FINALE PICCOLA APPOGGIATA A STRADA ========
    // =======================================================
    private void drawSmallEndHouse(Graphics2D g2, int worldX, int platformY, int width) {
        int x = worldX - cameraX;
        int roofY = platformY - 110;
        int wallY = roofY + 20;
        int height = platformY - wallY;

        if (x + width < -50 || x > screenWidth + 50) return;

        Color wallBase = new Color(205, 160, 120);
        Color wallDark = new Color(150, 110, 80);

        // Ombra
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRect(x + width, wallY + 8, 8, height - 8);

        // Parete casina
        GradientPaint wallGrad = new GradientPaint(x, wallY, wallBase, x, platformY, wallDark);
        g2.setPaint(wallGrad);
        g2.fillRect(x, wallY, width, height);

        // Tetto piccolo
        g2.setColor(new Color(165, 60, 40));
        int[] px = {x - 6, x + width / 2, x + width + 6};
        int[] py = {wallY - 2, roofY - 15, wallY - 2};
        g2.fillPolygon(px, py, 3);

        g2.setColor(new Color(100, 35, 20));
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(px, py, 3);

        // Finestrella in alto a destra (separata dalla porta)
        int winW = 22, winH = 28;
        int winX = x + width - winW - 20;
        int winY = wallY + 10;

        g2.setColor(new Color(255, 215, 110));
        g2.fillRect(winX, winY, winW, winH);
        g2.setColor(new Color(80, 50, 30));
        g2.drawRect(winX, winY, winW, winH);

        // Fioriera finestra
        g2.setColor(new Color(110, 50, 30));
        g2.fillRect(winX - 3, winY + winH, winW + 6, 4);
        g2.setColor(new Color(255, 90, 110));
        g2.fillOval(winX, winY + winH - 3, 5, 5);
        g2.fillOval(winX + 10, winY + winH - 3, 5, 5);

        // PORTA D'INGRESSO ESATTAMENTE SULLA STRADA (Y = platformY - 60)
        drawHouseEntrance(g2, x + 25, platformY - 60, 50, 60);
    }

    // =======================================================
    // ===== INGRESSO DI CASA CON PORTA E GATTAIOLA ==========
    // =======================================================
    private void drawHouseEntrance(Graphics2D g2, int x, int y, int w, int h) {
        // Tappetino "HOME"
        g2.setColor(new Color(130, 85, 55));
        g2.fillRect(x - 8, y + h - 6, w + 16, 6);
        g2.setColor(new Color(255, 235, 190));
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("HOME", x + 10, y + h - 1);

        // Telaio porta
        g2.setColor(new Color(90, 50, 30));
        g2.fillRect(x - 3, y - 3, w + 6, h + 3);

        // Porta in legno
        g2.setColor(new Color(145, 80, 45));
        g2.fillRect(x, y, w, h);

        g2.setColor(new Color(90, 45, 20));
        g2.drawRect(x + 4, y + 4, w - 8, 22);

        // Pomello
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(x + w - 10, y + 25, 5, 5);

        // GATTAIOLA ILLUMINATA SULLA STRADA
        int flapW = 22;
        int flapH = 18;
        int flapX = x + (w - flapW) / 2;
        int flapY = y + h - flapH - 2;

        g2.setColor(new Color(255, 220, 100, 200));
        g2.fillRect(flapX, flapY, flapW, flapH);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRect(flapX, flapY, flapW, flapH);

        // Lampioncino d'ingresso acceso
        int lampX = x + w / 2 - 5;
        int lampY = y - 18;

        g2.setColor(new Color(255, 220, 120, 90));
        g2.fillOval(lampX - 12, lampY - 8, 34, 34);

        g2.setColor(new Color(50, 40, 30));
        g2.fillRect(lampX, lampY, 10, 10);
        g2.setColor(new Color(255, 235, 150));
        g2.fillRect(lampX + 2, lampY + 2, 6, 6);
    }

    // =======================================================
    // ===== STRADA IN MATTONI ===============================
    // =======================================================
    private void drawRoadHazard(Graphics2D g2) {
        int roadY = floorHazard.y;
        int roadH = floorHazard.height;

        g2.setColor(new Color(60, 35, 28));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, roadH);

        Color brickColor1 = new Color(140, 65, 45);
        Color brickColor2 = new Color(115, 50, 35);
        int brickW = 20;
        int brickH = 8;

        for (int r = 0; r < 3; r++) {
            int rowY = roadY + 2 + (r * (brickH + 2));
            int offset = (r % 2 == 0) ? 0 : 10;

            for (int bx = -20; bx < worldWidth + 20; bx += brickW + 3) {
                int drawX = bx + offset - cameraX;
                if (drawX + brickW < 0 || drawX > screenWidth) continue;

                g2.setColor((bx % 2 == 0) ? brickColor1 : brickColor2);
                g2.fillRect(drawX, rowY, brickW, brickH);
            }
        }

        g2.setColor(new Color(180, 90, 60));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, 2);
    }

    // =======================================================
    // ===== CORIANDOLI CADENTI (VITTORIA) ===================
    // =======================================================
    private void drawConfetti(Graphics2D g2, long time) {
        Color[] confettiColors = {Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.GREEN, Color.WHITE, Color.PINK};
        for (int i = 0; i < 140; i++) {
            int x = (i * 47 + i * i * 3) % screenWidth;
            int y = (int) ((time / 7.0 + i * 35) % (screenHeight + 100)) - 50;
            g2.setColor(confettiColors[i % confettiColors.length]);

            // Alterna coriandoli tondi e rettangolari
            if (i % 2 == 0) {
                g2.fillOval(x, y, 7, 7);
            } else {
                g2.fillRect(x, y, 8, 5);
            }
        }
    }

    // =======================================================
    // ===== ANIMAZIONE FUOCHI D'ARTIFICIO (VITTORIA) ========
    // =======================================================
    private void drawFireworks(Graphics2D g2, long time) {
        int[][] centers = {
                {150, 120},
                {380, 80},
                {620, 110},
                {250, 160},
                {520, 140}
        };

        Color[][] fireworkPalettes = {
                {Color.YELLOW, Color.ORANGE, Color.RED, new Color(255, 220, 100)},
                {Color.CYAN, Color.GREEN, Color.WHITE, new Color(100, 255, 200)},
                {Color.MAGENTA, Color.PINK, Color.WHITE, new Color(255, 150, 255)},
                {Color.ORANGE, Color.YELLOW, Color.WHITE, new Color(255, 180, 50)},
                {new Color(150, 180, 255), Color.CYAN, Color.WHITE, new Color(200, 220, 255)}
        };

        for (int f = 0; f < centers.length; f++) {
            int cx = centers[f][0];
            int cy = centers[f][1];
            Color[] palette = fireworkPalettes[f % fireworkPalettes.length];

            double cycle = ((time + f * 450) % 1800) / 1800.0;
            float radius = (float) (cycle * 110);
            int alpha = (int) Math.max(0, 255 * (1.0 - cycle));

            if (alpha <= 0) continue;

            // Bagliore centrale
            g2.setColor(new Color(255, 255, 255, alpha / 2));
            g2.fillOval((int) (cx - radius * 0.15), (int) (cy - radius * 0.15), (int) (radius * 0.3), (int) (radius * 0.3));

            // Particelle che esplodono
            int particleCount = 20;
            for (int p = 0; p < particleCount; p++) {
                double angle = (2 * Math.PI / particleCount) * p;
                int px = (int) (cx + Math.cos(angle) * radius);
                int py = (int) (cy + Math.sin(angle) * radius + (cycle * cycle * 20));

                Color particleColor = palette[p % palette.length];
                g2.setColor(new Color(particleColor.getRed(), particleColor.getGreen(), particleColor.getBlue(), alpha));

                g2.fillOval(px - 3, py - 3, 6, 6);

                int trailX = (int) (cx + Math.cos(angle) * (radius * 0.75));
                int trailY = (int) (cy + Math.sin(angle) * (radius * 0.75));
                g2.fillOval(trailX - 1, trailY - 1, 3, 3);
            }
        }
    }

    // =======================================================
    // ===== INTERFACCIA HUD STILIZZATA =======================
    // =======================================================
    private void drawStylizedHUD(Graphics2D g2) {
        int panelX = 16;
        int panelY = 16;
        int panelW = 250;
        int panelH = 86;

        GradientPaint bgGrad = new GradientPaint(
                panelX, panelY, new Color(20, 25, 45, 200),
                panelX, panelY + panelH, new Color(10, 15, 30, 220)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setColor(new Color(200, 215, 255, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(220, 235, 255));
        g2.drawString("LIVELLO 3: Notte", panelX + 14, panelY + 22);

        int progress = (int) Math.min(100, Math.max(0, cat.x / 28.5));
        int barX = panelX + 14;
        int barY = panelY + 31;
        int barW = 145;
        int barH = 12;

        g2.setColor(new Color(15, 20, 35, 220));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);

        int fillW = (int) (barW * (progress / 100.0));
        if (fillW > 0) {
            GradientPaint progressGrad = new GradientPaint(
                    barX, barY, new Color(100, 160, 255),
                    barX + barW, barY, new Color(180, 210, 255)
            );
            g2.setPaint(progressGrad);
            g2.fillRoundRect(barX, barY, fillW, barH, 8, 8);
        }

        g2.setColor(new Color(240, 245, 255));
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.drawString(progress + "%", barX + barW + 10, barY + 10);

        int badgeX = panelX + 14;
        int badgeY = panelY + 51;
        int badgeW = 222;
        int badgeH = 24;

        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
        g2.setColor(new Color(255, 200, 120, 80));
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);

        g2.setColor(new Color(210, 130, 45));
        g2.fillOval(badgeX + 10, badgeY + 6, 13, 10);
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(badgeX + 12, badgeY + 7, 5, 3);

        g2.setColor(new Color(255, 245, 220));
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("Croccantini :  " + treatsCollected, badgeX + 30, badgeY + 16);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. CIELO NOTTURNO
        drawNightSky(g2);

        // 2. COLLINE TOSCANE E CIPRESSI
        drawTuscanHillsAndCypresses(g2);

        // 3. CASE RUSTICHE TOSCANE
        drawRusticTuscanHouse(g2, 0, 400, 230, 0);
        drawRusticTuscanHouse(g2, 360, 340, 190, 1);
        drawRusticTuscanHouse(g2, 700, 250, 250, 2);
        drawRusticTuscanHouse(g2, 1100, 360, 180, 3);
        drawRusticTuscanHouse(g2, 1440, 220, 270, 0);
        drawRusticTuscanHouse(g2, 1880, 320, 200, 1);
        drawRusticTuscanHouse(g2, 2240, 180, 280, 2);

        // 4. CASINA FINALE PICCOLA APPOGGIATA A STRADA (Y = 520)
        drawSmallEndHouse(g2, 2720, 520, 180);

        // 5. STRADA IN MATTONI
        drawRoadHazard(g2);

        // 6. TETTI / PIATTAFORME
        for (Platform p : platforms) {
            p.draw(g2, cameraX);
        }

        // 7. CROCCANTINI
        for (FallingTreat t : treats) {
            t.draw(g2, cameraX);
        }

        // 8. GATTO
        cat.draw(g2, cameraX);

        // 9. HUD STILIZZATO
        drawStylizedHUD(g2);

        long time = System.currentTimeMillis();

        // ==========================================
        // ===== SCHERMATA DI VITTORIA FINALE ======
        // ==========================================
        if (gameWon) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            // 1. FUOCHI D'ARTIFICIO
            drawFireworks(g2, time);

            // 2. CORIANDOLI CADENTI
            drawConfetti(g2, time);

            int titleSize = 34 + (int) (Math.sin(time * 0.006) * 3);
            g2.setFont(new Font("Arial", Font.BOLD, titleSize));
            String title = "IL GATTINO E' TORNATO A CASA!";
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleX = (screenWidth - fmTitle.stringWidth(title)) / 2;

            g2.setColor(new Color(80, 40, 0, 180));
            g2.drawString(title, titleX + 4, 74);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(title, titleX, 70);

            String message = "Sei entrato in casa sano e salvo! Ora puoi finalmente dormire! Zzz...";
            g2.setFont(new Font("Arial", Font.BOLD, 17));
            FontMetrics fmMsg = g2.getFontMetrics();
            int msgX = (screenWidth - fmMsg.stringWidth(message)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(message, msgX, 115);

            String rankText;
            Color rankColor;
            if (treatsCollected >= 9) {
                rankText = "RANGO FINALE: RE DEL RIPOSO";
                rankColor = new Color(255, 215, 0);
            } else if (treatsCollected >= 4) {
                rankText = "RANGO FINALE: SOGNATORE FELINO";
                rankColor = new Color(100, 230, 120);
            } else {
                rankText = "RANGO FINALE: GATTO PIGRO";
                rankColor = new Color(180, 210, 255);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            FontMetrics fmRank = g2.getFontMetrics();
            int rankX = (screenWidth - fmRank.stringWidth(rankText)) / 2;
            g2.setColor(rankColor);
            g2.drawString(rankText, rankX, 155);

            String score = "Croccantini raccolti: " + treatsCollected;
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fmScore = g2.getFontMetrics();
            int scoreX = (screenWidth - fmScore.stringWidth(score)) / 2;
            g2.setColor(new Color(255, 240, 140));
            g2.drawString(score, scoreX, 188);

            // ANIMAZIONE DEL GATTO
            int catBounce = (int) (Math.sin(time * 0.008) * 8);
            if (cat != null && cat.getCatImage() != null) {
                BufferedImage img = cat.getCatImage();
                int targetH = 80;
                float aspectRatio = (float) img.getWidth() / img.getHeight();
                int targetW = (int) (targetH * aspectRatio);
                int winCatX = (screenWidth - targetW) / 2;
                int winCatY = 220 + catBounce;

                g2.drawImage(img, winCatX, winCatY, targetW, targetH, null);
            }

            // 1. BOTTONE PRIMARIO: RICOMINCIA DA CAPO (GamePanel - LIVELLO 1)
            int restartGameW = 260, restartGameH = 45;
            int restartGameX = (screenWidth - restartGameW) / 2, restartGameY = 380;
            restartGameFromBeginningButton = new Rectangle(restartGameX, restartGameY, restartGameW, restartGameH);
            int mainOffset = mousePressedRestartGame ? 3 : 0;

            g2.setColor(mousePressedRestartGame ? new Color(30, 160, 80) : mouseOverRestartGame ? new Color(70, 220, 120) : new Color(45, 190, 100));
            g2.fillRoundRect(restartGameX, restartGameY + mainOffset, restartGameW, restartGameH, 25, 25);

            String mainText = "RICOMINCIA DA CAPO";
            g2.setFont(new Font("Arial", Font.BOLD, 17));
            FontMetrics fmMain = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(mainText, restartGameX + (restartGameW - fmMain.stringWidth(mainText)) / 2, restartGameY + 29 + mainOffset);

            // 2. BOTTONE SECONDARIO: RIGIOCA LIVELLO 3
            int playW = 180, playH = 38;
            int playX = (screenWidth - playW) / 2, playY = 440;
            restartLevelButton = new Rectangle(playX, playY, playW, playH);
            int playOffset = mousePressedRestartLevel ? 3 : 0;

            g2.setColor(mousePressedRestartLevel ? new Color(220, 150, 0) : mouseOverRestartLevel ? new Color(255, 220, 50) : new Color(255, 170, 0));
            g2.fillRoundRect(playX, playY + playOffset, playW, playH, 20, 20);

            String playText = "RIGIOCA LIVELLO 3";
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fmPlay = g2.getFontMetrics();
            g2.setColor(new Color(40, 40, 60));
            g2.drawString(playText, playX + (playW - fmPlay.stringWidth(playText)) / 2, playY + 24 + playOffset);

            // 3. BOTTONE TERZIARIO: ESCI
            int exitW = 130, exitH = 35;
            int exitX = (screenWidth - exitW) / 2, exitY = 490;
            winExitButton = new Rectangle(exitX, exitY, exitW, exitH);
            int winExitOffset = mousePressedWinExit ? 3 : 0;

            g2.setColor(mousePressedWinExit ? new Color(180, 70, 70) : mouseOverWinExit ? new Color(255, 120, 120) : new Color(220, 90, 90));
            g2.fillRoundRect(exitX, exitY + winExitOffset, exitW, exitH, 20, 20);

            String exitText = "ESCI";
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fmExit = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(exitText, exitX + (exitW - fmExit.stringWidth(exitText)) / 2, exitY + 23 + winExitOffset);

            // ==========================================
            // ===== SCHERMATA DI GAME OVER =============
            // ==========================================
        } else if (gameOver) {
            g2.setColor(new Color(15, 15, 25, 185));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            g2.setColor(new Color(140, 170, 230, 110));
            for (int i = 0; i < 45; i++) {
                int sx = (i * 73) % screenWidth;
                int sy = (int) ((time / 12.0 + i * 45) % (screenHeight + 50)) - 20;
                g2.fillRect(sx, sy, 3, 8);
            }

            int panelW = 600, panelH = 370;
            int panelX = (screenWidth - panelW) / 2;
            int panelY = (screenHeight - panelH) / 2;

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(panelX + 8, panelY + 8, panelW, panelH, 35, 35);

            GradientPaint panelGrad = new GradientPaint(
                    panelX, panelY, new Color(45, 35, 55),
                    panelX, panelY + panelH, new Color(25, 20, 35)
            );
            g2.setPaint(panelGrad);
            g2.fillRoundRect(panelX, panelY, panelW, panelH, 35, 35);

            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(255, 140, 60));
            g2.drawRoundRect(panelX, panelY, panelW, panelH, 35, 35);

            String title = "OH NO! IL GATTINO E' CADUTO!";
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleX = panelX + (panelW - fmTitle.stringWidth(title)) / 2;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(title, titleX + 2, panelY + 52);

            g2.setColor(new Color(255, 100, 80));
            g2.drawString(title, titleX, panelY + 50);

            int catSadBounce = (int) (Math.sin(time * 0.005) * 5);
            if (cat != null && cat.getCatImage() != null) {
                BufferedImage img = cat.getCatImage();
                int targetH = 75;
                float aspectRatio = (float) img.getWidth() / img.getHeight();
                int targetW = (int) (targetH * aspectRatio);
                int catX = panelX + (panelW - targetW) / 2;
                int catY = panelY + 75 + catSadBounce;

                g2.drawImage(img, catX, catY, targetW, targetH, null);
            }

            g2.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fmPhrase = g2.getFontMetrics();
            int phraseX = panelX + (panelW - fmPhrase.stringWidth(currentGameOverPhrase)) / 2;
            g2.setColor(new Color(240, 220, 180));
            g2.drawString(currentGameOverPhrase, phraseX, panelY + 190);

            String score = "Croccantini raccolti prima della caduta: " + treatsCollected;
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fmScore = g2.getFontMetrics();
            int scoreX = panelX + (panelW - fmScore.stringWidth(score)) / 2;
            g2.setColor(new Color(255, 200, 100));
            g2.drawString(score, scoreX, panelY + 225);

            int btnW = 200, btnH = 50;
            int retryX = panelX + (panelW / 2) - btnW - 15;
            int btnY = panelY + 275;

            retryButton = new Rectangle(retryX, btnY, btnW, btnH);
            int retryOffset = mousePressedRetry ? 3 : 0;

            g2.setColor(mousePressedRetry ? new Color(220, 140, 0) : mouseOverRetry ? new Color(255, 210, 50) : new Color(255, 160, 0));
            g2.fillRoundRect(retryX, btnY + retryOffset, btnW, btnH, 25, 25);

            String retryText = "RIPROVA";
            g2.setFont(new Font("Arial", Font.BOLD, 19));
            FontMetrics fmRetry = g2.getFontMetrics();
            g2.setColor(new Color(40, 30, 20));
            g2.drawString(retryText, retryX + (btnW - fmRetry.stringWidth(retryText)) / 2, btnY + 32 + retryOffset);

            int gameOverExitX = panelX + (panelW / 2) + 15;
            gameOverExitButton = new Rectangle(gameOverExitX, btnY, btnW, btnH);
            int gameOverExitOffset = mousePressedGameOverExit ? 3 : 0;

            g2.setColor(mousePressedGameOverExit ? new Color(170, 60, 60) : mouseOverGameOverExit ? new Color(240, 110, 110) : new Color(210, 80, 80));
            g2.fillRoundRect(gameOverExitX, btnY + gameOverExitOffset, btnW, btnH, 25, 25);

            String exitText = "ESCI";
            g2.setFont(new Font("Arial", Font.BOLD, 19));
            FontMetrics fmExit = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(exitText, gameOverExitX + (btnW - fmExit.stringWidth(exitText)) / 2, btnY + 32 + gameOverExitOffset);
        }

        g2.dispose();
    }
}