
package com.videosteg;

import java.io.IOException;

public class VideoProcessor {

    private static final int FPS = 10;
    private static final int COVER_WIDTH = 1280;
    private static final int COVER_HEIGHT = 720;
    private static final int SECRET_WIDTH = 320;
    private static final int SECRET_HEIGHT = 180;

    public static void extractFrames(
            String input, String pattern, boolean secret)
            throws IOException, InterruptedException {

        int width = secret ? SECRET_WIDTH : COVER_WIDTH;
        int height = secret ? SECRET_HEIGHT : COVER_HEIGHT;

        String filter =
                "fps=" + FPS
                        + ",scale=" + width + ":" + height
                        + ":force_original_aspect_ratio=decrease"
                        + ":force_divisible_by=2"
                        + ",pad=" + width + ":" + height
                        + ":(ow-iw)/2:(oh-ih)/2";

        run(
                "ffmpeg", "-y",
                "-i", input,
                "-vf", filter,
                "-an",
                "-start_number", "1",
                pattern
        );
    }

    public static void extractStegoFrames(
            String input, String pattern)
            throws IOException, InterruptedException {

        run(
                "ffmpeg", "-y",
                "-i", input,
                "-fps_mode", "passthrough",
                "-start_number", "1",
                pattern
        );
    }

    public static void createVideo(
            String pattern, String output)
            throws IOException, InterruptedException {

        run(
                "ffmpeg", "-y",
                "-framerate", String.valueOf(FPS),
                "-start_number", "1",
                "-i", pattern,
                "-c:v", "ffv1",
                "-level", "3",
                "-pix_fmt", "bgr0",
                output
        );
    }

    private static void run(String... command)
            throws IOException, InterruptedException {

        Process process = new ProcessBuilder(command)
                .inheritIO()
                .start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "FFmpeg failed with exit code " + exitCode
                            + ". Check the video file and FFmpeg output."
            );
        }
    }
}
