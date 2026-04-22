import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The Test class provides centralized control over evaluation and debugging
 * behavior within the OCR system.
 *
 * It defines global configuration flags that determine:
 * - which evaluation modes are enabled (glyph-level or full-text accuracy testing),
 * - whether intermediate preprocessing outputs are saved for debugging,
 * - and the directory where these outputs are stored.
 *
 * In addition, this class contains utility methods for evaluating full-text
 * recognition performance, which is the Levenshtein distance.
 *
 * Centralizing these settings and evaluation utilities improves modularity,
 * maintainability, and experimental flexibility without modifying core OCR logic.
 */
public class Test {

    // Enables or disables glyph-level evaluation using a confusion matrix
    public static boolean GLYPH_ACCURACY_TEST = false;

    // Enables or disables full-text evaluation using metrics character error rate
    public static boolean FULL_TEXT_ACCURACY_TEST = false;

    // Enables saving of intermediate preprocessing outputs (e.g., binarized or segmented images)
    // Useful for debugging and visual analysis of preprocessing steps
    public static boolean SAVE_PREPROCESSING_OUTPUTS = false;

    // Specifies the root directory where preprocessing outputs are stored.
    // Outputs are organized using the input image filename for easier traceability.
    public static String BASE_OUTPUT_PATH = "outputs/";

    /**
     * Computes the Levenshtein distance between two strings.
     *
     * This metric is used to measure full-text OCR accuracy by calculating
     * the minimum number of single-character edits (insertions, deletions,
     * substitutions) required to transform the predicted text into the actual text.
     *
     * @param predicted OCR output produced by the system
     * @param actual    ground truth text
     * @return          edit distance between predicted and actual text
     */
    public static int levenshteinDistance(String predicted, String actual) {
        int[][] editDistanceMatrix = new int[predicted.length() + 1][actual.length() + 1];

        // Initialize base cases
        for (int i = 0; i <= predicted.length(); i++) {
            editDistanceMatrix[i][0] = i;
        }
        for (int j = 0; j <= actual.length(); j++) {
            editDistanceMatrix[0][j] = j;
        }

        // Fills the edit distance matrix (or Dynamic Programming Table)
        for (int i = 1; i <= predicted.length(); i++) {
            for (int j = 1; j <= actual.length(); j++) {

                // Cost is 0 if characters match, otherwise 1
                int cost = (predicted.charAt(i - 1) == actual.charAt(j - 1)) ? 0 : 1;

                editDistanceMatrix[i][j] = Math.min(
                        Math.min(editDistanceMatrix[i - 1][j] + 1, // Deletion
                                editDistanceMatrix[i][j - 1] + 1), // Insertion
                        editDistanceMatrix[i - 1][j - 1] + cost // Substitution
                );
            }
        }

        // Final edit distance value
        return editDistanceMatrix[predicted.length()][actual.length()];
    }

