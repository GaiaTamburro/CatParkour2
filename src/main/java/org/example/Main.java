package org.example;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("Cat Parkour");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Aggiungiamo il pannello di gioco
        MenuPanel menu = new MenuPanel(window);
        window.add(menu);
        window.pack(); // Adatta la finestra alle dimensioni del pannello

        window.setLocationRelativeTo(null); // Centra lo schermo
        window.setVisible(true);

        // Avviamo il loop di gioco

    }
}
