import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
//imports used for normalizing the glyph image after segmenting
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
//imports used to display the image on screen for debugging purposes -> will be deleted on final version of the code
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

public class Segmentation {
    //delete in the final code
    public static void main(String[] args) {
        //load image file
        String inputFileName = "datasets2.png";
        File file = new File(inputFileName);
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
        int[] edgesCoordinates = detectBoundingBox(img);

        //crops the image to the detected word space if coordinates for pixels are detected
        if (edgesCoordinates[0] == 0) {
            System.out.println("There was no word detected in the image.");
            System.exit(0); //The program is terminated if there is no black pixel detected
        }
        else {
            wordOnlyImage = crop(img, edgesCoordinates);
        }

        BufferedImage binarizedImage = binarize(wordOnlyImage);
        ArrayList <int[]> columnCoordinates = segmentColumns(binarizedImage);
        //finds the coordinates for every word in every column
        ArrayList<int[]> everyWordCoordinates = segmentWordsPerColumn(binarizedImage, columnCoordinates);
        ArrayList<BufferedImage> glyphs = new ArrayList<>(); //variable to save the resized image of the letters

        //calls the method to segment letters for each word and then resize it
        for (int w = 0; w < everyWordCoordinates.size(); w++) {
            int[] eachWordCoordinates = everyWordCoordinates.get(w);
            glyphs = segmentGlyphsAndNormalize(binarizedImage, eachWordCoordinates, inputFileName, w);
            for (BufferedImage glyph : glyphs) {
                double[] inputVector = Segmentation.flattenImage(glyph);
                System.out.println(Arrays.toString(inputVector));
            }
        }
    }

    // display an image in a JPanel popup for debugging purposes -> delete for final version
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

    //binarizing an image to only black and white
    public static BufferedImage binarize(BufferedImage img) {
        System.out.println("Binarizing Image");
        // binarizedImage uses TYPE_BYTE_GRAY, which represents a single-channel grayscale image where each pixel is stored as 8-bit intensity (0–255)
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

    //method to detect the bounding box that identifies the text region in the given image
    public static int[] detectBoundingBox(BufferedImage img){
        int width = img.getWidth(); //number of columns
        int height = img.getHeight(); //number of rows
        int threshold = 0; //threshold to identify the darker pixel for the words
        int leftestWidth = width;
        int highestHeight = 0;
        int rightestWidth = 0;
        int lowestHeight = height;
        boolean found = false; //variable to keep track of the detection of black pixels in the image

        //black strokes should be less than or equal to 120
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff; //extracting the intensity value
                if (pixel == threshold) {
                    found = true; //sets to true if black pixel was found
                    //comparing the values to find the bounding box
                    if (x < leftestWidth) leftestWidth = x;
                    if (x > rightestWidth) rightestWidth = x;
                    if (y > highestHeight) highestHeight = y;
                    if (y < lowestHeight) lowestHeight = y;
                }
            }
        }

