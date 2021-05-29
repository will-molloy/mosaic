package com.wilmol.mosaic;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.math.IntMath;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents an image.
 *
 * @author <a href=https://wilmol.com>Will Molloy</a>
 */
@Immutable
@ThreadSafe
final class Image {

  private static final Logger log = LogManager.getLogger();

  private final BufferedImage bufferedImage;

  private Image(BufferedImage image) {
    checkArgument(
        IntMath.saturatedMultiply(image.getWidth(), image.getHeight()) < Integer.MAX_VALUE,
        "Image too big");

    if (image.getType() == BufferedImage.TYPE_INT_RGB) {
      this.bufferedImage = image;
    } else {
      log.debug("converting image");
      BufferedImage convertedImage =
          new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics graphics = convertedImage.getGraphics();
      graphics.drawImage(image, 0, 0, null);
      graphics.dispose();
      this.bufferedImage = convertedImage;
    }
  }

  int width() {
    return bufferedImage.getWidth();
  }

  int height() {
    return bufferedImage.getHeight();
  }

  Image resize(int width, int height) {
    try {
      return new Image(Thumbnails.of(bufferedImage).forceSize(width, height).asBufferedImage());
    } catch (IOException e) {
      throw uncheckedIoException("Error resizing image", e);
    }
  }

  Image resize(double scale) {
    return resize((int) (scale * width()), (int) (scale * height()));
  }

  Image resizeSquare(int sideLength) {
    return resize(sideLength, sideLength);
  }

  RgbPixel getPixel(int x, int y) {
    int[] pixel = bufferedImage.getRaster().getPixel(x, y, new int[3]);
    return new RgbPixel(pixel[0], pixel[1], pixel[2]);
  }

  RgbPixel averagePixel() {
    long red = 0;
    long green = 0;
    long blue = 0;
    int count = 0;
    for (int x = 0; x < width(); x++) {
      for (int y = 0; y < height(); y++) {
        RgbPixel pixel = getPixel(x, y);
        red += pixel.red();
        green += pixel.green();
        blue += pixel.blue();
        count++;
      }
    }
    return new RgbPixel((int) (red / count), (int) (green / count), (int) (blue / count));
  }

  void savePng(Path path) {
    log.info("savePng({})", path);
    try {
      ImageIO.write(bufferedImage, "png", path.toFile());
    } catch (IOException e) {
      throw uncheckedIoException("Error writing image", e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Image image = (Image) o;
    return Objects.equals(pixels(), image.pixels());
  }

  @Override
  public int hashCode() {
    return Objects.hash(pixels());
  }

  private List<List<RgbPixel>> pixels() {
    return IntStream.range(0, width())
        .mapToObj(x -> IntStream.range(0, height()).mapToObj(y -> getPixel(x, y)).toList())
        .toList();
  }

  static Image read(Path path) {
    log.info("read({})", path);
    try {
      BufferedImage bufferedImage = ImageIO.read(path.toFile());
      return new Image(bufferedImage);
    } catch (IOException e) {
      throw uncheckedIoException("Error reading image", e);
    }
  }

  static Image combinedImage(List<List<Image>> imagesGrid) {
    checkArgument(
        !imagesGrid.isEmpty() && !imagesGrid.get(0).isEmpty(), "Expected grid to be non-empty");
    checkArgument(
        imagesGrid.stream().map(List::size).distinct().count() == 1,
        "Expected all rows in the grid to be of the same size");
    checkArgument(
        imagesGrid.stream().flatMap(List::stream).map(Image::width).distinct().count() == 1,
        "Expected all images to have the same width");
    checkArgument(
        imagesGrid.stream().flatMap(List::stream).map(Image::height).distinct().count() == 1,
        "Expected all images to have the same height");

    int rows = imagesGrid.size();
    int cols = imagesGrid.get(0).size();

    int singleWidth = imagesGrid.get(0).get(0).width();
    int singleHeight = imagesGrid.get(0).get(0).height();

    int combinedWidth = singleWidth * cols;
    int combinedHeight = singleHeight * rows;

    BufferedImage combined =
        new BufferedImage(combinedWidth, combinedHeight, BufferedImage.TYPE_INT_RGB);
    Graphics graphics = combined.getGraphics();
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        Image image = imagesGrid.get(row).get(col);
        int offSetWidth = col * singleWidth;
        int offsetHeight = row * singleHeight;
        graphics.drawImage(image.bufferedImage, offSetWidth, offsetHeight, null);
      }
    }
    graphics.dispose();
    return new Image(combined);
  }

  private static UncheckedIOException uncheckedIoException(String msg, IOException e) {
    log.error(msg, e);
    throw new UncheckedIOException(msg, e);
  }
}