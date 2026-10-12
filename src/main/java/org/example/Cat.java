package org.example;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Cat {

    GamePanel gp;
    KeyHandler keyH;

    public float x, y;
    public int width, height;

    public float speed = 4.8f;
    public float velY = 0;
    public float gravity = 0.48f;
    public float jumpStrength = -11.8f;
    public boolean isGrounded = false;
    public boolean facingRight = true;

    public Cat(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;
        this.width = gp.tileSize;
        this.height = gp.tileSize;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void update(ArrayList<Platform> platforms) {
        float nextX = x;
        if (keyH.leftPressed) {
            nextX -= speed;
            facingRight = false;
        }
        if (keyH.rightPressed) {
            nextX += speed;
            facingRight = true;
        }

        if (nextX < 0) nextX = 0;
        if (nextX > gp.worldWidth - width) nextX = gp.worldWidth - width;

        Rectangle futureBoundsX = new Rectangle((int) nextX, (int) y, width, height);
        boolean collisionX = false;
        for (Platform p : platforms) {
            if (futureBoundsX.intersects(p.getBounds())) {
                collisionX = true;
                break;
            }
        }
        if (!collisionX) x = nextX;

        if ((keyH.spacePressed || keyH.upPressed) && isGrounded) {
            velY = jumpStrength;
            isGrounded = false;
        }

        velY += gravity;
        float nextY = y + velY;

        Rectangle futureBoundsY = new Rectangle((int) x, (int) nextY, width, height);
        isGrounded = false;

        for (Platform p : platforms) {
            if (futureBoundsY.intersects(p.getBounds())) {
                if (velY > 0 && y + height <= p.y + 12) {
                    nextY = p.y - height;
                    velY = 0;
                    isGrounded = true;
                } else if (velY < 0 && y >= p.y + p.height - 12) {
                    nextY = p.y + p.height;
                    velY = 0;
                }
            }
        }
        y = nextY;
    }

    public void draw(Graphics2D g2, int cameraX) {
        int ix = (int) x - cameraX;
        int iy = (int) y;

        // Corpo gattino
        g2.setColor(new Color(230, 120, 30));
        g2.fillRoundRect(ix + 4, iy + 12, width - 8, height - 12, 10, 10);

        // Testa
        g2.setColor(new Color(245, 140, 45));
        g2.fillOval(ix + 6, iy + 2, width - 12, height - 14);

        // Orecchie e Baffi
        g2.setColor(new Color(180, 80, 10));
        if (facingRight) {
            g2.fillPolygon(new int[]{ix + 10, ix + 18, ix + 14}, new int[]{iy + 4, iy + 4, iy - 4}, 3);
            g2.fillPolygon(new int[]{ix + 24, ix + 32, ix + 28}, new int[]{iy + 4, iy + 4, iy - 4}, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(ix + 24, iy + 10, 8, 8);
            g2.setColor(Color.BLACK);
            g2.fillOval(ix + 28, iy + 12, 4, 4);
            g2.setColor(Color.WHITE);
            g2.drawLine(ix + 30, iy + 18, ix + 42, iy + 16);
            g2.drawLine(ix + 30, iy + 20, ix + 42, iy + 22);
        } else {
            g2.fillPolygon(new int[]{ix + 16, ix + 24, ix + 20}, new int[]{iy + 4, iy + 4, iy - 4}, 3);
            g2.fillPolygon(new int[]{ix + 30, ix + 38, ix + 34}, new int[]{iy + 4, iy + 4, iy - 4}, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval(ix + 14, iy + 10, 8, 8);
            g2.setColor(Color.BLACK);
            g2.fillOval(ix + 14, iy + 12, 4, 4);
            g2.setColor(Color.WHITE);
            g2.drawLine(ix + 16, iy + 18, ix + 4, iy + 16);
            g2.drawLine(ix + 16, iy + 20, ix + 4, iy + 22);
        }

        // Coda
        g2.setColor(new Color(200, 100, 20));
        g2.fillOval(facingRight ? ix - 2 : ix + width - 6, iy + 20, 10, 18);
    }
}
