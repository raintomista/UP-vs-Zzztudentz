package avs.ui;

import avs.models.Grid;
import avs.models.Game;
import avs.utils.BufferedImageLoader;
import avs.utils.Textures;

import java.awt.*;
import java.awt.BorderLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GamePanel extends JPanel implements Runnable {
    private Game game;
    private boolean running;
    private Thread thread;
    private ProgressBarPanel progressBarPanel;
    private EnergyBar energyBar;

    private static BufferedImage bg;
    private static BufferedImage spriteSheet;
    private Textures tex;

    public GamePanel() {
        this.game = Game.getInstance();
        this.tex = new Textures(this);
        this.running = false;

        createPanelUI();
        addListeners();
    }

    private void createPanelUI() {
        this.setLayout(new BorderLayout());
        this.setOpaque(false);

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel westPanel = new JPanel(new BorderLayout());
        JPanel eastPanel = new JPanel(new BorderLayout());
        progressBarPanel = new ProgressBarPanel();
        energyBar = new EnergyBar();

        westPanel.setOpaque(false);
        westPanel.add(energyBar, BorderLayout.WEST);
        westPanel.add(new PlantPickerPanel(this.tex), BorderLayout.EAST);

        eastPanel.setOpaque(false);
        eastPanel.add(progressBarPanel, BorderLayout.WEST);
        eastPanel.add(new InGameMenuPanel(0), BorderLayout.EAST);
        eastPanel.setPreferredSize(new Dimension(350, 45));

        topPanel.setOpaque(false);
        topPanel.add(westPanel, BorderLayout.WEST);
        topPanel.add(eastPanel, BorderLayout.EAST);

        this.add(topPanel, BorderLayout.NORTH);
    }

    private void addListeners() {
        this.addMouseListener(new MouseListener() {
            public void mouseEntered(MouseEvent me){}
            public void mouseExited(MouseEvent me){}
            public void mouseReleased(MouseEvent me){}
            public void mousePressed(MouseEvent me){}
            public void mouseClicked(MouseEvent me){
                if (game.getSelectedPlant() == null) return;

                for (int i = 0; i < Grid.ROWS; ++i) {
                    for (int j = 0; j < Grid.COLS; ++j) {
                        Rectangle rect = game.getGrid().getRectangle(i, j);
                        if (rect.contains(me.getPoint())) {
                            if (!game.getGrid().hasPlant(i, j)) {
                                game.addPlant(i, j);
                                game.reduceEnergy();
                                game.startButtonCoolDown();
                                game.setPendingButton(null);
                                game.selectPlant(null);
                            }
                            return;
                        }
                    }
                }
            }
        });
    }

    public void run(){
		Long lastTime = System.nanoTime();
		final double amountOfTicks = 60.0;
		double ns = 1000000000 / amountOfTicks;
		double delta = 0;

		while(running){
			Long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;

			if(delta >= 1){
				tick();
				delta--;
			}
            repaint();
            progressBarPanel.repaint();
            energyBar.setValue(game.getEnergy());
		}
		stop();
	}

    public void tick() {
        if (!progressBarPanel.isDone()) {
            progressBarPanel.update();
        }

        for (int i = 0; i < Grid.ROWS; ++i) {
            for (int j = 0; j < Grid.COLS; ++j) {
                if (game.getGrid().hasPlant(i, j)) game.getGrid().getPlant(i, j).tick();
            }
        }
    }

    public synchronized void start() {
        if (running) return;

        running = true;
        thread = new Thread(this);

        game.init();
        thread.start();

    }

    public synchronized void stop() {
        if (!running) return;

        running = false;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        super.paintComponent(g);

        try {
            g2d.drawImage(GamePanel.bg, 0, 0,
                this.getWidth(), this.getHeight(), null);

            g2d.setColor(new Color(1, 68, 33));

            Point p = this.getMousePosition();
            if (p != null && game.getSelectedPlant() != null) {
                createHiglightThread(p, g2d).start();
            }

            createPrintPlantThread(g).start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private Thread createHiglightThread(final Point p, final Graphics2D g2d) {
        return new Thread() {
            public void run() {
                for (int i = 0; i < Grid.ROWS; ++i) {
                    for (int j = 0; j < Grid.COLS; ++j) {
                        Rectangle rect = game.getGrid().getRectangle(i, j);
                        if (rect.contains(p)) {
                            g2d.draw(rect);
                        };
                    }
                }
            }
        };
    }

    private Thread createPrintPlantThread(final Graphics g) {
        return new Thread() {
            public void run() {
                for (int i = 0; i < Grid.ROWS; ++i) {
                    for (int j = 0; j < Grid.COLS; ++j) {
                        if (game.getGrid().hasPlant(i, j)) {
                            final int row = i;
                            final int col = j;
                            new Thread() {
                                public void run() {
                                    game.getGrid().getPlant(row, col).render(g);
                                }
                            }.start();
                        }
                    }
                }
            }
        };
    }

    public static BufferedImage getSpriteSheet() {
        return GamePanel.spriteSheet;
    }

    public static class AssetLoader implements Runnable {
        @Override
        public void run() {
            BufferedImageLoader loader = new BufferedImageLoader();

            try {
                GamePanel.bg = loader.loadImage("../assets/img/background.png");
                GamePanel.spriteSheet = loader.loadImage("../assets/img/spritesheets/spritesheet-fullres.png");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
