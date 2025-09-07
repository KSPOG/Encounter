package com.pokemmo.encounter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class EncounterCounter {
    private int encounters = 0;
    private String latestShiny = "None";
    private final JLabel encountersLabel = new JLabel("Encounters: 0");
    private final JLabel shinyLabel = new JLabel("Latest shiny encounter: None");
    private final JCheckBox autoResetCheck = new JCheckBox("Auto-reset on shiny", true);
    private final TrayNotifier notifier = new TrayNotifier();
    private final PokemonDetector detector = new PokemonDetector();

    public EncounterCounter() {
        JFrame frame = new JFrame("PokeMMO Encounter Counter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 1));
        frame.add(encountersLabel);
        frame.add(shinyLabel);
        frame.add(autoResetCheck);
        frame.pack();
        frame.setVisible(true);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Pokemon pokemon = detector.detectEncounter();
            if (pokemon != null) {
                encounters++;
                encountersLabel.setText("Encounters: " + encounters);
                notifier.notify("Encountered " + pokemon.name);
                if (pokemon.isShiny || pokemon.isLegendary) {
                    String type = pokemon.isLegendary ? "Legendary" : "Shiny";
                    notifier.notify(type + " " + pokemon.name + " encountered!");
                    if (pokemon.isShiny) {
                        latestShiny = pokemon.name;
                        shinyLabel.setText("Latest shiny encounter: " + latestShiny);
                        if (autoResetCheck.isSelected()) {
                            encounters = 0;
                            encountersLabel.setText("Encounters: 0");
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EncounterCounter::new);
    }

    static class Pokemon {
        final String name;
        final boolean isShiny;
        final boolean isLegendary;
        Pokemon(String name, boolean isShiny, boolean isLegendary) {
            this.name = name;
            this.isShiny = isShiny;
            this.isLegendary = isLegendary;
        }
    }

    static class TrayNotifier {
        private final TrayIcon icon;
        TrayNotifier() {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                icon = new TrayIcon(image, "PokeMMO Encounter Counter");
                icon.setImageAutoSize(true);
                try {
                    tray.add(icon);
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }
            } else {
                icon = null;
            }
        }
        void notify(String message) {
            if (icon != null) {
                icon.displayMessage("PokeMMO Encounter", message, TrayIcon.MessageType.INFO);
            }
        }
    }

    static class PokemonDetector {
        private final Robot robot;
        private final Rectangle captureArea = new Rectangle(100, 100, 300, 50); // adjust to game window
        private final ITesseract ocr;
        PokemonDetector() {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            ocr = new Tesseract();
            // Configure tessdata path if needed: ocr.setDatapath("tessdata");
        }
        Pokemon detectEncounter() {
            BufferedImage image = robot.createScreenCapture(captureArea);
            try {
                String text = ocr.doOCR(image).trim();
                if (text.isEmpty()) {
                    return null;
                }
                String name = text.replace("Shiny", "").replace("Legendary", "").trim();
                boolean shiny = text.contains("Shiny");
                boolean legendary = text.contains("Legendary");
                return new Pokemon(name, shiny, legendary);
            } catch (TesseractException e) {
                return null;
            }
        }
    }
}
