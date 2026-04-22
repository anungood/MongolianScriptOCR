/**
 * Represents a fully connected neural network layer within a Multilayer Perceptron (MLP)
 * that consist of neurons.
 *
 * This class is responsible for:
 * - Forward propagation: passing input data through all neurons in the layer
 * - Aggregating neuron outputs from forward propagation into a single output vector
 * - Backpropagation: distributing and combining gradients from the next layer
 *
 * The layer itself does not perform individual neuron computations, but instead
 * coordinates data flow and gradient flow between consecutive layers in the network.
 */

public class Layer {

    private Neuron[] neurons;

    /**
     * Initializes a fully connected layer.
     */
    public Layer(int hiddenLayerSize, int inputSize) {

        neurons = new Neuron[hiddenLayerSize];

        // Initialize neurons with  input size
        for (int i = 0; i < hiddenLayerSize; i++) {
            neurons[i] = new Neuron(inputSize);
        }
    }

    /**
     * Forward propagation through the layer.
     */
    public double[] forward(double[] inputs, boolean useActivation) {

        // Stores outputs produced by each neuron in this layer
        double[] outputs = new double[neurons.length];

        // The layer aggregates all neuron outputs into a single output vector
        // Delegates forward computation to each neuron
        for (int i = 0; i < neurons.length; i++) {
            outputs[i] = neurons[i].forward(inputs, useActivation);
        }

        return outputs;
    }

    /**
     * Backpropagation through the layer.
     */
    public double[] backward(double[] errors, double learningRate, boolean isOutputLayer) {

        double[] previousErrors = new double[neurons[0].getWeights().length];

        // Backpropagate error through each neuron
        for (int i = 0; i < neurons.length; i++) {
            double[] neuronErrors = neurons[i].backward(errors[i], learningRate, !isOutputLayer);

            // Accumulate gradients from all neurons
            for (int j = 0; j < neuronErrors.length; j++) {
                previousErrors[j] += neuronErrors[j];
            }
        }

        return previousErrors;
    }
}

