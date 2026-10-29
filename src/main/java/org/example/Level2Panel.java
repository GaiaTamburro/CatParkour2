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

public class Level2Panel extends JPanel implements Runnable, Restartable {

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

    public Rectangle bowlGoal;
    public Rectangle floorHazard;
    public int cameraX = 0;

    public boolean gameWon = false;
    public boolean gameOver = false;

    // ===== FRASI CASUALI PER GAME OVER =====
    private String currentGameOverPhrase = "";
    private final String[] gameOverPhrases = {
            "I gatti cadono sempre sui piedi... quasi sempre!",
            "Un piccolo riposino e ci riproviamo!",
            "La ciotola non scappa, ritenta!",
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
    private Rectangle nextLevelButton;
    private boolean mouseOverNextLevel = false;
    private boolean mousePressedNextLevel = false;

    private Rectangle winRestartButton;
    private boolean mouseOverWinRestart = false;
    private boolean mousePressedWinRestart = false;

    private Rectangle exitButton;
    private boolean mouseOverExit = false;
    private boolean mousePressedExit = false;

    // ===== CROCCANTINI =====
    public ArrayList<FallingTreat> treats = new ArrayList<>();
    public int treatsCollected = 0;

    public Level2Panel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(25, 28, 36));
        this.setDoubleBuffered(true);

