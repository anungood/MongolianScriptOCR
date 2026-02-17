//Takes the input neurons and converts it into the output layer

public class Layer {
    //array of neurons from the Neuron class
    private Neuron[] neurons;

    //Constructor with the size of the hidden layer and the input layer
    public Layer(int hiddenLayerSize, int inputSize) {
        //empty array that can have Neuron class objects with the hidden layer size
        neurons = new Neuron[hiddenLayerSize];

        //Initializes every neuron in the hidden layer with the input size
        for (int i = 0; i < hiddenLayerSize; i++) {
            neurons[i] = new Neuron(inputSize);
        }
    }

    //For each neuron in the hidden layer, forward pass the neurons with the
    //activation function and save the output
    public double[] forward(double[] inputs) {
        //array to save the calculation after the forward pass for each neurons
        double[] outputs = new double[neurons.length];
        //uses the forward method in the Neuron class to calculate the activation function
        for (int i = 0; i < neurons.length; i++) {
            outputs[i] = neurons[i].forward(inputs);
        }

        return outputs;
    }

}

