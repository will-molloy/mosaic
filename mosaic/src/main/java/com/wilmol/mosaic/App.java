package com.wilmol.mosaic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.getNameWithoutExtension;

import com.google.common.base.Stopwatch;
import com.google.common.math.IntMath;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Core app runner.
 *
 * @author <a href=https://wilmol.com>Will Molloy</a>
 */
class App {

  private static final Logger log = LogManager.getLogger();

  private final Path bigImagePath = Path.of("data/big-image.jpg");
  private final Path smallImagesPath = Path.of("data/small-images");
  private final Path combinedImageOutputPath =
      bigImagePath.resolveSibling(
          getNameWithoutExtension(checkNotNull(bigImagePath.getFileName()).toString())
              + "-output.png");
  private static final int RESIZED_SMALL_IMAGE_SIDE_LENGTH = 100;

  void run() throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();
    log.info("run() started");

    ImagePlus bigImage = resize(IJ.openImage(bigImagePath.toString()), 1);
    checkArgument(bigImage.getType() == ImagePlus.COLOR_RGB, "Expected big image to be COLOUR_RGB");
    log.info("{}x{} big image dimensions", bigImage.getWidth(), bigImage.getHeight());
    log.info("{} elapsed", stopwatch.elapsed());

    int combinedWidth = bigImage.getWidth() * RESIZED_SMALL_IMAGE_SIDE_LENGTH;
    int combinedHeight = bigImage.getHeight() * RESIZED_SMALL_IMAGE_SIDE_LENGTH;
    log.info("{}x{} combined image dimensions", combinedWidth, combinedHeight);
    // ColourProcessor stores pixels in 1d array (width * height), so check that will work
    if (IntMath.saturatedMultiply(combinedWidth, combinedHeight) == Integer.MAX_VALUE) {
      throw new OutOfMemoryError("Combined image too big");
    }

    List<ImagePlus> smallImages =
        Files.list(Path.of(smallImagesPath.toString()))
            .map(path -> IJ.openImage(path.toString()))
            .filter(Objects::nonNull)
            .filter(image -> image.getType() == ImagePlus.COLOR_RGB)
            .map(smallImage -> resizeSquare(smallImage, RESIZED_SMALL_IMAGE_SIDE_LENGTH))
            .limit(100)
            .toList();
    log.info("{} small images", smallImages.size());
    log.info("{} elapsed", stopwatch.elapsed());

    // preprocess smaller images avg pixel value
    List<int[]> avgPixels = smallImages.stream().map(this::averagePixelValue).toList();

    // select smaller images to replace each big image pixel
    List<List<ImagePlus>> grid = new ArrayList<>();
    for (int row = 0; row < bigImage.getHeight(); row++) {
      grid.add(new ArrayList<>(Collections.nCopies(bigImage.getWidth(), null)));
    }
    for (int row = 0; row < bigImage.getHeight(); row++) {
      for (int col = 0; col < bigImage.getWidth(); col++) {
        int[] targetPixel = bigImage.getPixel(row, col);
        int i = closestPoint(targetPixel, avgPixels);
        grid.get(row).set(col, smallImages.get(i));
      }
    }

    ImagePlus combinedImage = combine(grid);
    //    savePng(combinedImage);
    combinedImage.show();
    log.info("run() finished - elapsed: {}", stopwatch.elapsed());
  }

  ImagePlus resize(ImagePlus image, double scale) {
    return resize(image, (int) (scale * image.getWidth()), (int) (scale * image.getHeight()));
  }

  ImagePlus resize(ImagePlus image, int width, int height) {
    return image.resize(width, height, "average");
  }

  ImagePlus resizeSquare(ImagePlus image, int sideLength) {
    return resize(image, sideLength, sideLength);
  }

  int[] averagePixelValue(ImagePlus image) {
    int pixelSize = image.getBytesPerPixel();
    long[] sum = new long[pixelSize];
    int count = 0;

    for (int w = 0; w < image.getWidth(); w++) {
      for (int h = 0; h < image.getHeight(); h++) {
        for (int i = 0; i < pixelSize; i++) {
          sum[i] += image.getPixel(w, h)[i];
          count++;
        }
      }
    }

    int[] avg = new int[pixelSize];
    for (int i = 0; i < pixelSize; i++) {
      avg[i] = (int) (sum[i] / count);
    }
    return avg;
  }

  // returns index
  int closestPoint(int[] target, List<int[]> points) {
    checkArgument(
        points.stream().allMatch(a -> a.length == target.length),
        "Expected all points to be of the same dimensions");

    int minDistance = Integer.MAX_VALUE;
    int min = 0;

    for (int i = 0; i < points.size(); i++) {
      int distance = 0;
      for (int j = 0; j < target.length; j++) {
        distance += (target[j] - points.get(i)[j]) * (target[j] - points.get(i)[j]);
      }
      if (distance < minDistance) {
        minDistance = distance;
        min = i;
      }
    }
    return min;
  }

  ImagePlus combine(List<List<ImagePlus>> imagesGrid) {
    checkArgument(
        !imagesGrid.isEmpty() && !imagesGrid.get(0).isEmpty(), "Expected grid to be non-empty");
    checkArgument(
        imagesGrid.stream().map(List::size).distinct().count() == 1,
        "Expected all rows in the grid to be of the same size");
    checkArgument(
        imagesGrid.stream().flatMap(List::stream).map(ImagePlus::getWidth).distinct().count() == 1,
        "Expected all images to have the same width");
    checkArgument(
        imagesGrid.stream().flatMap(List::stream).map(ImagePlus::getHeight).distinct().count() == 1,
        "Expected all images to have the same height");

    int rows = imagesGrid.size();
    int cols = imagesGrid.get(0).size();

    int singleWidth = imagesGrid.get(0).get(0).getWidth();
    int singleHeight = imagesGrid.get(0).get(0).getHeight();

    int combinedWidth = singleWidth * cols;
    int combinedHeight = singleHeight * rows;

    ImageProcessor processor = new ColorProcessor(combinedWidth, combinedHeight);

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        ImagePlus image = imagesGrid.get(row).get(col);
        int offSetWidth = col * singleWidth;
        int offsetHeight = row * singleHeight;
        for (int w = 0; w < singleWidth; w++) {
          for (int h = 0; h < singleHeight; h++) {
            processor.putPixel(w + offSetWidth, h + offsetHeight, image.getPixel(w, h));
          }
        }
      }
    }

    ImagePlus combinedImage = new ImagePlus();
    combinedImage.setProcessor(processor);
    return combinedImage;
  }

  void savePng(ImagePlus image) {
    FileSaver fileSaver = new FileSaver(image);
    fileSaver.saveAsPng(combinedImageOutputPath.toString());
  }

  public static void main(String[] args) throws Exception {
    new App().run();
  }
}
