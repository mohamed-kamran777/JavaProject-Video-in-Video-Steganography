
package com.videosteg;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class VideoSteganographyService {

    private static final int SECRET_WIDTH = 320;
    private static final int SECRET_HEIGHT = 180;
    private static final int FPS = 10;

    private static final Path COVER_INPUT =
            Path.of("input/cover.mp4");

    private static final Path SECRET_INPUT =
            Path.of("input/secret_small.mp4");

    private static final Path STEGO_OUTPUT =
            Path.of("output/stego.mkv");

    private static final Path METADATA_OUTPUT =
            Path.of("output/metadata.properties");

    private static final Path RECOVERED_OUTPUT =
            Path.of("output/recovered_secret.mkv");

    public static void hideVideo() throws Exception {

        if (!Files.isRegularFile(COVER_INPUT)) {
            throw new IOException(
                    "Cover video not found: input/cover.mp4");
        }

        if (!Files.isRegularFile(SECRET_INPUT)) {
            throw new IOException(
                    "Secret video not found: input/secret_small.mp4");
        }

        for (String folder : List.of(
                "frames/cover",
                "frames/secret",
                "frames/stego",
                "frames/recovered",
                "frames/stego_decoded")) {
            reset(folder);
        }

        Files.createDirectories(Path.of("output"));

        // Convert both videos to consistent frame sizes and 10 FPS.
        VideoProcessor.extractFrames(
                COVER_INPUT.toString(),
                "frames/cover/frame_%04d.png",
                false);

        VideoProcessor.extractFrames(
                SECRET_INPUT.toString(),
                "frames/secret/frame_%04d.png",
                true);

        List<Path> coverFrames = frames("frames/cover");
        List<Path> secretFrames = frames("frames/secret");

        if (coverFrames.isEmpty()) {
            throw new IOException(
                    "No cover frames found. Check the cover video.");
        }

        if (secretFrames.isEmpty()) {
            throw new IOException(
                    "No secret frames found. Check the secret video.");
        }

        // Each secret frame is embedded into one cover frame.
        if (secretFrames.size() > coverFrames.size()) {
            double coverDuration =
                    (double) coverFrames.size() / FPS;
            double secretDuration =
                    (double) secretFrames.size() / FPS;

            throw new IOException(
                    String.format(
                            "Secret video is too long. "
                                    + "Cover capacity: about %.1f seconds; "
                                    + "secret duration: about %.1f seconds. "
                                    + "Choose a longer cover or a shorter secret.",
                            coverDuration,
                            secretDuration
                    )
            );
        }

        for (int i = 0; i < secretFrames.size(); i++) {

            BufferedImage coverImage =
                    ImageIO.read(coverFrames.get(i).toFile());

            BufferedImage secretImage =
                    ImageIO.read(secretFrames.get(i).toFile());

            if (coverImage == null || secretImage == null) {
                throw new IOException(
                        "Could not read frame " + (i + 1));
            }

            BufferedImage stegoImage =
                    LSBEncoder.hideImage(coverImage, secretImage);

            Path outputFrame = Path.of(
                    "frames/stego",
                    coverFrames.get(i).getFileName().toString());

            ImageIO.write(stegoImage, "png", outputFrame.toFile());
        }

        // Preserve the remaining cover frames unchanged.
        for (int i = secretFrames.size();
             i < coverFrames.size(); i++) {

            Files.copy(
                    coverFrames.get(i),
                    Path.of(
                            "frames/stego",
                            coverFrames.get(i).getFileName().toString())
            );
        }

        // Create a lossless video so embedded pixel data survives.
        VideoProcessor.createVideo(
                "frames/stego/frame_%04d.png",
                STEGO_OUTPUT.toString());

        Properties metadata = new Properties();
        metadata.setProperty(
                "frameCount", String.valueOf(secretFrames.size()));
        metadata.setProperty(
                "width", String.valueOf(SECRET_WIDTH));
        metadata.setProperty(
                "height", String.valueOf(SECRET_HEIGHT));
        metadata.setProperty(
                "fps", String.valueOf(FPS));

        try (var out = Files.newOutputStream(METADATA_OUTPUT)) {
            metadata.store(out, "Video steganography metadata");
        }

        System.out.println(
                "Stego video created: " + STEGO_OUTPUT);
        System.out.println(
                "Embedded secret frames: " + secretFrames.size());
        System.out.println(
                "Cover frames: " + coverFrames.size());
    }

    public static void extractVideo() throws Exception {

        if (!Files.isRegularFile(STEGO_OUTPUT)) {
            throw new IOException(
                    "Stego video not found: output/stego.mkv. "
                            + "Hide a video first.");
        }

        if (!Files.isRegularFile(METADATA_OUTPUT)) {
            throw new IOException(
                    "Metadata is missing: output/metadata.properties.");
        }

        reset("frames/stego_decoded");
        reset("frames/recovered");

        // Extract frames from the saved stego video, not the
        // original in-memory images.
        VideoProcessor.extractStegoFrames(
                STEGO_OUTPUT.toString(),
                "frames/stego_decoded/frame_%04d.png");

        Properties metadata = new Properties();

        try (var in = Files.newInputStream(METADATA_OUTPUT)) {
            metadata.load(in);
        }

        int count = Integer.parseInt(
                metadata.getProperty("frameCount"));

        int width = Integer.parseInt(
                metadata.getProperty("width"));

        int height = Integer.parseInt(
                metadata.getProperty("height"));

        List<Path> stegoFrames = frames("frames/stego_decoded");

        if (stegoFrames.size() < count) {
            throw new IOException(
                    "The saved stego video contains fewer frames "
                            + "than the metadata requires.");
        }

        for (int i = 0; i < count; i++) {

            BufferedImage stegoImage =
                    ImageIO.read(stegoFrames.get(i).toFile());

            if (stegoImage == null) {
                throw new IOException(
                        "Could not read stego frame " + (i + 1));
            }

            BufferedImage recoveredImage =
                    LSBDecoder.extractImage(
                            stegoImage, width, height);

            Path recoveredFrame = Path.of(
                    "frames/recovered",
                    String.format("frame_%04d.png", i + 1));

            ImageIO.write(
                    recoveredImage, "png", recoveredFrame.toFile());
        }

        VideoProcessor.createVideo(
                "frames/recovered/frame_%04d.png",
                RECOVERED_OUTPUT.toString());

        System.out.println(
                "Secret video recovered: " + RECOVERED_OUTPUT);
    }

    private static List<Path> frames(String folder)
            throws IOException {

        try (Stream<Path> stream = Files.list(Path.of(folder))) {
            return stream
                    .filter(path -> path.getFileName()
                            .toString().endsWith(".png"))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private static void reset(String folder)
            throws IOException {

        Path directory = Path.of(folder);
        Files.createDirectories(directory);

        try (Stream<Path> stream = Files.list(directory)) {
            for (Path file : stream.toList()) {
                Files.delete(file);
            }
        }
    }
}