        // Management del movimento del mouse per bottoni
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                mouseOverRetry = retryButton != null && retryButton.contains(p);
                mouseOverGameOverExit = gameOverExitButton != null && gameOverExitButton.contains(p);
                mouseOverNextLevel = nextLevelButton != null && nextLevelButton.contains(p);
                mouseOverWinRestart = winRestartButton != null && winRestartButton.contains(p);
                mouseOverExit = exitButton != null && exitButton.contains(p);
                repaint();
            }
        });

        // Management dei click del mouse sui bottoni
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (gameOver) {
                    if (retryButton != null && retryButton.contains(p)) mousePressedRetry = true;
                    if (gameOverExitButton != null && gameOverExitButton.contains(p)) mousePressedGameOverExit = true;
                }
                if (gameWon) {
                    if (nextLevelButton != null && nextLevelButton.contains(p)) mousePressedNextLevel = true;
                    if (winRestartButton != null && winRestartButton.contains(p)) mousePressedWinRestart = true;
                    if (exitButton != null && exitButton.contains(p)) mousePressedExit = true;
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

                if (mousePressedNextLevel && nextLevelButton != null && nextLevelButton.contains(p)) {
                    goToNextLevel();
                }
                if (mousePressedWinRestart && winRestartButton != null && winRestartButton.contains(p)) {
                    restartGame();
                }
                if (mousePressedExit && exitButton != null && exitButton.contains(p)) {
                    System.exit(0);
                }

                mousePressedNextLevel = false;
                mousePressedWinRestart = false;
                mousePressedRetry = false;
                mousePressedGameOverExit = false;
                mousePressedExit = false;
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
        // COLORI TETTO PER IL LIVELLO 2 (Ardesia scura)
        Color roofSlate = new Color(55, 65, 80);
        Color roofSlateDark = new Color(35, 42, 55);

        // Strada asfaltata in basso
        floorHazard = new Rectangle(0, screenHeight - 25, worldWidth, 25);

        // PIATTAFORME DEL LIVELLO 2
        platforms.clear();
        platforms.add(new Platform(0, 420, 240, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(380, 340, 200, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(730, 260, 260, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(1150, 380, 180, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(1480, 230, 280, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(1920, 340, 210, 30, roofSlate, roofSlateDark));
        platforms.add(new Platform(2280, 190, 290, 30, roofSlate, roofSlateDark));

        // Tetto finale
        platforms.add(new Platform(2780, 210, 240, 30, roofSlate, roofSlateDark));

        // Ciotola posizionata sopra l'ultimo tetto (Y = 210 - 45 = 165)
        bowlGoal = new Rectangle(2880, 165, 55, 45);

        cat = new Cat(this, keyH);
        cat.x = 50;
        cat.y = 420 - cat.height;
    }

    public void restartGame() {
        cat.x = 50;
        cat.y = 420 - cat.height;
        cat.velY = 0;
        cameraX = 0;
        gameOver = false;
        gameWon = false;

        treats.clear();
        treatsCollected = 0;
    }

    // ===== METODO PER PASSARE AL LIVELLO 3 (NOTTE TOSCANA) =====
    public void goToNextLevel() {
        if (gameThread != null) {
            gameThread = null; // Interrompe il thread del Livello 2
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            frame.getContentPane().removeAll();

            Level3Panel level3 = new Level3Panel();
            frame.add(level3);

            frame.revalidate();
            frame.repaint();

            level3.requestFocusInWindow();
            level3.startGameThread();
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

        if (cat.getBounds().intersects(bowlGoal)) {
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
    // ===== SFONDO PAESAGGIO TRAMONTO (SUNSET) ============
    // =======================================================
    private void drawSunsetSky(Graphics2D g2) {
        if (gameOver) {
            GradientPaint stormGrad = new GradientPaint(
                    0, 0, new Color(30, 35, 50),
                    0, screenHeight, new Color(70, 80, 105)
            );
            g2.setPaint(stormGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawSoftCloud(g2, 60 - (int) (cameraX * 0.08), 45, 1.3f, new Color(50, 55, 70, 230));
            drawSoftCloud(g2, 320 - (int) (cameraX * 0.08), 60, 1.1f, new Color(40, 45, 60, 220));
            drawSoftCloud(g2, 620 - (int) (cameraX * 0.08), 35, 1.4f, new Color(55, 60, 75, 240));
            drawSoftCloud(g2, 1000 - (int) (cameraX * 0.08), 50, 1.2f, new Color(45, 50, 65, 230));
        } else {
            GradientPaint sunsetGrad = new GradientPaint(
                    0, 0, new Color(80, 40, 95),
                    0, screenHeight, new Color(245, 130, 65)
            );
            g2.setPaint(sunsetGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            int sunX = 520 - (int) (cameraX * 0.05);
            int sunY = 160;

            float[] dists = {0.0f, 0.5f, 1.0f};
            Color[] colors = {
                    new Color(255, 240, 180, 255),
                    new Color(255, 160, 60, 190),
                    new Color(230, 90, 50, 0)
            };
            RadialGradientPaint sunGlow = new RadialGradientPaint(
                    sunX + 50, sunY + 50, 110, dists, colors
            );
            g2.setPaint(sunGlow);
            g2.fillOval(sunX - 60, sunY - 60, 220, 220);

            g2.setColor(new Color(255, 220, 140));
            g2.fillOval(sunX, sunY, 65, 65);

            Color sunsetCloud = new Color(230, 140, 160, 180);
            drawSoftCloud(g2, 80 - (int) (cameraX * 0.08), 60, 1.1f, sunsetCloud);
            drawSoftCloud(g2, 420 - (int) (cameraX * 0.08), 90, 0.9f, sunsetCloud);
            drawSoftCloud(g2, 850 - (int) (cameraX * 0.08), 55, 1.2f, sunsetCloud);
            drawSoftCloud(g2, 1300 - (int) (cameraX * 0.08), 75, 1.0f, sunsetCloud);
            drawSoftCloud(g2, 1850 - (int) (cameraX * 0.08), 65, 1.15f, sunsetCloud);
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

        g2.setColor(gameOver ? new Color(20, 25, 35, 120) : new Color(120, 50, 80, 80));
        g2.fillOval(x + 10, y + (int) (15 * scaleFactor), w - 20, (int) (20 * scaleFactor));
    }

    // =======================================================
    // ===== EDIFICI DEL LIVELLO 2: ARCHITETTURA MODERNA ====
    // =======================================================
    private void drawModernBuilding(Graphics2D g2, int worldX, int roofY, int width, int buildingType) {
        int x = worldX - cameraX;
        int floorY = screenHeight - 25;
        int wallY = roofY + 30;
        int height = floorY - wallY;

        if (x + width < -50 || x > screenWidth + 50) return;

        Color[][] palette = {
                {new Color(160, 85, 75), new Color(120, 60, 50), new Color(80, 35, 30)},
                {new Color(75, 95, 115), new Color(50, 65, 80), new Color(30, 40, 55)},
                {new Color(110, 120, 100), new Color(80, 90, 70), new Color(50, 60, 45)},
                {new Color(140, 110, 130), new Color(100, 75, 90), new Color(65, 45, 60)}
        };
        Color[] chosenColors = palette[buildingType % palette.length];

        g2.setColor(new Color(0, 0, 0, 45));
        g2.fillRect(x + width, wallY + 10, 12, height - 10);

        GradientPaint wallGrad = new GradientPaint(
                x, wallY, chosenColors[0],
                x, floorY, chosenColors[1]
        );
        g2.setPaint(wallGrad);
        g2.fillRect(x, wallY, width, height);

        g2.setColor(chosenColors[2]);
        for (int lineY = wallY + 20; lineY < floorY; lineY += 35) {
            g2.fillRect(x, lineY, width, 4);
        }

        g2.setColor(new Color(45, 55, 70));
        g2.fillRect(x - 6, wallY - 10, width + 12, 12);
        g2.setColor(new Color(75, 90, 110));
        g2.fillRect(x - 6, wallY - 10, width + 12, 3);

        int windowWidth = 24;
        int windowHeight = 46;
        int cols = Math.max(2, (width - 25) / (windowWidth + 18));
        int spacingX = (width - (cols * windowWidth)) / (cols + 1);

        for (int winY = wallY + 28; winY < floorY - 80; winY += windowHeight + 20) {
            for (int c = 0; c < cols; c++) {
                int winX = x + spacingX + c * (windowWidth + spacingX);

                g2.setColor(new Color(210, 190, 170));
                g2.fillRect(winX - 2, winY - 2, windowWidth + 4, windowHeight + 4);

                g2.setColor(new Color(255, 210, 110));
                g2.fillRect(winX, winY, windowWidth, windowHeight);

                g2.setColor(new Color(255, 140, 50, 100));
                g2.fillRect(winX, winY + windowHeight / 2, windowWidth, windowHeight / 2);

                g2.setColor(new Color(60, 50, 40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(winX, winY, windowWidth, windowHeight);
                g2.drawLine(winX + windowWidth / 2, winY, winX + windowWidth / 2, winY + windowHeight);
                g2.drawLine(winX, winY + windowHeight / 2, winX + windowWidth, winY + windowHeight / 2);
            }
        }

        int doorWidth = 34;
        int doorHeight = 60;
        int doorX = x + (width - doorWidth) / 2;
        int doorY = floorY - doorHeight;

        g2.setColor(new Color(40, 30, 25));
        g2.fillRect(doorX, doorY, doorWidth, doorHeight);
        g2.setColor(new Color(255, 180, 0));
        g2.fillOval(doorX + doorWidth - 8, doorY + doorHeight / 2, 4, 4);
    }

    private void drawBackgroundSkyline(Graphics2D g2) {
        g2.setColor(gameOver ? new Color(60, 70, 90, 160) : new Color(110, 50, 70, 150));
        int[] bgX = {0, 240, 520, 880, 1220, 1580, 1980, 2380, 2880, 3200};
        int[] bgH = {190, 230, 170, 250, 200, 260, 180, 240, 210, 190};

        for (int i = 0; i < bgX.length - 1; i++) {
            int bx = bgX[i] - (int) (cameraX * 0.3);
            int bw = bgX[i + 1] - bgX[i] - 25;
            int by = screenHeight - bgH[i];
            g2.fillRect(bx, by, bw, bgH[i]);

            g2.setColor(new Color(255, 200, 100, 70));
            for (int fx = bx + 8; fx < bx + bw - 8; fx += 16) {
                for (int fy = by + 20; fy < screenHeight - 40; fy += 28) {
                    g2.fillRect(fx, fy, 5, 8);
                }
            }
            g2.setColor(gameOver ? new Color(60, 70, 90, 160) : new Color(110, 50, 70, 150));
        }
    }

    // =======================================================
    // ===== STRADA ASFALTATA PER IL LIVELLO 2 ============
    // =======================================================
    private void drawRoadHazard(Graphics2D g2) {
        int roadY = floorHazard.y;
        int roadH = floorHazard.height;

        g2.setColor(new Color(40, 42, 50));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, roadH);

        g2.setColor(new Color(150, 155, 165));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, 4);

        g2.setColor(new Color(245, 180, 50));
        for (int i = 0; i < worldWidth; i += 60) {
            g2.fillRect(i - cameraX, roadY + 12, 35, 4);
        }
    }

    // =======================================================
    // ===== CIOTOLA DEL CIBO ================================
    // =======================================================
    private void drawRealisticBowl(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 4, y + h - 6, w + 8, 12);

        g2.setColor(new Color(170, 30, 30));
        g2.fillOval(x, y + h / 3, w, h * 2 / 3);
        g2.setColor(new Color(220, 50, 50));
        g2.fillOval(x + 1, y + h / 3 - 2, w - 2, h * 2 / 3 - 2);

        g2.setColor(new Color(255, 90, 80));
        g2.fillOval(x, y + 8, w, h / 3 + 4);

        g2.setColor(new Color(90, 35, 15));
        g2.fillOval(x + 3, y + 10, w - 6, h / 3);

        g2.setColor(new Color(130, 65, 25));
        g2.fillOval(x + 5, y + 6, w - 10, h / 3 + 2);

        Color kibble1 = new Color(160, 85, 35);
        Color kibble2 = new Color(210, 130, 45);

        g2.setColor(kibble1);
        g2.fillOval(x + 6, y + 5, 12, 8);
        g2.fillOval(x + w - 18, y + 5, 12, 8);
        g2.fillOval(x + 14, y + 2, 14, 9);
        g2.fillOval(x + 24, y + 3, 13, 8);

        g2.setColor(kibble2);
        g2.fillOval(x + 10, y + 6, 7, 5);
        g2.fillOval(x + 18, y + 3, 8, 5);
        g2.fillOval(x + 29, y + 5, 7, 5);

        g2.setColor(Color.WHITE);
        int pawX = x + w / 2;
        int pawY = y + h / 2 + 6;
        g2.fillOval(pawX - 4, pawY - 1, 8, 6);
        g2.fillOval(pawX - 5, pawY - 5, 3, 3);
        g2.fillOval(pawX - 1, pawY - 7, 3, 3);
        g2.fillOval(pawX + 3, pawY - 5, 3, 3);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(x + 6, y + h / 2 + 1, 6, 12);
    }

    // =======================================================
    // ===== INTERFACCIA HUD STILIZZATA (LIVELLO 2) ==========
    // =======================================================
    private void drawStylizedHUD(Graphics2D g2) {
        int panelX = 16;
        int panelY = 16;
        int panelW = 250;
        int panelH = 86;

        GradientPaint bgGrad = new GradientPaint(
                panelX, panelY, new Color(40, 25, 45, 200),
                panelX, panelY + panelH, new Color(20, 15, 30, 220)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setColor(new Color(245, 180, 140, 150));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(255, 220, 200));
        g2.drawString("LIVELLO 2: Tramonto", panelX + 14, panelY + 22);

        int progress = (int) Math.min(100, Math.max(0, cat.x / 28.5));
        int barX = panelX + 14;
        int barY = panelY + 31;
        int barW = 145;
        int barH = 12;

        g2.setColor(new Color(25, 15, 25, 220));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);

        int fillW = (int) (barW * (progress / 100.0));
        if (fillW > 0) {
            GradientPaint progressGrad = new GradientPaint(
                    barX, barY, new Color(245, 130, 60),
                    barX + barW, barY, new Color(255, 200, 80)
            );
            g2.setPaint(progressGrad);
            g2.fillRoundRect(barX, barY, fillW, barH, 8, 8);
        }

        g2.setColor(new Color(255, 235, 220));
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.drawString(progress + "%", barX + barW + 10, barY + 10);

        int badgeX = panelX + 14;
        int badgeY = panelY + 51;
        int badgeW = 222;
        int badgeH = 24;

        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
        g2.setColor(new Color(255, 180, 120, 90));
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

        // 1. CIELO TRAMONTO
        drawSunsetSky(g2);

        // 2. SKYLINE SFONDO
        drawBackgroundSkyline(g2);

        // 3. EDIFICI E CASE MODERNE
        drawModernBuilding(g2, 0, 390, 240, 0);
        drawModernBuilding(g2, 380, 310, 200, 1);
        drawModernBuilding(g2, 730, 230, 260, 2);
        drawModernBuilding(g2, 1150, 350, 180, 3);
        drawModernBuilding(g2, 1480, 200, 280, 0);
        drawModernBuilding(g2, 1920, 310, 210, 1);
        drawModernBuilding(g2, 2280, 160, 290, 2);
        drawModernBuilding(g2, 2780, 180, 240, 3);

        // 4. STRADA ASFALTATA
        drawRoadHazard(g2);

        // 5. TETTI / PIATTAFORME
        for (Platform p : platforms) {
            p.draw(g2, cameraX);
        }

        // 6. CROCCANTINI
        for (FallingTreat t : treats) {
            t.draw(g2, cameraX);
        }

        // 7. CIOTOLA FINALE
        int bowlScreenX = bowlGoal.x - cameraX;
        drawRealisticBowl(g2, bowlScreenX, bowlGoal.y, bowlGoal.width, bowlGoal.height);

        // 8. GATTO
        cat.draw(g2, cameraX);

        // 9. HUD STILIZZATO
        drawStylizedHUD(g2);

        long time = System.currentTimeMillis();

        // ==========================================
        // ===== SCHERMATA DI VITTORIA (GAME WON) =====
        // ==========================================
        if (gameWon) {
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            Color[] colors = {Color.YELLOW, Color.PINK, Color.CYAN, Color.ORANGE, Color.GREEN, Color.MAGENTA};
            for (int i = 0; i < 150; i++) {
                int x = (i * 47 + i * i) % screenWidth;
                int y = (int) ((time / 8.0 + i * 35) % (screenHeight + 300)) - 150;
                g2.setColor(colors[i % colors.length]);
                g2.fillOval(x, y, 8, 8);
            }

            int titleSize = 34 + (int) (Math.sin(time * 0.006) * 3);
            g2.setFont(new Font("Arial", Font.BOLD, titleSize));
            String title = "LIVELLO 2 COMPLETATO!";
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleX = (screenWidth - fmTitle.stringWidth(title)) / 2;

            g2.setColor(new Color(80, 40, 0, 180));
            g2.drawString(title, titleX + 4, 74);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(title, titleX, 70);

            String message = "Il gattino ha conquistato anche i tetti al tramonto!";
            g2.setFont(new Font("Arial", Font.BOLD, 19));
            FontMetrics fmMsg = g2.getFontMetrics();
            int msgX = (screenWidth - fmMsg.stringWidth(message)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(message, msgX, 115);

            String rankText;
            Color rankColor;
            if (treatsCollected >= 9) {
                rankText = "RANGO: RE DEI TETTI";
                rankColor = new Color(255, 215, 0);
            } else if (treatsCollected >= 4) {
                rankText = "RANGO: CACCIATORE NOTTURNO";
                rankColor = new Color(100, 230, 120);
            } else {
                rankText = "RANGO: GATTO PIGRO";
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

            // BOTTONI ABBASSATI NELLA PARTE INFERIORE DELLO SCHERMO
            // 1. BOTTONE PRIMARIO: PROSSIMO LIVELLO (LIVELLO 3)
            int nextW = 250, nextH = 45;
            int nextX = (screenWidth - nextW) / 2, nextY = 380;
            nextLevelButton = new Rectangle(nextX, nextY, nextW, nextH);
            int nextOffset = mousePressedNextLevel ? 3 : 0;

            g2.setColor(mousePressedNextLevel ? new Color(30, 160, 80) : mouseOverNextLevel ? new Color(70, 220, 120) : new Color(45, 190, 100));
            g2.fillRoundRect(nextX, nextY + nextOffset, nextW, nextH, 25, 25);

            String nextText = "PROSSIMO LIVELLO";
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fmNext = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(nextText, nextX + (nextW - fmNext.stringWidth(nextText)) / 2, nextY + 29 + nextOffset);

            // 2. BOTTONE SECONDARIO: RICOMINCIA LIVELLO 2
            int playW = 180, playH = 38;
            int playX = (screenWidth - playW) / 2, playY = 440;
            winRestartButton = new Rectangle(playX, playY, playW, playH);
            int playOffset = mousePressedWinRestart ? 3 : 0;

            g2.setColor(mousePressedWinRestart ? new Color(220, 150, 0) : mouseOverWinRestart ? new Color(255, 220, 50) : new Color(255, 170, 0));
            g2.fillRoundRect(playX, playY + playOffset, playW, playH, 20, 20);

            String playText = "RICOMINCIA";
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fmPlay = g2.getFontMetrics();
            g2.setColor(new Color(40, 40, 60));
            g2.drawString(playText, playX + (playW - fmPlay.stringWidth(playText)) / 2, playY + 24 + playOffset);

            // 3. BOTTONE TERZIARIO: ESCI
            int exitW = 130, exitH = 35;
            int exitX = (screenWidth - exitW) / 2, exitY = 490;
            exitButton = new Rectangle(exitX, exitY, exitW, exitH);
            int exitOffset = mousePressedExit ? 3 : 0;

            g2.setColor(mousePressedExit ? new Color(180, 70, 70) : mouseOverExit ? new Color(255, 120, 120) : new Color(220, 90, 90));
            g2.fillRoundRect(exitX, exitY + exitOffset, exitW, exitH, 20, 20);

            String exitText = "ESCI";
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fmExit = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(exitText, exitX + (exitW - fmExit.stringWidth(exitText)) / 2, exitY + 23 + exitOffset);

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

            int exitX = panelX + (panelW / 2) + 15;
            gameOverExitButton = new Rectangle(exitX, btnY, btnW, btnH);
            int exitOffset = mousePressedGameOverExit ? 3 : 0;

            g2.setColor(mousePressedGameOverExit ? new Color(170, 60, 60) : mouseOverGameOverExit ? new Color(240, 110, 110) : new Color(210, 80, 80));
            g2.fillRoundRect(exitX, btnY + exitOffset, btnW, btnH, 25, 25);

            String exitText = "ESCI";
            g2.setFont(new Font("Arial", Font.BOLD, 19));
            FontMetrics fmExit = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(exitText, exitX + (btnW - fmExit.stringWidth(exitText)) / 2, btnY + 32 + exitOffset);
        }

        g2.dispose();
    }
}