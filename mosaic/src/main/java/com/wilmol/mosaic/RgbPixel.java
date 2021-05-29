package com.wilmol.mosaic;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Represents an RGB pixel.
 *
 * @param red red value
 * @param blue blue value
 * @param green green value
 * @author <a href=https://wilmol.com>Will Molloy</a>
 */
@Immutable
@ThreadSafe
record RgbPixel(int red, int green, int blue) {
  RgbPixel {
    checkArgument(red >= 0 && red <= 255);
    checkArgument(green >= 0 && green <= 255);
    checkArgument(blue >= 0 && blue <= 255);
  }
}
