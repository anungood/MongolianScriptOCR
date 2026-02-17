//Store weights, bias, compute weighted sum of each neurons.
//Apply activation function
import java.util.Random;

public class Neuron {
    //variable declarations
    private double[] weights;
    private double bias;
    private Random rand = new Random();

    // constructor with the input size
    public Neuron(int inputSize) {
        weights = new double[inputSize];

        // assigning random weights for each neurons with the random function
        for (int i = 0; i < inputSize; i++) {
            //nextDouble() gives random number between 0 (included) and 1 (not included)
            //multiplying by 2 and minusing 1 makes the number between -1 and 1
            //assigns positive and negative weights
            weights[i] = rand.nextDouble() * 2 - 1; // range: -1 to 1
        }
        //assigns random number for bias that is between -1 and 1
        bias = rand.nextDouble() * 2 - 1;
    }

    //forward pass to calculate z= weights*inputs+ sum
    public double forward(double[] inputs) {
        double sum = 0.0;

        for (int i= 0; i < weights.length; i++) {
            sum+= weights[i] * inputs[i];
        }

        sum+= bias;

        return sum;
    }

    // activation function: sigmoid(x) → 1 / (1 + e^-x)
    // make any input value between 0 and 1
    //makes it non-linear: output=σ(z)
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}
