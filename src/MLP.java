//Class to create the layers and functions between the Layer and Neuron class
public class MLP {
    //Variable Declaration
    private Layer[] layers;

    //Construction that take layer sizes as arguments
    //how much hidden layers to have? 1?
    public MLP(int inputSize, int[] hiddenLayerNeurons, int outputNeurons) {
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
        double[] output = input;

        for (Layer layer : layers) {
            output = layer.forward(output);
        }
        return output;
    }
}