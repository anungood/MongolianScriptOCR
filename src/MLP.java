//Class to create the layers and functions between the Layer and Neuron class
public class MLP {
    //Variable Declaration
    private Layer[] layers;

    //Construction that take layer sizes as arguments
    //how much hidden layers to have? 1?
    public MLP(int inputSize, int[] hiddenLayerNeurons, int outputNeurons) {
        System.out.println("Inside MLP class");
        //saves the input layer size
        int previousLayerSize = inputSize;
        //number of layers to create including the output layer
        layers = new Layer[hiddenLayerNeurons.length + 1];

        //creates hidden layer
        for (int i = 0; i < hiddenLayerNeurons.length; i++) {
            layers[i] = new Layer(hiddenLayerNeurons[i], previousLayerSize);
            previousLayerSize = hiddenLayerNeurons[i];
        }

        //creates output layer
        layers[layers.length - 1] = new Layer(outputNeurons, previousLayerSize);
    }

    //does forward propagation for all tha layers and neurons
    public double[] forward(double[] input) {
        System.out.println("MLP class's forward method");
        double[] output = input;

        for (Layer layer : layers) {
            output = layer.forward(output);
        }
        return output;
    }

    //softmax function to turn the output into probabilities for each letter
    public static double[] softmax(double[] output) {
        System.out.println("MLP class's softmax method");
        //variable declarations
        double max = output[0];
        double sum = 0.0;
        double[] exponentNum = new double[output.length];

        //finding the maximum number in the output array-> used for normalization
        for (double num : output) {
            if (num > max) {
                max = num;
            }
        }
        
        //funds sum of the exponent for each numbers in the array
        for (int i = 0; i < output.length; i++) {
            //max amount is subtracted to avoid overflow and large numbers
            exponentNum[i] = Math.exp(output[i] - max);
            sum +=exponentNum[i];
        }

        //find the probabilities by dividing by sum
        for (int i = 0; i < output.length; i++) {
            exponentNum[i] /= sum;
        }

        return exponentNum;
    }
}