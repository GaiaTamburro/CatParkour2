package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class FallingTreat {

    public float x, y;
    public int size = 48; // più grande per immagine
    public float velY = 3.5f;

    private static BufferedImage treatImage;

    public FallingTreat(float x, float y) {
        this.x = x;
        this.y = y;

        // Carica l'immagine UNA SOLA VOLTA
        if (treatImage == null) {
            try {
                treatImage = ImageIO.read(getClass().getResource("/croccantino_nosfondo.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, size, size);
    }

    public void update() {
        y += velY;
    }

    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int)x - cameraX;
        int sy = (int)y;

        if (treatImage != null) {
            g2.drawImage(treatImage, sx, sy, size, size, null);
        } else {
            // fallback se l'immagine non viene trovata
            g2.setColor(Color.ORANGE);
            g2.fillOval(sx, sy, size, size);
        }
    }
}
