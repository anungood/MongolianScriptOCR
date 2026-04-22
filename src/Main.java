import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

/**
 * Main
 *
 * This class is responsible for orchestrating the full pipeline of the system,
 * including dataset loading, model training, evaluation, and deployment via
 * a lightweight HTTP server interface.
 *
 * The system supports two operational modes:
 * 1. Glyph-level classification (single character recognition)
 * 2. Full-text recognition (end-to-end OCR pipeline)
 *
 * Workflow:
 * - Load preprocessed dataset from storage
 * - Train a Multi-Layer Perceptron (MLP) classifier
 * - Launch HTTP server for real-time OCR inference
 * - Return the predicted text to the browser
 */

public class Main {

     static MLP mlp;
     static DatasetProcessor.Dataset dataset;

    public static void main(String[] args) throws Exception {

         // Loads the preprocessed dataset and trains the MLP model
         dataset = DatasetProcessor.loadDataset("finalTrainingDataset");
         mlp = trainModel(dataset);

         System.out.println("Dataset loaded!");
         System.out.println("Samples: " + dataset.inputs.length);
         System.out.println("Input size: " + dataset.inputs[0].length);
         System.out.println("Classes: " + dataset.targets[0].length);

         // Flags controlling testing and debugging
         Test.SAVE_PREPROCESSING_OUTPUTS = false; // Enable saving of intermediate preprocessing stages for inspection
         Test.GLYPH_ACCURACY_TEST = false; // Run glyph-level classification evaluation (confusion matrix analysis)
         Test.FULL_TEXT_ACCURACY_TEST = false; // Performs full text accuracy test using CER if enabled

         // Execute glyph classification evaluation pipeline
         if (Test.GLYPH_ACCURACY_TEST) {
              GlyphConfusionMatrix.runEvaluation(mlp, "glyphTest");
         }

         // Execute full OCR pipeline evaluation using character error rate (CER)
         if (Test.FULL_TEXT_ACCURACY_TEST) {

              Test.BASE_OUTPUT_PATH = "output/fullTextTest";
              double CER = Test.runlevenshteinDistance();
              System.out.println("\nFinal CER: " + CER * 100 + "%");
              System.out.println("Accuracy (1 - CER): " + (1 - CER) * 100 + "%");
         }

         // Initializes and starts the embedded HTTP server
         startServer();
    }

     /**
      * Trains the Multi-Layer Perceptron (MLP).
      *
      * The training process uses:
      * - Forward propagation to compute predictions
      * - Categorical cross-entropy as the loss function
      * - Backpropagation for weight updates
      * - Implements Stochastic Gradient Descent (SGD)
      * - Fisher-Yates shuffle per epoch to improve generalization
      *
      * @param dataset dataset containing input feature vectors and one-hot encoded labels
      * @return trained MLP model ready for prediction
      */
    private static MLP trainModel(DatasetProcessor.Dataset dataset) {

         // Define the MLP architecture and parameters
         int inputSize = dataset.inputs[0].length;
         int[] hiddenLayers = {180};
         int outputNeurons = dataset.targets[0].length;
         double learningRate = 0.005;
         int epochs = 600;

         MLP mlp = new MLP(inputSize, hiddenLayers, outputNeurons);

         for (int epoch = 0; epoch < epochs; epoch++) {

              // Fisher–Yates dataset shuffle to reduce training bias
              shuffleDataset(dataset.inputs, dataset.targets);

              double totalLoss = 0.0;
              double averageLoss = 0.0;
              double loss = 0.0;

              for (int i = 0; i < dataset.inputs.length; i++) {

                   // Forward propagation through network
                   double[] predicted = mlp.forward(dataset.inputs[i]);

                   // Categorical cross entropy loss function to track loss during training
                   loss = 0.0;
                   for (int j = 0; j < predicted.length; j++) {
                        loss += -dataset.targets[i][j] * Math.log(predicted[j] + 1e-15); // 1e-15 ensures numerical stability term to avoid log(0) underflow
                   }

                   totalLoss += loss;

                   // Stochastic Gradient Descent based weight update with backpropagation
                   mlp.backward(predicted, dataset.targets[i], learningRate);
              }

              averageLoss = totalLoss/ dataset.inputs.length;

              // Periodic training loss monitoring
              if (epoch % 100 == 0) {
                   System.out.printf("Epoch %d - Average Loss: %.4f%n", epoch, averageLoss);
              }
         }

         System.out.println("\nMLP training is finished.");

         return mlp;
    }

