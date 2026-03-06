//Store weights, bias, compute weighted sum of each neurons.
//Apply activation function
import java.util.Random;

public class Neuron {
    //variable declarations
    private double[] weights;
    private double bias;
    private Random rand = new Random();
    //variables to save the last input and output z
    private double[] previousInputs;
    private double previousOutput;

    // constructor with the input size
    public Neuron(int inputSize) {
        weights = new double[inputSize];

        // assigning random weights for each neurons with the random function
        for (int i = 0; i < inputSize; i++) {
            //nextDouble() gives random number between 0 (included) and 1 (not included)
            //multiplying by 2 and minusing 1 makes the number between -1 and 1
            //assigns positive and negative weights
            weights[i] = (rand.nextDouble() -0.5) * 0.1; // range: -0.5 to 0.5
        }
        //assigns random number for bias that is between -0.5 and 0.5
        bias = (rand.nextDouble() -0.5) * 0.1;

    }

    // activation function: sigmoid(x) → 1 / (1 + e^-x)
    // make any input value between 0 and 1
    //makes it non-linear: output=σ(z)
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    //forward pass for every neuron
    public double forward(double[] inputs, boolean useActivation) {
        //z: variable to hold the sum of weighted sum+bias
        double sum = 0.0;
        //saving the inputs as it will be useful for backprop
        previousInputs = inputs.clone();

        //calculates z= weights*inputs + bias
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i] * inputs[i];
        }
        sum += bias;

        //applies sigmoid function if it is not the output layer
        if (useActivation) {
            previousOutput = sigmoid(sum);
        } else {
            previousOutput = sum;
        }
        //returns value between 0 and 1 for hidden layers and raw numbers for output layer
        return previousOutput;
    }

    //derivative of sigmoid function
    public double sigmoidDerivative() {
        return previousOutput * (1 - previousOutput);
    }

    //backward propagation for each neuron
    public double[] backward(double error, double learningRate, boolean useDerivative) {
        double delta;

        if (useDerivative) {
            //calculates delta = error * sigmoid'(z)
            //adjusted error
            delta = error * sigmoidDerivative();
        } else {
            //the output layer uses softmax and cross entropy so no need to apply the derivative
            delta = error;
        }

        //stores errors for the previous layer’s neurons
        double[] previousErrors = new double[weights.length];
        //propagate error to previous layer BEFORE updating weights
        for (int i = 0; i < weights.length; i++) {
            previousErrors[i] = delta * weights[i];
        }

        //update weights (gradient descent)
        for (int i = 0; i < weights.length; i++) {
            weights[i] -= learningRate * delta * previousInputs[i];
        }
        //update bias
        bias -= learningRate * delta;

        return previousErrors;
    }

    //getters and setters
    public double[] getLastInputs() {
        return previousInputs;
    }

    public double getLastOutput() {
        return previousOutput;
    }

    public double[] getWeights() {
        return weights;
    }
}
