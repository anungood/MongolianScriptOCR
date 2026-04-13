//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import com.sun.net.httpserver.HttpServer;

public class Main {

     static MLP mlp;
     static DatasetProcessor.Dataset dataset;

    public static void main(String[] args) throws Exception {
         //loads and trains dataset into a static variable
         dataset = DatasetProcessor.loadDataset("finalTrainingDataset");
         mlp = trainModel(dataset);

         System.out.println("Dataset loaded!");
         System.out.println("Samples: " + dataset.inputs.length);
         System.out.println("Input size: " + dataset.inputs[0].length);
         System.out.println("Classes: " + dataset.targets[0].length);
          //starts the HTTP server
         startServer();
    }
     //method to train the MLP by loading the dataset
    private static MLP trainModel(DatasetProcessor.Dataset dataset) {
         //variable declarations for mlp
         int inputSize = dataset.inputs[0].length;
         int[] hiddenLayers = {180};
         int outputNeurons = dataset.targets[0].length;
         double learningRate = 0.005;
         int epochs = 600;

         //initializing MLP
         MLP mlp = new MLP(inputSize, hiddenLayers, outputNeurons);
         double[] input = new double[inputSize];

         for (int epoch = 0; epoch < epochs; epoch++) {
              //shuffle dataset before each epoch
              shuffleDataset(dataset.inputs, dataset.targets);

              double totalLoss = 0.0;
              double averageLoss = 0.0;
              double loss = 0.0;

              for (int i = 0; i < dataset.inputs.length; i++) {
                   //forward pass
                   double[] predicted = mlp.forward(dataset.inputs[i]);

                   // Cathegorical cross entropy loss function to track loss during training
                   loss = 0.0;
                   for (int j = 0; j < predicted.length; j++) {
                        loss += -dataset.targets[i][j] * Math.log(predicted[j] + 1e-15); // 1e-15 is a small number to prevent error when there is log(0)
                   }

                   //backpropagation
                   mlp.backward(predicted, dataset.targets[i], learningRate);
              }

              totalLoss += loss;
              averageLoss = totalLoss/ dataset.count;

              //print loss every 200 epochs
              if (epoch % 100 == 0) {
                   System.out.printf("Epoch %d - Average Loss: %.4f%n", epoch, averageLoss);
              }
         }

         System.out.println("\nMLP training is finished.");

         return mlp;
    }
     //method to start the server, get the user uploaded image, and return the prediction
     private static void startServer() throws IOException {
          //creates Http Server on port 8080
          HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
          //serve the HTML page that is defined
          server.createContext("/", exchange -> {
               //path to the HTML file on the computer
               Path filePath = Paths.get("C:\\Users\\HP\\IdeaProjects\\SegmentationV1\\src\\index.html");
               byte[] bytes = Files.readAllBytes(filePath);
               exchange.getResponseHeaders().add("Content-Type", "text/html");
               exchange.sendResponseHeaders(200, bytes.length); //status 200: OK
               try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes); //loads the website on the browser
               }
          });
          //API endpoint to receive image request from the front-end
          server.createContext("/predict", exchange -> {
               //reads the header for "Mode" selected by the user: either text or glyph
               String mode = exchange.getRequestHeaders().getFirst("Mode");
               if (mode == null) mode = "text"; // default mode
               //allows the browser to communicate with the backend
               exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
               exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS"); //allows POST request
               exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, File-Name"); //allows custom headers

               //handles OPTIONS request that comes before the real browser request
               if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
               }

               //handle the POST request where the user uploads an image to the browser
               if ("POST".equals(exchange.getRequestMethod())) {
                    String result = "";
                    String fileName = exchange.getRequestHeaders().getFirst("File-Name"); //extracts the file name of the image
                    if (fileName == null) fileName = "uploaded.png"; //default file name

                    InputStream is = exchange.getRequestBody(); //gets the uploaded image
                    BufferedImage img = ImageIO.read(is); //turns the uploaded image into BufferedImage object

                    //feed the image into the predict methods depending on if it is for predicting glyph or text
                    if ("glyph".equalsIgnoreCase(mode)) {
                         result = predictSingleGlyph(img);
                    } else {
                         try {
                              result = predictText(img, fileName);
                         }
                         catch (Exception e) {
                              e.printStackTrace();
                              result = "Error processing image";
                         }
                    }

                    //send the predicted sentence back as HTTP response by converting the string into byte form
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
     //method to predict the glyph in the given image
     private static String predictSingleGlyph(BufferedImage img) {
          BufferedImage binarizedGlyphImage = Segmentation.binarize(img);
          double[] inputVector = Segmentation.flattenImage(binarizedGlyphImage);
          double[] prediction = mlp.forward(inputVector);

          int predictedIndex = 0;
          double maxProb = prediction[0];

          for (int i = 1; i < prediction.length; i++) {
               if (prediction[i] > maxProb) {
                    maxProb = prediction[i];
                    predictedIndex = i;
               }
          }

          String predictedFolder = dataset.classList.get(predictedIndex);
          String predictedGlyph = dataset.variantToGlyph.get(predictedFolder);
          return predictedGlyph;
     }

    private static String predictText (BufferedImage img, String inputFileName) throws Exception {
         //finds the bounding box of the text in the image
         BufferedImage textOnlyImage = null;
         //binarizing the input image
         BufferedImage binarizedImage = Segmentation.binarize(img);
         int[] edgesCoordinates = Segmentation.detectBoundingBox(binarizedImage);

         //crops the binarized image to a rectangle bounding box that contains the word region
         if (edgesCoordinates == null ) {
              System.out.println("There was no word detected in the image.");
              return "No text detected"; //returns error message if there is no black pixel detected
         } else {
              textOnlyImage = Segmentation.crop(binarizedImage, edgesCoordinates);
         }

         //segments the columns in the text image
         ArrayList<int[]> columnCoordinates = Segmentation.segmentColumns(textOnlyImage);
         //finds the coordinates for every word in every column
         ArrayList<int[]> everyWordCoordinates = Segmentation.segmentWordsPerColumn(textOnlyImage, columnCoordinates);
         StringBuilder predictedText = new StringBuilder(); //variable to keep the predicted words in the image

         for (int w = 0; w < everyWordCoordinates.size(); w++) {
              StringBuilder predictedWord = new StringBuilder(); //variable to keep the word from the predicted glyphs
              int[] eachWordCoordinates = everyWordCoordinates.get(w);
              //calls the method to segment glyphs for each word and then resize it
              ArrayList<BufferedImage> glyphs = Segmentation.segmentGlyphsAndNormalize(textOnlyImage, eachWordCoordinates, inputFileName, w);

              for (BufferedImage glyph : glyphs) {
                   //the glyph image is flattened and then fed to the MLP
                   double[] inputVector = Segmentation.flattenImage(glyph);
                   double[] prediction = mlp.forward(inputVector);
                   int predictedIndex = 0;
                   double maxProb = prediction[0];

                   for (int i = 1; i < prediction.length; i++) {
                        if (prediction[i] > maxProb) {
                             maxProb = prediction[i];
                             predictedIndex = i;
                        }
                   }
                   String predictedFolder = dataset.classList.get(predictedIndex);
                   String predictedGlyph = dataset.variantToGlyph.get(predictedFolder);

                   //append the predicted letter unless it is an endcut
                   if (!predictedGlyph.equals("0")) {

                        if ((predictedGlyph.equals("ᠢ") && ( w == 0))) {
                             predictedGlyph = "ᠵ";
                        }

                        //map predicted folder back to glyph
                        System.out.println("Predicted Glyph: " + predictedGlyph);
                        double maxPercentage = Math.min(maxProb * 100, 99.99); //turning the probability to percentage
                        System.out.printf("Probability: %.2f%%%n", maxPercentage);
                        predictedWord.append(predictedGlyph); //add the predicted glyph to word
                   }
              }

              System.out.println("Predicted word: " + predictedWord);
              predictedText.append(predictedWord); //adds the word to the predicted sentence
              predictedText.append(" "); //adds space between words
         }

         System.out.println("Predicted sentence: " + predictedText.toString().trim());
         return predictedText.toString().trim();
    }

    //shuffle dataset during training using Fisher–Yates shuffle algorithm
    private static void shuffleDataset(double[][] inputs, double[][] targets) {
         //initialize random number generator for shuffling
         Random rand = new Random();
         //ensure inputs and targets have the same length to maintain correct pairing
         if (inputs.length != targets.length) {
              throw new IllegalArgumentException("Inputs and targets must have same length");
         }

         for (int i = inputs.length - 1; i > 0; i--) {
              int j = rand.nextInt(i + 1); //picks a random position between 0 and i (inclusive)

              //swaps input vectors at indices i and j
              double[] tempInput = inputs[i];
              inputs[i] = inputs[j];
              inputs[j] = tempInput;

              //swaps corresponding target labels to maintain correct mapping with the input
              double[] tempTarget = targets[i];
              targets[i] = targets[j];
              targets[j] = tempTarget;
         }
    }
}



