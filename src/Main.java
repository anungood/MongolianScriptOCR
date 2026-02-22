//Main class that calls functions from other classes
//Will need to move the code in the Segmentation class's Main method later

public class Main {
    public static void main(String[] args) {

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

    }
}