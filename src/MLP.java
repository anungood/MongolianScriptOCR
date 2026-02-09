import java.util.Random;

public class MLP {
    //Variable Declaration
    //sizes for Hidden Layer, Input Layer, Output Layer
    int inputSize;
    int hiddenSize;
    int outputSize;
    //weights
    double[][] weightInput;
    double[][] weightHidden;
    //biases
    double[] biasHidden;
    double[] biasOutput;
    //activations
    double[] hidden;
    double[] output;
    //new random variable
    Random rand = new Random();

    //Constructor with 3 arguments
    public MLP(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        weightInput = new double[inputSize][hiddenSize];
        weightHidden = new double[hiddenSize][outputSize];

        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];

        hidden = new double[hiddenSize];
        output = new double[outputSize];

        initWeights();
    }

    //Initialize weights with random numbers
    private void initWeights() {
        for (int i = 0; i < inputSize; i++)
            for (int j = 0; j < hiddenSize; j++)
                weightInput[i][j] = rand.nextDouble();

        for (int i = 0; i < hiddenSize; i++)
            for (int j = 0; j < outputSize; j++)
                weightHidden[i][j] = rand.nextDouble();
    }
}