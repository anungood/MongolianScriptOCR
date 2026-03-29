import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Segmentation {
    public static void main(String[] args) {
        //load image file
        String inputFileName = "dataset6.png";
        File file = new File(inputFileName);
        ArrayList <int[]> letterCoordinates = new ArrayList<>();
        //Buffered Image object
        BufferedImage img = null;
        BufferedImage wordOnlyImage = null;
        //Error Handling try-catch block to see if the file exists
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        if (img != null) {
            //method to display the image
            display(img);
        }

        //method to find the bounding box of the text in the image
        int[] edgesCoordinates = detectWordSpace(img);

        //crops the image to the detected word space if coordinates for pixels are detected
        if (edgesCoordinates[0] == 0) {
            System.out.println("There was no word detected in the image.");
            System.exit(0); //The program is terminated if there is not black pixel detected
        }
        else {
            wordOnlyImage = crop(img, edgesCoordinates);
        }

        //finds the coordinates of each column of words in the image
        BufferedImage binarizedImage = binarize(wordOnlyImage);
        ArrayList <int[]> columnCoordinates = segmentColumns(binarizedImage);
        int columnCount = columnCoordinates.size();
        int[] connectionLineIndex = new int[columnCount];
        System.out.println("Column count is: " + columnCount);
        //finds the overarching line the words are written for each column
        for (int c = 0; c < columnCount; c++) {
            System.out.println(Arrays.toString(columnCoordinates.get(c)));
            connectionLineIndex[c] = findConnectionLine(binarizedImage, columnCoordinates.get(c));
        }

        for  (int s : connectionLineIndex) {
            System.out.println("Min Column");
            System.out.println(s);
        }

        //finds the coordinates for every word in every column
        ArrayList<int[]> everyWordCoordinates = segmentWordsPerColumn(binarizedImage, columnCoordinates);

        //counter variable to keep track of which column the words are in
        int i = 0;
        //calls the method to segment letters for each word
        for (int w = 0; w < everyWordCoordinates.size(); w++) {
            int[] eachWordCoordinates = everyWordCoordinates.get(w);
            //if the connection line is within the coordinates range for width of the word, the method is called
            if (everyWordCoordinates.get(w)[0] < connectionLineIndex[i] && connectionLineIndex[i] < everyWordCoordinates.get(w)[2]) {
               letterCoordinates.addAll(segmentLetters(wordOnlyImage, eachWordCoordinates, connectionLineIndex[i]));
            }
            //or else, the next connection line is used to call the method for segmenting the words until it reaches its maximum
            else if (everyWordCoordinates.get(w)[0] > connectionLineIndex[i]) {
                i++;
                if(i < connectionLineIndex.length) {
                   letterCoordinates.addAll(segmentLetters(wordOnlyImage, eachWordCoordinates, connectionLineIndex[i]));
                }
            }
        }
        //resize every letters and save images to prepare to feed it for the MLP
        //resizeLetters(wordOnlyImage, letterCoordinates, inputFileName);
    }

    // display an image in a JPanel popup
    public static void display(BufferedImage img) {
        System.out.println("Displaying image.");
        JFrame frame = new JFrame(); // main application window
        JLabel label = new JLabel(); // display text or images inside the container of JFrame
        frame.setSize(img.getWidth(), img.getHeight()); // makes the frame same size as the image
        label.setIcon(new ImageIcon(img)); //sets an image on JLabel to display
        frame.getContentPane().add(label, BorderLayout.CENTER); //makes the JLabel the center of JFrame
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //frame will exit on close command
        frame.pack();
        frame.setVisible(true);
    }

    //method to detect where the words are within the given image; finding the bounding box
    public static int[] detectWordSpace(BufferedImage img){
        //Variable Declarations
        int width = img.getWidth(); // number of columns
        int height = img.getHeight();  // number of rows
        int threshold = 120; // threshold to identify the darker pixel for the words
        int leftestWidth = width;
        int highestHeight = 0;
        int rightestWidth = 0;
        int lowestHeight = height;
        boolean found = false;

        //detecting letters in pure black and white environment
        //black strokes should be less than or equal to 10; Value of 0 is ideal
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                if (pixel <= threshold) {
                    found = true;
                    //comparing the values to find the bounding box
                    if (x < leftestWidth) leftestWidth = x;
                    if (x > rightestWidth) rightestWidth = x;
                    if (y > highestHeight) highestHeight = y;
                    if (y < lowestHeight) lowestHeight = y;
                }
            }
        }

        //return empty array if there was no black pixel detected, assuming there isn't any word in the photo
        if (!found) {
            return new int[0];
        }
        return new int[] {leftestWidth, lowestHeight, rightestWidth, highestHeight};
    }

    //binarizing an image to only black and white
    public static BufferedImage binarize(BufferedImage img) {
        System.out.println("Binarizing Image");
        //create new BufferedImage called binarizedImage
        //TYPE_BYTE_GRAY: each pixel is stored as one byte (8 bits) and they represent intensity/brightness
        BufferedImage binarizedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        int threshold = 120; //threshold to either make the pixel black or white
        int rgb = 0, r = 0, g = 0, b = 0; //variable declarations for rgb value

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                rgb = (img.getRGB(x, y)); //get the rgb value of each pixel
                //separate red, green, and blue through shift operation
                //bit manipulation to extract RGB components from a single integer pixel value in Java
                r = ((rgb >> 16) & 0xFF);
                g = ((rgb >> 8) & 0xFF);
                b = (rgb & 0xFF);
                //luminance grayscale conversion formula; weighted luminance
                rgb = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                //choosing to make the pixel either black or white by comparing it to the threshold
                int value;
                if (rgb > threshold) {
                    value = 255; //pure white pixel value
                } else {
                    value = 0; //pure black pixel value
                }

                //overriding the rgb value with shifted value
                rgb = (255 << 24) | (value << 16) | (value << 8) | value;
                //setting the rgb value to the new value
                binarizedImage.setRGB(x, y, rgb);
            }
        }
        return binarizedImage;
    }

    //crops the input image into the given coordinates
    public static BufferedImage crop (BufferedImage img, int[] edgeCoordinates) {
        BufferedImage croppedImg = img.getSubimage( edgeCoordinates[0], edgeCoordinates[1],
                edgeCoordinates[2]-edgeCoordinates[0],
                edgeCoordinates[3]-edgeCoordinates[1]);
        System.out.println("Printing cropped image");
        display (croppedImg);
        return croppedImg;
    }

    //finds the main body line from the letters by calculating the column
    public static int findConnectionLine(BufferedImage img, int[] columnCoordinate) {
        // Variable Declarations
        int startX = columnCoordinate[0];
        int startY = columnCoordinate[1];
        int endX = columnCoordinate[2];
        int endY = columnCoordinate[3];
        int[] colSums = new int[endX];
        int minColumnIndex = startX; //variable to save the index for the lowest column value

        //gets the sum for each column pixels
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //finds column sum for each column;
                colSums[x] +=pixel;
                }
            }

        int minColumn = colSums[startX]; //variable to hold the value of the minimum column sum which will have the most black pixels

        //finds the index for the column with the lowest value
        for (int x = startX; x < endX; x++) {
            if (colSums[x] < minColumn) {
                minColumn = colSums[x];
                minColumnIndex = x;
            }
        }
        System.out.println("Min Column Sum: " + minColumn + " column x: " + minColumnIndex);
        return minColumnIndex;
    }

    //method to find the coordinates for each column in the input image using vertical projection profile
    public static ArrayList<int[]> segmentColumns(BufferedImage img) {
        ArrayList<Integer> transitions = new ArrayList<>();  //variable to keep the transitions
        ArrayList<int[]> columnCoordinates = new ArrayList<>(); //variable to keep track of the column coordinates
        int width = img.getWidth();
        int height = img.getHeight();
        int[] vertSums = new int[width];

        //find the vertical projection profile by summing the count of black pixels
        for (int x = 0; x < width; x++) {
            int sum = 0;
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //add 1 if the pixel is black, add 0 if the pixel is otherwise
                sum = (pixel < 50) ? 1 : 0;
                //finds column sum for each column
                vertSums[x] += sum;
            }
        }

        boolean textExist = false;
        int startColumn = 0;

        for (int x = 0; x < width; x++) {
            if (!textExist && vertSums[x] > 0) {
                //column starts when the sum of the column is greater than 0
                textExist = true;
                startColumn = x;
            } else if (textExist && vertSums[x] == 0) {
                //column ends when the sum of the column reaches 0
                textExist = false;
                int endColumn = x - 1;
                //the start and end of the columns are added as transitions only when their length is greater or equal to 1
                if (endColumn - startColumn >= 1) {
                    transitions.add(startColumn);
                    transitions.add(endColumn);
                }
            }
        }

        //if there is still text in the last column of the image, add the width to the transition zone
        if (textExist) {
            transitions.add(startColumn);
            transitions.add(width - 1);
        }
        System.out.println(transitions);

        //if no transition was added it will return the image
        if (transitions.isEmpty()) columnCoordinates.add(new int[]{0, 0, width, height});
        else {
            //displays the cropped columns and saves the coordinates to an int[] arraylist
            for (int i = 0; i < transitions.size(); i +=2 ) {
                int startX = transitions.get(i);
                int endX = transitions.get(i + 1);
                int[] segmentCoordinates = new int[]{startX, 0, endX, height - 1};
                //saves the x,y,width,height values
                columnCoordinates.add(new int[]{startX, 0, endX, height - 1});
                crop(img, segmentCoordinates);
            }
        }
        return columnCoordinates;
    }

    //method to segment the words within each column
    public static ArrayList<int[]> segmentWordsPerColumn(BufferedImage img, ArrayList <int[]> columnCoordinates) {
        ArrayList<int[]> wordCoordinates = new ArrayList<>(); //variable to save the word coordinates

        //looping each column
        for (int i = 0; i < columnCoordinates.size(); i++) {
            int[] coords = columnCoordinates.get(i);
            //getting coordinates for each column
            int startX = coords[0];
            int startY = coords[1];
            int endX = coords[2];
            int endY = coords[3];
            boolean wordFound= false;
            int wordStart = 0;
            System.out.println(startX + " " + startY + " " + endX + " " + endY);
            //finds the Horizontal Projection Profile
            int[] hppSums = findHPP(img, startX, startY, endX, endY);

            //transition is added when there is change in pixel values between two rows and the latter turns fully white
            for (int y = 1; y < hppSums.length ; y++) {
                //word starts when hpp has a value over 0
                if (!wordFound && hppSums[y] > 0) {
                    wordFound = true;
                    wordStart = y;
                }
                //word ends when hpp equals 0
                if (wordFound && hppSums[y] == 0) {
                    wordFound = false;
                    //saves the segmented word coordinates
                    int[] segmentCoordinates = new int[]{
                            startX,
                            startY + wordStart,
                            endX,
                            startY + y
                    };
                    wordCoordinates.add(segmentCoordinates); //adds each word coordinates to the arraylist
                    crop(img, segmentCoordinates);
                }
            }

            //if the word continues until the end of the column, add the transition until endY
            if (wordFound) {
                int[] segmentCoordinates = new int[] {
                        startX,
                        startY + wordStart,
                        endX,
                        endY
                };
                wordCoordinates.add(segmentCoordinates); //adds to the arraylist that keeps all the word coordinates
                crop(img, segmentCoordinates);
            }
        }
        return wordCoordinates;
    }

    //find horizontal projection profile for each row in the image
    public static int[] findHPP (BufferedImage img, int startX, int startY, int endX, int endY) {
        int height = endY - startY;
        int[] hpp = new int[height];

        for (int y = startY; y < endY; y++) {
            int count = 0;
            for (int x = startX; x < endX; x++) {
                int pixel = img.getRGB(x, y);
                int gray = pixel & 0xFF;
                if (gray <= 50) { //increase the count if the pixel value is close to black
                    count++;
                }
            }
            hpp[y - startY] = count;
        }
        return hpp;
    }

    //segments every letter in each word using the connection line
    public static ArrayList<int[]> segmentLetters(BufferedImage img, int[] eachWordCoordinates, int connectionLineIndex) {
        //Variable Declarations
        int startX = eachWordCoordinates[0];
        int startY = eachWordCoordinates[1];
        int endX = eachWordCoordinates[2];
        int endY = eachWordCoordinates[3];
        ArrayList<int[]> letterCoordinates = new ArrayList<>(); //variable to save the coordinates of the letters
        ArrayList<Integer> transitions = new ArrayList<>();

        //dynamic way to cut the words to create white space between letters by using where the connection line is
        int leftZoneEnd = connectionLineIndex - (connectionLineIndex - startX)/4;
        //find horizontal projection line within the left zone of the word
        int[] hppSum = findHPP(img, startX, startY, leftZoneEnd, endY);

        //first letter always starts at the top of the word
        if (hppSum.length > 0) {
            transitions.add(startY);
        }

        //loop over the hpp values and find the transition zone
        for (int i = 1; i < hppSum.length - 1; i++) {
            //transition is added when black pixel row turns to fully white
            if (hppSum[i] == 0 && Math.abs(hppSum[i] - hppSum[i-1]) > 0) {
                transitions.add(startY + i + 1);
            }
        }

        int minHeight = 5; //threshold for whether or not to include the transition or not
        int lastStart = transitions.getLast();
        if (endY - lastStart < minHeight && transitions.size() > 1) {
            //merge with previous transition if the transition length is too short
            transitions.set(transitions.size() - 1, endY);
        } else if (lastStart < endY) {
            transitions.add(endY);
        }

        //adds the transitions to the letter coordinates; the end of the letter becomes the start of the next letter
        for (int i = 0; i < transitions.size() - 1; i++) {
            int start = transitions.get(i);
            int end = transitions.get(i + 1);

            if (end - start >= minHeight) { //transitions must be longer than the minimum height to avoid having small segments
                int[] segmentCoordinates = new int[] { startX, start, endX, end };
                letterCoordinates.add(segmentCoordinates);
                crop(img, segmentCoordinates);
            }
        }
        return letterCoordinates;
    }

    public static void resizeLetters (BufferedImage croppedImage, ArrayList<int[]> letterCoordinates, String inputFileName) {
        int targetWidth = 30;
        int targetHeight = 20;
        String extractedInputName = inputFileName;
        String outputFolder = "output";


        //gets the index of where the dot is placed inside the input file name to cut the word
        int cutIndex = inputFileName.lastIndexOf('.');
        //cuts the input filename to where the dot is placed
        if (cutIndex > 0) {
            extractedInputName = inputFileName.substring(0, cutIndex);
        }

        //creates a folder to save the output
        //do a try and catch block to get exceptions
        File folder = new File(outputFolder, extractedInputName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        //just displaying coordinates
        for (int[] coords : letterCoordinates) {
            System.out.println(Arrays.toString(coords));
        }
        //separate image for the letter
        for (int i = 0; i < letterCoordinates.size(); i++) {
            int width = letterCoordinates.get(i)[2] - letterCoordinates.get(i)[0];
            int height = letterCoordinates.get(i)[3] - letterCoordinates.get(i)[1];

            //displaying segmented letters
            BufferedImage letter = croppedImage.getSubimage(letterCoordinates.get(i)[0], letterCoordinates.get(i)[1],
                    width, height);
            display(letter);

            //calculating scaling value
            double scale = Math.min((double) targetWidth / width,
                    (double) targetHeight / height
            );

            //calculating updated width and height to resize
            int scaledWidth = (int) Math.round(width * scale);
            int scaledHeight = (int) Math.round(height * scale);
            //creates an image with the size of the scaled width and scaled height
            BufferedImage scaledImage = new BufferedImage(
                    scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY
            );
            display(scaledImage);

            //writes the letter into the resized image using Graphics2D tool
            Graphics2D gScaled = scaledImage.createGraphics();
            gScaled.drawImage(letter, 0, 0, scaledWidth, scaledHeight, null);
            gScaled.dispose();
            //padding canvas to reach the target width and target height
            BufferedImage resizedLetter = new BufferedImage(
                    targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY
            );

            //fills the empty canvas with white pixels
            Graphics2D gOut = resizedLetter.createGraphics();
            gOut.fillRect(0, 0, targetWidth, targetHeight);

            //how much space to leave when writing the image in order to make it center
            int xSpace = (targetWidth - scaledWidth) / 2;
            int ySpace = (targetHeight - scaledHeight) / 2;

            gOut.drawImage(scaledImage, xSpace, ySpace, null);
            gOut.dispose();
            display(resizedLetter);

            //save the image into a folder-> need to have dynamic folder name
            //inside the main folder so each attempt on the app is saved separately
            File outputFile = new File(folder, "letter__" + i + ".png");
            try {
                //Saves the BufferedImage into a png file
                ImageIO.write(resizedLetter, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
/**
//removes empty columns by calculating the density and comparing it with the threshold
public static void removeEmptyColumns(BufferedImage img, ArrayList<int[]> columnCoordinates) {
    double threshold = 0.005; //amount of density the columns should at least have
    Iterator<int[]> iterator = columnCoordinates.iterator();
    int height = img.getHeight();

    while (iterator.hasNext()) {
        int[] column = iterator.next();
        int startX = column[0];
        int endX = column[2];
        int blackPixelCount = 0;
        int width = endX - startX;

        for (int x = startX; x < endX; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = img.getRGB(x, y) & 0xFF;

                if (rgb < 10) { //detect black pixel
                    blackPixelCount++;
                }
            }
        }

        double density = blackPixelCount / (double)(width * height);
        System.out.println("Column density: " + density);
        //remove column if density is lower than the threshold
        if (density < threshold) {
            iterator.remove();
        }
    }
}


//method to find the word coordinates by using the row sum
public static ArrayList<int[]> segmentWordsPerColumn(BufferedImage img, ArrayList <int[]> columnCoordinates) {
    System.out.println("Starting segmenting words " + columnCoordinates.size());
    ArrayList<Integer> transitions = new ArrayList<>(); //variable to keep track of the word transitions
    ArrayList<int[]> wordCoordinates = new ArrayList<>(); //variable to save the word coordinates

    for (int[] arr : columnCoordinates) {
        System.out.println(Arrays.toString(arr));
    }

    //maybe like make a general method for finding row sums bcs it is so redundant
    for (int i = 0; i < columnCoordinates.size(); i++) {
        int[] coords = columnCoordinates.get(i);
        //each column starts with 0 height
        //saves the needed variables for each column
        int startX = coords[0];
        int startY = coords[1];
        int endX = coords[2];
        int endY = coords[3];
        transitions.add(startY);
        System.out.println(startX + " " + startY + " " + endX + " " + endY);
        int[] horizontalSums = findRowSum(img, startX, startY, endX, endY);
        int maxRowValue = findMaxRowValue(horizontalSums);

        //transition is added when there is change in pixel values between two rows and the latter turns fully white
        for (int y = 1; y < endY; y++) {
            if ((Math.abs(horizontalSums[y-1] - horizontalSums[y]) > 0) && (horizontalSums[y] == maxRowValue)) {
                transitions.add(y);
            }
        }

        System.out.println("Transition: " + transitions);

        for (int t = 0; t < transitions.size()-1; t++) {
            int[] segmentCoordinates = new int[] {startX, transitions.get(t), endX, transitions.get(t+1)};
            //saves the x,y,width,height values
            wordCoordinates.add(new int[] {startX, transitions.get(t), endX, transitions.get(t+1)});
            crop(img, segmentCoordinates);
        }
        transitions.clear();
    }
    return wordCoordinates;
}

//find the sum of pixel values in rows
public static int[] findRowSum (BufferedImage img, int x, int y, int x2, int y2) {
    int[] rowSum = new int[y2];

    for (int h = y; h < y2; h++) {
        for (int w = x; w < x2; w++) {
            int pixel = img.getRGB(w, h) & 0xff;
            //finds row sum for each row
            rowSum[h] += pixel;
        }
    }
    return rowSum;
}

//find the maximum row
public static int findMaxRowValue (int[] rowSums) {
    int maxRowSum = 0;

    for (int i = 1; i < rowSums.length; i++) {
        if (rowSums[i] > maxRowSum) {
            maxRowSum = rowSums[i];
        }
    }

    return maxRowSum;
}
 **/