import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ImageComparisonTest {

    private ImageComparison imageComparison;

    @BeforeEach
    public void setup() {
        imageComparison = new ImageComparison(new ImageComparisonConfig());
    }

    @Test
    public void compareDifferentImages() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/different-image.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compareImages(expectedImage, differentImage);

        assertImageComparisonFailed(result, "different");
    }

    @Test
    public void compareSameImages() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/base.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compareImages(expectedImage, differentImage);

        assertImageComparisonPassed(result);
    }

    @Test
    public void compareImagesWithSlightMovement() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/slight-movement.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compareImages(expectedImage, differentImage);

        assertImageComparisonPassed(result);
    }

    @Test
    public void compareImagesWithLargeMovement() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/big-movement.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compareImages(expectedImage, differentImage);

        assertImageComparisonFailed(result, "large-movement");
    }

  @Test
  public void compareDocumentsWithTuning() throws IOException {
    var expected = new File("./src/test/resources/document.png");
    var textMissing = new File("./src/test/resources/document-with-missing-text.png");
    var smallMovement = new File("./src/test/resources/document-with-slight-movement.png");

    var expectedImage = ImageIO.read(expected);
    var textMissingImage = ImageIO.read(textMissing);
    var smallMovementImage = ImageIO.read(smallMovement);

    var imageComparison = new ImageComparison(new ImageComparisonConfig()
        .setMaxPixelUsage(4)
        .setMaxSearchDistance(8));
    var missingTextResult = imageComparison.compareImages(expectedImage, textMissingImage);
    var smallMovementResult = imageComparison.compareImages(expectedImage, smallMovementImage);

    assertImageComparisonFailed(missingTextResult, "document-missing-text");
    assertImageComparisonPassed(smallMovementResult);
  }

    private static void assertImageComparisonPassed(ImageComparison.ImageComparisonResult result) {
      assertSame(ImageComparison.ImageComparisonState.IMAGES_MATCH, result.outcome());
    }

    private static void assertImageComparisonFailed(ImageComparison.ImageComparisonResult result, String outputFileName) throws IOException {
        assertSame(ImageComparison.ImageComparisonState.IMAGES_DO_NOT_MATCH, result.outcome());

        var outputFolder = new File("./build/test-images/%s".formatted(ImageComparisonTest.class.getName()));
        //noinspection ResultOfMethodCallIgnored
        outputFolder.mkdirs();

        var forwardComparison = new File(outputFolder, "comparison-%s-f.png".formatted(outputFileName));
        var backwardsComparison = new File(outputFolder, "comparison-%s-b.png".formatted(outputFileName));
        var forwardDiff = new File(outputFolder, "difference-%s-f.png".formatted(outputFileName));
        var backwardsDiff = new File(outputFolder, "difference-%s-b.png".formatted(outputFileName));

        ImageIO.write(result.forwardComparisonImage(), "png", forwardComparison);
        ImageIO.write(result.backwardComparisonImage(), "png", backwardsComparison);
        ImageIO.write(result.forwardDifferenceImage(), "png", forwardDiff);
        ImageIO.write(result.backwardDifferenceImage(), "png", backwardsDiff);
    }

}
