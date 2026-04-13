//Takes the input neurons and converts it into the output layer

public class Layer {
    //array of neurons from the Neuron class
    private Neuron[] neurons;

    //Constructor with the size of the hidden layer and the input layer
    public Layer(int hiddenLayerSize, int inputSize) {
        //empty array that can have Neuron class objects with th hidden layer size
        System.out.println("Inside layer class");
        neurons = new Neuron[hiddenLayerSize];

        //Initializes every neuron in the hidden layer with the input size
        for (int i = 0; i < hiddenLayerSize; i++) {
            neurons[i] = new Neuron(inputSize);
        }
    }

    // Performs the forward pass for all neurons in the layer
    public double[] forward(double[] inputs, boolean useActivation) {
        // Array to store the output of each neuron in the layer
        double[] outputs = new double[neurons.length];
        // Forward propagate inputs through each neuron in the layer
        // Each neuron computes its weighted sum and applies activation based on the boolean variable
        for (int i = 0; i < neurons.length; i++) {
            outputs[i] = neurons[i].forward(inputs, useActivation);
        }
        // Return the layer's output vector
        return outputs;
    }

    //backward propagation for the whole layer
    public double[] backward(double[] errors, double learningRate, boolean isOutputLayer) {
        //array to store total error contribution from the previous layer
        double[] previousErrors = new double[neurons[0].getWeights().length];

        //loop through each neuron to calculate the error
        for (int i = 0; i < neurons.length; i++) {
            double[] neuronErrors = neurons[i].backward(errors[i], learningRate, !isOutputLayer);
            //sum all errors to calculate total errors that is contributing to the previous layer
            for (int j = 0; j < neuronErrors.length; j++) {
                previousErrors[j] += neuronErrors[j];
            }
        }
        //returns the total error to propagate to the previous layer
        return previousErrors;
    }
}

