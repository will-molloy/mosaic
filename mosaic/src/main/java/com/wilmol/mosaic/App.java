package com.wilmol.mosaic;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.getNameWithoutExtension;

import com.google.common.base.Stopwatch;
import com.google.common.math.IntMath;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Core app runner.
 *
 * @author <a href=https://wilmol.com>Will Molloy</a>
 */
class App {

  private static final Logger log = LogManager.getLogger();

  @CanIgnoreReturnValue
  Image run(
      Path bigImagePath,
      double resizedBigImageScale,
      Path smallImagesPath,
      int resizedSmallImagesSideLength)
      throws Exception {
    Stopwatch stopwatch = Stopwatch.createStarted();
    log.info(
        "run(bigImagePath={}, resizedBigImageScale={}, smallImagesPath={}, resizedSmallImagesSideLength={}) started",
        bigImagePath,
        resizedBigImageScale,
        smallImagesPath,
        resizedSmallImagesSideLength);

    log.info("Reading big image");
    Image bigImage = Image.read(bigImagePath).orElseThrow().resize(resizedBigImageScale);
    log.info("{}x{} big image dimensions", bigImage.width(), bigImage.height());
    log.info("{} elapsed", stopwatch.elapsed());

    int combinedWidth = bigImage.width() * resizedSmallImagesSideLength;
    int combinedHeight = bigImage.height() * resizedSmallImagesSideLength;
    log.info("{}x{} combined image dimensions", combinedWidth, combinedHeight);
    if (IntMath.saturatedMultiply(combinedWidth, combinedHeight) == Integer.MAX_VALUE) {
      throw new OutOfMemoryError("Combined image too big");
    }

    log.info("Reading small images");
    List<Image> smallImages =
        Files.list(Path.of(smallImagesPath.toString()))
            .map(Image::read)
            .flatMap(Optional::stream)
            .map(smallImage -> smallImage.resizeSquare(resizedSmallImagesSideLength))
            .toList();
    log.info("{} small images", smallImages.size());
    log.info("{} elapsed", stopwatch.elapsed());

    log.info("Preprocessing smaller images avg pixel value");
    List<RgbPixel> avgPixels = smallImages.stream().map(Image::averagePixel).toList();
    log.info("{} elapsed", stopwatch.elapsed());

    log.info("Selecting smaller images to replace each big image pixel");
    List<List<Image>> grid = new ArrayList<>();
    for (int row = 0; row < bigImage.height(); row++) {
      grid.add(new ArrayList<>(Collections.nCopies(bigImage.width(), null)));
    }
    for (int row = 0; row < bigImage.height(); row++) {
      for (int col = 0; col < bigImage.width(); col++) {
        RgbPixel targetPixel = bigImage.getPixel(col, row);
        int i = closestPoint(targetPixel, avgPixels);
        grid.get(row).set(col, smallImages.get(i));
      }
    }
    log.info("{} elapsed", stopwatch.elapsed());

    log.info("Combining images");
    Image combinedImage = Image.combinedImage(grid);
    log.info("{} elapsed", stopwatch.elapsed());

    log.info("Saving combined image");
    combinedImage.savePng(
        bigImagePath.resolveSibling(
            getNameWithoutExtension(checkNotNull(bigImagePath.getFileName()).toString())
                + "-output.png"));
    log.info("run() finished - elapsed: {}", stopwatch.elapsed());
    return combinedImage;
  }

  // returns index
  private int closestPoint(RgbPixel target, List<RgbPixel> points) {
    int minDistance = Integer.MAX_VALUE;
    int minI = 0;
    for (int i = 0; i < points.size(); i++) {
      int redDiff = target.red() - points.get(i).red();
      int greenDiff = target.green() - points.get(i).green();
      int blueDiff = target.blue() - points.get(i).blue();
      int distance = redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff;
      if (distance < minDistance) {
        minDistance = distance;
        minI = i;
      }
    }
    return minI;
  }

  public static void main(String[] args) throws Exception {
    Path bigImagePath = Path.of("data/big-image.jpg");
    double bigImageScale = 0.2;

    Path smallImagesPath = Path.of("data/small-images");
    int smallImagesSideLength = 80;

    new App().run(bigImagePath, bigImageScale, smallImagesPath, smallImagesSideLength);
  }
}
