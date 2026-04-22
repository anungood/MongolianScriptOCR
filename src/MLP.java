/**
 * Multilayer Perceptron (MLP) implementation for glyph classification in the Mongolian Script OCR system.
 *
 * This class represents the top-level neural network model responsible for coordinating both
 * training and prediction across multiple fully connected layers.
 *
 * The network is structured hierarchically as follows:
 * - Neuron: Basic computational unit that performs a weighted sum of inputs plus a bias and applies an
 *   activation function during forward propagation, and updates its parameters during backpropagation.
 *
 * - Layer: A collection of neurons that transforms input vectors into output vectors and
 *   manages the flow of activations and gradients between adjacent layers.
 *
 * - MLP: The overall model that connects multiple layers into a deep feedforward network,
 *   performing forward propagation to generate predictions and backpropagation to update
 *   all trainable parameters using gradient descent.
 *
 * This implementation supports classification by producing probability distributions over
 * output classes using a final softmax activation.
 */

public class MLP {
    private Layer[] layers;

    /**
     * Initializes MLP architecture from layer configuration.
     */
    public MLP(int inputSize, int[] hiddenLayerSize, int outputLayerSize) {

        int previousLayerSize = inputSize;

        // Number of layers to create including the output layer
        layers = new Layer[hiddenLayerSize.length + 1];

        // Creates hidden layer(s)
        for (int i = 0; i < hiddenLayerSize.length; i++) {
            layers[i] = new Layer(hiddenLayerSize[i], previousLayerSize);
            previousLayerSize = hiddenLayerSize[i];
        }

        // Creates output layer
        layers[layers.length - 1] = new Layer(outputLayerSize, previousLayerSize);
    }

    /**
     * Forward propagation through the network for glyph classification.
     */
    public double[] forward(double[] input) {
        double[] layerOutput = input;

        for (int i = 0; i < layers.length; i++) {

            // Apply activation for hidden layers
            boolean useActivation = (i != layers.length - 1);
            layerOutput = layers[i].forward(layerOutput, useActivation);
        }

        return softmax(layerOutput); // Applies softmax at the output layer
    }

    /**
     * Backpropagation for weight update.
     */
    public void backward(double[] predicted, double[] target, double learningRate) {
        double[] errors = new double[predicted.length];

        // Compute error at output layer (Categorical Cross Entropy + Softmax implementation)
        for (int i = 0; i < predicted.length; i++) {
            errors[i] = predicted[i] - target[i];
        }

        // Propagate error backward through all layers
        for (int i = layers.length - 1; i >= 0; i--) {

            // Start from last layer and move backward
            boolean isOutputLayer = (i == layers.length - 1);

            errors = layers[i].backward(errors, learningRate, isOutputLayer);
        }
    }

    /**
     * Softmax activation for calculating probability distribution in the output layer.
     */
    public static double[] softmax(double[] logits) {
        double max = logits[0];
        double sum = 0.0;
        double[] exponentNum = new double[logits.length];

        // Find max value for numerical stability
        for (double num : logits) {
            if (num > max) {
                max = num;
            }
        }

        // Compute exponential
        for (int i = 0; i < logits.length; i++) {

            // Max amount is subtracted to avoid overflow and large numbers
            exponentNum[i] = Math.exp(logits[i] - max);

            sum += exponentNum[i];
        }

        // Find the probabilities by dividing by sum
        for (int i = 0; i < logits.length; i++) {
            exponentNum[i] /= sum;
        }

        return exponentNum;
    }
}