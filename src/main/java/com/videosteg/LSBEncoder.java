
package com.videosteg;

import java.awt.image.BufferedImage;

public class LSBEncoder {

    public static BufferedImage hideImage(
            BufferedImage cover, BufferedImage secret) {

        long requiredPixels = (long) secret.getWidth()
                * secret.getHeight() * 3;

        if (requiredPixels > (long) cover.getWidth()
                * cover.getHeight()) {
            throw new IllegalArgumentException(
                    "Secret image is too large for the cover image.");
        }

        BufferedImage result = new BufferedImage(
                cover.getWidth(), cover.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < cover.getHeight(); y++) {
            for (int x = 0; x < cover.getWidth(); x++) {
                result.setRGB(x, y, cover.getRGB(x, y));
            }
        }

        int position = 0;

        for (int y = 0; y < secret.getHeight(); y++) {
            for (int x = 0; x < secret.getWidth(); x++) {

                int pixel = secret.getRGB(x, y);

                int[] channels = {
                        (pixel >> 16) & 0xFF,
                        (pixel >> 8) & 0xFF,
                        pixel & 0xFF
                };

                for (int channel : channels) {
                    int coverX = position % cover.getWidth();
                    int coverY = position / cover.getWidth();

                    int coverPixel = result.getRGB(coverX, coverY);

                    int r = (coverPixel >> 16) & 0xFF;
                    int g = (coverPixel >> 8) & 0xFF;
                    int b = coverPixel & 0xFF;

                    // Store one complete secret byte in three
                    // consecutive cover pixels, 3 bits per pixel.
                    int shift = 0;
                    int bits = channel;

                    int[] values = {r, g, b};

                    for (int i = 0; i < 3; i++) {
                        int bitGroup = (bits >> shift) & 7;
                        values[i] = (values[i] & 0xF8) | bitGroup;
                        shift += 3;
                    }

                    result.setRGB(coverX, coverY,
                            (values[0] << 16)
                                    | (values[1] << 8)
                                    | values[2]);

                    position++;
                }
            }
        }

        return result;
    }
}
