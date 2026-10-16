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
        Color mogano1 = new Color(160, 60, 35);
        Color mogano2 = new Color(90, 25, 12);
        Color parquet1 = new Color(181, 101, 29);
        Color parquet2 = new Color(117, 59, 12);

        floorHazard = new Rectangle(0, screenHeight - 20, worldWidth, 20);

        platforms.add(new Platform(0, screenHeight - 100, 300, 80, parquet1, parquet2));
        platforms.add(new Platform(380, 420, 150, 20, mogano1, mogano2));
        platforms.add(new Platform(600, 350, 140, 20, mogano1, mogano2));
        platforms.add(new Platform(810, 420, 120, 20, mogano1, mogano2));

        platforms.add(new Platform(1000, 380, 250, 20, parquet1, parquet2));

        platforms.add(new Platform(1320, 320, 130, 20, mogano1, mogano2));
        platforms.add(new Platform(1520, 260, 130, 20, mogano1, mogano2));
        platforms.add(new Platform(1720, 340, 140, 20, mogano1, mogano2));

        platforms.add(new Platform(1950, 380, 300, 20, parquet1, parquet2));

        platforms.add(new Platform(2320, 320, 120, 20, mogano1, mogano2));
        platforms.add(new Platform(2500, 240, 130, 20, mogano1, mogano2));

        platforms.add(new Platform(2700, 220, 250, 100, parquet1, parquet2));
        bowlGoal = new Rectangle(2850, 180, 40, 40);

        cat = new Cat(this, keyH);
        cat.x = 50;
        cat.y = screenHeight - 160;
    }

    public void restartGame() {
        cat.x = 50;
        cat.y = screenHeight - 160;
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

        g2.setColor(new Color(220, 40, 30));
        g2.fillRect(floorHazard.x - cameraX, floorHazard.y, floorHazard.width, floorHazard.height);

        g2.setColor(new Color(255, 180, 0));
        for (int i = 0; i < worldWidth; i += 40) {
            g2.fillRect(i - cameraX, floorHazard.y, 20, 5);
        }

        for (Platform p : platforms) {
            p.draw(g2, cameraX);
        }

        for (FallingTreat t : treats) {
            t.draw(g2, cameraX);
        }

        int bowlScreenX = bowlGoal.x - cameraX;
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(bowlScreenX, bowlGoal.y + 15, bowlGoal.width, 20);
        g2.setColor(Color.YELLOW);
        g2.fillOval(bowlScreenX + 5, bowlGoal.y + 10, bowlGoal.width - 10, 12);

        cat.draw(g2, cameraX);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("CORRI IN AVANTI VERSO DESTRA! ->", 20, 30);
        g2.drawString("Avanzamento: " + (int)(cat.x / 28.5) + "%", 20, 50);
        g2.drawString("Croccantini: " + treatsCollected, 20, 70);

        if (treatMessageTimer > 0) {
            g2.setColor(new Color(255, 200, 50));
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.drawString("+1 croccantino!", 20, 100);
        }

        if (gameWon) {
            g2.setColor(new Color(0, 0, 0, 190));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            g2.setColor(Color.GREEN);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.drawString("VITTORIA! TRAGUARDO RAGGIUNTO!", 60, screenHeight / 2 - 20);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Premi [SPAZIO] per Giocare di nuovo", 200, screenHeight / 2 + 40);

        } else if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 190));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.drawString("SEI CADUTA SUL PAVIMENTO!", 140, screenHeight / 2 - 20);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Premi [SPAZIO] per Riprovare", 230, screenHeight / 2 + 40);
        }

        g2.dispose();
    }
}
