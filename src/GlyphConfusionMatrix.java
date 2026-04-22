import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Evaluates the performance of the trained MLP model using a confusion matrix.
 *
 * This class performs model inference on a labeled dataset and computes:
 * - Confusion matrix (actual vs predicted classes)
 * - Overall classification accuracy
 * - Misclassification analysis (visual inspection of incorrect predictions)
 *
 * It also provides a visual interface for examining incorrectly classified samples,
 * supporting qualitative evaluation of model performance.
 */
public class GlyphConfusionMatrix {

    // Confusion matrix: rows = actual class, columns = predicted class
    private static int[][] confusionMatrix;

    // Stores misclassified samples for visualization
    private static List<BufferedImage> errorImages = new ArrayList<>();
    private static List<String> actualLabels = new ArrayList<>();
    private static List<String> predictedLabels = new ArrayList<>();

    // Tracks frequency of specific misclassification pairs (e.g., A → B)
    private static Map<String, Integer> confusionPairs = new HashMap<>();

    /**
     * Initializes confusion matrix with given number of classes.
     */
    public GlyphConfusionMatrix(int size) {
        confusionMatrix = new int[size][size];
    }

    /**
     * Runs evaluation on a dataset using a trained MLP model.
     *
     * Pipeline:
     * 1. Load dataset folders (each folder = class)
     * 2. Perform forward pass for each image
     * 3. Update confusion matrix
     * 4. Record misclassified samples
     * 5. Compute and display evaluation metrics
     *
     * @param mlp trained neural network model
     * @param datasetPath path to evaluation dataset
     */
    public static void runEvaluation(MLP mlp, String datasetPath) {

        File datasetDir = new File(datasetPath);
        File[] folders = datasetDir.listFiles(File::isDirectory);

        // Validate dataset
        if (folders == null || folders.length == 0) {
            System.out.println("Dataset not found.");
            return;
        }

        Arrays.sort(folders); // ensures consistent label ordering

        // Maps class labels to indices and vice versa
        Map<String, Integer> labelToIndex = new HashMap<>();
        Map<Integer, String> indexToLabel = new HashMap<>();

        int index = 0;
        for (File folder : folders) {
            String label = folder.getName();
            labelToIndex.put(label, index);
            indexToLabel.put(index, label);
            index++;
        }

        int numClasses = labelToIndex.size();

        // Initialize evaluation structures
        confusionMatrix = new int[numClasses][numClasses];
        confusionPairs.clear();
        errorImages.clear();
        actualLabels.clear();
        predictedLabels.clear();

        // Evaluation Loop
        for (File folder : folders) {

            String actualLabel = folder.getName();
            int actualIndex = labelToIndex.get(actualLabel);

            File[] images = folder.listFiles();
            if (images == null) continue;
            BufferedImage testImg = null;

            for (File img : images) {
                try {
                    testImg = ImageIO.read(img);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Convert image to feature vector
                double[] input = MongolianScriptPreprocessor.flattenImage(testImg);

                // Perform forward propagation
                double[] output = mlp.forward(input);

                // Get predicted class index (argmax)
                int predictedIndex = getPredictedLabel(output);

                // Update confusion matrix
                confusionMatrix[actualIndex][predictedIndex]++;

                // Track misclassifications
                if (actualIndex != predictedIndex) {
                    String key = indexToLabel.get(actualIndex) + " -> " + indexToLabel.get(predictedIndex);

                    // Count frequency of this confusion pair
                    confusionPairs.put(
                            key,
                            confusionPairs.getOrDefault(key, 0) + 1
                    );

                    // Store sample for visualization
                    errorImages.add(testImg);
                    actualLabels.add(indexToLabel.get(actualIndex));
                    predictedLabels.add(indexToLabel.get(predictedIndex));
                }
            }
        }

        // Display results
        printConfusionMatrix(confusionMatrix, indexToLabel);
        showMisclassifications();

        double acc = computeAccuracy(confusionMatrix);
        System.out.println("\nAccuracy: " + acc*100 + "%");
    }

    /**
     * Returns index of highest probability (argmax).
     */
    public static int getPredictedLabel(double[] outputs) {
        int maxIndex = 0;
        double maxVal = outputs[0];

        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > maxVal) {
                maxVal = outputs[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * Prints confusion matrix in formatted table form.
     */
    public static void printConfusionMatrix(int[][] matrix, Map<Integer, String> labels) {

        int n = matrix.length;

        int cellWidth = 18;
        int seperatorWidth = 8;

        String format = "%-" + cellWidth + "s";

        // Header row
        System.out.printf(format, "Actual\\Pred");

        for (int j = 0; j < n; j++) {
            System.out.printf(format, labels.get(j));
        }
        System.out.println();

        // Separator line
        for (int i = 0; i < seperatorWidth * (n + 1); i++) {
            System.out.print("=");
        }
        System.out.println();

        // Matrix rows
        for (int i = 0; i < n; i++) {

            System.out.printf(format, labels.get(i));

            for (int j = 0; j < n; j++) {
                System.out.printf(format, matrix[i][j]);
            }

            System.out.println();
        }
    }

    /**
     * Computes overall classification accuracy.
     *
     * Accuracy = correct predictions / total predictions
     */
    public static double computeAccuracy(int[][] matrix) {

        int correct = 0;
        int total = 0;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {

                total += matrix[i][j];

                if (i == j) {
                    correct += matrix[i][j];
                }
            }
        }

        return (double) correct / total;
    }


    /**
     * Displays misclassified samples with actual and predicted labels.
     *
     * Provides qualitative insight into model errors.
     */
    public static void showMisclassifications() {

        JFrame frame = new JFrame("Misclassified Samples");
        frame.setLayout(new GridLayout(0, 1));

        for (int i = 0; i < errorImages.size(); i++) {

            BufferedImage img = errorImages.get(i);

            JLabel label = new JLabel(
                    "Actual: " + actualLabels.get(i) +
                            " | Predicted: " + predictedLabels.get(i),
                    new ImageIcon(img),
                    JLabel.LEFT
            );

            frame.add(label);
        }

        frame.pack();
        frame.setVisible(true);
    }
}