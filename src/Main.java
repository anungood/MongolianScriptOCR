//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        /**
        //variable declaration
        int inputSize = 30 * 20;
        int[] hiddenLayers = {64};
        int outputNeurons = 20;

        //initializing MLP
        MLP network = new MLP(inputSize, hiddenLayers, outputNeurons);
        //sample testing the MLP, Layer and segmentation class
        // Step 3: Example input (flattened 30x20 image)
        double[] input = new double[inputSize];

        //fill the input with random variables as my dataset isn't ready yet
        for (int i = 0; i < inputSize; i++) {
            input[i] = Math.random();
        }

        //forward pass through entire network
        double[] output = network.forward(input);
        //output
        System.out.println("Network Output:");
        for (double num : output) {
            System.out.println(num);
        }
        //turning output into probabilities
        double[] probOutput = network.softmax(output);
        System.out.println("Probability of Output:");
        for (double num : probOutput) {
            System.out.println(num);
        }

        //checking probability with sum
        double sum = 0;
        for (double p : probOutput) {
            sum += p;
        }
        System.out.println("Total sum: " + sum);
        **/

        //variable declarations
        int inputSize = 2;
        int[] hiddenLayers = {4};
        int outputNeurons = 2;
        double learningRate = 0.05;
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