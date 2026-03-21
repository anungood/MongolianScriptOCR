//class to process the training dataset

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
        public ArrayList<String> lettersList;
    }

    //load images from folders
    public static Dataset loadDataset(String datasetPath) throws Exception {
        //Variable declarations
        File datasetDir = new File(datasetPath);
        File[] folders = datasetDir.listFiles(File::isDirectory);
        ArrayList<double[]> inputList = new ArrayList<>();
        ArrayList<double[]> targetList = new ArrayList<>();

        //throw an exception if the folder is empty or the folder doesn't exist
        if (folders == null || folders.length == 0) {
            throw new Exception("Dataset folder doesn't exist or it is empty!");
        }

        //saving unique letters from the training set by cutting before the underscore in each folder name
        HashSet<String> lettersSet = new HashSet<>();
        for (File folder : folders) {
            String uniqueLetter = folder.getName().split("_")[0];
            lettersSet.add(uniqueLetter);
        }

        //creating an arraylist with the unique letters
        ArrayList<String> lettersList = new ArrayList<>(lettersSet);
        //Collections.sort(lettersList);

        //mapping letters to numerical indexes
        HashMap<String, Integer> letterToIndex = new HashMap<>();
        for (int i = 0; i < lettersList.size(); i++) {
            letterToIndex.put(lettersList.get(i), i);
        }
        //System.out.println("Detected classes: " + letterToIndex);

        Dataset dataset = new Dataset();
        return dataset;
    }

}
