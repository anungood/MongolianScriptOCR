//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
         //variable declarations
         int inputSize = 2;
         int[] hiddenLayers = {5};
         int outputNeurons = 2;
         double learningRate = 0.06;
         int epochs = 10000;
         MLP mlp = new MLP(inputSize, hiddenLayers, outputNeurons);

         //sample input and target data
         double[][] inputs = {
         {0, 5},
         {0, 3},
         {6, 0},
         {1, 2},
         {0.5, 0.5},
         };

         double[][] targets = {
         {0.6, 0.4 },
         {1, 0},
         {0, 1},
         {0.5, 0.5},
         {0.1, 0.9}
         };

         //sample training loop
         for (int epoch = 0; epoch < epochs; epoch++) {
              double totalLoss = 0.0;

              for (int i = 0; i < inputs.length; i++) {
                   //forward pass
                   double[] output = mlp.forward(inputs[i]);
                   //softmax probabilities
                   double[] predicted = MLP.softmax(output);

                   // calculate cross-entropy loss
                   double loss = 0.0;
                   for (int j = 0; j < predicted.length; j++) {
                        //Loss = - Σ target * log(prediction)
                        //adds 1e-15 to avoid mathematical error
                        loss += -targets[i][j] * Math.log(predicted[j] + 1e-15);
                   }
                   totalLoss += loss;

                   //backward pass
                   mlp.backward(predicted, targets[i], learningRate);

              }

              //print loss amount after every 500 epoch
              if (epoch % 500 == 0) {
                   System.out.println("Epoch: " + epoch + " Loss: " + totalLoss);

              }
         }

         System.out.println("Testing MLP:");
         for (int i = 0; i < inputs.length; i++) {
         double[] output = mlp.forward(inputs[i]);
         double[] predicted = MLP.softmax(output); //applies softmax to turn to probabilities
         //formatted output with string and floating point numbers to display the results
         System.out.printf("Input: %s, Predicted: [%f, %f], Target: [%f, %f]%n",
         Arrays.toString(inputs[i]),
         predicted[0], predicted[1],
         targets[i][0], targets[i][1]);
         }
    }

}


