import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;

/**
 * MongolianScriptPreprocessor
 *
 * This class implements a full preprocessing pipeline for Mongolian Script OCR.
 * It transforms input images into normalized glyph-level feature vectors
 * suitable for classification using a Multilayer Perceptron (MLP).
 *
 * The pipeline consists of the following stages:
 * 1. Image binarization (grayscale thresholding)
 * 2. Text region detection (bounding box extraction)
 * 3. Column segmentation using Vertical Projection Profile (VPP)
 * 4. Word segmentation using Horizontal Projection Profile (HPP)
 * 5. Glyph segmentation using heuristic projection profile analysis adapted to Mongolian script structure
 * 6. Glyph normalization (fixed-size resizing and centering)
 * 7. Flattening glyph image into 1D vectors for MLP input
 *
 * The implementation is rule-based and relies on empirically tuned thresholds
 * and heuristics tailored for the structural characteristics of Mongolian script.
 *
 */

public class MongolianScriptPreprocessor {

    /**
     * Empirically tuned parameters for Mongolian OCR segmentation.
     */
    private static final int BINARIZATION_THRESHOLD = 120;
    private static final int FOREGROUND_THRESHOLD = 0;

    /**
     * Converts an RGB image into a binary (black/white) representation using
     * luminance-based grayscale conversion followed by thresholding.
     *
     * This step simplifies the image and prepares it for segmentation by
     * removing color information and reducing noise.
     *
     * @param img input RGB image
     * @return binarized grayscale image
     */
    public static BufferedImage binarizeImage(BufferedImage img) {
        BufferedImage binarizedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = (img.getRGB(x, y));

                // Extract RGB channels for luminance computation
                int r = ((rgb >> 16) & 0xFF);
                int g = ((rgb >> 8) & 0xFF);
                int b = (rgb & 0xFF);

                // Convert to grayscale using weighted luminance model
                int grayscale = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                // Apply binarization threshold
                int value;
                if (grayscale > BINARIZATION_THRESHOLD) {
                    value = 255;
                } else {
                    value = 0;
                }

                // Bit operation to set the new rgb value
                int newRGB = (255 << 24) | (value << 16) | (value << 8) | value;
                binarizedImage.setRGB(x, y, newRGB);
            }
        }
        // Save debug output
        saveDebugImage(binarizedImage, Test.BASE_OUTPUT_PATH + "/binarizedImage", "binarizedImage" + ".png");
        
        return binarizedImage;
    }

    /**
     * Detects the minimal bounding box that contains all foreground (text) pixels.
     * The bounding box is computed using a foreground intensity threshold.
     *
     * This step isolates the region of interest containing Mongolian script.
     *
     * @param img binary input image
     * @return bounding box as {minX, topY, maxX, bottomY}, or null if no text is found
     */
    public static int[] detectTextBoundingBox(BufferedImage img){
        int width = img.getWidth();
        int height = img.getHeight();

        int minX = width;
        int maxX = 0;
        int bottomY = 0;
        int topY = height;
        boolean textFound = false;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                int pixel = img.getRGB(x, y) & 0xFF;

                // Detect potential text pixel using threshold
                if (pixel <= FOREGROUND_THRESHOLD) {
                    textFound = true;

                    // Update bounding box extremes
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y > bottomY) bottomY = y;
                    if (y < topY) topY = y;
                }
            }
        }

        // Return null if no text region detected
        if (!textFound) {
            System.out.println("No text region was detected in the image"); //add error code
            return null;
        }

        int[] textRegionCoordinates = new int[] {minX, topY, maxX, bottomY};

        return textRegionCoordinates;
    }

    /**
     * Crops an image into the given coordinates.
     *
     *  @param img source image
     *  @param edgeCoordinates array {x1, y1, x2, y2} defining crop region
     *  @return cropped sub-image
     */
    public static BufferedImage cropImageRegion (BufferedImage img, int[] edgeCoordinates) {
        BufferedImage croppedImg = img.getSubimage( edgeCoordinates[0], edgeCoordinates[1],
                edgeCoordinates[2]-edgeCoordinates[0],
                edgeCoordinates[3]-edgeCoordinates[1]);

        return croppedImg;
    }

    /**
     * Segments the image into vertical text columns using Vertical Projection Profile (VPP).
     *
     * Columns are identified by analyzing pixel density along the vertical axis
     * and detecting transitions between foreground and background regions.
     *
     * @param img binary input image
     * @return list of column bounding boxes {x1, y1, x2, y2}
     */
    public static ArrayList<int[]> segmentTextColumns(BufferedImage img) {
        ArrayList<Integer> columnBoundaries = new ArrayList<>();
        ArrayList<int[]> columnCoordinates = new ArrayList<>();

        int width = img.getWidth();
        int height = img.getHeight();
        int[] verticalProjectionProfile = new int[width];
        int minTransitionWidth = 1;

        // Compute vertical projection profile
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xFF;

                // Accumulate foreground pixel density per column
                verticalProjectionProfile[x] += (pixel == FOREGROUND_THRESHOLD) ? 1 : 0;
            }
        }

        boolean inColumn = false;
        int startColumn = 0;

        // Detects column start when foreground pixel exists in the column
        for (int x = 0; x < width; x++) {
            if (!inColumn && verticalProjectionProfile[x] > 0) {
                inColumn = true;
                startColumn = x;

            } else if (inColumn && verticalProjectionProfile[x] == 0) {
                // Detect end of column when no foreground pixels remain
                inColumn = false;

                int endColumn = x - 1; // Last column with the foreground pixels was the one before this

                if (endColumn - startColumn >= minTransitionWidth) {
                    columnBoundaries.add(startColumn);
                    columnBoundaries.add(endColumn);
                }
            }
        }

        // Add the image width to the transition during a case where column extends to image boundary
        if (inColumn) {
            columnBoundaries.add(startColumn);
            columnBoundaries.add(width - 1);
        }

        // If no transition was added, it will return the image
        if (columnBoundaries.isEmpty()) {
            System.out.println("No column was detected");
            columnCoordinates.add(new int[]{0, 0, width, height}); //add error
        }

        else {
            for (int i = 0; i < columnBoundaries.size(); i += 2 ) {
                int startX = columnBoundaries.get(i);
                int endX = columnBoundaries.get(i + 1);

                columnCoordinates.add(new int[] {startX, 0, endX, height - 1});

                // Crops and saves debug column image
                int[] segmentCoordinates = new int[]{startX, 0, endX, height - 1};
                BufferedImage columnImage = cropImageRegion(img, segmentCoordinates);
                saveDebugImage(columnImage, Test.BASE_OUTPUT_PATH + "/columns", "column_" + columnCoordinates.size() + ".png");
            }
        }
        return columnCoordinates;
    }

    /**
     * Segments individual words from a given text column using Horizontal Projection Profile (HPP).
     *
     * Word boundaries are determined by detecting transitions in horizontal pixel density.
     *
     *  @param img binary input image
     *  @param columnCoordinates list of column bounding boxes {x1, y1, x2, y2}
     *  @return list of word bounding boxes {x1, y1, x2, y2}
     */
    public static ArrayList<int[]> segmentWordsFromColumn(BufferedImage img, ArrayList <int[]> columnCoordinates) {
        ArrayList<int[]> wordCoordinates = new ArrayList<>();

        // Compute HPP for each column
        for (int i = 0; i < columnCoordinates.size(); i++) {
            int[] coords = columnCoordinates.get(i);
            int startX = coords[0];
            int startY = coords[1];
            int endX = coords[2];
            int endY = coords[3];
            boolean inWord = false;
            int wordStart = 0;

            int[] horizontalProjectionProfile = findHPP(img, startX, startY, endX, endY);

            // Transition is added when there is change in pixel values between two rows and the latter turns fully white
            for (int y = 1; y < horizontalProjectionProfile.length ; y++) {
                // Word starts when hpp has a value over 0
                if (!inWord && horizontalProjectionProfile[y] > 0) {
                    inWord = true;
                    wordStart = y;
                }

                // Word ends when hpp equals 0
                if (inWord && horizontalProjectionProfile[y] == 0) {
                    inWord = false;

                    // Saves the segmented word coordinates
                    int[] segmentCoordinates = new int[]{
                            startX,
                            startY + wordStart,
                            endX,
                            startY + y
                    };

                    wordCoordinates.add(segmentCoordinates);

                    // Crops and saves the debug word image
                    BufferedImage wordImage = cropImageRegion(img, segmentCoordinates);
                    saveDebugImage(wordImage, Test.BASE_OUTPUT_PATH + "/words", "column_" + i + "_word_" + wordCoordinates.size() + ".png");
                }
            }

            // If the word continues until the end of the column, add the transition until the end of the image
            if (inWord) {
                int[] segmentCoordinates = new int[] {
                        startX,
                        startY + wordStart,
                        endX,
                        endY
                };

                wordCoordinates.add(segmentCoordinates);

                // Crops and saves the debug word image
                BufferedImage wordImage = cropImageRegion(img, segmentCoordinates);
                saveDebugImage(wordImage, Test.BASE_OUTPUT_PATH + "/words", "column_" + i + "_lastword.png");
            }
        }

        return wordCoordinates;
    }

    /**
     * Segments a word into individual glyph candidates using projection-based heuristics.
     * Each glyph is then normalized into a fixed-size representation suitable for MLP input.
     *
     * Heuristic thresholds are adjusted based on word aspect ratio to improve segmentation stability.
     *
     * @param img input image
     * @param eachWordCoordinates bounding box of the word {x1, y1, x2, y2}
     * @param wordCount index of the word (used for debugging file naming)
     * @return list of normalized glyph images
     */
    public static ArrayList<BufferedImage> segmentGlyphsAndNormalize(BufferedImage img, int[] eachWordCoordinates, int wordCount) {
        ArrayList<BufferedImage> glyphImages = new ArrayList<>();
        ArrayList<int[]> glyphCoordinates = new ArrayList<>(); // variable to save the glyph coordinates
        ArrayList<Integer> glyphBoundaries = new ArrayList<>();

        int startX = eachWordCoordinates[0];
        int startY = eachWordCoordinates[1];
        int endX = eachWordCoordinates[2];
        int endY = eachWordCoordinates[3];

        // Saving the details of the original word image
        int wordWidth = endX - startX;
        int wordHeight = endY - startY;
        double leftZoneRatio = 0.0;
        int targetWidth = 0;
        int minSegmentHeight = 0;
        BufferedImage wordImg = img.getSubimage(startX, startY, wordWidth, wordHeight);

        double ratio = (double) wordWidth/(double) wordHeight;

       // Heuristic tuning based on word width and height ratio
        if (ratio >= 2.0) {
            targetWidth = 30;
            leftZoneRatio = 0.35;
        }
        else if ((2.0 > ratio)  && (ratio >= 0.9))  {
            targetWidth = 30;
            leftZoneRatio = 0.38;
            minSegmentHeight = 3;
        }
        else if ((0.9 > ratio)  && (ratio >= 0.40))  {
            targetWidth = 30;
            leftZoneRatio = 0.39;
            minSegmentHeight = 3;
        }
        else if ( 0.40 > ratio) {
            targetWidth = 40;
            leftZoneRatio = 0.40;
            minSegmentHeight = 4;
        }

        double scale = (double) targetWidth / wordWidth;
        int resizedHeight = (int) (wordHeight * scale);

        // Resize word image
        BufferedImage resizedWord = resizeImage(wordImg, targetWidth, resizedHeight);

        // Reassigning the coordinates for the scaled word image
        startX = 0;
        startY = 0;
        endX = resizedWord.getWidth();
        endY = resizedWord.getHeight();

        // The length of the left zone of the image is determined by the left zone ratio
        int leftZoneEnd = (int) (resizedWord.getWidth() * leftZoneRatio);

        // Compute HPP in left region for glyph boundary detection
        int[] horizontalProjectionProfile = findHPP(resizedWord, startX, startY, leftZoneEnd, endY);

        if (horizontalProjectionProfile.length > 0) {
            glyphBoundaries.add(startY); // The first glyph of the word always starts at the beginning of the word
        }

        // Detect vertical breaks between glyphs
        for (int i = 1; i < horizontalProjectionProfile.length - 1; i++) {

            // Transition is added when black pixel row turns to fully white
            if (horizontalProjectionProfile[i] == 0 && horizontalProjectionProfile[i - 1] > 0) {
                glyphBoundaries.add(i + 1);
            }
        }

        if (glyphBoundaries.isEmpty()) return glyphImages;

        // Checking whether or not to include the end of the image as a transition by comparing to the minimum height
        int lastStart = glyphBoundaries.get(glyphBoundaries.size()- 1);

        if (resizedWord.getHeight() - lastStart < minSegmentHeight && !glyphBoundaries.isEmpty()) {

            // Merge the end of the image with the last transition if the transition length is too short
            glyphBoundaries.set(glyphBoundaries.size() - 1, endY);

        } else if (lastStart < endY) {

            if (glyphBoundaries.isEmpty() || glyphBoundaries.get(glyphBoundaries.size() - 1) != endY) {
                glyphBoundaries.add(endY); // Add the last height coordinate to the transition if the previous transition wasn't long enough
            }
        }

        // Extract glyphs coordinates from transition boundaries
        for (int i = 0; i < glyphBoundaries.size() - 1; i++) {
            int startTransition = glyphBoundaries.get(i);
            int endTransition = glyphBoundaries.get(i + 1);

            // Skips duplicate or invalid transitions
            if (endTransition <= startTransition) {
                continue;
            }

            // Merges small segments that is less than the minHeight  with the next one
            if (endTransition - startTransition < minSegmentHeight) {
                if (i + 2 < glyphBoundaries.size()) {

                    // Merges the next transition
                    endTransition = glyphBoundaries.get(i + 2);

                    i++; // Skips to the next transition
                } else {
                    continue;
                }
            }

            int[] segmentCoordinates = new int[] { startX, startTransition, endX, endTransition };
            glyphCoordinates.add(segmentCoordinates);
            BufferedImage glyphImage = cropImageRegion(resizedWord, segmentCoordinates);

            // Normalize glyph for MLP input
            BufferedImage normalizedGlyphImage = normalizeGlyphImage(glyphImage);
            glyphImages.add(normalizedGlyphImage);

            // Saves the debug glyph image
            saveDebugImage(normalizedGlyphImage, Test.BASE_OUTPUT_PATH + "/glyphs", "word_" + wordCount + "_glyph_" + i + ".png");
        }

        return glyphImages;
    }

    /**
     * Computes the Horizontal Projection Profile (HPP) of a region.
     *
     * Each value represents the number of foreground pixels per row and is used
     * to detect horizontal segmentation boundaries.
     *
     *  @param img input image
     *  @param startX left boundary of region
     *  @param startY top boundary of region
     *  @param endX right boundary of region
     *  @param endY bottom boundary of region
     *  @return array representing horizontal pixel density per row
     */
    public static int[] findHPP (BufferedImage img, int startX, int startY, int endX, int endY) {
        int height = endY - startY;
        int[] hpp = new int[height];

        for (int y = startY; y < endY; y++) {
            int count = 0;

            for (int x = startX; x < endX; x++) {
                int pixel = img.getRGB(x, y) & 0xFF;

                // Count pixels that are close to black (foreground)
                if (pixel == FOREGROUND_THRESHOLD) {
                    count++;
                }
            }
            // Store the black pixel count for this row
            hpp[y - startY] = count;
        }
        return hpp;
    }

    /**
     * Resizes an image to fixed dimensions using nearest-neighbor interpolation.
     * This preserves binary edge structure and avoids smoothing artifacts.
     *
     * @param originalImage input image
     * @param targetWidth desired width
     * @param targetHeight desired height
     * @return resized image
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();

        // Use nearest-neighbor to avoid blurring edges in binary images
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Scale original image into target dimensions
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Normalizes a glyph image into a fixed-size canvas while preserving aspect ratio.
     *
     * The glyph is scaled and centered to ensure consistent spatial representation
     * for neural network input.
     *
     * @param glyphImage input glyph image
     * @return normalized 30x20 grayscale image
     */
    public static BufferedImage normalizeGlyphImage (BufferedImage glyphImage) {
        int targetWidth = 30;
        int targetHeight = 20;
        BufferedImage resizedGlyph = null;

        int width = glyphImage.getWidth();
        int height = glyphImage.getHeight();

        // Compute scale factor to fit glyph within target dimensions
        double scale = Math.min((double) targetWidth / width, (double) targetHeight / height);

        // Calculate new dimensions after scaling
        int scaledWidth = (int) Math.round(width * scale);
        int scaledHeight = (int) Math.round(height * scale);

        // Create scaled glyph image
        BufferedImage scaledImage = new BufferedImage(
                scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D gScaled = scaledImage.createGraphics();

        // Use high-quality scaling to preserve shape details
        gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gScaled.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); //focus on quality
        gScaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //reduce wonkiness
        // Draw resized glyph
        gScaled.drawImage(glyphImage, 0, 0, scaledWidth, scaledHeight, null); //draw the image to the scaled width and height
        gScaled.dispose();

        // Create final fixed-size canvas
        resizedGlyph = new BufferedImage( targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gOut = resizedGlyph.createGraphics();

        // Fill background with white to standardize input
        gOut.setColor(Color.WHITE);
        gOut.fillRect(0, 0, targetWidth, targetHeight); //makes the whole image background to white

        // Compute padding to center glyph
        int xSpace = (targetWidth - scaledWidth) / 2;
        int ySpace = (targetHeight - scaledHeight) / 2;

        // Place scaled glyph in center of canvas
        gOut.drawImage(scaledImage, xSpace, ySpace, null);
        gOut.dispose();

        return resizedGlyph;
    }

    /**
     * Converts a 2D glyph image into a normalized 1D feature vector.
     *
     * Pixel values are scaled to [0, 1] to serve as input for the MLP classifier.
     *
     * @param resizedGlyph normalized glyph image
     * @return flattened feature vector
     */
    public static double[] flattenImage(BufferedImage resizedGlyph) {
        int width = resizedGlyph.getWidth();
        int height = resizedGlyph.getHeight();
        double[] input = new double[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int pixel = resizedGlyph.getRGB(x, y) & 0xFF;

                // normalize intensity to [0,1] range
                input[y * width + x] = pixel / 255.0;
            }
        }
        return input;
    }

    /**
     * Saves intermediate processing results for debugging purposes.
     * Only executed when DEBUG mode is enabled.
     *
     * @param img image to save
     * @param folderPath destination folder path
     * @param fileName name of output file
     */
    public static void saveDebugImage(BufferedImage img, String folderPath, String fileName) {
        if (!Test.SAVE_PREPROCESSING_OUTPUTS) return;

        try {
            File dir = new File(folderPath);
            if (!dir.exists()) dir.mkdirs();

            ImageIO.write(img, "png", new File(dir, fileName));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
