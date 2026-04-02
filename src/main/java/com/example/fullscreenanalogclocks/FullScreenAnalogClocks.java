package com.example.fullscreenanalogclocks;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FullScreenAnalogClocks extends Application {

    private static final int S = 45;   // Clock size
    private static final int G = 5;    // Gap
    private int themeIndex = 0;

    private final Theme[] THEMES = {
            new Theme("#0a0a0f", "#0d0d18", "#1a1a30", "#ffdd00", "#1e1e30"),
            new Theme("#0a0f0a", "#0d180d", "#1a301a", "#00ffaa", "#0e2018"),
            new Theme("#0f0a0a", "#180d0d", "#301a1a", "#ff4466", "#200e10"),
            new Theme("#0f0d0a", "#18160d", "#30281a", "#ff8800", "#201508")
    };

    private List<ClockGrid> digitBlocks = new ArrayList<>();
    private List<Canvas> colonDots = new ArrayList<>();
    private Text dateLabel = new Text();
    private VBox root = new VBox();

    // Movement constants
    private final double[] VV = {0, 180}, HH = {90, 270};
    private final double[] TL = {180, 90}, TR = {180, 270}, BL = {0, 90}, BR = {0, 270};
    private final double[] XX = {45, 225};

    @Override
    public void start(Stage stage) {
        root.setSpacing(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        HBox clockRow = new HBox(8);
        clockRow.setAlignment(Pos.CENTER);

        // Create 6 digits with colons at indices 2 and 4
        for (int i = 0; i < 6; i++) {
            if (i == 2 || i == 4) clockRow.getChildren().add(createColon());
            ClockGrid block = new ClockGrid();
            digitBlocks.add(block);
            clockRow.getChildren().add(block.getPane());
        }

        // Bottom UI
        dateLabel.setFont(Font.font("Monospaced", 14));
        Button themeBtn = new Button("THEME ↻");
        themeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #334; -fx-text-fill: #667; -fx-border-radius: 20;");
        themeBtn.setOnAction(e -> {
            themeIndex = (themeIndex + 1) % THEMES.length;
            updateThemeStyles();
        });

        HBox bottom = new HBox(20, dateLabel, themeBtn);
        bottom.setAlignment(Pos.CENTER);

        root.getChildren().addAll(clockRow, bottom);
        updateThemeStyles();

        Scene scene = new Scene(root, 1000, 500);
        stage.setTitle("Kinetic Analog Clock");
        stage.setScene(scene);
        stage.show();

        startAnimation();
    }

    private void updateThemeStyles() {
        Theme t = THEMES[themeIndex];
        root.setStyle("-fx-background-color: " + t.bg + ";");
        dateLabel.setFill(Color.web("#444455"));
    }

    private VBox createColon() {
        VBox vbox = new VBox(18);
        vbox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 2; i++) {
            Canvas dot = new Canvas(8, 8);
            colonDots.add(dot);
            vbox.getChildren().add(dot);
        }
        return vbox;
    }

    private void startAnimation() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                LocalDateTime time = LocalDateTime.now();
                String timeStr = time.format(DateTimeFormatter.ofPattern("HHmmss"));

                // Update digits
                for (int i = 0; i < 6; i++) {
                    digitBlocks.get(i).setDigit(timeStr.charAt(i));
                }

                // Update colons (blink)
                double opacity = (System.currentTimeMillis() % 1000 < 500) ? 1.0 : 0.15;
                for (Canvas dot : colonDots) {
                    GraphicsContext gc = dot.getGraphicsContext2D();
                    gc.clearRect(0, 0, 8, 8);
                    gc.setFill(Color.web(THEMES[themeIndex].on).deriveColor(0, 1, 1, opacity));
                    gc.fillOval(0, 0, 8, 8);
                }

                // Update Clock movement
                for (ClockGrid grid : digitBlocks) {
                    grid.updateAndDraw(THEMES[themeIndex]);
                }

                // Update Date
                dateLabel.setText(time.format(DateTimeFormatter.ofPattern("EEE  dd MMM yyyy")).toUpperCase());
            }
        }.start();
    }

    private class ClockGrid {
        private GridPane pane = new GridPane();
        private ClockUnit[][] clocks = new ClockUnit[5][3];

        public ClockGrid() {
            pane.setHgap(G);
            pane.setVgap(G);
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 3; c++) {
                    clocks[r][c] = new ClockUnit();
                    pane.add(clocks[r][c].canvas, c, r);
                }
            }
        }

        public void setDigit(char digit) {
            double[][][] pattern = getPattern(digit);
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 3; c++) {
                    double[] angles = pattern[r][c];
                    boolean lit = !(angles == XX);
                    clocks[r][c].setTarget(angles[0], angles[1], lit);
                }
            }
        }

        public void updateAndDraw(Theme t) {
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 3; c++) {
                    clocks[r][c].tick();
                    clocks[r][c].draw(t);
                }
            }
        }

        public GridPane getPane() { return pane; }
    }

    private class ClockUnit {
        Canvas canvas = new Canvas(S, S);
        double ha = 45, ma = 225, ht = 45, mt = 225;
        double litAlpha = 0;
        boolean litTarget = false;

        void setTarget(double h, double m, boolean lit) {
            this.ht = h; this.mt = m; this.litTarget = lit;
        }

        void tick() {
            ha = lerp(ha, ht, 0.15);
            ma = lerp(ma, mt, 0.15);
            litAlpha += ((litTarget ? 1.0 : 0.0) - litAlpha) * 0.12;
        }

        void draw(Theme t) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            double r = S / 2.0;
            gc.clearRect(0, 0, S, S);

            // Face
            gc.setFill(Color.web(t.face));
            gc.fillOval(0.5, 0.5, S - 1, S - 1);

            // Rim
            Color rimColor = interpolate(Color.web(t.rim), Color.web(t.on), litAlpha * 0.35);
            gc.setStroke(rimColor);
            gc.setLineWidth(1.5);
            gc.strokeOval(0.5, 0.5, S - 1, S - 1);

            // Hands
            drawHand(gc, r, ha, r * 0.46, Color.web(t.dim), 1.5);
            drawHand(gc, r, ma, r * 0.46, Color.web(t.dim), 1.5);

            if (litAlpha > 0.02) {
                Color activeColor = Color.web(t.on).deriveColor(0, 1, 1, litAlpha);
                drawHand(gc, r, ha, r * 0.46, activeColor, 4.0);
                drawHand(gc, r, ma, r * 0.46, activeColor, 4.0);
            }

            // Pin
            gc.setFill(litAlpha > 0.5 ? Color.web(t.on) : Color.web(t.rim));
            gc.fillOval(r - 2, r - 2, 4, 4);
        }

        private void drawHand(GraphicsContext gc, double r, double deg, double len, Color color, double w) {
            double rad = Math.toRadians(deg - 90);
            gc.setStroke(color);
            gc.setLineWidth(w);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(r, r, r + Math.cos(rad) * len, r + Math.sin(rad) * len);
        }
    }

    // Logic Helpers
    private double lerp(double a, double b, double t) {
        double d = ((b - a + 540) % 360) - 180;
        return (a + d * t + 360) % 360;
    }

    private Color interpolate(Color a, Color b, double t) {
        return a.interpolate(b, t);
    }

    private double[][][] getPattern(char c) {
        return switch (c) {
            case '1' -> new double[][][]{{XX,TL,XX},{XX,VV,XX},{XX,VV,XX},{XX,VV,XX},{XX,BR,XX}};
            case '2' -> new double[][][]{{TL,HH,TR},{XX,XX,VV},{TL,HH,BR},{VV,XX,XX},{BL,HH,BR}};
            case '3' -> new double[][][]{{TL,HH,TR},{XX,XX,VV},{HH,HH,BR},{XX,XX,VV},{HH,HH,BR}};
            case '4' -> new double[][][]{{VV,XX,VV},{VV,XX,VV},{BL,HH,BR},{XX,XX,VV},{XX,XX,VV}};
            case '5' -> new double[][][]{{TL,HH,TR},{VV,XX,XX},{BL,HH,TR},{XX,XX,VV},{HH,HH,BR}};
            case '6' -> new double[][][]{{TL,HH,TR},{VV,XX,XX},{BL,HH,TR},{VV,XX,VV},{BL,HH,BR}};
            case '7' -> new double[][][]{{TL,HH,TR},{XX,XX,VV},{XX,XX,VV},{XX,XX,VV},{XX,XX,VV}};
            case '8' -> new double[][][]{{TL,HH,TR},{VV,XX,VV},{BL,HH,BR},{VV,XX,VV},{BL,HH,BR}};
            case '9' -> new double[][][]{{TL,HH,TR},{VV,XX,VV},{BL,HH,BR},{XX,XX,VV},{HH,HH,BR}};
            default ->  new double[][][]{{TL,HH,TR},{VV,XX,VV},{VV,XX,VV},{VV,XX,VV},{BL,HH,BR}};
        };
    }

    private static class Theme {
        String bg, face, rim, on, dim;
        Theme(String b, String f, String r, String o, String d) {
            bg=b; face=f; rim=r; on=o; dim=d;
        }
    }

    public static void main(String[] args) { launch(args); }
}