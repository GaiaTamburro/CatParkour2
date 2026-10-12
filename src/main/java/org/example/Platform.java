package org.example;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Platform {
    public int x, y, width, height;
    public Color colorStart, colorEnd;

    public Platform(int x, int y, int width, int height, Color colorStart, Color colorEnd) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g2, int cameraX) {
        int screenX = x - cameraX;

        // Disegna solo se la piattaforma è visibile a schermo
        if (screenX + width < -100 || screenX > 1000) return;

        GradientPaint gradient = new GradientPaint(screenX, y, colorStart, screenX, y + height, colorEnd);
        g2.setPaint(gradient);
        g2.fillRoundRect(screenX, y, width, height, 8, 8);

        // Bordo superiore illuminato (effetto 3D)
        g2.setColor(colorStart.brighter());
        g2.fillRect(screenX + 2, y, width - 4, 3);

        // Venature del legno
        g2.setColor(new Color(0, 0, 0, 40));
        for (int i = screenX + 15; i < screenX + width - 10; i += 30) {
            g2.drawLine(i, y + 4, i + 10, y + height - 4);
        }
    }
}