    /**
     * Evaluates the performance of the OCR system using Character Error Rate (CER)
     * based on Levenshtein distance.
     *
     * This method tests the OCR model on a predefined set of input images and compares
     * the predicted text against manually provided ground truth strings.
     *
     * For each image:
     * - The image is loaded and passed through the OCR prediction pipeline.
     * - The predicted text is compared to the actual text using Levenshtein distance,
     *   which measures the minimum number of single-character edits (insertions,
     *   deletions, substitutions) required to transform one string into another.
     *
     * The Character Error Rate (CER) is computed as:
     * CER = (total number of edits across all samples) / (total number of characters
     * in the ground truth texts).
     *
     * A lower CER indicates better OCR accuracy.
     *
     * @return the overall Character Error Rate (CER) of the OCR system on the test set
     * @throws Exception if image loading or OCR prediction fails
     */
    public static double runlevenshteinDistance() throws Exception {

        // Paths to test images
        String[] imagePaths = {
                "textTest/textTest1.png",
                "textTest/textTest2.png",
                "textTest/textTest3.png",
                "textTest/textTest4.png",
                "textTest/textTest5.png"
        };

        // Ground truth (manually labeled correct text for each image)
        String actual1 = "ᠡᠡᠷᠤᠤᠯ ᠡᠡᠷᠡᠯ ᠡᠤᠷᠤᠨ ᠡᠡᠮᠡᠨ ᠮᠤᠢᠷᠣᠨ ᠨᠤᠢᠳᠡᠯ ᠲᠤᠮᠢᠨᠪ ᠪᠥᠢᠳᠡᠨ ᠡᠢᠷᠡᠡᠳᠤᠢ ᠡᠤᠯᠡᠨ ᠪᠣᠳᠤᠨ ᠪᠡᠷᠡᠮᠡᠨ ᠪᠣᠳᠡᠯ ᠨᠡᠡᠡᠳᠡᠨ ᠡᠤᠢᠳᠡᠨ ᠨᠡᠢᠮᠡᠨ ᠮᠤᠳᠤᠨᠨ ᠮᠡᠨᠡᠢ ᠡᠪᠥᠨ ᠮᠡᠷᠡᠯ ᠡᠷᠢᠪᠣᠯ ᠡᠣᠯᠢᠷᠯ ᠡᠣᠷᠢᠯ ᠷᠡᠳᠡᠮ ᠣᠡᠯ ᠷᠢᠯᠨ ᠡᠢᠷᠡᠮ ᠡᠯᠢᠳᠣᠷᠡᠯ ᠡᠣᠷᠣᠯ ᠪᠣᠷᠡᠮ ᠡᠢᠷᠡᠢᠯ ᠡᠷᠢᠲ ᠡᠤᠮᠢᠲ ᠪᠢᠷᠠᠲ ᠷᠢᠤᠲ ᠡᠢᠷᠡᠲ ᠡᠪᠥᠲ ᠡᠤᠷᠢᠲ ᠪᠢᠷᠡᠲ ᠷᠤᠢᠳᠢ ᠡᠡᠷᠢᠪᠣᠲ ᠡᠣᠪᠢᠷᠲ ᠪᠨᠮᠡᠯᠡᠨ ᠡᠢᠷᠢᠲ ᠡᠷᠡᠳᠡᠨ ᠡᠡᠮᠢᠳᠡᠨ ᠡᠣᠢᠷᠡᠲ ᠲᠡᠮᠡᠨᠠ ᠲᠡᠮᠡᠷᠡᠨ ᠲᠣᠮᠣᠳᠣᠢ ᠡᠮᠢᠯᠡᠪᠣᠨ ᠡᠣᠯᠢᠠᠨ ᠪᠣᠨᠢᠯᠲ ᠷᠣᠨᠤᠨ ᠡᠪᠣᠷᠢ ᠡᠷᠢᠮᠡᠲ ᠡᠣᠨᠢᠯᠳᠡᠮ ᠪᠣᠯᠢᠡᠲ ᠷᠢᠳᠣᠯᠨ ᠮᠠᠯᠡᠳᠡᠷᠢ ᠡᠷᠣᠢᠯ ᠡᠣᠨᠡᠢᠮ ᠪᠣᠯᠣᠢ ᠷᠢᠨᠣᠲ ᠡᠮᠣᠢᠯ ᠡᠣᠯᠡᠢᠮ ᠪᠣᠨᠣᠢᠯ ᠷᠢᠯᠣᠡᠮ ᠡᠨᠣᠮᠢ ᠪᠣᠮᠣᠨ ᠡᠣᠨᠢᠯᠣᠮ ᠪᠣᠯᠢᠣᠮ ᠡᠮᠢᠯᠣᠮ ᠡᠣᠯᠢᠮᠣᠯ ᠪᠣᠨᠢᠯᠣᠮ ᠷᠢᠯᠡᠯᠣᠯ ᠶᠡᠮᠯᠢ ᠡᠷᠢᠮᠯᠣᠮ ᠡᠣᠨᠢᠯᠮᠣᠯ ᠪᠣᠯᠢᠮᠣᠯ ᠡᠮᠢᠯᠮᠣᠯ ᠡᠣᠯᠢᠮᠯᠣᠮ ᠪᠣᠨᠢᠯᠮᠣᠯ ᠷᠢᠯᠡᠮᠯᠣᠮ ᠡᠨ ᠡᠣᠯ ᠪᠣᠮ ᠷᠢᠯ ᠡᠯ ᠡᠣᠮ ᠪᠣᠯ ᠷᠢᠨ ᠡᠮᠢ ᠡᠣᠮᠢ ᠪᠣᠨᠢ ᠷᠢᠮᠮ  ᠡᠷᠢᠮ ᠡᠣᠨᠢᠯ ᠪᠣᠯᠢ ᠷᠢᠡᠮ";
        String actual2 = "ᠡᠯᠢᠮ ᠡᠣᠯᠢᠯ ᠪᠣᠨᠢᠮ ᠷᠢᠯᠨ ᠡᠷᠣᠢᠮ ᠡᠣᠨᠡᠢᠯ ᠪᠣᠯᠣᠮ ᠷᠢᠨᠣᠯ ᠡᠯᠣᠢᠮ ᠡᠣᠯᠡᠢᠯ ᠪᠣᠨᠣᠢᠮ ᠷᠢᠯᠣᠨ ᠡᠷᠢᠮᠣᠮ ᠡᠣᠨᠢᠯᠣᠯ ᠪᠣᠯᠢᠣᠮ ᠡᠢᠯᠡᠮᠢ ᠡᠮᠢᠯᠣᠮ ᠡᠣᠯᠢᠮᠣᠯ ᠪᠣᠨᠢᠯᠣᠮ ᠷᠢᠯᠡᠮᠣᠯ ᠡᠢᠡᠮᠯᠢ ᠡᠢᠡᠮᠢ ᠡᠷᠣᠢᠯ ᠡᠣᠨᠡᠢᠮ ᠪᠣᠯᠣᠢ ᠷᠢᠨᠣᠲ ᠡᠢᠯᠢ ᠡᠮᠣᠢᠯ ᠡᠣᠯᠡᠢᠮ ᠪᠣᠨᠣᠢᠯ ᠷᠢᠯᠣᠡᠮ ᠡᠢᠨᠣᠮᠢ ᠡᠯ ᠡᠣᠮ ᠪᠣᠨ ᠷᠢᠯ ᠡᠢᠮ ᠡᠮ ᠡᠣᠯ ᠪᠣᠯ ᠷᠢᠨ ᠡᠢᠯ ᠡᠷᠪ ᠪᠣᠯᠢ ᠷᠢᠡᠮ ᠡᠢᠯᠪ ᠡᠮᠪ ᠡᠣᠯᠪ ᠪᠣᠨᠢ ᠡᠢᠡᠮ ᠡᠷᠢᠯ ᠡᠣᠨᠢᠮ ᠪᠣᠯᠢ ᠡᠢᠯᠢ ᠡᠮᠢᠮ ᠡᠣᠯᠢᠯ ᠪᠣᠨᠢᠮ ᠷᠢᠯᠡᠮ ᠡᠮᠣᠢᠯᠢ ᠡᠣᠯᠡᠢᠡᠮᠢ ᠪᠣᠨᠣᠢᠯᠢ ᠷᠢᠮᠣᠡᠯᠢ ᠡᠯᠨᠣᠮᠢ ᠡᠷᠢᠮᠣᠯᠢ ᠡᠣᠨᠡᠮᠣᠯᠢ ᠪᠣᠯᠡᠣᠮᠢ ᠡᠢᠮᠡᠯᠢ ᠡᠮᠡᠯᠣᠯᠢ ᠡᠣᠮᠢᠮᠣᠯᠢ ᠪᠣᠨᠢᠯᠣᠯᠢ ᠷᠯᠡᠯᠣᠮᠢ ᠡᠢᠡᠮᠢ ᠡᠷᠢᠯᠮᠣᠯᠢ ᠡᠣᠡᠮᠡᠯᠣᠯᠢ ᠪᠣᠯᠡᠮᠣᠯᠢ ᠡᠡᠯᠡᠯᠣᠢ ᠪᠣᠡᠮᠮᠣᠯᠢ ᠷᠢᠯᠡᠮᠯᠣᠮᠢ ᠡᠡᠢᠯᠲᠢ ᠡᠯᠢ ᠡᠣᠮᠢ ᠢᠮᠢ ᠡᠢᠨᠢ ᠡᠮᠢ ᠡᠣᠮᠢ ᠷᠢᠨᠢ ᠡᠢᠯᠢ ᠡᠢᠮᠢ ᠡᠣᠨᠢᠯᠢ ᠪᠣᠯᠢᠨᠢ ᠡᠢᠯᠨᠢ ᠡᠮᠡᠯᠢ ᠯᠣᠯᠢᠮᠢ ᠡᠨᠢᠯᠢ ᠡᠢᠮᠡᠯᠢ ᠡᠣᠷᠢᠯ ᠡᠣᠷᠡᠮ ᠡᠣᠮᠢᠯ ᠡᠢᠷᠣᠮ ᠡᠢᠮᠣᠯ";
        String actual3 = "ᠡᠣᠯᠢᠮ ᠡᠢᠯᠣᠮ ᠡᠣᠷᠢᠮ ᠡᠢᠷᠢᠮ ᠡᠣᠯᠢᠨ ᠪᠣᠷᠢᠯ ᠪᠣᠷᠡᠮ ᠪᠣᠮᠢᠯ ᠪᠢᠷᠣᠮ ᠪᠡᠮᠣᠯ ᠪᠣᠯᠢᠮ ᠪᠡᠯᠣᠮ ᠪᠣᠷᠢᠮ ᠪᠡᠷᠢᠮ ᠪᠣᠮᠢᠨ ᠨᠣᠷᠢᠯ ᠨᠣᠷᠡᠮ ᠨᠣᠯᠢᠯ ᠨᠢᠷᠣᠮ ᠨᠢᠮᠣᠯ ᠨᠣᠯᠢᠮ ᠨᠢᠯᠣᠮ ᠨᠣᠷᠢᠮ ᠨᠢᠷᠢᠮ ᠨᠣᠯᠢᠨ ᠨᠡᠣᠷᠢᠯ ᠨᠡᠨᠣᠷᠡᠮ ᠨᠡᠨᠡᠮᠢᠯ ᠨᠡᠷᠷᠣᠮ ᠨᠡᠷᠮᠣᠯ ᠨᠡᠣᠯᠢᠮ ᠨᠡᠡᠯᠣᠮ ᠡᠣᠷᠢᠮ ᠡᠢᠷᠡᠮ ᠨᠣᠮᠡᠨ ᠷᠣᠯᠢᠯ ᠷᠣᠯᠡᠮ ᠷᠣᠯᠡᠯ ᠷᠢᠯᠣᠮ ᠷᠢᠮᠣᠯ ᠷᠣᠯᠢᠮ ᠷᠢᠯᠣᠮ ᠷᠣᠯᠢᠮ ᠷᠢᠯᠢᠮ ᠷᠣᠯᠢᠨ ᠡᠣᠨᠢᠯ ᠡᠣᠨᠡᠮ ᠡᠣᠨᠢᠯ ᠡᠢᠨᠣᠮ ᠡᠢᠨᠣᠯ ᠡᠣᠯᠢᠮ ᠡᠢᠯᠣᠮ ᠡᠣᠨᠢᠮ ᠡᠢᠨᠢᠮ ᠡᠣᠨᠢᠨ ᠪᠣᠨᠢᠯ ᠪᠣᠨᠡᠮ ᠪᠣᠨᠢᠯ ᠪᠢᠨᠣᠮ ᠪᠡᠨᠣᠯ ᠪᠣᠯᠢᠮ ᠪᠨᠯᠣᠮ ᠪᠣᠨᠢᠮ ᠪᠨᠨᠢᠮ ᠪᠣᠨᠢᠨ ᠷᠣᠯᠢᠯ ᠷᠣᠯᠡᠮ ᠷᠣᠯᠡᠯ ᠷᠢᠯᠣᠮ ᠷᠢᠯᠣᠯ ᠷᠣᠯᠢᠮ ᠷᠢᠯᠣᠮ ᠷᠣᠯᠢᠮ ᠷᠢᠯᠢᠮ ᠷᠣᠯᠢᠨ ᠡᠣᠮᠡᠯ ᠡᠣᠮᠡᠮ ᠡᠣᠮᠷᠡᠯ ᠡᠷᠢᠯᠡᠮ ᠡᠢᠷᠮᠣᠯ ᠡᠣᠯᠢᠮ ᠡᠷᠢᠯᠡᠮ ᠡᠣᠷᠡᠮ ᠡᠷᠡᠮᠢᠮ ᠡᠣᠮᠷᠨ ᠪᠣᠯᠡᠯ ᠪᠣᠯᠡᠮ ᠪᠣᠯᠡᠯ ᠪᠡᠯᠣᠮ ᠪᠷᠯᠣᠯ ᠪᠣᠯᠡᠮ ᠪᠷᠯᠡᠮ ᠪᠣᠯᠡᠮ ᠪᠷᠯᠷᠮ ᠪᠣᠯᠷᠨ ᠡᠨ ᠡᠣᠯᠢ ᠪᠣᠯ ᠷᠢᠮ ᠡᠮᠢ";
        String actual4 = "ᠡᠣᠯᠢᠮ ᠪᠣᠨᠢ ᠷᠢᠯᠢ ᠡᠷᠢᠯᠢ ᠡᠣᠨᠢᠮ ᠪᠣᠯᠢᠯ ᠷᠢᠡᠳᠢ ᠡᠮᠢᠯ ᠡᠣᠯᠢᠮᠢ ᠪᠣᠨᠢᠯ ᠷᠢᠯᠣᠢ ᠡᠢᠮᠡᠯ ᠡᠷᠢᠮᠢ ᠡᠣᠨᠢᠯᠢ ᠪᠣᠯᠢᠨᠢ ᠷᠢᠡᠳᠢ ᠡᠢᠯᠨᠢ ᠡᠮᠢᠯᠢ ᠡᠣᠯᠢᠮᠢ ᠪᠣᠨᠢᠯᠢ ᠷᠢᠯᠡᠮᠢ ᠡᠢᠡᠮᠢ ᠡᠣᠮ ᠡᠣᠷᠢᠯ ᠪᠣᠷᠢ ᠷᠢᠮᠯ ᠡᠢᠯᠮ ᠡᠣᠯᠢ ᠡᠣᠷᠢᠮᠢ ᠪᠣᠯᠮ ᠷᠢᠨᠢ ᠡᠢᠮᠯ ᠡᠣᠷᠢᠮ ᠡᠣᠢᠯᠮ ᠪᠣᠨᠢᠯ ᠷᠢᠯᠢᠮ ᠡᠢᠨᠢᠯ ᠡᠣᠮᠢᠯ ᠡᠣᠢᠯᠢᠮ ᠪᠣᠷᠢᠯ ᠷᠢᠨᠢᠯ ᠡᠢᠯᠢᠮ ᠡᠣᠷᠢᠯᠢ ᠡᠣᠢᠮᠢᠯ ᠪᠣᠯᠢᠯ ᠷᠢᠯᠢᠯ ᠡᠢᠨᠷᠮ ᠡᠣᠮᠢᠯᠢ ᠡᠣᠢᠯᠢᠮᠢ ᠪᠣᠷᠢᠯᠢ ᠷᠢᠨᠢᠯᠢ ᠡᠢᠯᠢᠮᠢ ᠡᠣᠷᠢᠮᠯ ᠡᠣᠢᠮᠢᠯᠢ ᠪᠣᠯᠢᠮ ᠷᠢᠯᠢᠮᠢ ᠡᠢᠨᠢᠯᠢ ᠡᠣᠮᠢᠯᠢᠮ ᠡᠣᠢᠯᠢᠮᠢᠯ ᠪᠣᠷᠢᠯᠢᠮ ᠷᠢᠨᠢᠯᠢᠮ ᠡᠢᠯᠢᠮᠢᠯ ᠡᠣᠷᠢᠯᠢᠮ ᠡᠣᠢᠮᠢᠯᠢᠯ ᠪᠣᠯᠢᠮᠢᠯ ᠷᠢᠯᠢᠮᠢᠯ ᠡᠢᠨᠢᠯᠢᠮ ᠡᠣᠮᠢᠯᠢᠮᠢ ᠯᠢᠯᠢᠮᠢᠯᠢ ᠷᠢᠨᠢᠯᠢᠮᠢ ᠯᠢᠯᠢᠮᠢᠯᠢ ᠡᠣᠪᠣᠢᠮᠢ ᠡᠣᠢᠮᠢᠯᠢᠯᠢ ᠢᠯᠢᠮᠢᠯᠢ ᠯᠢᠨᠢᠯᠢᠮᠢ ᠡᠣᠯ ᠡᠣᠢᠮ ᠪᠣᠷᠢᠯ ᠷᠢᠮᠢ ᠡᠢᠯᠨ ᠡᠣᠷᠢ ᠡᠣᠢᠯᠢ ᠪᠣᠯᠢ ᠷᠢᠨᠢ ᠡᠢᠮᠢ ᠡᠣᠯᠢ ᠡᠣᠢᠯᠨ ᠪᠣᠷᠢᠮ ᠷᠢᠯᠢᠯ ᠡᠢᠨᠢᠮ ᠡᠣᠮᠢᠮ ᠡᠣᠢᠮᠢᠯ ᠪᠣᠯᠨᠢ ᠷᠢᠨᠢᠯ ᠡᠢᠯᠢᠮ ᠡᠣᠷᠢᠯᠢ ᠡᠣᠢᠯᠢᠮ ᠪᠣᠷᠢᠯ ᠷᠢᠮᠢᠯ ᠡᠢᠨᠢᠯ";
        String actual5 = "ᠡᠣᠮᠢᠯ ᠡᠣᠢᠯᠢᠮᠢ ᠪᠣᠯᠢᠯ ᠷᠢᠨᠢ ᠡᠢᠯᠢᠮᠢ ᠡᠣᠷᠢᠮᠯ ᠡᠣᠢᠮᠡᠯᠢ ᠪᠣᠷᠢᠯᠢ ᠷᠢᠯᠡᠮᠢ ᠡᠢᠨᠢᠯᠢ ᠡᠣᠯᠢᠮᠢ ᠡᠣᠢᠮᠢᠯᠢᠯ ᠪᠣᠯᠢᠮᠢ ᠷᠢᠨᠢᠯᠢᠮ ᠡᠢᠮᠢᠮᠢᠯ ᠡᠣᠷᠢᠮᠢᠮ ᠡᠣᠢᠮᠢᠯᠢᠮ ᠪᠣᠷᠢᠯᠢᠮ ᠷᠢᠯᠢᠮᠢᠯ ᠡᠢᠨᠢᠯᠢᠮ ᠡᠣᠯᠢᠮᠢᠯ ᠡᠣᠢᠮᠢᠯᠢᠯᠢ ᠪᠣᠯᠢᠮᠢᠯᠢ ᠷᠢᠨᠢᠯᠢᠯᠢ ᠡᠢᠯᠢᠮᠢᠯᠢ ᠡᠣᠷᠢᠯᠢᠮᠢ ᠡᠣᠢᠮᠢᠯᠢᠯᠢ ᠪᠣᠯᠢᠮᠢᠯᠢ ᠷᠢᠯᠢᠮᠢᠯᠢ ᠡᠢᠨᠢᠯᠢᠮᠢ ᠡᠣᠨ ᠡᠣᠢᠯ ᠪᠣᠮᠢ ᠷᠢᠯᠮ ᠡᠢᠮᠯ ᠡᠣᠷᠢ ᠡᠣᠢᠮᠢ ᠪᠣᠯᠢ ᠷᠢᠨᠢ ᠡᠢᠯᠢ ᠡᠣᠯᠢ ᠡᠣᠢᠯᠨ ᠪᠣᠷᠢᠮ ᠷᠢᠯᠢᠯ ᠡᠢᠨᠢᠮ ᠡᠣᠯᠢᠮ ᠡᠣᠢᠮᠢᠯ ᠪᠣᠯᠨᠢ ᠷᠢᠨᠢᠯ ᠡᠢᠮᠢᠮ ᠡᠣᠷᠢᠯ ᠡᠣᠢᠮᠢᠮ ᠪᠣᠷᠢᠯ ᠷᠢᠮᠢᠯ ᠡᠢᠨᠢᠯ ᠡᠣᠮᠢᠯ ᠡᠣᠢᠯᠢᠮᠢ ᠪᠣᠯᠢᠯ ᠷᠢᠨᠢᠯ ᠡᠢᠮᠢᠮᠢ ᠡᠣᠷᠢᠮᠯ ᠡᠣᠢᠮᠢᠯᠢ ᠪᠣᠷᠢᠮᠢ ᠷᠢᠮᠢᠮᠢ ᠡᠢᠨᠢᠮᠢ ᠡᠣᠯᠢᠯᠢ ᠡᠣᠢᠮᠢᠯᠢᠯ ᠪᠣᠯᠢᠯᠢ ᠷᠢᠨᠢᠮᠢᠮ ᠡᠢᠯᠢᠮᠢᠯ ᠡᠣᠷᠢᠮᠢᠮ ᠡᠣᠢᠮᠢᠯᠢᠮ ᠪᠣᠷᠢᠯᠢᠮ ᠷᠢᠯᠢᠮᠢᠯ ᠡᠢᠨᠢᠮᠢᠮ ᠡᠣᠯᠢᠮᠢᠯ ᠡᠣᠢᠮᠢᠯᠢᠯᠢ ᠪᠣᠯᠢᠮᠢᠯᠢ ᠢᠨᠢᠮᠢᠮᠢ ᠡᠢᠮᠢᠮᠢᠯᠢ ᠡᠣᠷᠯᠢᠮᠢ ᠡᠣᠢᠮᠢᠯᠢᠯᠢ ᠪᠣᠮᠢᠮᠡᠮᠢ ᠷᠢᠯᠢᠮᠡᠮᠢ ᠡᠢᠨᠡᠯᠢᠮᠢ ᠡᠣᠨᠢᠮᠢ ᠡᠣᠢᠯᠢᠯᠢ ᠪᠣᠡᠡᠮᠢ ᠷᠢᠮᠢᠮᠢ ᠡᠢᠨᠢᠮᠢ ᠡᠣᠮᠢᠮᠢᠯ ᠡᠣᠢᠮᠢᠯᠢᠮ ᠪᠣᠯᠢᠮᠢᠯ ᠷᠢᠨᠢᠯᠢᠯ ᠷᠢᠨᠢᠯ ᠡᠢᠳᠢ ᠡᠨᠣᠢᠯ ᠡᠣᠮᠡᠢᠮ ᠪᠣᠳᠣᠢ ᠡᠤᠨᠢᠮᠢ";

        // Array of ground truth texts aligned with image order
        String[] actualTexts = { actual1, actual2, actual3, actual4, actual5 };

        int totalEdits = 0; // cumulative Levenshtein distance (errors)
        int totalChars = 0; // total number of characters in ground truth
        double CER = 0.0;

        for (int i = 0; i < imagePaths.length; i++) {

            BufferedImage img = ImageIO.read(new File(imagePaths[i]));

            String predictedSentence = Main.predictTextForTest(img);

            // Get corresponding ground truth text
            String actual = actualTexts[i];

            // Levenshtein distance between prediction and ground truth
            int distance = Test.levenshteinDistance(predictedSentence, actual);

            int charCount = actual.length();

            // Accumulate total errors and total characters
            totalEdits += distance;
            totalChars += charCount;

            System.out.println("Image " + (i + 1) + " CER: " + ((double) distance / charCount));

            // CER = total edit distance / total number of characters
            CER = (double) totalEdits / totalChars;
        }

        // Return final averaged CER across dataset
        return CER;
    }
}
