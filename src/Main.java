//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
         //loads dataset
         DatasetProcessor.Dataset dataset = DatasetProcessor.loadDataset("sampleTrainingDataset");
         double[][] inputs = dataset.inputs; // one image flattened
         double[][] targets = dataset.targets; // one-hot vector
         ArrayList<String> lettersList = dataset.lettersList;

         System.out.println("Dataset loaded!");
         System.out.println("Samples: " + inputs.length);
         System.out.println("Input size: " + inputs[0].length);
         System.out.println("Classes: " + targets[0].length);

         //variable declarations for mlp
         int inputSize = inputs[0].length;
         int[] hiddenLayers = {120};
         int outputNeurons = dataset.targets[0].length;
         double learningRate = 0.05;
         int epochs = 1000;

         //initializing MLP
         MLP mlp = new MLP(inputSize, hiddenLayers, outputNeurons);
         double[] input = new double[inputSize];

         for (int epoch = 0; epoch < epochs; epoch++) {
              double totalLoss = 0.0;
              for (int i = 0; i < inputs.length; i++) {
                   //forward pass
                   double[] output = mlp.forward(inputs[i]);
                   //softmax for the output
                   double[] predicted = MLP.softmax(output);
                   //cross entropy loss
                   double loss = 0.0;
                   for (int j = 0; j < predicted.length; j++) {
                        loss += -targets[i][j] * Math.log(predicted[j] + 1e-15); //prevent error when there is log(0)
                   }
                   totalLoss += loss;

                   //backpropagation
                   mlp.backward(predicted, targets[i], learningRate);
              }

              //print loss every 200 epochs
              if (epoch % 200 == 0) {
                   System.out.printf("Epoch %d - Loss: %.4f%n", epoch, totalLoss);
              }
         }

         //testing the mlp
         System.out.println("\nTesting:");

         for (int i = 0; i < inputs.length; i++) {
              double[] output = mlp.forward(inputs[i]);
              double[] predicted = MLP.softmax(output);

              System.out.println("Predicted: " + Arrays.toString(predicted));
              System.out.println("Target: " + Arrays.toString(targets[i]));
              System.out.println();
         }

         //loads the image that needs training
         String inputFileName = "dataset7.png";
         File file = new File(inputFileName);
         BufferedImage img = null;
         BufferedImage wordOnlyImage = null;

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

         //finds the bounding box of the text in the image
         int[] edgesCoordinates = Segmentation.detectWordSpace(img);

         //crops the image to the detected word space if coordinates for pixels are detected
         if (edgesCoordinates[0] == 0) {
              System.out.println("There was no word detected in the image.");
              System.exit(0); //The program is terminated if there is no black pixel detected
         } else {
              wordOnlyImage = Segmentation.crop(img, edgesCoordinates);
         }

         //binarizing the cropped image
         BufferedImage binarizedImage = Segmentation.binarize(wordOnlyImage);
         //segments the columns in the binarized image
         ArrayList<int[]> columnCoordinates = Segmentation.segmentColumns(binarizedImage);
         //finds the coordinates for every word in every column
         ArrayList<int[]> everyWordCoordinates = Segmentation.segmentWordsPerColumn(binarizedImage, columnCoordinates);
         ArrayList<double[]> allVectors = new ArrayList<>(); //save all the vectors that is being returned
         //calls the method to segment letters for each word and then resize it
         for (int w = 0; w < everyWordCoordinates.size(); w++) {
              int[] eachWordCoordinates = everyWordCoordinates.get(w);
              double[] vectorLetter = Segmentation.segmentLetters(binarizedImage, eachWordCoordinates, 40, inputFileName, w);
              allVectors.add(vectorLetter);
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

    }

}



