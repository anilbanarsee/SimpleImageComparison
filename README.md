# Simple Image Comparison
Compare two images for similarity. There are two thresholds to consider:
1. **Pixel threshold**, how close two pixels need to be in rgb to be considered equal
2. **Max search distance**, how far two similar pixels need to be to be considered a match.

## Purpose

The purpose of these to metrics is to allow for finer control when comparing images, where elements in one image may be slightly offset compared to the other. In this case, comparing a pixel purely against it's sister coordinate pixel, is unlikely to have the desired effect. 

For example if a vertical line moved one pixel to the right, that entire column will be considered invalid when compared coordinate for coordinate. More importantly, that diff as a whole would be considered twice as different compared to an image which didn't have the vertical line at all (!).

## Quickstart
Using it is as simple as creating an ImageComparison, and calling .compare():
```java
var imageComparison = new ImageComparison(new ImageComparisonConfig());
 var result = imageComparison.compare(expectedImage, differentImage);
```

ImageComparisonConfig is constructed with default values, but you can change them directly:
```java
var config = ImageComparisonConfig()
        .setMaxSearchDistance(10)
        .setPixelRgbThreshold(0.5);
var imageComparison = new ImageComparison(config);
var result = imageComparison.compare(expectedImage, differentImage);

```

## Examples
### Different Images
#### Image 1
![dog](/src/test/resources/base.png)
#### Image 2
![world](/src/test/resources/different-image.png)
#### Output
Failed
##### Comparison Image
![comparison](/example-outputs/comparison-different.png)

### Small Movement
#### Image 1
![dog](/src/test/resources/base.png)
#### Image 2
![world](/src/test/resources/slight-movement.png)
#### Output
Passed

### Larger Movement
#### Image 1
![dog](/src/test/resources/base.png)
#### Image 2
![world](/src/test/resources/big-movement.png)
#### Output
Failed
##### Comparison Image
![comparison](/example-outputs/comparison-large-movement.png)
