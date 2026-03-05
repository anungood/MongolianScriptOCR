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
        System.out.println("Inside neuron class");
        weights = new double[inputSize];

        // assigning random weights for each neurons with the random function
        for (int i = 0; i < inputSize; i++) {
            //nextDouble() gives random number between 0 (included) and 1 (not included)
            //multiplying by 2 and minusing 1 makes the number between -1 and 1
            //assigns positive and negative weights
            weights[i] = rand.nextDouble() * 2 - 1; // range: -1 to 1
            System.out.println("Weight " + i + " " + weights[i]);
        }
        //assigns random number for bias that is between -1 and 1
        bias = rand.nextDouble() * 2 - 1;

    }

    //forward pass to calculate z= weights*inputs+ bias
    public double forward(double[] inputs) {
        System.out.println("Inside neuron class's forward method");
        double sum = 0.0;
        //saving the inputs as it will be useful for backprop
        previousInputs = inputs.clone();

        //finding the sum of weights * inputs and bias
        for (int i= 0; i < weights.length; i++) {
            sum+= weights[i] * inputs[i];
        }
        sum+= bias;
        //saving the output as it will useful for backprop
        previousOutput = sigmoid(sum);
        return previousOutput;
    }

    // activation function: sigmoid(x) → 1 / (1 + e^-x)
    // make any input value between 0 and 1
    //makes it non-linear: output=σ(z)
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    //derivative of sigmoid function
    public double sigmoidDerivative() {
        return previousOutput * (1 - previousOutput);
    }

    //backward propagation method to compute error, store the error of the
    //previous layer, update weight and bias
    public double[] backward(double error, double learningRate) {
        //compute delta = error * sigmoid derivative
        double delta = error * sigmoidDerivative();

        //array to store error for previous layer
        double[] previousErrors = new double[weights.length];

        for (int i = 0; i < weights.length; i++) {
            // calculate error contribution to previous neuron
            previousErrors[i] = delta * weights[i];
            //update weight after calculation
            weights[i]-= learningRate * delta * previousInputs[i];
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
