
package com.videosteg;

import java.awt.image.BufferedImage;

public class LSBDecoder {

    public static BufferedImage extractImage(
            BufferedImage stego, int width, int height) {

        long requiredPixels = (long) width * height * 3;

        if (width <= 0 || height <= 0
                || requiredPixels > (long) stego.getWidth()
                * stego.getHeight()) {
            throw new IllegalArgumentException(
                    "Invalid secret image dimensions.");
        }

        BufferedImage secret = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);

        int position = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int[] channels = new int[3];

                for (int c = 0; c < 3; c++) {
                    int coverX = position % stego.getWidth();
                    int coverY = position / stego.getWidth();

                    int pixel = stego.getRGB(coverX, coverY);

                    int r = (pixel >> 16) & 7;
                    int g = (pixel >> 8) & 7;
                    int b = pixel & 7;

                    channels[c] = r | (g << 3) | (b << 6);
                    position++;
                }

                secret.setRGB(x, y,
                        (channels[0] << 16)
                                | (channels[1] << 8)
                                | channels[2]);
            }
        }

        return secret;
    }
}
