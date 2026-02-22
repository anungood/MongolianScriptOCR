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
        String inputFileName = "overalltest.png";
        File file = new File(inputFileName);
        //Buffered Image object
        BufferedImage img = null;
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

        //method to segment the word letter by letter
        int[] edgesCoordinates = detectWordSpace(img);
        //crops the image to the detected coordinates
        BufferedImage wordOnlyImage = crop(img, edgesCoordinates);
        int spineColumn = findSpine(wordOnlyImage);
        ArrayList <int[]> columnCoordinates = segmentColumns(wordOnlyImage);
        int numberOfColumns = columnCoordinates.size();
        ArrayList<int[]> everyWordCoordinates = segmentWordsPerColumn(wordOnlyImage, columnCoordinates);

        for (int c[] : everyWordCoordinates) {
            System.out.println(Arrays.toString(c));
        }
        //int numberOfWords = everyWordCoordinates.size();

        //calls the method to segment letters for each word

        for (int w = 0; w < everyWordCoordinates.size()-1; w++) {
            System.out.println("Word: " + w);
            System.out.println(Arrays.toString(everyWordCoordinates.get(w)));
            int[] eachWordCoordinates = everyWordCoordinates.get(w);
            //how to save the letter coordinates and access it later? do i need to access it? or just keep the pics?
            ArrayList <int[]> letterCoordinates = segmentLetters(wordOnlyImage, eachWordCoordinates, spineColumn);
        }


    }

    //display image in a JPanel popup
    public static void display(BufferedImage img) {
        System.out.println("Displaying image.");
        JFrame frame = new JFrame(); //main application window. Picture frame that holds
        JLabel label = new JLabel(); //display text or images inside the container, JFrame
        frame.setSize(img.getWidth(), img.getHeight()); //same size as the image
        label.setIcon(new ImageIcon(img)); //sets an image on JLabel
        //ImageIcon is a Swing class that wraps an image so Swing can display it
        //new ImageIcon(img) → converts that image into a Swing-friendly icon
        //label.setIcon(...) → tells the label: “show this image”
        frame.getContentPane().add(label, BorderLayout.CENTER);
        //Adds the JLabel to the main area of the JFrame
        //.add(label, ...) → puts the label into that container
        //BorderLayout.CENTER → places it in the center region
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    //method to detect where the words are within the given image
    public static int[] detectWordSpace(BufferedImage img){
        //Variable Declarations
        int width = img.getWidth();    // number of columns
        int height = img.getHeight();  // number of rows
        ArrayList<Point> letterPixels = new ArrayList<>();
        int threshold = 10;
        ArrayList<Point> edgePixels = new ArrayList<>(); //will i use this?
        int leftestWidth = img.getWidth(), lowestHeight = img.getHeight();
        int rightestWidth=0, highestHeight=0;

        //detecting letters in pure black and white environment
        //black strokes should be less than or equal to 10
        //0 is ideal
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                if (pixel <= threshold) {
                    letterPixels.add(new Point(x,y));
                }
            }
        }

        // letterPixels now contains all (x, y) coordinates of black pixels
        //printing the pixels in the word
        for (int i= 0; i < letterPixels.size(); i++) {
            Point p = letterPixels.get(i);
            System.out.println("Total word pixels: " + letterPixels.get(i));
            int x = p.x;
            int y = p.y;
        }

        if (!letterPixels.isEmpty()) {
            //saving the leftest width (lowest)
            leftestWidth = letterPixels.getFirst().x;

            for (int i= 0; i < letterPixels.size()-1; i++) {
                //getting the highest height
                if (highestHeight < letterPixels.get(i).y) {
                    highestHeight = letterPixels.get(i).y;
                }
                //getting the rightest width
                //why is it less than 1
                if (rightestWidth < letterPixels.get(i).x) {
                    rightestWidth = letterPixels.get(i).x+1;
                }
                if (lowestHeight > letterPixels.get(i).y) {
                    lowestHeight = letterPixels.get(i).y;
                }
            }

            System.out.println(leftestWidth);
            System.out.println(rightestWidth);
            System.out.println(lowestHeight);
            System.out.println(highestHeight);

            // adding the edges to the Arraylist. Will I use this?
            edgePixels.add(new Point(leftestWidth, lowestHeight));
            edgePixels.add(new Point(leftestWidth, highestHeight));
            edgePixels.add(new Point(rightestWidth, lowestHeight));
            edgePixels.add(new Point(rightestWidth, highestHeight));
        }
        return new int[] {leftestWidth, lowestHeight, rightestWidth, highestHeight};
    }

    //crops the input image into the given coordinates
    public static BufferedImage crop (BufferedImage img, int[] edgeCoordinates) {
        System.out.println(
                "x1=" + edgeCoordinates[0] +
                        " y1=" + edgeCoordinates[1] +
                        " x2=" + edgeCoordinates[2] +
                        " y2=" + edgeCoordinates[3] +
                        " | imgW=" + img.getWidth() +
                        " imgH=" + img.getHeight()
        );
        BufferedImage croppedImg = img.getSubimage( edgeCoordinates[0], edgeCoordinates[1],
                edgeCoordinates[2]-edgeCoordinates[0],
                edgeCoordinates[3]-edgeCoordinates[1]);
        System.out.println("Printing cropped image with words only");
        display (croppedImg);
        return croppedImg;
    }

    //finds the main body line from the letters by calculating the column
    //that has the lowest value; how will i use this?
    public static int findSpine(BufferedImage img) {
        int width= img.getWidth();
        int height = img.getHeight();
        int[] colSums = new int[width];
        int minColumnIndex = 0;
        //gets the sum for each column pixels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //finds column sum for each column;
                colSums[x] +=pixel;
                }
            }
        //where to place this?
        int minColumn = colSums[0];
        //finds the index for the column with the lowest value
        for (int x = 0; x < width-1; x++) {
            System.out.println(colSums[x]);
            if (colSums[x] < minColumn) {
                minColumn = colSums[x];
                minColumnIndex = x;
            }
        }
        System.out.println("Min Column Sum: " + minColumn + " column x: " + minColumnIndex);
        return minColumnIndex;
    }

    //method to count how many column there is in the input image and find where each of the column start and end
    //by using the white space
    //should I use sobel algorithm? or implement 2d matrices for complexity?
    public static ArrayList<int[]> segmentColumns(BufferedImage img) {
        //variable to keep the transitions
        ArrayList<Integer> transitions = new ArrayList<>();
        transitions.add(0); //picture starts with the column 0
        ArrayList<int[]> columnCoordinates = new ArrayList<>(); //variable to keep track of the column coordinates
        int width = img.getWidth();
        int height = img.getHeight();
        int[] vertSums = new int[width];
        int maxColumn = vertSums[0];

        //find sum of the columns
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //finds column sum for each column
                vertSums[x] +=pixel;
            }
        }

        //displaying the values
        for (int v : vertSums) {
            System.out.println(v);
        }

        //finding the column with the maximum sum
        for (int i = 1; i < vertSums.length; i++) {
            if (vertSums[i] > maxColumn) {
                maxColumn = vertSums[i];
            }
        }

        //finds the transition zone by checking if the column sum changes into fully white while it was less than the maximum column sum
        //and also when the fully white column turns into lesser values
        for (int x = 1; x < width - 1; x++) {
            if (((Math.abs(vertSums[x-1] - vertSums[x]) > 0) && (vertSums[x] == maxColumn)) || (vertSums[x] == maxColumn) && Math.abs(vertSums[x+1] - vertSums[x]) > 0) {
                transitions.add(x);
                //transition has to be at least 2 pixel long to be considered a transition
                //make the threshold dynamic
                int length = transitions.size();
                System.out.println(length);
                if (transitions.get(length-1) - transitions.get(length-2) < 10) {
                    transitions.remove(length-1);
                }
            }
        }

        //if the last transition is not until the last width of the image, include it; including the last column
        if (transitions.getLast() != width) {
            transitions.add(width);
        }

        System.out.println(transitions);

        //if no transition was added

        if (transitions.size() == 1) {
            columnCoordinates.add(new int[] { 0, 0, width, height});
        }
        else {
            //displays the cropped letters and saves the coordinates to an int[] arraylist
            //i+2 helps ignore the white space in between the columns
            for (int i = 0; i < transitions.size() - 1; i ++) {
                //display purposes
                int[] segmentCoordinates = new int[]{transitions.get(i), 0, transitions.get(i + 1), height - 1};
                //saves the x,y,width,height values
                columnCoordinates.add(new int[]{transitions.get(i), 0, transitions.get(i + 1), height - 1});
                //crop(img, segmentCoordinates);
            }
        }
        //add the last column
        System.out.println(("Finishing column coordinates"));
        return columnCoordinates;
    }

    //method to count how many word there is in each column and find where the words start and end by using the white space
    public static ArrayList<int[]> segmentWordsPerColumn(BufferedImage img, ArrayList <int[]> columnCoordinates) {
        //variable to keep track of the transitions
        System.out.println("Starting segmenting words " + columnCoordinates.size() );
        ArrayList<Integer> transitions = new ArrayList<>();
        transitions.add(0);
        ArrayList<int[]> wordCoordinates = new ArrayList<>();

        for (int[] arr : columnCoordinates) {
            System.out.println(Arrays.toString(arr));
            System.out.println("whatt");
        }

        System.out.println("displaying column coordinates");
        //maybe like make a general method for finding row sums bcs it is so redundant
        for (int i = 0; i < columnCoordinates.size(); i++) {
            int[] coords = columnCoordinates.get(i);
            //each column starts with 0 height
            //saves the needed variables for each column
            int startX = coords[0];
            int startY = coords[1];
            int endX = coords[2];
            int endY = coords[3];
            int height = endY - startY;
            System.out.println(startX + " " + startY + " " + endX + " " + endY);
            int[] horizontalSums = findRowSum(img, startX, startY, endX, endY);
            //displaying the values
            for (int h = 0; h < horizontalSums.length; h++) {
                System.out.println("Row " + h + " is " + horizontalSums[h]);
            }
            int maxRowValue = findMaxRowValue(horizontalSums);

            //do smthing dynamic to find the segmentation of words?
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
                //crop(img, segmentCoordinates);

            }

            transitions.clear();
            transitions.add(0);

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
    //preprocess before doing the code so the white pixels are clearer
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

    //Segments the letter from cropped image by dividing the picture with 3 by width
    //and getting white spaces between each letter
    public static ArrayList<int[]> segmentLetters(BufferedImage img, int[] eachWordCoordinates, int num) {
        //use the int num to count the letters?
        int numberOfWords = eachWordCoordinates.length;
        ArrayList<Integer> transitions = new ArrayList<>();
        transitions.add(0); //picture starts with 0 height
        int startX = eachWordCoordinates[0];
        int startY = eachWordCoordinates[1];
        int endX = eachWordCoordinates[2];
        int endY = eachWordCoordinates[3];

        System.out.println(startX + " " +  startY + " " + endX + " " + endY);
        int width = endX - startX;
        int height = endY - startY;
        int leftZoneEnd = width/2 + startX; //divides the picture's width into 3 to create some white space that will help with the segmentation
        int[] horizontalSums = new int[endY];
        int maxRow = horizontalSums[0];
        ArrayList<int[]> letterCoordinates = new ArrayList<>(); //variable to save the coordinates of the letters

        //displays the divided image
        int[] leftZone = new int[] {startX, startY, leftZoneEnd, endY};
        crop(img,leftZone);

        //find vertical sum to find where the white spaces are after
        //the image is divided into 3 parts by width
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < leftZoneEnd; x++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //finds row sum for each row
                horizontalSums[y] +=pixel;
            }
        }
        //displaying the values
        for (int h : horizontalSums) {
            System.out.println(h);
        }

        //finding the row with the maximum value because that is equal to the white space and thus the transition
        for (int i = 1; i < horizontalSums.length; i++) {
            if (horizontalSums[i] > maxRow) {
                maxRow = horizontalSums[i];
            }
        }

        //finds the transition zone by checking if there is change between rows and
        //the row becomes fully white
        for (int y = 1; y < endY - 1; y++) {
            if ((Math.abs(horizontalSums[y-1] - horizontalSums[y]) > 0) && (horizontalSums[y] == maxRow)) {
                transitions.add(y);
            }
        }

        System.out.println(transitions);

        //displays the cropped letters and saves the coordinates to an int[] arraylist
        for (int i = 0; i < transitions.size()-1; i++) {
            int[] segmentCoordinates = new int[] {startX, transitions.get(i), endX, transitions.get(i+1)};
            //saves the x,y,width,height values
            letterCoordinates.add(new int[] {startX, transitions.get(i), endX, transitions.get(i+1)});
            crop(img, segmentCoordinates);
        }
        transitions.clear();

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

        //just diplaying coordinates
        for (int[] coords : letterCoordinates) {
            System.out.println(Arrays.toString(coords));
        }
        //separate image for the letter
        for (int i=0; i<letterCoordinates.size(); i++) {
            int width = letterCoordinates.get(i)[2];
            int height =  letterCoordinates.get(i)[3];

            //displaying segmented letters
            BufferedImage letter = croppedImage.getSubimage(letterCoordinates.get(i)[0], letterCoordinates.get(i)[1],
                    width, height);
            display(letter);

            //calculating scaling value
            double scale = Math.min( (double) targetWidth / width,
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
            File outputFile = new File(folder, "letter_" + i + ".png");
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
 for (int num : colSums) {
 System.out.println(num);
 }
 **/
// print column sums
// System.out.println("Sum of column " + x + ": " + colSums[x]);

//detect transition zone by comparing it with the threshold value
//0- black; 255-white

/**
ArrayList<Integer> transitionCols = new ArrayList<>();
boolean wordExists = false;
int threshold = 50; // tweak depending on your image

        for (int x = 0; x < width; x++) {
        if (colSums[x] > threshold && !wordExists) {
// start of a stroke/word
wordExists = true;
        transitionCols.add(x); // store start column
            } else if (colSums[x] <= threshold && wordExists) {
// end of a stroke/word
wordExists = false;
        transitionCols.add(x); // store end column
            }
                    }

**/
//threshold to detect transition zone

//detecting the column with the lowest value to determine where the words are written
//what if there are multiple columns? -> find transition zone

/**
 //gets sum of columns to determine where the letter exists
 //used for gradient decent?
 for (int x = 0; x < width; x++) {       // loop columns
 for (int y = 0; y < height; y++) {  // loop rows
 colSums[x] += img.getRGB(x, y) & 0xFF;
 }
 }

 for (int x = 0; x < width; x++) {
 //find max value column in the picture to know where the white spaces are
 if (colSums[x] > maxVal) {
 maxVal = colSums[x];
 }
 }
 //detecting the whitest space in the picture to cut it?
 System.out.println("Max Value of Columns " + maxVal);

 for (int i= 0; i < letterPixels.size(); i++) {
 Point p = letterPixels.get(i);
 System.out.println("Total dark pixels: " + letterPixels.get(i));
 int x = p.x;
 int y = p.y;
 }

 edgePixels.add(letterPixels.getFirst());
 for (int i= 0; i < letterPixels.size()-1; i++) {
 if ((Math.abs(letterPixels.get(i).x - letterPixels.get(i+1).x) > 1) &&
 (Math.abs(letterPixels.get(i).y - letterPixels.get(i+1).y) > 1)) {
 edgePixels.add(letterPixels.get(i));
 System.out.println(letterPixels.get(i));
 }
 }
 **/

//letterEdges[1] = letterPixels.getFirst().y;
//saving the leftest width
//edgePixels.add(letterPixels.getFirst());
// letters.add(letterCorners);

/**
 //saves the changes of value between rows to an array
 for (int y = 0; y < height - 1; y++) {
 diff[y] = Math.abs(vertSums[y + 1] - vertSums[y]);
 }


 for (int y = 2; y < height - 1; y++) {
 if ((vertSums[y-2] - vertSums[y - 1] > 0)  && (vertSums[y] - vertSums[y + 1] == 0))  {
 transitions.add(y);
 }
 }
 **/
/**
 for (int y = 2; y < diff.length; y++) {
 if ((diff[y-1] - diff [y-2] > 0) && (diff[y] == 0)) {
 transitions.add(y);
 }
 }
 **/

/**
 for (int d : diff) mean += d;
 mean /= diff.length;
 double threshold = mean * 2;
 System.out.println(threshold);
 //find the transition zone by seeing the changes of value compared with
 //the next row

 for (int y = 0; y < diff.length; y++) {
 if (diff[y] > threshold) {
 transitions.add(y);
 }
 }
 **/
