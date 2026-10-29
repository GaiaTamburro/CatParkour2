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

public class GamePanel extends JPanel implements Runnable, Restartable {

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
            "Niente paura, i gatti hanno 9 vite!"
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

    public GamePanel() {
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
        Color roofRed = new Color(175, 55, 45);
        Color roofDark = new Color(110, 30, 25);

        // Strada asfaltata in basso
        floorHazard = new Rectangle(0, screenHeight - 25, worldWidth, 25);

        // TETTI DELLE CASE / PIATTAFORME
        platforms.clear();
        platforms.add(new Platform(0, 450, 220, 30, roofRed, roofDark));
        platforms.add(new Platform(370, 390, 180, 30, roofRed, roofDark));
        platforms.add(new Platform(700, 300, 240, 30, roofRed, roofDark));
        platforms.add(new Platform(1120, 410, 170, 30, roofRed, roofDark));
        platforms.add(new Platform(1450, 250, 260, 30, roofRed, roofDark));
        platforms.add(new Platform(1900, 360, 190, 30, roofRed, roofDark));
        platforms.add(new Platform(2250, 200, 280, 30, roofRed, roofDark));

        // Tetto finale ad altezza bilanciata (Y = 220)
        platforms.add(new Platform(2750, 220, 250, 30, roofRed, roofDark));

        // Ciotola posizionata sopra il tetto (Y = 220 - 45 = 175)
        bowlGoal = new Rectangle(2850, 175, 55, 45);

        cat = new Cat(this, keyH);
        cat.x = 50;
        cat.y = 450 - cat.height;
    }

    public void restartGame() {
        cat.x = 50;
        cat.y = 450 - cat.height;
        cat.velY = 0;
        cameraX = 0;
        gameOver = false;
        gameWon = false;

        treats.clear();
        treatsCollected = 0;
    }

