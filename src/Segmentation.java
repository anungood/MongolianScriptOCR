import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;

public class Segmentation {
    public static void main(String[] args) {
        //load image file
        File file = new File("ᠮᠣᠩᠭᠤᠯ.png");
        //Buffered Image object
        //Buffered Image object
        BufferedImage img = null;
        //try-catch block to see if the file exists
        //Error Handling
        ArrayList<Point> edges = new ArrayList<>();
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        if (img != null) {
            display(img);
        }

        //method to segment the word letter by letter
        edges= detectWordSpace(img);
        segmentLetters();
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

    public static ArrayList<Point> detectWordSpace(BufferedImage img){
        int width = img.getWidth();    // number of columns
        int height = img.getHeight();  // number of rows
        int[] colSums = new int[width];
        int maxVal=0;
        ArrayList<Point> letterPixels = new ArrayList<>();
        int threshold =0;
        ArrayList<Point> edgePixels = new ArrayList<>();

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

        int leftestWidth, lowestHeight, rightestWidth, highestHeight;

        if (!letterPixels.isEmpty()) {
            int letterCount = 1;
            //saving the leftest width (lowest)
            leftestWidth = letterPixels.getFirst().x;
            //initialization variable
            rightestWidth = 0;
            lowestHeight = letterPixels.getFirst().y;
            highestHeight = 0;

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

            // detect the area of pixels that has letters
            edgePixels.add(new Point(leftestWidth, lowestHeight));
            edgePixels.add(new Point(leftestWidth, highestHeight));
            edgePixels.add(new Point(rightestWidth, lowestHeight));
            edgePixels.add(new Point(rightestWidth, highestHeight));

            //display cropped image
            System.out.println("Printing cropped image");
            BufferedImage croppedImg = img.getSubimage(leftestWidth, lowestHeight,
                    rightestWidth-leftestWidth+1, highestHeight-lowestHeight+1);
            JFrame frame = new JFrame();
            frame.add(new JLabel(new ImageIcon(croppedImg)));
            frame.pack();
            frame.setVisible(true);

        }

        return edgePixels;
    }
    //how to pass ArrayListPoint as an argument?
    public static void segmentLetters() {

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