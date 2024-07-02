import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ImageComparison {

  private final float threshold;
  private final int maxPixelUsage;
  private final float percentAllowedDifferent;

  private final int maxComparisonBoxWidth;
  private final int maxComparisonBoxHeight;
  private final int comparisonBoxRgb;

  //Computed
  private final int[][] concentricSquares;

  public ImageComparison(ImageComparisonConfig config) {
    this.threshold = config.getPixelRgbThreshold();
    this.maxPixelUsage = config.getMaxPixelUsage();
    this.maxComparisonBoxWidth = config.getMaxComparisonBoxWidth();
    this.maxComparisonBoxHeight = config.getMaxComparisonBoxHeight();
    this.comparisonBoxRgb = config.getComparisonBoxColor().getRGB();
    this.percentAllowedDifferent = config.getPercentageAllowedDifferent();
    this.concentricSquares = concentricSquares(config.getMaxSearchDistance());
  }

  public ImageComparison() {
    this(new ImageComparisonConfig());
  }

  public ImageComparisonResult compareImages(BufferedImage a, BufferedImage b) {
    if (imagesAreDifferentSize(a, b)) {
      return imagesAreDifferentSizes();
    }

    var forwardResult = compare(a, b);
    var backwardResult = compare(b, a);
    var totalPixels = a.getWidth() * b.getWidth() * 2;
    var totalPixelsFailed = forwardResult.numberOfPixelsWhichFailedToMatch() + backwardResult.numberOfPixelsWhichFailedToMatch();
    var percentageDifferent = (float) totalPixelsFailed / totalPixels;

    if (percentageDifferent <= percentAllowedDifferent) {
      return imagesMatchSufficiently();
    }

    return imageContentsAreDifferent(forwardResult, backwardResult);
  }


  private PartialImageComparisonResult compare(BufferedImage image1, BufferedImage image2) {

    var matchedPixels = new int[image1.getWidth()][image1.getHeight()];
    var numFailedPixels = 0;
    var comparisonImage = copyImage(image1);
    List<ComparisonBox> boxes = new ArrayList<>();

    for (int x = 0; x < image1.getWidth(); x++) {
      for (int y = 0; y < image1.getHeight(); y++) {
        var pixel = getPixelAt(image1, x, y);
        if (!anyPixelWithinRangeMeetsThreshold(image2, pixel, matchedPixels)) {
          addPointToBoxInListOrCreateNew(boxes, x, y);
          numFailedPixels++;
        }
      }
    }

    var diffImage = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < diffImage.getWidth(); x++) {
      for (int y = 0; y < diffImage.getHeight(); y++) {
        var intensity = getScaledRGB(matchedPixels[x][y]);
        var color = new Color(intensity, intensity, intensity);
        diffImage.setRGB(x, y, color.getRGB());
      }
    }

    if (!boxes.isEmpty()) {
      drawComparisonBoxesOnImage(comparisonImage, boxes);
      return new PartialImageComparisonResult(numFailedPixels, comparisonImage, diffImage);
    }


    return new PartialImageComparisonResult(numFailedPixels, comparisonImage, diffImage);
  }

  private int getScaledRGB(int matchCount) {
    return (255 * matchCount / maxPixelUsage);
  }

  private BufferedImage copyImage(BufferedImage image) {
    ColorModel cm = image.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = image.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }

  private void drawComparisonBoxesOnImage(BufferedImage image, List<ComparisonBox> boxes) {
    boxes.forEach(box -> drawBoxOnImage(image, box));
  }

  private void drawBoxOnImage(BufferedImage image, ComparisonBox box) {
    IntStream.range(box.x1, box.x2).forEach(x -> {
      image.setRGB(x, box.y1, comparisonBoxRgb);
      image.setRGB(x, box.y2, comparisonBoxRgb);
    });

    IntStream.range(box.y1, box.y2).forEach(y -> {
      image.setRGB(box.x1, y, comparisonBoxRgb);
      image.setRGB(box.x2, y, comparisonBoxRgb);
    });
  }

  private void addPointToBoxInListOrCreateNew(List<ComparisonBox> boxes, int x, int y) {
    boolean added = false;
    for (ComparisonBox box : boxes) {
      added = addPointToBox(box, x, y);
      if (added) {
        break;
      }
    }

    if (!added) {
      boxes.add(newBox(x, y));
    }
  }

  private ComparisonBox newBox(int x, int y) {
    return new ComparisonBox(x, y, x, y);
  }

  private boolean addPointToBox(ComparisonBox box, int x, int y) {
    int newX1 = Math.min(box.x1, x);
    int newX2 = Math.max(box.x2, x);
    int newY1 = Math.min(box.y1, y);
    int newY2 = Math.max(box.y2, y);

    if (boxWouldExceedMaxValues(newX1, newY1, newX2, newY2)) {
      return false;
    }

    box.setNewPoints(newX1, newY1, newX2, newY2);

    return true;
  }

  private boolean boxWouldExceedMaxValues(int x1, int y1, int x2, int y2) {
    return Math.abs(x2 - x1) > maxComparisonBoxWidth || Math.abs(y2 - y1) > maxComparisonBoxHeight;
  }

  private int[][] concentricSquares(int size) {
     var length = (size * 2) - 1;
    var squares = new int[length * length][2];
    squares[0] = new int[]{0, 0};
    var count = 1;
    for (int level = 1; level < size; level++) {
      for(int dx = -level; dx <= level; dx++) {
        squares[count] = new int[]{dx, level};
        count++;
      }
      for (int dy = level-1; dy >= -level; dy--) {
        squares[count] = new int[]{level, dy};
        count++;
      }
      for (int dx = level-1; dx >= -level; dx--) {
        squares[count] = new int[]{dx, -level};
        count++;
      }
      for(int dy = -level+1; dy < level; dy++) {
        squares[count] = new int[]{-level, dy};
        count++;
      }
    }
    return squares;
  }

  private boolean anyPixelWithinRangeMeetsThreshold(BufferedImage image, Pixel pixel, int[][] matchedPixels) {
    for (int[] dxy : concentricSquares) {
      var x = Math.min(Math.max(0, dxy[0] + pixel.x), image.getWidth() - 1);
      var y = Math.min(Math.max(0, dxy[1] + pixel.y), image.getHeight() - 1);
      var pixelInSearchImage = getPixelAt(image, x, y);
      if (pixelsAreWithinThresholdValuesOfEachOther(pixel, pixelInSearchImage)
          && pixelMatchesUnderThreshold(pixelInSearchImage, matchedPixels)) {
        matchedPixels[x][y]++;
        return true;
      }
    }
    return false;
  }

  private boolean pixelMatchesUnderThreshold(Pixel pixel, int[][] matchedPixels) {
    return matchedPixels[pixel.x][pixel.y] < maxPixelUsage;
  }

  private boolean pixelsAreWithinThresholdValuesOfEachOther(Pixel a, Pixel b) {
    return withinTheshold(a.r, b.r)
        && withinTheshold(a.g, b.g)
        && withinTheshold(a.b, b.b);
  }

  private boolean withinTheshold(float a, float b) {
    return Math.abs(a - b) <= threshold;
  }

  private Pixel getPixelAt(BufferedImage image, int x, int y) {
    int clr = image.getRGB(x, y);
    final float red = ((clr & 0x00ff0000) >> 16) / 255f;
    final float green = ((clr & 0x0000ff00) >> 8) / 255f;
    final float blue = (clr & 0x000000ff) / 255f;

    return new Pixel(x, y, red, green, blue);
  }

  private boolean imagesAreDifferentSize(BufferedImage image1, BufferedImage image2) {
    return image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight();
  }

  private static ImageComparisonResult imagesAreDifferentSizes() {
    return new ImageComparisonResult(ImageComparisonState.IMAGES_ARE_DIFFERENT_SIZES, null, null, null, null);
  }

  private static ImageComparisonResult imageContentsAreDifferent(PartialImageComparisonResult forwardResult,
                                                                 PartialImageComparisonResult backwardResult) {
    return new ImageComparisonResult(ImageComparisonState.IMAGES_DO_NOT_MATCH, forwardResult.comparisonImage,
        backwardResult.comparisonImage, forwardResult.differenceImage, backwardResult.differenceImage);
  }

  private static ImageComparisonResult imagesMatchSufficiently() {
    return new ImageComparisonResult(ImageComparisonState.IMAGES_MATCH, null, null, null, null);
  }

  public record ImageComparisonResult(ImageComparisonState outcome,
                                      BufferedImage forwardComparisonImage,
                                      BufferedImage backwardComparisonImage,
                                      BufferedImage forwardDifferenceImage,
                                      BufferedImage backwardDifferenceImage) {
  }

  public record PartialImageComparisonResult(int numberOfPixelsWhichFailedToMatch,
                                             BufferedImage comparisonImage,
                                             BufferedImage differenceImage) {
  }

  private static class ComparisonBox {
    private int x1;
    private int y1;
    private int x2;
    private int y2;

    private ComparisonBox(int x1, int y1, int x2, int y2) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
    }

    private void setNewPoints(int x1, int y1, int x2, int y2) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
    }
  }

  private record Pixel(int x, int y, float r, float g, float b) {
  }

  public enum ImageComparisonState {
    IMAGES_ARE_DIFFERENT_SIZES,
    IMAGES_DO_NOT_MATCH,
    IMAGES_MATCH
  }
}