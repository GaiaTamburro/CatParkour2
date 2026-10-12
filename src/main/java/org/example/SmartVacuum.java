package org.example;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public class SmartVacuum {

    public enum State { PATROL, CHASE, ATTACK }

    public float x, y;
    public int width, height;
    public float speed = 1.8f;
    public float chaseSpeed = 3.2f;
    public int direction = 1;
    public State currentState = State.PATROL;

    public double detectionRadius = 220.0;
    public double attackRadius = 30.0;

    public SmartVacuum(float startX, float startY, GamePanel gp) {
        this.x = startX;
        this.y = startY;
        this.width = gp.tileSize;
        this.height = (int) (gp.tileSize * 0.7);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void update(Cat cat, ArrayList<Platform> platforms) {
        double distanceToCat = Math.hypot(cat.x - this.x, cat.y - this.y);

        switch (currentState) {
            case PATROL:
                movePatrol(platforms);
                if (distanceToCat < detectionRadius && Math.abs(cat.y - this.y) < 80) {
                    currentState = State.CHASE;
                }
                break;

            case CHASE:
                moveChase(cat, platforms);
                if (distanceToCat > detectionRadius * 1.4) {
                    currentState = State.PATROL;
                } else if (distanceToCat < attackRadius) {
                    currentState = State.ATTACK;
                }
                break;

            case ATTACK:
                if (distanceToCat > attackRadius) {
                    currentState = State.CHASE;
                }
                break;
        }
    }

    private void movePatrol(ArrayList<Platform> platforms) {
        float nextX = x + (speed * direction);
        if (checkWallCollision(nextX, platforms)) {
            direction *= -1;
        } else {
            x = nextX;
        }
    }

    private void moveChase(Cat cat, ArrayList<Platform> platforms) {
        direction = (cat.x > this.x) ? 1 : -1;
        float nextX = x + (chaseSpeed * direction);
        if (!checkWallCollision(nextX, platforms)) {
            x = nextX;
        }
    }

    private boolean checkWallCollision(float nextX, ArrayList<Platform> platforms) {
        Rectangle futureBounds = new Rectangle((int) nextX, (int) y, width, height);
        for (Platform p : platforms) {
            if (futureBounds.intersects(p.getBounds())) return true;
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        int ix = (int) x;
        int iy = (int) y;

        // Ombra sotto
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(ix, iy + height - 6, width, 10);

        // Corpo Roomba
        g2.setColor(new Color(50, 50, 55));
        g2.fillOval(ix, iy, width, height);

        // Anello interno
        g2.setColor(new Color(80, 80, 90));
        g2.fillOval(ix + 6, iy + 4, width - 12, height - 8);

        // Luce stato IA
        if (currentState == State.PATROL) g2.setColor(Color.GREEN);
        else if (currentState == State.CHASE) g2.setColor(Color.ORANGE);
        else g2.setColor(Color.RED);

        g2.fillOval(ix + width / 2 - 5, iy + height / 2 - 5, 10, 10);

        // Sensore campo visivo
        g2.setColor(new Color(255, 255, 255, 20));
        g2.drawOval((int) (x + width / 2 - detectionRadius), (int) (y + height / 2 - detectionRadius),
                (int) detectionRadius * 2, (int) detectionRadius * 2);
    }
}