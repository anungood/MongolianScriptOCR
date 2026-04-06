//class to process the training dataset
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.*;

public class DatasetProcessor {
    //nested class variable declaration
    public static class Dataset {
        public double[][] inputs;
        public double[][] targets;
        public ArrayList<String> lettersList;// unique letters, used for mapping later
        public ArrayList<String> classList;   // each variant = class for training
        public HashMap<String, String> variantToLetter; // map variant -> letter
    }

    //method to load images from folders and apply one-hot encoding
    public static Dataset loadDataset(String datasetPath) throws Exception {
        //Variable declarations
        File datasetDir = new File(datasetPath); //setting directory
        File[] folders = datasetDir.listFiles(File::isDirectory); //getting all the folders inside the directory
        ArrayList<double[]> inputList = new ArrayList<>();
        ArrayList<double[]> targetList = new ArrayList<>();

        //throw an exception if the folder is empty or the folder doesn't exist
        if (folders == null || folders.length == 0) {
            throw new Exception("Dataset folder doesn't exist or it is empty!");
        }

        //saving unique letters from the training set by cutting before the underscore in each folder name
        //HashSet<String> lettersSet = new HashSet<>();
        ArrayList<String> classList = new ArrayList<>(); //saves all the different variants of the letters from the letters
        HashMap<String, String> variantToLetter = new HashMap<>(); //maps the variant to each letter
        HashSet<String> lettersSet = new HashSet<>();
        for (File folder : folders) {
            //String uniqueLetter = folder.getName().split("_")[0];
            //lettersSet.add(uniqueLetter);
            String variantName = folder.getName(); //gets the folder name
            String letter = variantName.split("_")[0]; //saves the letter from the letter name to map back
            classList.add(variantName);
            variantToLetter.put(variantName, letter); //maps the variant name and the letter
            lettersSet.add(letter);
        }
        //creating an arraylist with the unique letters
        ArrayList<String> lettersList = new ArrayList<>(lettersSet);

        //mapping variants to numerical indexes
        HashMap<String, Integer> letterToIndex = new HashMap<>();
        for (int i = 0; i < classList.size(); i++) {
            letterToIndex.put(classList.get(i), i);
        }
        //System.out.println("Detected classes: " + letterToIndex);

        //loop over all the folders in the training set and get all the png images in each folder
        for (File folder : folders) {
            String folderName = folder.getName();
            //String letter = folderName.split("_")[0];
            int classIndex = classList.indexOf(folderName); //gets the index of the variant
            //int letterIndex = letterToIndex.get(uniqueLetter);
            File[] images = folder.listFiles((dir, name) -> name.endsWith(".png"));
            //skip if the folder is empty
            if (images == null) continue;

            //read each images in the folder
            for (File imgFile : images) {
                BufferedImage img = ImageIO.read(imgFile);
                int width = img.getWidth();
                int height = img.getHeight();
                double[] input = new double[width * height];

                //iterates through all the pixels in the images
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        //gets the rgb value of the images
                        int pixel = img.getRGB(x, y) & 0xff;
                        //flattens the 2D image into 1D array
                        input[y * width + x] = pixel / 255.0; //divides by 255 to normalize the value from 0 to 1
                    }
                }

                //target array to save the one-hot encoding
                double[] target = new double[classList.size()];
                Arrays.fill(target, 0.0); //fill the array with 0
                target[classIndex] = 1.0; //the index of the letter becomes 1 in the array
                inputList.add(input); //vector of the input image
                targetList.add(target); //array of that has the target letter
                //System.out.println("Folder: " + folderName + " Letter : " + uniqueLetter + " Index Number: " + letterIndex);
                //System.out.println("Target vector: " + Arrays.toString(target));
            }
        }

        //converting arraylist to arrays that are fixed size
        double[][] inputs = inputList.toArray(new double[0][]);
        double[][] targets = targetList.toArray(new double[0][]);

        //creates dataset object and assigns the data from the training dataset
        Dataset dataset = new Dataset();
        dataset.inputs = inputs;
        dataset.targets = targets;
        dataset.lettersList = lettersList;
        dataset.classList = classList;
        dataset.variantToLetter = variantToLetter;

        return dataset;
    }

    //testing the dataset processor if it is reading the folders
    public static void main(String[] args) {
        //training data path
        File datasetDir = new File("sampleTrainingDataset");

        //show error message if the dataset folder isn't found
        if (!datasetDir.exists() || !datasetDir.isDirectory()) {
            System.out.println("Dataset folder is not found or is not a directory!");
            return;
        }
        File[] folders = datasetDir.listFiles(File::isDirectory); //getting all the subfolders

        for (File folder : folders) {
            //folder name
            String folderName = folder.getName();
            //getting unique letters
            String letter = folderName.split("_")[0];

            //count all image files in the folder
            File[] images = folder.listFiles((dir, name) -> name.endsWith(".png"));
            int count;
            if (images != null) {
                count = images.length;
            } else {
                count = 0;
            }

            System.out.println("Letter: " + letter + " Folder Name: " + folderName + " Image Count: " + count);
        }
    }
}



