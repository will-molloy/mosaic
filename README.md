# mosaic

[![build](https://github.com/will-molloy/mosaic/workflows/build/badge.svg?event=push)](https://github.com/will-molloy/mosaic/actions?query=workflow%3Abuild)
[![codecov](https://codecov.io/gh/will-molloy/mosaic/branch/main/graph/badge.svg)](https://codecov.io/gh/will-molloy/mosaic)

recreate a big image from smaller images

## Example

|<img src=data/example.jpg width=200>|<img src=data/example-output.png width=200>|
|---|---|
|*Toby*|*Toby recreated from various pets*|

## Running the app

### Requirements:
- JDK 17

### Build:
```
./gradlew build
```

### Run:
Run main [`App`](mosaic/src/main/java/com/willmolloy/mosaic/App.java) class
- Point to big image file (e.g. `./data/big-image.jpg`)
- And small images directory (e.g. `./data/small-images`)