    // ===== METODO PER PASSARE A LEVEL 2 PANEL =====
    public void goToNextLevel() {
        if (gameThread != null) {
            gameThread = null; // Interrompe il thread del livello attuale
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            frame.getContentPane().removeAll();

            Level2Panel level2 = new Level2Panel();
            frame.add(level2);

            frame.revalidate();
            frame.repaint();

            level2.requestFocusInWindow();
            level2.startGameThread();
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
    // ===== RENDERING AVANZATO: CIELO, SOLE E NUVOLE =====
    // =======================================================
    private void drawRealisticSky(Graphics2D g2) {
        if (gameOver) {
            // CIELO TEMPORALESCO/NUVOLOSO IN CASO DI GAME OVER
            GradientPaint stormGrad = new GradientPaint(
                    0, 0, new Color(30, 35, 50),
                    0, screenHeight, new Color(70, 80, 105)
            );
            g2.setPaint(stormGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            // Nuvole grigie scure
            drawSoftCloud(g2, 60 - (int) (cameraX * 0.08), 45, 1.3f, new Color(50, 55, 70, 230));
            drawSoftCloud(g2, 320 - (int) (cameraX * 0.08), 60, 1.1f, new Color(40, 45, 60, 220));
            drawSoftCloud(g2, 620 - (int) (cameraX * 0.08), 35, 1.4f, new Color(55, 60, 75, 240));
            drawSoftCloud(g2, 1000 - (int) (cameraX * 0.08), 50, 1.2f, new Color(45, 50, 65, 230));
            drawSoftCloud(g2, 1500 - (int) (cameraX * 0.08), 40, 1.3f, new Color(50, 55, 70, 230));
            drawSoftCloud(g2, 2000 - (int) (cameraX * 0.08), 65, 1.1f, new Color(40, 45, 60, 220));
        } else {
            // CIELO SERENO E SOLEGGIATO DURANTE IL GIOCO
            GradientPaint skyGrad = new GradientPaint(
                    0, 0, new Color(70, 130, 220),
                    0, screenHeight, new Color(185, 225, 250)
            );
            g2.setPaint(skyGrad);
            g2.fillRect(0, 0, screenWidth, screenHeight);

            int sunX = 580 - (int) (cameraX * 0.05);
            int sunY = 50;
            float[] dists = {0.0f, 0.4f, 1.0f};
            Color[] colors = {
                    new Color(255, 255, 240, 255),
                    new Color(255, 220, 120, 180),
                    new Color(255, 180, 50, 0)
            };
            RadialGradientPaint sunGlow = new RadialGradientPaint(
                    sunX + 40, sunY + 40, 80, dists, colors
            );
            g2.setPaint(sunGlow);
            g2.fillOval(sunX - 40, sunY - 40, 160, 160);

            g2.setColor(new Color(255, 253, 220));
            g2.fillOval(sunX + 15, sunY + 15, 50, 50);

            Color cloudColor = new Color(255, 255, 255, 210);
            drawSoftCloud(g2, 60 - (int) (cameraX * 0.08), 65, 1.0f, cloudColor);
            drawSoftCloud(g2, 380 - (int) (cameraX * 0.08), 85, 0.85f, cloudColor);
            drawSoftCloud(g2, 680 - (int) (cameraX * 0.08), 50, 0.95f, cloudColor);
            drawSoftCloud(g2, 1100 - (int) (cameraX * 0.08), 75, 0.9f, cloudColor);
            drawSoftCloud(g2, 1600 - (int) (cameraX * 0.08), 60, 1.0f, cloudColor);
            drawSoftCloud(g2, 2100 - (int) (cameraX * 0.08), 90, 0.8f, cloudColor);
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

        g2.setColor(gameOver ? new Color(20, 25, 35, 120) : new Color(200, 215, 235, 90));
        g2.fillOval(x + 10, y + (int) (15 * scaleFactor), w - 20, (int) (20 * scaleFactor));
    }

    // =======================================================
    // ===== RENDERING AVANZATO: PALAZZI REALISTICI ======
    // =======================================================
    private void drawRealisticBuilding(Graphics2D g2, int worldX, int roofY, int width, int buildingType) {
        int x = worldX - cameraX;
        int floorY = screenHeight - 25;
        int wallY = roofY + 30;
        int height = floorY - wallY;

        if (x + width < -50 || x > screenWidth + 50) return;

        Color[][] palette = {
                {new Color(215, 175, 140), new Color(175, 135, 100), new Color(130, 95, 65)},
                {new Color(185, 195, 205), new Color(145, 155, 168), new Color(105, 115, 128)},
                {new Color(230, 210, 165), new Color(190, 170, 125), new Color(140, 120, 80)},
                {new Color(200, 170, 170), new Color(160, 130, 130), new Color(120, 90, 90)}
        };
        Color[] chosenColors = palette[buildingType % palette.length];

        // Ombra laterale
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRect(x + width, wallY + 10, 12, height - 10);

        // Facciata
        GradientPaint wallGrad = new GradientPaint(
                x, wallY, chosenColors[0],
                x, floorY, chosenColors[1]
        );
        g2.setPaint(wallGrad);
        g2.fillRect(x, wallY, width, height);

        // Mattoncini
        g2.setColor(new Color(0, 0, 0, 18));
        for (int lineY = wallY + 12; lineY < floorY; lineY += 14) {
            g2.drawLine(x, lineY, x + width - 1, lineY);
        }

        // Angolari in pietra
        g2.setColor(chosenColors[2]);
        for (int qY = wallY; qY < floorY; qY += 20) {
            g2.fillRect(x, qY, 8, 10);
            g2.fillRect(x + width - 8, qY, 8, 10);
        }

        // Cornicione tetto
        g2.setColor(chosenColors[2]);
        g2.fillRect(x - 4, wallY - 6, width + 8, 8);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.fillRect(x - 4, wallY - 6, width + 8, 2);

        // Tetto triangolare
        g2.setColor(new Color(150, 45, 35));
        int[] px = {x - 6, x + width / 2, x + width + 6};
        int[] py = {wallY - 6, roofY - 20, wallY - 6};
        g2.fillPolygon(px, py, 3);
        g2.setColor(new Color(90, 25, 20));
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(px, py, 3);

        // Finestre
        int windowWidth = 28;
        int windowHeight = 42;
        int cols = Math.max(2, (width - 30) / (windowWidth + 20));
        int spacingX = (width - (cols * windowWidth)) / (cols + 1);

        for (int winY = wallY + 30; winY < floorY - 90; winY += windowHeight + 25) {
            for (int c = 0; c < cols; c++) {
                int winX = x + spacingX + c * (windowWidth + spacingX);

                g2.setColor(chosenColors[2]);
                g2.fillRect(winX - 3, winY - 3, windowWidth + 6, windowHeight + 6);

                g2.setColor(new Color(230, 230, 230));
                g2.fillRect(winX - 5, winY + windowHeight, windowWidth + 10, 5);
                g2.setColor(Color.GRAY);
                g2.drawRect(winX - 5, winY + windowHeight, windowWidth + 10, 5);

                g2.setColor(new Color(40, 75, 110));
                g2.fillRect(winX, winY, windowWidth, windowHeight);

                g2.setColor(new Color(255, 255, 255, 90));
                int[] refX = {winX + 4, winX + 16, winX + 8, winX + 2};
                int[] refY = {winY + 2, winY + 2, winY + windowHeight - 2, winY + windowHeight - 2};
                g2.fillPolygon(refX, refY, 4);

                g2.setColor(new Color(240, 240, 240));
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(winX, winY, windowWidth, windowHeight);
                g2.drawLine(winX + windowWidth / 2, winY, winX + windowWidth / 2, winY + windowHeight);
                g2.drawLine(winX, winY + windowHeight / 3, winX + windowWidth, winY + windowHeight / 3);

                g2.setColor(new Color(60, 95, 60));
                g2.fillRect(winX - 11, winY, 8, windowHeight);
                g2.fillRect(winX + windowWidth + 3, winY, 8, windowHeight);
            }
        }

        // Porta
        int doorWidth = 36;
        int doorHeight = 65;
        int doorX = x + (width - doorWidth) / 2;
        int doorY = floorY - doorHeight;

        g2.setColor(chosenColors[2]);
        g2.fillRect(doorX - 4, doorY - 4, doorWidth + 8, doorHeight + 4);

        g2.setColor(new Color(110, 55, 25));
        g2.fillRect(doorX, doorY, doorWidth, doorHeight);
        g2.setColor(new Color(70, 35, 15));
        g2.drawRect(doorX + 3, doorY + 4, (doorWidth / 2) - 5, doorHeight - 8);
        g2.drawRect(doorX + (doorWidth / 2) + 2, doorY + 4, (doorWidth / 2) - 5, doorHeight - 8);

        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(doorX + doorWidth - 8, doorY + doorHeight / 2, 4, 4);

        g2.setColor(new Color(180, 180, 180));
        g2.fillRect(doorX - 8, floorY - 4, doorWidth + 16, 4);

        // Pluviale
        g2.setColor(new Color(130, 135, 140));
        g2.fillRect(x + 4, wallY, 4, height);
        g2.setColor(new Color(80, 85, 90));
        g2.drawRect(x + 4, wallY, 4, height);
    }

    private void drawBackgroundSkyline(Graphics2D g2) {
        g2.setColor(gameOver ? new Color(60, 70, 90, 160) : new Color(140, 175, 210, 140));
        int[] bgX = {0, 250, 500, 850, 1200, 1550, 1950, 2350, 2850, 3200};
        int[] bgH = {180, 220, 160, 240, 190, 250, 170, 230, 200, 180};

        for (int i = 0; i < bgX.length - 1; i++) {
            int bx = bgX[i] - (int) (cameraX * 0.3);
            int bw = bgX[i + 1] - bgX[i] - 30;
            int by = screenHeight - bgH[i];
            g2.fillRect(bx, by, bw, bgH[i]);

            g2.setColor(gameOver ? new Color(255, 220, 130, 40) : new Color(255, 245, 200, 80));
            for (int fx = bx + 10; fx < bx + bw - 10; fx += 18) {
                for (int fy = by + 15; fy < screenHeight - 40; fy += 25) {
                    g2.fillRect(fx, fy, 6, 10);
                }
            }
            g2.setColor(gameOver ? new Color(60, 70, 90, 160) : new Color(140, 175, 210, 140));
        }
    }

    // =======================================================
    // ===== RENDERING STRADA URBANA NORMALE (ASFALTO) ======
    // =======================================================
    private void drawRoadHazard(Graphics2D g2) {
        int roadY = floorHazard.y;
        int roadH = floorHazard.height;

        // 1. Asfalto scuro stradale
        g2.setColor(new Color(45, 48, 55));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, roadH);

        // 2. Cordolo / Marciapiede superiore in grigio chiaro
        g2.setColor(new Color(160, 165, 175));
        g2.fillRect(floorHazard.x - cameraX, roadY, floorHazard.width, 4);

        // Segnaletica per sezioni marciapiede
        g2.setColor(new Color(100, 105, 115));
        for (int i = 0; i < worldWidth; i += 30) {
            g2.fillRect(i - cameraX, roadY, 2, 4);
        }

        // 3. Strisce pedonali / Segnaletica orizzontale al centro strada
        g2.setColor(new Color(245, 210, 60));
        for (int i = 0; i < worldWidth; i += 60) {
            g2.fillRect(i - cameraX, roadY + 12, 35, 4);
        }

        // Texture granulare asfalto
        g2.setColor(new Color(255, 255, 255, 15));
        for (int i = 0; i < worldWidth; i += 18) {
            g2.fillRect(i - cameraX, roadY + 6, 4, 2);
            g2.fillRect(i + 8 - cameraX, roadY + 18, 5, 2);
        }
    }

    // =======================================================
    // ===== DISEGNO DETTAGLIATO DELLA CIOTOLA DI CIBO =====
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
    // ===== RENDERING INTERFACCIA UTENTE (HUD ARMONIOSO) ===
    // =======================================================
    private void drawStylizedHUD(Graphics2D g2) {
        int panelX = 16;
        int panelY = 16;
        int panelW = 250;
        int panelH = 86;

        GradientPaint bgGrad = new GradientPaint(
                panelX, panelY, new Color(30, 42, 60, 190),
                panelX, panelY + panelH, new Color(18, 25, 38, 210)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setColor(new Color(180, 215, 245, 140));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.setColor(new Color(220, 235, 250));
        g2.drawString("LIVELLO 1: Mattina", panelX + 14, panelY + 22);

        int progress = (int) Math.min(100, Math.max(0, cat.x / 28.5));
        int barX = panelX + 14;
        int barY = panelY + 31;
        int barW = 145;
        int barH = 12;

        g2.setColor(new Color(20, 28, 40, 220));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);

        int fillW = (int) (barW * (progress / 100.0));
        if (fillW > 0) {
            GradientPaint progressGrad = new GradientPaint(
                    barX, barY, new Color(45, 205, 160),
                    barX + barW, barY, new Color(80, 235, 200)
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

        // =====================================
        // 1. CIELO REALISTICO E SOLE / TEMPESTA
        // =====================================
        drawRealisticSky(g2);

        // =====================================
        // 2. SKYLINE E PALAZZI DI SFONDO
        // =====================================
        drawBackgroundSkyline(g2);

        // =====================================
        // 3. PALAZZI E FABBRICATI PRINCIPALI
        // =====================================
        drawRealisticBuilding(g2, 0, 420, 220, 0);
        drawRealisticBuilding(g2, 370, 360, 180, 1);
        drawRealisticBuilding(g2, 700, 270, 240, 2);
        drawRealisticBuilding(g2, 1120, 380, 170, 3);
        drawRealisticBuilding(g2, 1450, 220, 260, 0);
        drawRealisticBuilding(g2, 1900, 330, 190, 1);
        drawRealisticBuilding(g2, 2250, 170, 280, 2);
        drawRealisticBuilding(g2, 2750, 190, 250, 3);

        // =====================================
        // 4. STRADA URBANA NORMALE (ASFALTO)
        // =====================================
        drawRoadHazard(g2);

        // =====================================
        // 5. TETTI / PIATTAFORME
        // =====================================
        for (Platform p : platforms) {
            p.draw(g2, cameraX);
        }

        // =====================================
        // 6. CROCCANTINI CADENTI
        // =====================================
        for (FallingTreat t : treats) {
            t.draw(g2, cameraX);
        }

        // =====================================
        // 7. CIOTOLA FINALE APPOGGIATA SUL TETTO
        // =====================================
        int bowlScreenX = bowlGoal.x - cameraX;
        drawRealisticBowl(g2, bowlScreenX, bowlGoal.y, bowlGoal.width, bowlGoal.height);

        // =====================================
        // 8. GATTO
        // =====================================
        cat.draw(g2, cameraX);

        // =====================================
        // 9. HUD / INTERFACCIA UTENTE
        // =====================================
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

            int titleSize = 36 + (int) (Math.sin(time * 0.006) * 3);
            g2.setFont(new Font("Arial", Font.BOLD, titleSize));
            String title = "CIOTOLA CONQUISTATA!";
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleX = (screenWidth - fmTitle.stringWidth(title)) / 2;

            g2.setColor(new Color(80, 40, 0, 180));
            g2.drawString(title, titleX + 4, 74);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(title, titleX, 70);

            String message = "Il gattino ha finalmente raggiunto la ciotola!";
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fmMsg = g2.getFontMetrics();
            int msgX = (screenWidth - fmMsg.stringWidth(message)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(message, msgX, 115);

            String rankText;
            Color rankColor;
            if (treatsCollected >= 9) {
                rankText = "RANGO: LEGGENDA FELINA";
                rankColor = new Color(255, 215, 0);
            } else if (treatsCollected >= 4) {
                rankText = "RANGO: CACCIATORE PROVETTO";
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

            // ANIMAZIONE DEL GATTO CHE SALTELLA LIBERO AL CENTRO
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
            // 1. BOTTONE PRIMARIO: PROSSIMO LIVELLO
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

            // 2. BOTTONE SECONDARIO: RICOMINCIA
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

            // PIOGGIA BATTENTE
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