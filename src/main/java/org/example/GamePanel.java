package org.example;

import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {

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

    // CROCCANTINI
    public ArrayList<FallingTreat> treats = new ArrayList<>();
    public int treatsCollected = 0;
    public int treatMessageTimer = 0;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(25, 28, 36));
        this.setDoubleBuffered(true);

        keyH = new KeyHandler(this);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        setupLevel();
    }

    public void setupLevel() {

        Color roofRed = new Color(180, 50, 50);
        Color roofDark = new Color(120, 25, 25);

        floorHazard = new Rectangle(0, screenHeight - 20, worldWidth, 20);

        // TETTI DELLE CASE

        platforms.clear();

        platforms.clear();

        platforms.add(new Platform(0,    450, 220, 30, roofRed, roofDark));

        platforms.add(new Platform(370,  390, 180, 30, roofRed, roofDark));

        platforms.add(new Platform(700,  300, 240, 30, roofRed, roofDark));

        platforms.add(new Platform(1120, 410, 170, 30, roofRed, roofDark));

        platforms.add(new Platform(1450, 250, 260, 30, roofRed, roofDark));

        platforms.add(new Platform(1900, 360, 190, 30, roofRed, roofDark));

        platforms.add(new Platform(2250, 200, 280, 30, roofRed, roofDark));

        platforms.add(new Platform(2750, 140, 250, 30, roofRed, roofDark));

        bowlGoal = new Rectangle(2700, 130, 50, 50);

        cat = new Cat(this, keyH);
        cat.x = 50;
        cat.y = 450 - cat.height;
    }

    private void drawHouse(Graphics2D g2, int worldX, int roofY, int width) {

        int x = worldX - cameraX;

        int houseHeight = 120;

        // muro
        Color[] colors = {
                new Color(255,220,200),
                new Color(255,255,180),
                new Color(210,255,210),
                new Color(210,230,255),
                new Color(255,210,240),
                new Color(240,220,170)
        };

        Color wallColor =
                colors[Math.abs(worldX / 200) % colors.length];

        g2.setColor(wallColor);
        int floorY = screenHeight - 20;

        g2.fillRect(
                x,
                roofY + 30,
                width,
                floorY - (roofY + 30)
        );

        // tetto triangolare
        g2.setColor(new Color(190, 50, 50));

        int[] px = {
                x,
                x + width / 2,
                x + width
        };

        int[] py = {
                roofY + 30,
                roofY - 30,
                roofY + 30
        };

        g2.fillPolygon(px, py, 3);

        // finestre
        g2.setColor(new Color(255, 240, 130));

        g2.fillRect(x + 30, roofY + 70, 40, 40);
        g2.fillRect(x + width - 70, roofY + 70, 40, 40);

        // porta

        g2.setColor(new Color(120, 70, 40));

        g2.fillRect(
                x + width / 2 - 20,
                roofY + 170,
                40,
                80
        );
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
        treatMessageTimer = 0;
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
            gameOver = true;
        }

        if (cat.getBounds().intersects(bowlGoal)) {
            gameWon = true;
        }

        // GENERA CROCCANTINI CASUALI
        if (Math.random() < 0.02) {
            float spawnX = cameraX + (float)(Math.random() * screenWidth);
            treats.add(new FallingTreat(spawnX, -40));
        }

        // AGGIORNA CROCCANTINI
        for (int i = 0; i < treats.size(); i++) {
            FallingTreat t = treats.get(i);
            t.update();

            if (t.y > screenHeight + 100) {
                treats.remove(i);
                i--;
                continue;
            }

            // RACCOLTA CROCCANTINO
            if (cat.getBounds().intersects(t.getBounds())) {
                treatsCollected++;
                treatMessageTimer = 60; // 1 secondo
                treats.remove(i);
                i--;
                continue;
            }
        }

        if (treatMessageTimer > 0) treatMessageTimer--;
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // =====================================
        // CIELO
        // =====================================

        g2.setColor(new Color(135, 206, 235));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // Sole

        g2.setColor(Color.YELLOW);
        g2.fillOval(580, 40, 100, 100);

        // Nuvole

        g2.setColor(Color.WHITE);

        g2.fillOval(80, 70, 80, 40);
        g2.fillOval(120, 55, 90, 55);
        g2.fillOval(170, 70, 80, 40);

        g2.fillOval(420, 90, 80, 40);
        g2.fillOval(460, 70, 90, 55);
        g2.fillOval(520, 90, 80, 40);

        // =====================================
        // CASE DI SFONDO
        // =====================================

        drawHouse(g2, 0,    420, 220);

        drawHouse(g2, 370,  360, 180);

        drawHouse(g2, 700,  270, 240);

        drawHouse(g2, 1120, 380, 170);

        drawHouse(g2, 1450, 220, 260);

        drawHouse(g2, 1900, 330, 190);

        drawHouse(g2, 2250, 170, 280);

        drawHouse(g2, 2750, 110, 250);

        // =====================================
        // LAVA / PAVIMENTO PERICOLOSO
        // =====================================

        g2.setColor(new Color(220, 40, 30));
        g2.fillRect(
                floorHazard.x - cameraX,
                floorHazard.y,
                floorHazard.width,
                floorHazard.height
        );

        g2.setColor(new Color(255, 180, 0));

        for (int i = 0; i < worldWidth; i += 40) {
            g2.fillRect(
                    i - cameraX,
                    floorHazard.y,
                    20,
                    5
            );
        }

        // =====================================
        // TETTI / PIATTAFORME
        // =====================================

        for (Platform p : platforms) {
            p.draw(g2, cameraX);
        }

        // =====================================
        // CROCCANTINI
        // =====================================

        for (FallingTreat t : treats) {
            t.draw(g2, cameraX);
        }

        // =====================================
        // CIOTOLA FINALE
        // =====================================

        int bowlScreenX = bowlGoal.x - cameraX;

        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(
                bowlScreenX,
                bowlGoal.y + 15,
                bowlGoal.width,
                20
        );

        g2.setColor(Color.YELLOW);
        g2.fillOval(
                bowlScreenX + 5,
                bowlGoal.y + 10,
                bowlGoal.width - 10,
                12
        );

        // =====================================
        // GATTO
        // =====================================

        cat.draw(g2, cameraX);

        // =====================================
        // HUD
        // =====================================

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));

        g2.drawString(
                "Vai verso destra!",
                20,
                30
        );

        g2.drawString(
                "Avanzamento: " + (int)(cat.x / 28.5) + "%",
                20,
                55
        );

        g2.drawString(
                "Croccantini: " + treatsCollected,
                20,
                80
        );

        // =====================================
        // MESSAGGIO CROCCANTINO
        // =====================================

        if (treatMessageTimer > 0) {

            g2.setColor(new Color(255, 150, 0));

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    22
            ));

            g2.drawString(
                    "+1 Croccantino!",
                    20,
                    115
            );
        }

        // =====================================
        // VITTORIA
        // =====================================

        if (gameWon) {

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            g2.setColor(Color.GREEN);

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    36
            ));

            g2.drawString(
                    "HAI RAGGIUNTO LA CIOTOLA!",
                    90,
                    screenHeight / 2
            );

            g2.setColor(Color.WHITE);

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    20
            ));

            g2.drawString(
                    "Premi SPAZIO per ricominciare",
                    180,
                    screenHeight / 2 + 50
            );
        }

        // =====================================
        // GAME OVER
        // =====================================

        else if (gameOver) {

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            g2.setColor(Color.RED);

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    36
            ));

            g2.drawString(
                    "IL GATTO E' CADUTO!",
                    150,
                    screenHeight / 2
            );

            g2.setColor(Color.WHITE);

            g2.setFont(new Font(
                    "Arial",
                    Font.BOLD,
                    20
            ));

            g2.drawString(
                    "Premi SPAZIO per riprovare",
                    190,
                    screenHeight / 2 + 50
            );
        }

        g2.dispose();
    }
}
