# Mongolian Script OCR using MLP classification and Custom Glyph Segmentation

## Overview

This project presents an end-to-end Optical Character Recognition (OCR) system designed to recognize printed 
Mongolian script from input images and convert it into digital text. The system is implemented in Java and integrates image 
preprocessing (binarization, text-region detection), hierarchal segmentation (column, word, and glyph-level), and 
glyph classification using Multi-Layer Perceptron. 

The system supports both single glyph recognition and full-text Mongolian Script OCR, enabling flexible evaluation and 
real-time usage through a web-based interface.

---

## Features

* Glyph-level classification (single character recognition)
* Full-text OCR pipeline (segmentation + reconstruction)
* Image preprocessing (binarization, bounding box detection)
* Column, word, and glyph segmentation
* Multi-Layer Perceptron (MLP) for glyph classification
* Lightweight HTTP server for real-time interaction
* Browser-based user interface

---

## System Architecture

The system is designed as a modular pipeline architecture consisting of multiple components responsible for preprocessing, 
recognition, application control, and data handling. Each module has a clearly defined responsibility within the overall OCR workflow.

### 1. Presentation Layer (UI)

* Provides a web-based interface for image upload and result visualization
* Handles user interaction only and communicates with the backend via HTTP requests

### 2. Controller Layer

* Manages communication between the frontend and the OCR processing pipeline
* Handles HTTP requests and coordinates the execution of OCR tasks
* Reconstructs predicted glyph sequences into words and sentences
* Applies post-processing heuristics to correct segmentation and Mongolian script keyboard encoding 

### 3. Preprocessing Layer

* Performs image preprocessing including binarization and normalization
* Detects text regions using bounding box extraction
* Segments input images into columns, words, and individual glyphs
* Prepares normalized glyph inputs for classification

### 4. Recognition Layer

* Implements a Multi-Layer Perceptron (MLP) for glyph classification
* Converts input feature vectors into probability distributions over classes
* Outputs predicted glyph labels based on highest probability

### 5. Data Layer (DatasetLoader)

* Loads and structures the preprocessed dataset used for training
* Provides input feature vectors and corresponding labels
* Used exclusively during the training phase of the model

---

Overall, the system follows a pipeline-based architecture where each stage progressively transforms 
Mongolian script text image input into structured text output through preprocessing, classification, and post-processing steps.

---

## Technologies Used

* Java
* Java AWT (BufferedImage)
* Custom Multi-Layer Perceptron (MLP) and Glyph Segmentation
* HTTP Server (`com.sun.net.httpserver`)
* Basic HTML/CSS (frontend interface)

---

## How to Run

1. Make sure the dataset path in the Main class is set. (Its default training set is set to finalTrainingSet)

2. Compile and run the `Main` class to train the dataset:

3. Open a web browser and navigate to:

   ```
   http://localhost:8080
   ```

4. Upload an image and select:

    * **Glyph mode** for single character prediction
    * **Text mode** for full OCR processing

---

## Input Requirements

* Images should contain a black and white printed Mongolian script, using Noto Sans Mongolian font with 40 pixel size
* Image must be in PNG format
* High contrast (preferably black text on light background) with fully vertical positioning
* For glyph mode: input image must be **30×20 pixels**
* For full-text recognition mode, the word count limit is 100. 

---

## Evaluation

The system supports two evaluation modes:

* **Glyph Accuracy Test**

    * Measures classification performance per character
    * Uses confusion matrix analysis

* **Full Text Accuracy Test**

    * Evaluated using **Character Error Rate (CER)**
    * Provides overall OCR performance

---

## Limitations

* Designed for printed Mongolian script text image
* Limited to trained glyph classes
* Performance depends on image quality and segmentation accuracy

---

## Future Work

* Extend dataset to include more glyph variations
* Replace MLP with more advanced deep learning models (e.g., CNNs)
* Enhance frontend interface and error handling

---

## Author

This project was developed as part of an undergraduate Computer Science thesis. 