     /**
      * Starts an embedded HTTP server that exposes the OCR system as a web service.
      *
      * The server provides two endpoints:
      * - "/" → serves the frontend HTML interface
      * - "/predict" → handles image uploads and returns OCR predictions
      *
      * Supports CORS headers for browser-based communication and
      * handles both glyph-level and full-text OCR modes depending on the request
      *
      * @throws IOException if server initialization or file access fails
      */
     private static void startServer() throws IOException {

          // Creates embedded HTTP server on port 8080
          HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

          // Serves the frontend HTML interface
          server.createContext("/", exchange -> {

               // Define frontend HTML path
               Path filePath = Paths.get("src", "index.html");
               byte[] bytes = Files.readAllBytes(filePath);

               // Sends the HTML page to the browser as a successful HTTP response
               exchange.getResponseHeaders().add("Content-Type", "text/html");
               exchange.sendResponseHeaders(200, bytes.length);

               try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
               }
          });

          // OCR prediction API endpoint for image upload requests
          server.createContext("/predict", exchange -> {

               // Reads OCR mode from request headers (glyph or text)
               String mode = exchange.getRequestHeaders().getFirst("Mode");
               if (mode == null) mode = "text"; // Default mode

               // Enables CORS for cross-origin frontend requests
               exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // Allows any website to call this server
               exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS"); // Allows POST request
               exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, File-Name"); // Allows custom headers

               // Handles OPTIONS request that comes before the real browser request
               if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
               }

               // Handle OCR prediction request with uploaded image
               if ("POST".equals(exchange.getRequestMethod())) {
                    String result = "";
                    String fileName = exchange.getRequestHeaders().getFirst("File-Name"); // Extracts the file name of the image
                    if (fileName == null) fileName = "uploaded.png"; // Default file name

                    InputStream is = exchange.getRequestBody();

                    // Decode binary image stream into buffered image format
                    BufferedImage img = ImageIO.read(is);

                    // Perform single glyph classification using the trained MLP model
                    if ("glyph".equalsIgnoreCase(mode)) {
                         result = predictSingleGlyph(img);
                    } else {
                         try {
                              // Perform full text prediction using the trained MLP model
                              result = predictText(img);
                         }
                         catch (Exception e) {
                              e.printStackTrace();
                              result = "Error processing image";
                         }
                    }

                    // Sends prediction response as byte array to client
                    byte[] responseBytes = result.getBytes();
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                         os.write(responseBytes);
                    }
               }
          });

          server.start();
          System.out.println("Server running at http://localhost:8080");
     }

     /**
      * Performs glyph-level classification on a single normalized image patch.
      *
      * The method assumes the input image is already resized to 30x20 pixels.
      * It applies binarization, forwards the feature vector through the MLP,
      * and selects the class with the highest probability.
      *
      * @param img input glyph image (30x20 pixels expected)
      * @return predicted glyph and confidence score
      */
     private static String predictSingleGlyph(BufferedImage img) {
          int width = img.getWidth();
          int height = img.getHeight();

          // Enforce fixed input size constraint for glyph classification
          if ((width != 30) || (height != 20)) {
               return "Wrong glyph dimension";
          }

          // Apply preprocessing pipeline before prediction
          BufferedImage binarizedGlyphImage = MongolianScriptPreprocessor.binarizeImage(img);
          double[] inputVector = MongolianScriptPreprocessor.flattenImage(binarizedGlyphImage);

          double[] prediction = mlp.forward(inputVector);

          int predictedIndex = 0;
          double maxProb = prediction[0];

          for (int i = 1; i < prediction.length; i++) {
               if (prediction[i] > maxProb) {
                    maxProb = prediction[i];
                    predictedIndex = i;
               }
          }

          // Extract predicted glyph
          String predictedFolder = dataset.classList.get(predictedIndex);
          String predictedGlyph = dataset.variantToGlyph.get(predictedFolder);

          //System.out.println(predictedGlyph + "Confidence score: " + String.format("%.2f", maxProb * 100) + "%");
          return predictedGlyph + "\n" + String.format("%.2f", maxProb * 100) + "%)" + "\n(Confidence Score: ";
     }

     /**
      * Performs full-text OCR by segmenting an input image into hierarchical components:
      * text region → columns → words → glyphs.
      *
      * Each glyph is classified using the trained MLP model, and results are merged
      * into a reconstructed sentence.
      *
      * Heuristics are applied to:
      * - Skip undersegmented glyph endcuts that has the folder names starting with "0"
      * - Remove duplicate glyph predictions caused by the predefined encoding rules of Mongolian Script keyboard
      *
      * @param img input image containing Mongolian script text
      * @return reconstructed predicted sentence
      * @throws Exception if segmentation or processing fails
      */
    private static String predictText (BufferedImage img) throws Exception {

         // Detect text region using bounding box extraction
         BufferedImage textRegionImage = null;

         // Convert image to binary representation for segmentation
         BufferedImage binarizedImage = MongolianScriptPreprocessor.binarizeImage(img);
         int[] edgesCoordinates = MongolianScriptPreprocessor.detectTextBoundingBox(binarizedImage);
         int maxWordCount = 100;

         if (edgesCoordinates == null ) {

              // Handle empty detection case (no foreground text found)
              System.out.println("There was no word detected in the image.");
              return "No text detected";

         } else {
              textRegionImage = MongolianScriptPreprocessor.cropImageRegion(binarizedImage, edgesCoordinates);
         }

         // Perform column segmentation using projection profiles
         ArrayList<int[]> columnCoordinates = MongolianScriptPreprocessor.segmentTextColumns(textRegionImage);

         // Perform word segmentation
         ArrayList<int[]> everyWordCoordinates = MongolianScriptPreprocessor.segmentWordsFromColumn(textRegionImage, columnCoordinates);

         if (everyWordCoordinates.isEmpty()) {
              return ("Word not detected");
         }
         else if (everyWordCoordinates.size() > maxWordCount) {
              return ("Max Word Count Reached");
         }

         StringBuilder predictedText = new StringBuilder();

         for (int w = 0; w < everyWordCoordinates.size(); w++) {

              StringBuilder predictedWord = new StringBuilder();
              int[] eachWordCoordinates = everyWordCoordinates.get(w);

              // Extract and normalize glyphs from segmented word region
              ArrayList<BufferedImage> glyphs = MongolianScriptPreprocessor.segmentGlyphsAndNormalize(textRegionImage, eachWordCoordinates, w);

              if (glyphs.isEmpty()) {
                   return ("No glyph detected");
              }

              String previousGlyph = "";

              for (BufferedImage glyph : glyphs) {

                   // Convert glyph image into feature vector for classification
                   double[] inputVector = MongolianScriptPreprocessor.flattenImage(glyph);
                   double[] prediction = mlp.forward(inputVector);

                   int predictedIndex = 0;
                   double maxProb = prediction[0];

                   for (int i = 1; i < prediction.length; i++) {
                        if (prediction[i] > maxProb) {
                             maxProb = prediction[i];
                             predictedIndex = i;
                        }
                   }

                   // Get glyph label from predicted index
                   String predictedFolder = dataset.classList.get(predictedIndex);
                   String predictedGlyph = dataset.variantToGlyph.get(predictedFolder);

                   // Heuristic to suppress duplicated glyph artifacts introduced by encoding of Mongolian Script keyboard
                   if ((predictedGlyph.equals("ᠢ") && previousGlyph.equals("ᠢ"))) {
                    continue;
                   }

                   // Append the predicted glyph unless it is an undersegmented part
                   if (!predictedGlyph.equals("0")) {

                        previousGlyph = predictedGlyph;
                        System.out.println("Predicted Glyph: " + predictedGlyph);

                        // Converts confidence score to percentage format
                        double maxPercentage = Math.min(maxProb * 100, 99.99);
                        System.out.printf("Probability: %.2f%%%n", maxPercentage);

                        // Accumulates glyph predictions to make up the full word
                        predictedWord.append(predictedGlyph);
                   }
              }

              System.out.println("Predicted word: " + predictedWord);

              // Stores reconstructed OCR output sentence
              predictedText.append(predictedWord);
              predictedText.append(" ");
         }

         // System.out.println("Predicted sentence: " + predictedText.toString().trim());
         return predictedText.toString().trim();
    }

     /**
      * Randomly shuffles dataset samples using the Fisher–Yates algorithm.
      *
      * Ensures that input feature vectors and corresponding labels remain aligned
      * after permutation. This improves training stability and reduces ordering bias.
      *
      * @param inputs feature vectors
      * @param targets one-hot encoded labels
      */
     private static void shuffleDataset(double[][] inputs, double[][] targets) {

          // Initialize random number generator for shuffling
          Random rand = new Random();

          // Ensure inputs and targets have the same length to maintain correct pairing
          if (inputs.length != targets.length) {
               throw new IllegalArgumentException("Inputs and targets must have same length");
          }

          for (int i = inputs.length - 1; i > 0; i--) {
               int j = rand.nextInt(i + 1); // Picks a random position between 0 and i (inclusive)

               // Swaps input vectors at indices i and j
               double[] tempInput = inputs[i];
               inputs[i] = inputs[j];
               inputs[j] = tempInput;

               // Swaps corresponding target labels to maintain correct mapping with the input
               double[] tempTarget = targets[i];
               targets[i] = targets[j];
               targets[j] = tempTarget;
          }
     }

     /**
      * Wrapper method used for testing purposes to access the OCR prediction logic.
      * It delegates the input image to the internal prediction method and returns
      * the recognized text output.
      *
      * This method is intended for evaluation and benchmarking, allowing external
      * test classes to invoke the prediction pipeline while preserving encapsulation
      * of the core implementation.
      *
      * @param img the input BufferedImage containing the text to be recognized
      * @return the predicted text extracted from the image
      * @throws Exception if an error occurs during the prediction process
      */
     public static String predictTextForTest (BufferedImage img) throws Exception{
          return predictText(img);
     }
}
