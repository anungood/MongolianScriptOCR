import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Segmentation {
    public static void main(String[] args) {
        //load image file
        File file = new File("ᠮᠣᠩᠭᠤᠯ.png");
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
        int[] edgesCoordinates= detectWordSpace(img);
        //crops the image to the detected coordinates
        BufferedImage wordOnly = crop(img, edgesCoordinates);
        int spineColumn = findSpine(wordOnly);
        ArrayList<Integer> cutPositions = segmentLetters(wordOnly, spineColumn);
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
            int letterCount = 1;
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

    //crops the input image into only words
    public static BufferedImage crop (BufferedImage img, int[] edgeCoordinates) {
        BufferedImage croppedImg = img.getSubimage( edgeCoordinates[0], edgeCoordinates[1],
                edgeCoordinates[2]-edgeCoordinates[0]+1,
                edgeCoordinates[3]-edgeCoordinates[1]+1);
        System.out.println("Printing cropped image with words only");
        display (croppedImg);
        return croppedImg;
    }

    //finds the main body line from the letters by calculating the column
    //that has the lowest value
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

    //Segments the letter from cropped image by dividing the picture with 3 by width
    //and getting white spaces between each letter
    public static ArrayList<Integer> segmentLetters(BufferedImage img, int num) {
        ArrayList<Integer> transitions = new ArrayList<>();
        transitions.add(0); //picture starts with 0 height
        int width = img.getWidth();
        int height= img.getHeight();
        int leftZoneEnd = width/3;
        int[] vertSums = new int[height];
        int maxRow = vertSums[0];
        //variable to keep track of the row changes
        int[] diff = new int[height-1];
        double mean = 0;

        int[] leftZone = new int[] {0,0, leftZoneEnd, height-1};
        crop(img,leftZone);

        //find vertical sum to find where the white spaces are after
        //the image is divided into 3 parts by width
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < leftZoneEnd; x++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //finds row sum for each column;
                vertSums[y] +=pixel;
            }
        }

        for (int v : vertSums) {
            System.out.println(v);
        }

        for (int i = 1; i < vertSums.length; i++) {
            if (vertSums[i] > maxRow) {
                maxRow = vertSums[i];
            }
        }

        //finds the transition zone by checking if there is change between rows and
        //the row becomes fully white
        for (int y = 1; y < height - 1; y++) {
            if ((Math.abs(vertSums[y-1] - vertSums[y]) > 0) && (vertSums[y] == 8856)) {
                transitions.add(y);
            }
        }

        System.out.println(transitions);

        //displays the cropped letters
        for (int i = 0; i < transitions.size()-1; i++) {
            int[] segmentCoordinates = new int[] {0, transitions.get(i), width-1, transitions.get(i+1)};
            crop(img, segmentCoordinates);
        }


        return transitions;
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
