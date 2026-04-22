import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Handles loading and preprocessing of the training dataset for the Mongolian Script OCR system.
 *
 * This class is responsible for converting a structured image dataset into a machine-learning
 * compatible format consisting of input feature vectors and corresponding one-hot encoded labels.
 *
 * The dataset is assumed to be organized in a hierarchical folder structure, where each folder
 * represents a glyph variant class and contains multiple PNG images of that class.
 *
 * Key responsibilities:
 * - Traversing dataset directory structure
 * - Extracting glyph class labels from folder names that has the format glyphName_variant
 * - Mapping glyph variants to their base glyph categories
 * - Converting images into normalized 1D feature vectors using preprocessing pipeline
 * - Generating one-hot encoded target vectors
 *
 * The output is a Dataset object containing:
 * - inputs: flattened image feature vectors
 * - targets: one-hot encoded class labels
 * - glyph and class mappings for evaluating the result
 */

/**
 * Dataset Processor class handles loading and preprocessing of the training dataset for the Mongolian Script OCR system.
 *
 * This class converts a folder-structured image dataset into a machine learning compatible format
 * consisting of input feature vectors and one-hot encoded labels.
 *
 * Each folder represents a glyph variant class, containing multiple image samples.
 */
public class DatasetProcessor {

    /**
     * Container class holding processed dataset data in MLP-ready format.
     */
    public static class Dataset {
        public double[][] inputs; // Flattened image vectors
        public double[][] targets; // One-hot encoded labels
        public ArrayList<String> glyphList; // Base glyphs
        public ArrayList<String> classList; // All variant classes (folder names)
        public HashMap<String, String> variantToGlyph; // Map different glyph variant classes to the same base glyph
    }

    /**
     * Loads dataset from a directory structure and converts images into feature vectors.
     *
     * Pipeline:
     * 1. Traverse dataset folders (each folder = one class)
     * 2. Extract glyph class and variant mapping from folder names
     * 3. Load PNG images from each folder
     * 4. Convert images into flattened feature vectors
     * 5. Generate one-hot encoded target labels
     *
     * @param datasetPath path to training dataset directory
     * @return Dataset object containing inputs and labels
     */
    public static Dataset loadDataset(String datasetPath) throws Exception {

        // Root dataset directory
        File datasetDir = new File(datasetPath);
        // All class folders inside dataset directory
        File[] folders = datasetDir.listFiles(File::isDirectory);

        // Storage for training data
        ArrayList<double[]> inputList = new ArrayList<>();
        ArrayList<double[]> targetList = new ArrayList<>();

        // Validate dataset existence
        if (folders == null || folders.length == 0) {
            throw new Exception("Dataset folder doesn't exist or it is empty!");
        }

        ArrayList<String> classList = new ArrayList<>(); // Store all the different variants of the glyphs
        HashMap<String, String> variantToGlyph = new HashMap<>(); // Map the variant to each glyph
        HashSet<String> glyphSet = new HashSet<>();

        for (File folder : folders) {
            String variantName = folder.getName(); // Gets full class name
            String glyph = variantName.split("_")[0]; // Base glyph label
            classList.add(variantName); // Store variant class
            variantToGlyph.put(variantName, glyph); // Maps the variant name to the glyph
            glyphSet.add(glyph); // Collect unique glyphs
        }

        // Unique glyph list (base classes)
        ArrayList<String> glyphList = new ArrayList<>(glyphSet);

        // Map each variant class to an index (for one-hot encoding)
        HashMap<String, Integer> glyphToIndex = new HashMap<>();
        for (int i = 0; i < classList.size(); i++) {
            glyphToIndex.put(classList.get(i), i);
        }

        for (File folder : folders) {

            String folderName = folder.getName();
            int classIndex = classList.indexOf(folderName);

            File[] images = folder.listFiles((dir, name) -> name.endsWith(".png"));

            // Skips empty folder
            if (images == null)  {
                continue;
            }

            // Process each image sample
            for (File imgFile : images) {
                BufferedImage img = ImageIO.read(imgFile);

                // Convert image to 1D feature vector using preprocessing pipeline
                double[] input = MongolianScriptPreprocessor.flattenImage(img);

                // Create one-hot encoded target vector
                double[] target = new double[classList.size()];
                Arrays.fill(target, 0.0);
                target[classIndex] = 1.0; // the index of the target glyph becomes 1 in the array

                // Store input feature vector and corresponding label
                inputList.add(input);
                targetList.add(target);
            }
        }

        // Convert dynamic lists into fixed-size arrays for MLP compatibility
        double[][] inputs = inputList.toArray(new double[0][]);
        double[][] targets = targetList.toArray(new double[0][]);

        // Construct a Dataset object to encapsulate inputs, labels, and associated mappings
        Dataset dataset = new Dataset();
        dataset.inputs = inputs;
        dataset.targets = targets;
        dataset.glyphList = glyphList;
        dataset.classList = classList;
        dataset.variantToGlyph = variantToGlyph;

        return dataset;
    }
}