        //return null if there was no black pixel detected, assuming there isn't any word in the photo
        if (!found) {
            return null;
        }
        //returns the coordinates for the text region
        return new int[] {leftestWidth, lowestHeight, rightestWidth, highestHeight};
    }

    //crops the input image into the given coordinates -> use this for testing purposes to save the images in a folder
    public static BufferedImage crop (BufferedImage img, int[] edgeCoordinates) {
        BufferedImage croppedImg = img.getSubimage( edgeCoordinates[0], edgeCoordinates[1],
                edgeCoordinates[2]-edgeCoordinates[0],
                edgeCoordinates[3]-edgeCoordinates[1]);
        System.out.println("Printing cropped image");
        display (croppedImg);
        return croppedImg;
    }

    //method to find the coordinates for each column in the input image using Vertical Projection Profile (VPP)
    public static ArrayList<int[]> segmentColumns(BufferedImage img) {
        ArrayList<Integer> transitions = new ArrayList<>(); //variable to keep the transitions
        ArrayList<int[]> columnCoordinates = new ArrayList<>(); //variable to keep track of the column coordinates
        int width = img.getWidth();
        int height = img.getHeight();
        int[] vertSums = new int[width];
        int minTransitionWidth = 1;

        //find the vertical projection profile by summing the count of black pixels
        for (int x = 0; x < width; x++) {
            int sum = 0;
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y) & 0xff;
                //add 1 if the pixel is black, add 0 if the pixel is otherwise
                sum = (pixel == 0 ) ? 1 : 0;
                //finds column sum for each column
                vertSums[x] += sum;
            }
        }

        boolean textExist = false;
        int startColumn = 0;

        for (int x = 0; x < width; x++) {
            if (!textExist && vertSums[x] > 0) {
                //column starts when the sum of the black pixel is greater than 0
                textExist = true;
                startColumn = x;
            } else if (textExist && vertSums[x] == 0) {
                //column ends when the sum of the column reaches 0
                textExist = false;
                int endColumn = x - 1;
                //the start and end of the columns are added as transitions only when their length is greater or equal to the minimum transition width
                if (endColumn - startColumn >= minTransitionWidth) {
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
                columnCoordinates.add(new int[] {startX, 0, endX, height - 1});
                crop(img, segmentCoordinates); //for debugging purposes
            }
        }
        return columnCoordinates;
    }

    //method to segment the words within each column using Horizontal Projection Profile (HPP)
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

    //segments every letter by creating artificial whitespace through cutting the word text image from the left
    public static ArrayList<BufferedImage> segmentGlyphsAndNormalize(BufferedImage img, int[] eachWordCoordinates, String inputFileName, int wordCount) {
        //Variable Declarations
        ArrayList<BufferedImage> glyphs = new ArrayList<>();
        int startX = eachWordCoordinates[0];
        int startY = eachWordCoordinates[1];
        int endX = eachWordCoordinates[2];
        int endY = eachWordCoordinates[3];
        ArrayList<int[]> glyphCoordinates = new ArrayList<>(); //variable to save the coordinates of the letters
        ArrayList<Integer> transitions = new ArrayList<>();
        boolean started = false; // Used to detect the start of the first glyph in the word

        // Saving the details of the original word img
        int wordWidth = endX - startX;
        int wordHeight = endY - startY;
        double cutValue = 0.0;
        int targetWidth = 0;
        int minHeight = 0;
        BufferedImage wordImg = img.getSubimage(startX, startY, wordWidth, wordHeight);


        double ratio = (double)wordWidth/(double)wordHeight;
        System.out.println( "word count " + wordCount + "word ratio: " + ratio);
        //depending on the ratio between the width and the height, the targetwidth, cutvalue, and minheight are set
        if (ratio >= 2.0) {
            targetWidth = 30;
            cutValue = 0.35;
            minHeight = 0;
        }
        else if ((2.0 > ratio)  && (ratio >= 0.9))  {
            targetWidth = 30;
            cutValue = 0.38;
            minHeight = 3;
        }
        else if ((0.9 > ratio)  && (ratio >= 0.75))  {
            targetWidth = 30;
            cutValue = 0.39;
            minHeight = 3;
        }
        else if ((0.75 > ratio)  && (ratio >= 0.58))  {
            targetWidth = 30;
            cutValue = 0.41;
            minHeight = 3;
        }
        else if (( 0.58 > ratio) && (ratio >= 0.50)) {
            targetWidth = 40;
            cutValue = 0.39;
            minHeight = 4;
        }
        else if ( 0.50 > ratio) {
            targetWidth = 40;
            cutValue = 0.42;
            minHeight = 4;
        }

        double scale = (double) targetWidth / wordWidth;
        int resizedHeight = (int) (wordHeight * scale);
        //resize the original image into a fix width and scale the height
        BufferedImage resizedWord = resizeImage(wordImg, targetWidth, resizedHeight);

        //reassigning the new values for the image coordinates
        startX = 0;
        startY = 0;
        endX = resizedWord.getWidth();
        endY = resizedWord.getHeight();

        //creates a left zone of the image by cutting before the connection line to create artificial whitespace
        //maybe check the connection line-> whether the cut amount is greater or lesser than it
        int leftZoneEnd = (int) (resizedWord.getWidth() * cutValue);
        //display(resizedWord);
        //find horizontal projection line within the left zone of the word
        int[] hppSum = findHPP(resizedWord, startX, startY, leftZoneEnd, endY);

        //loop over the hpp values and find the transition zone
        for (int i = 1; i < hppSum.length - 1; i++) {
            //start of glyph in the word image is detected if hppSum becomes greater than 0
            if (!started) {
                if (hppSum[i] > 0) {
                    transitions.add(i);
                    started = true;
                }
                continue; // Move to the next iteration when the first transition is added to skip the rest of the transition detection code
            }


            //transition is added when black pixel row turns to fully white
            if (hppSum[i] == 0 && Math.abs(hppSum[i] - hppSum[i - 1]) > 0) {
                transitions.add(i + 1);
            }

        }

        //threshold for whether or not to include the transition by comparing to the minimum height
        int lastStart = transitions.get(transitions.size()- 1);
        if (resizedWord.getHeight() - lastStart < minHeight && !transitions.isEmpty()) {
            //merge with previous transition if the transition length is too short
            transitions.set(transitions.size() - 1, endY);
        } else if (lastStart < endY) {
            if (transitions.isEmpty() || transitions.get(transitions.size() - 1) != endY) {
                transitions.add(endY); //add the last height coordinate if the previous transition wasn't long enough
            }
        }

        //makes letter coordinates with the transitions; the end of the letter becomes the start of the next letter
        for (int i = 0; i < transitions.size() - 1; i++) {
            int startTransition = transitions.get(i);
            int endTransition = transitions.get(i + 1);

            //skips duplicate or invalid transitions
            if (endTransition <= startTransition) {
                continue;
            }

            //merges small segments that is less than the minHeight  with the next one
            if (endTransition - startTransition < minHeight) {
                if (i + 2 < transitions.size()) {
                    //merges the next transition
                    endTransition = transitions.get(i + 2);
                    i++; //skips to the next transition
                } else {
                    continue;
                }
            }

            if (endTransition - startTransition >= minHeight) { //enforcing that the transitions must be longer than the minimum height to avoid having small segments
                int[] segmentCoordinates = new int[] { startX, startTransition, endX, endTransition };
                glyphCoordinates.add(segmentCoordinates);
                crop(resizedWord, segmentCoordinates);
                //calls the resize letter method to scale the letter to 30*20 pixels, in order to feed it for the mlp
                BufferedImage glyph = normalizeGlyphImage(resizedWord, glyphCoordinates, inputFileName, wordCount);
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();

        // Keep the image sharp for binary images
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public static BufferedImage normalizeGlyphImage (BufferedImage croppedImage, ArrayList<int[]> glyphCoordinates, String inputFileName, int wordCount) {
        int targetWidth = 30;
        int targetHeight = 20;
        BufferedImage resizedLetter = null;
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

        //separate image for the letter
        for (int i = 0; i < glyphCoordinates.size(); i++) {
            int width = glyphCoordinates.get(i)[2] - glyphCoordinates.get(i)[0];
            int height = glyphCoordinates.get(i)[3] - glyphCoordinates.get(i)[1];

            //displaying segmented letters
            BufferedImage letter = croppedImage.getSubimage(glyphCoordinates.get(i)[0], glyphCoordinates.get(i)[1], width, height);
            //display(letter);
            //calculating scaling value
            double scale = Math.min((double) targetWidth / width, (double) targetHeight / height);

            //calculating updated width and height to resize
            int scaledWidth = (int) Math.round(width * scale);
            int scaledHeight = (int) Math.round(height * scale);
            //creates an image with the size of the scaled width and scaled height
            BufferedImage scaledImage = new BufferedImage(
                    scaledWidth, scaledHeight, BufferedImage.TYPE_BYTE_GRAY
            );
            //display(scaledImage);

            Graphics2D gScaled = scaledImage.createGraphics(); //creates Graphics2D object gScaled
            //bicubic interpolation to resize image in a better quality
            gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            gScaled.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); //focus on quality
            gScaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //reduce wonkiness
            gScaled.drawImage(letter, 0, 0, scaledWidth, scaledHeight, null); //draw the image to the scaled width and height
            gScaled.dispose();

            //new image with the desired width and height
            resizedLetter = new BufferedImage( targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gOut = resizedLetter.createGraphics();
            gOut.setColor(Color.WHITE);
            gOut.fillRect(0, 0, targetWidth, targetHeight); //makes the whole image background to white
            int xSpace = (targetWidth - scaledWidth) / 2;
            int ySpace = (targetHeight - scaledHeight) / 2;
            //padding to center the resized letter image
            gOut.drawImage(scaledImage, xSpace, ySpace, null);
            gOut.dispose();

            //saves the resized image into a folder -> used for making letter training dataset
            File outputFile = new File(folder, "word__" + wordCount + "_letter__" + i + ".png");
            try {
                ImageIO.write(resizedLetter, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return resizedLetter;
    }

    //method flattens 2D image to 1D vector
    public static double[] flattenImage(BufferedImage resizedLetter) {
        int width = resizedLetter.getWidth();
        int height = resizedLetter.getHeight();
        double[] input = new double[width * height];

        //flattens the image and returns it
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = resizedLetter.getRGB(x, y) & 0xff; //assuming grayscale images
                input[y * width + x] = pixel / 255.0; //normalized to the range [0,1]
            }
        }
        return input;
    }
}
