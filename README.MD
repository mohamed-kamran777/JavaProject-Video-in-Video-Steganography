# Video-in-Video Steganography

## Overview
A Java application that hides a secret video inside a cover video using Least Significant Bit (LSB) steganography. A JavaFX interface allows users to select videos, embed the secret, and extract it from the generated stego video.

## Technologies
- Java
- JavaFX
- Maven
- FFmpeg
- LSB steganography
- FFV1 lossless video encoding

## How It Works
1. The user selects a cover video and a secret video.
2. FFmpeg extracts frames and converts the cover frames to 1280 × 720 and secret frames to 320 × 180, both at 10 FPS.
3. The LSB encoder embeds each secret frame into a corresponding cover frame.
4. The modified cover frames are combined into `output/stego.mkv`.
5. The decoder extracts the hidden frames from the saved stego video.
6. The recovered frames are combined into `output/recovered_secret.mkv`.

## Requirements
- A compatible JDK
- IntelliJ IDEA or another Java IDE
- Maven
- FFmpeg installed and available through the system PATH

## Running the Project
1. Clone or download the repository.
2. Open the project as a Maven project in IntelliJ IDEA.
3. Configure the required JDK.
4. Install FFmpeg and verify that `ffmpeg -version` works in a terminal.
5. Run `Main.java`.
6. Select the cover and secret videos.
7. Click **Hide Video** to generate the stego video.
8. Click **Extract Video** to recover the secret.

## Project Structure
- `Main.java`: JavaFX user interface.
- `VideoProcessor.java`: FFmpeg-based frame extraction and video creation.
- `VideoSteganographyService.java`: Embedding and extraction workflow.
- `LSBEncoder.java`: Hides secret image data in cover frames.
- `LSBDecoder.java`: Recovers secret image data.
- `pom.xml`: Maven dependencies and build configuration.

## Current Limitations
- Videos are converted to 10 FPS.
- Cover frames are resized to 1280 × 720.
- Secret frames are resized to 320 × 180, reducing recovered video detail.
- The secret video cannot exceed the cover video's processed frame capacity.
- Audio is not preserved.
- Extraction requires `output/metadata.properties` alongside the stego video.
- Temporary frames can consume significant disk space.

## Future Improvements
- Preserve original frame rates and resolution.
- Preserve audio.
- Automatically clean temporary files.
- Improve video quality and evaluate the recovered video.
- Improve metadata management and error handling.

## Academic Project
Developed as an MCA course project to demonstrate frame-based video steganography using LSB encoding and decoding.
