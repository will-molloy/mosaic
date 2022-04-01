package com.willmolloy.mosaic;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.getNameWithoutExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads the small images for the mosaic.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class SmallImagesLoader {

  private static final Logger log = LogManager.getLogger();

  private final Path cacheDirectory = Path.of("data/cache");

  List<Image> loadSmallImages(Path smallImagesDirectory, int resizedSmallImagesSideLength)
      throws IOException {
    Files.createDirectories(cacheDirectory);
    return Files.list(Path.of(smallImagesDirectory.toString()))
        .map(path -> load(path, resizedSmallImagesSideLength))
        .flatMap(Optional::stream)
        .map(smallImage -> smallImage.resizeSquare(resizedSmallImagesSideLength))
        // TODO ImageIO is synchronised so this does nothing?
        .parallel()
        .toList();
  }

  private Optional<Image> load(Path smallImagePath, int resizedSideLength) {
    // reading a large image can be expensive, so caching the resized small images (to disk)
    // (usual case is the resized small images are much smaller, something like 100x100, which is
    // faster to read - the resizing itself is not slow)
    String cachedFileName =
        "%s-%dx%d.png"
            .formatted(
                getNameWithoutExtension(checkNotNull(smallImagePath.getFileName()).toString()),
                resizedSideLength,
                resizedSideLength);
    Path cachePath = cacheDirectory.resolve(cachedFileName);
    if (Files.exists(cachePath)) {
      log.debug("reading from cache: {}", cachePath);
      return Image.read(cachePath);
    }

    Optional<Image> optionalImage =
        Image.read(smallImagePath).map(image -> image.resizeSquare(resizedSideLength));

    optionalImage.ifPresent(
        image -> {
          log.debug("writing to cache: {}", cachePath);
          image.savePng(cachePath);
        });
    return optionalImage;
  }
}
