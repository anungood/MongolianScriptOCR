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
         double[] sampleInput = preprocessImage(new File("test4.png"));
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
    }

     //method to read the image that needs predicting
     public static double[] preprocessImage(File imgFile) throws IOException {
          BufferedImage img = ImageIO.read(imgFile);
          int width = img.getWidth();
          int height = img.getHeight();
          double[] input = new double[width * height];

          //flattens the image and returns it
          for (int y = 0; y < height; y++) {
               for (int x = 0; x < width; x++) {
                    int pixel = img.getRGB(x, y) & 0xff; //assuming grayscale images
                    input[y * width + x] = pixel / 255.0;
               }
          }
          return input;
     }

}



