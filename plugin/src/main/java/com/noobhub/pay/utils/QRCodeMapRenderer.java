package com.noobhub.pay.utils;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

public class QRCodeMapRenderer extends MapRenderer {
    private final File imageFile;
    private final Logger logger;
    private BufferedImage image;
    private boolean rendered = false;

    public QRCodeMapRenderer(File imageFile, Logger logger) {
        this.imageFile = imageFile;
        this.logger = logger;
        loadImage();
    }

    private void loadImage() {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(original, 0, 0, 128, 128, null);
            graphics.dispose();

            logger.info("Successfully loaded QR code image: " + imageFile.getName());
        } catch (Exception e) {
            logger.severe("Failed to load QR code image: " + imageFile.getPath());
            e.printStackTrace();
            image = null;
        }
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered || image == null) {
            return;
        }

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                byte pixelColor = MapPalette.matchColor(color);
                canvas.setPixel(x, y, pixelColor);
            }
        }

        rendered = true;
    }
}
