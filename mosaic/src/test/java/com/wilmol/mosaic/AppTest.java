package com.wilmol.mosaic;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import ij.IJ;
import ij.ImagePlus;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Component test.
 *
 * @author <a href=https://wilmol.com>Will Molloy</a>
 */
class AppTest {

  private final App app = new App();

  @Test
  void recreateTreeUsingBasicColours() throws Exception {
    Path bigImage = Path.of(Resources.getResource("AppTest/tree.jpg").toURI());
    Path smallImages = Path.of(Resources.getResource("AppTest/basic-colours").toURI());

    app.run(bigImage, 0.1, smallImages, 1);

    ImagePlus actual =
        IJ.openImage(Path.of(Resources.getResource("AppTest/tree-output.png").toURI()).toString());
    ImagePlus expected =
        IJ.openImage(
            Path.of(Resources.getResource("AppTest/expected-tree-output.png").toURI()).toString());
    assertThat(toPixels(actual)).isEqualTo(toPixels(expected));
  }

  private List<List<List<Integer>>> toPixels(ImagePlus image) {
    return IntStream.range(0, image.getHeight())
        .mapToObj(
            h ->
                IntStream.range(0, image.getWidth())
                    .mapToObj(w -> Ints.asList(image.getPixel(w, h)))
                    .toList())
        .toList();
  }
}
