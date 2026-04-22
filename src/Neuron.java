import java.util.Random;

/**
 * Represents a single computational unit (neuron) in a fully connected neural network.
 *
 * A neuron performs a linear transformation of its inputs followed by an optional
 * non-linear activation function, forming the fundamental building block of the MLP.
 *
 * This class is responsible for:
 * - Computing the weighted sum of inputs plus bias (linear transformation)
 * - Applying an activation function during forward propagation (if required)
 * - Storing intermediate values for backpropagation
 * - Updating weights and bias using gradient descent during training
 *
 */

public class Neuron {
    private double[] weights;
    private double bias;
    private Random rand = new Random();
    private double[] previousInputs;
    private double previousOutput;

    /**
     * Initializes neuron with random weights and bias.
     * Weights are initialized in a small range for stable training.
     */
    public Neuron(int inputSize) {
        weights = new double[inputSize];

        // Initialize weights in small range for stable training
        for (int i = 0; i < inputSize; i++) {
            weights[i] = (rand.nextDouble() - 0.5) * 0.1; // range: -0.05 to 0.05
        }

        // Bias initialized in range [-0.05, 0.05]
        bias = (rand.nextDouble() - 0.5) * 0.1;

    }

    /**
     * Sigmoid activation function.
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * Forward pass: computes z = w·x + b and applies activation if needed.
     */
    public double forward(double[] inputs, boolean useActivation) {

        double z = 0.0;
        // Saving the inputs as it will be useful for backpropagation
        previousInputs = inputs.clone();

        // Applies linear transformation
        for (int i = 0; i < weights.length; i++) {
            z += weights[i] * inputs[i];
        }
        z += bias;

        // Applies sigmoid function if it is not the output layer
        if (useActivation) {
            previousOutput = sigmoid(z);

        } else {
            previousOutput = z;
        }

        return previousOutput;
    }

    /**
     * Computes sigmoid derivative
     */
    private double sigmoidDerivative() {
        return previousOutput * (1 - previousOutput);
    }

    /**
     * Backpropagation step with gradient descent weight updates.
     */
    public double[] backward(double error, double learningRate, boolean useDerivative) {

        double delta;

        if (useDerivative) {
            // Calculates delta for the hidden layer
            delta = error * sigmoidDerivative();
        } else {
            // The output layer uses softmax and cross entropy so no need to apply sigmoid derivative
            delta = error;
        }

        // Stores errors for the previous layer’s neurons
        double[] previousErrors = new double[weights.length];

        // Propagate error to previous layer before updating weights
        for (int i = 0; i < weights.length; i++) {
            previousErrors[i] = delta * weights[i];
        }

        // Update weights
        for (int i = 0; i < weights.length; i++) {
            weights[i] -= learningRate * delta * previousInputs[i];
        }

        // Update bias
        bias -= learningRate * delta;

        return previousErrors;
    }

    public double[] getWeights() {
        return weights;
    }
}
