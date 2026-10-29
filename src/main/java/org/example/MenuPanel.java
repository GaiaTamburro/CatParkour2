package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class MenuPanel extends JPanel {

    private JFrame window;
    private BufferedImage catImage;

    private int animationTick = 0;
    private Timer timer;

    public MenuPanel(JFrame window) {
        this.window = window;

        setPreferredSize(new Dimension(768, 576));
        setLayout(null);

        // Caricamento sicuro della risorsa
        try {
            InputStream is = getClass().getResourceAsStream("/cat_walk.png");
            if (is != null) {
                catImage = ImageIO.read(is);
            } else {
                System.err.println("Attenzione: Immagine '/cat_walk.png' non trovata in res/");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Creazione pulsanti stilizzati
        JButton playButton = createButton("GIOCA");
        playButton.setBounds(264, 390, 240, 65);

        JButton exitButton = createButton("ESCI");
        exitButton.setBounds(264, 470, 240, 65);

        add(playButton);
        add(exitButton);

        // Azione del tasto GIOCA
        playButton.addActionListener(e -> {
            if (timer != null && timer.isRunning()) {
                timer.stop(); // Ferma l'animazione per risparmiare risorse
            }

            window.getContentPane().removeAll();

            GamePanel gamePanel = new GamePanel();
            window.add(gamePanel);

            window.pack();
            window.revalidate();
            window.repaint();

            gamePanel.startGameThread();
            gamePanel.requestFocusInWindow();
        });

        // Azione del tasto ESCI
        exitButton.addActionListener(e -> System.exit(0));

        // Timer per animare gatto e nuvole (60 FPS circa)
        timer = new Timer(16, e -> {
            animationTick++;
            repaint();
        });
        timer.start();
    }

    private JButton createButton(String text) {
        RoundedButton button = new RoundedButton(text);

        button.setBackground(new Color(255, 170, 0));
        button.setForeground(Color.BLACK);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 210, 50));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 170, 0));
            }
        });

        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sfondo a gradiente dal blu sfumato all'arancio tenue
        GradientPaint gradient = new GradientPaint(
                0, 0,
                new Color(120, 180, 255),
                0, getHeight(),
                new Color(255, 220, 170)
        );

        g2.setPaint(gradient);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Nuvole animate a scorrimento continuo
        int screenW = getWidth() + 200;
        drawCloud(g2, ((80 + animationTick) % screenW) - 150, 80);
        drawCloud(g2, ((420 + animationTick) % screenW) - 150, 150);
        drawCloud(g2, ((620 + animationTick) % screenW) - 150, 60);

        // Zampette decorative in basso
        g2.setColor(new Color(255, 255, 255, 40));
        for (int i = 0; i < 8; i++) {
            int x = i * 100;

            g2.fillOval(x, 500, 25, 25);
            g2.fillOval(x + 10, 515, 8, 8);
            g2.fillOval(x + 30, 515, 8, 8);
            g2.fillOval(x + 20, 530, 8, 8);
        }

        // ===== TITOLO CENTRATO CON OMBRA =====
        g2.setFont(new Font("Arial", Font.BOLD, 54));
        FontMetrics fmTitle = g2.getFontMetrics();

        String line1 = "CAT";
        String line2 = "PARKOUR";

        int x1 = (getWidth() - fmTitle.stringWidth(line1)) / 2;
        int x2 = (getWidth() - fmTitle.stringWidth(line2)) / 2;

        // Ombra del titolo
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(line1, x1 + 7, 72);
        g2.drawString(line2, x2 + 7, 132);

        // Testo del titolo
        g2.setColor(new Color(255, 215, 0));
        g2.drawString(line1, x1, 65);
        g2.drawString(line2, x2, 125);

        // ===== SOTTOTITOLO CENTRATO =====
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fmSub = g2.getFontMetrics();

        String subtitle = "Aiuta il gattino a raggiungere la ciotola!";
        int subX = (getWidth() - fmSub.stringWidth(subtitle)) / 2;

        // Ombra del sottotitolo
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(subtitle, subX + 2, 167);

        // Testo del sottotitolo
        g2.setColor(new Color(255, 245, 180));
        g2.drawString(subtitle, subX, 165);

        // ===== GATTO ANIMATO PROPORZIONATO E CENTRATO =====
        int bounce = (int) (Math.sin(animationTick * 0.05) * 10);

        if (catImage != null) {
            int targetHeight = 190;
            float aspectRatio = (float) catImage.getWidth() / catImage.getHeight();
            int targetWidth = (int) (targetHeight * aspectRatio);
            int catX = (getWidth() - targetWidth) / 2; // Centrato perfettamente

            g2.drawImage(
                    catImage,
                    catX,
                    175 + bounce,
                    targetWidth,
                    targetHeight,
                    null
            );
        }
    }

    private void drawCloud(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 180));

        g2.fillOval(x, y, 60, 60);
        g2.fillOval(x + 30, y - 20, 70, 70);
        g2.fillOval(x + 70, y, 60, 60);

        g2.fillRoundRect(
                x + 15,
                y + 20,
                90,
                30,
                30,
                30
        );
    }
}