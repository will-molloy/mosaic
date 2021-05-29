package com.wilmol.mosaic;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.Resources;
import java.nio.file.Path;
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

    app.run(bigImage, 1, smallImages, 1);

    Image actual =
        Image.read(Path.of(Resources.getResource("AppTest/tree-output.png").toURI())).orElseThrow();
    Image expected =
        Image.read(Path.of(Resources.getResource("AppTest/expected-tree-output.png").toURI()))
            .orElseThrow();
    assertThat(actual).isEqualTo(expected);
  }
}
