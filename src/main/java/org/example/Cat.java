package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JPanel;
public class Cat {

    public float x, y;
    public int width, height;
    public float velX = 0;
    public float velY = 0;
    public boolean isGrounded = false;

    public float speed = 4.8f;
    public float gravity = 0.48f;
    public float jumpStrength = -13f;

    private JPanel gp;
    private KeyHandler keyH;

    // 🔥 UNICA MODIFICA: immagine del gatto
    private BufferedImage catImage;

    public Cat(JPanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;
        //this.width = gp.tileSize;
        //this.height = gp.tileSize;
        this.height = 80;
        this.width = 104;

        // 🔥 UNICA MODIFICA: carica il nuovo sprite
        try {
            catImage = ImageIO.read(getClass().getResource("/cat_walk2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    public void update(ArrayList<Platform> platforms) {

        // Movimento orizzontale
        if (keyH.leftPressed) {
            velX = -speed;
        } else if (keyH.rightPressed) {
            velX = speed;
        } else {
            velX = 0;
        }

        // Salto
        if ((keyH.spacePressed || keyH.upPressed) && isGrounded) {
            velY = jumpStrength;
            isGrounded = false;
        }

        // Gravità (IDENTICA A PRIMA)
        velY += gravity;

        float nextX = x + velX;
        float nextY = y + velY;

        Rectangle futureBoundsX = new Rectangle((int)nextX, (int)y, width, height);
        Rectangle futureBoundsY = new Rectangle((int)x, (int)nextY, width, height);

        // Collisione orizzontale
        for (Platform p : platforms) {
            if (futureBoundsX.intersects(p.getBounds())) {
                if (velX > 0) nextX = p.x - width;
                if (velX < 0) nextX = p.x + p.width;
                velX = 0;
            }
        }

        // Collisione verticale
        isGrounded = false;
        for (Platform p : platforms) {
            Rectangle pb = p.getBounds();

            // Atterraggio
            if (velY > 0 && y + height <= pb.y + 8 && futureBoundsY.intersects(pb)) {
                nextY = pb.y - height;
                velY = 0;
                isGrounded = true;
            }

            // Testata
            else if (velY < 0 && y >= pb.y + pb.height && futureBoundsY.intersects(pb)) {
                nextY = pb.y + pb.height;
                velY = 0;
            }
        }
        if (nextY < 0) {
            nextY = 0;
            velY = 0;
        }
        x = nextX;
        y = nextY;

    }

    public void draw(Graphics2D g2, int cameraX) {
        int ix = (int)x - cameraX;
        int iy = (int)y;

        // 🔥 UNICA MODIFICA: disegna il nuovo gatto
        if (catImage != null) {
            g2.drawImage(catImage, ix, iy, width, height, null);
        } else {
            // fallback se l'immagine non viene trovata
            g2.setColor(Color.ORANGE);
            g2.fillOval(ix, iy, width, height);
        }
    }

    public BufferedImage getCatImage() {
        return catImage;
    }

    public void setCatImage(BufferedImage catImage) {
        this.catImage = catImage;
    }
}
