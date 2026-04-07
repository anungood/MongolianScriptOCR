//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import com.sun.net.httpserver.HttpServer;

public class Main {

     static MLP mlp;
     static DatasetProcessor.Dataset dataset;

    public static void main(String[] args) throws Exception {
         //loads and trains dataset into a static variable
         dataset = DatasetProcessor.loadDataset("sampleTrainingDataset");
         mlp = trainModel(dataset);

         System.out.println("Dataset loaded!");
         System.out.println("Samples: " + dataset.inputs.length);
         System.out.println("Input size: " + dataset.inputs[0].length);
         System.out.println("Classes: " + dataset.targets[0].length);
          //starts the HTTP server
         startServer();
          /**
           get the image from website
         String inputFileName = "segMLPtest.png";
         File file = new File(inputFileName);
         BufferedImage img = null;

         //check if the image exists
         try {
              img = ImageIO.read(file);
         } catch (IOException e) {
              e.printStackTrace();
              System.exit(1);
         }

         if (img != null) {
              Segmentation.display(img);
         }
           **/

    }
     //method to train the MLP by loading the dataset
    private static MLP trainModel(DatasetProcessor.Dataset dataset) {
         //variable declarations for mlp
         int inputSize = dataset.inputs[0].length;
         int[] hiddenLayers = {180};
         int outputNeurons = dataset.targets[0].length;
         double learningRate = 0.05;
         int epochs = 800;

         //initializing MLP
         MLP mlp = new MLP(inputSize, hiddenLayers, outputNeurons);
         double[] input = new double[inputSize];

         for (int epoch = 0; epoch < epochs; epoch++) {
              double totalLoss = 0.0;
              for (int i = 0; i < dataset.inputs.length; i++) {
                   //forward pass
                   double[] output = mlp.forward(dataset.inputs[i]);
                   //softmax for the output
                   double[] predicted = MLP.softmax(output);
                   //cross entropy loss
                   double loss = 0.0;
                   for (int j = 0; j < predicted.length; j++) {
                        loss += -dataset.targets[i][j] * Math.log(predicted[j] + 1e-15); //prevent error when there is log(0)
                   }
                   totalLoss += loss;

                   //backpropagation
                   mlp.backward(predicted, dataset.targets[i], learningRate);
              }

              //print loss every 200 epochs
              if (epoch % 200 == 0) {
                   System.out.printf("Epoch %d - Loss: %.4f%n", epoch, totalLoss);
              }
         }

         System.out.println("\nMLP training is finished.");
         /**
         //testing the mlp
         System.out.println("\nTesting:");

         for (int i = 0; i < dataset.inputs.length; i++) {
              double[] output = mlp.forward(dataset.inputs[i]);
              double[] predicted = MLP.softmax(output);

              System.out.println("Predicted: " + Arrays.toString(predicted));
              System.out.println("Target: " + Arrays.toString(dataset.targets[i]));
              System.out.println();
         }
          **/
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
               exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, File-Name"); //allows customer headers

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

                    //feed the image into the predict methods depending on if it is for prediciting glyph or text
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
          double[] inputVector = Segmentation.preprocessImage(img);
          double[] output = mlp.forward(inputVector);
          double[] prediction = MLP.softmax(output);

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
         BufferedImage wordOnlyImage = null;
         int[] edgesCoordinates = Segmentation.detectWordSpace(img);

         //crops the image to the detected word space if coordinates for pixels are detected
         if (edgesCoordinates == null ) {
              System.out.println("There was no word detected in the image.");
              return "No text detected"; //The program is terminated if there is no black pixel detected
         } else {
              wordOnlyImage = Segmentation.crop(img, edgesCoordinates);
         }

         //binarizing the cropped image
         BufferedImage binarizedImage = Segmentation.binarize(wordOnlyImage);
         //segments the columns in the binarized image
         ArrayList<int[]> columnCoordinates = Segmentation.segmentColumns(binarizedImage);
         //finds the coordinates for every word in every column
         ArrayList<int[]> everyWordCoordinates = Segmentation.segmentWordsPerColumn(binarizedImage, columnCoordinates);
         StringBuilder predictedText = new StringBuilder(); //variable to keep the predicted words in the image

         for (int w = 0; w < everyWordCoordinates.size(); w++) {
              StringBuilder predictedWord = new StringBuilder(); //variable to keep the word from the predicted glyphs
              int[] eachWordCoordinates = everyWordCoordinates.get(w);
              //calls the method to segment glyphs for each word and then resize it
              ArrayList<BufferedImage> glyphs = Segmentation.segmentGlyphsAndResize(binarizedImage, eachWordCoordinates, inputFileName, w);

              for (BufferedImage glyph : glyphs) {
                   //the glyph image is flattened and then fed to the MLP
                   double[] inputVector = Segmentation.preprocessImage(glyph);
                   double[] output = mlp.forward(inputVector);
                   double[] prediction = MLP.softmax(output); //softmax probability
                   int predictedIndex = 0;
                   double maxProb = prediction[0];

                   for (int i = 1; i < prediction.length; i++) {
                        if (prediction[i] > maxProb) {
                             maxProb = prediction[i];
                             predictedIndex = i;
                        }
                   }

                   //map predicted folder back to glyph
                   String predictedFolder = dataset.classList.get(predictedIndex);
                   String predictedGlyph = dataset.variantToGlyph.get(predictedFolder);
                   System.out.println("Predicted Glyph: " + predictedGlyph);
                   double maxPercentage = Math.min(maxProb * 100, 99.99); //turning the probability to percentage
                   System.out.printf("Probability: %.2f%%%n", maxPercentage);
                   predictedWord.append(predictedGlyph); //add the predicted glyph to word
              }
              System.out.println("Predicted word: " + predictedWord.toString());
              predictedText.append(predictedWord.toString()); //adds the word to the predicted sentence
              predictedText.append(" "); //adds space between words
         }

         System.out.println("Predicted sentence: " + predictedText.toString().trim());
         return predictedText.toString().trim();
    }
}


/**
 double[] sampleInput = vectorLetter;
 double[] output = mlp.forward(sampleInput);
 double[] prediction = MLP.softmax(output); //softmax probability
 int predictedIndex = 0;
 double maxProb = prediction[0];

 for (int i = 1; i < prediction.length; i++) {
 if (prediction[i] > maxProb) {
 maxProb = prediction[i];
 predictedIndex = i;
 }
 }

 //map index back to letter
 String predictedLetter = lettersList.get(predictedIndex); // lettersList from DatasetProcessor
 System.out.println("Predicted letter: " + predictedLetter);
 double maxPercentage = Math.min(maxProb * 100, 99.99); //turning the probability to percentage
 System.out.printf("Probability: %.2f%%%n", maxPercentage);
 **/


