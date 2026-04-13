//Class to create the layers and functions between the Layer and Neuron class
public class MLP {
    //Variable Declaration
    private Layer[] layers;

    //Construction that take layer sizes as arguments
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

    //does forward propagation with the activation function for each layer except the output layer because output layer uses softmax
    public double[] forward(double[] input) {
        double[] output = input;

        for (int i = 0; i < layers.length; i++) {
            //applies the activation function with sigmoid if it is not the output layer
            boolean applyActivation = (i != layers.length - 1);
            output = layers[i].forward(output, applyActivation);
        }
        return softmax(output); //applies softmax at the output layer automatically
    }

    //backward propagation for the MLP
    public void backward(double[] predicted, double[] target, double learningRate) {
        double[] errors = new double[predicted.length];
        //compute error at output layer
        for (int i = 0; i < predicted.length; i++) {
            errors[i] = predicted[i] - target[i];
        }

        //propagate error backward through all layers
        for (int i = layers.length - 1; i >= 0; i--) {
            //start from last layer and move backward
            boolean isOutputLayer = (i == layers.length - 1);
            errors = layers[i].backward(errors, learningRate, isOutputLayer);
        }
    }

    //softmax function to turn the output into probabilities for each glyph
    public static double[] softmax(double[] output) {
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