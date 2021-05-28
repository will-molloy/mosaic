# mosaic

[![build](https://github.com/wilmol/mosaic/workflows/build/badge.svg?event=push)](https://github.com/wilmol/mosaic/actions?query=workflow%3Abuild)
[![codecov](https://codecov.io/gh/wilmol/mosaic/branch/master/graph/badge.svg)](https://codecov.io/gh/wilmol/mosaic)

recreate a big image from smaller images

## Running the app

### Requirements:
- JDK 16

### Build:
```
./gradlew build
```

### Run:
Run main [`App`](mosaic/src/main/java/com/wilmol/mosaic/App.java) class
- Point to big image file (e.g. `./data/big-image.jpg`)
- And small images directory (e.g. `./data/small-images`)
