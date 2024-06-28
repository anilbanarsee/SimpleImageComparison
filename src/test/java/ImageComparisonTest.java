import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

        var result = imageComparison.compare(expectedImage, differentImage);

        assertImageComparisonFailed(result, "different");
    }

    @Test
    public void compareSameImages() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/base.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compare(expectedImage, differentImage);

        assertImageComparisonPassed(result);
    }

    @Test
    public void compareImagesWithSlightMovement() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/slight-movement.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compare(expectedImage, differentImage);

        assertImageComparisonPassed(result);
    }

    @Test
    public void compareImagesWithLargeMovement() throws IOException {
        var expected = new File("./src/test/resources/base.png");
        var different = new File("./src/test/resources/big-movement.png");

        var expectedImage = ImageIO.read(expected);
        var differentImage = ImageIO.read(different);

        var result = imageComparison.compare(expectedImage, differentImage);

        assertImageComparisonFailed(result, "large-movement");
    }

    private static void assertImageComparisonPassed(ImageComparison.ImageComparisonResult result) {
        assertTrue(result.passed());
    }

    private static void assertImageComparisonFailed(ImageComparison.ImageComparisonResult result, String outputFileName) throws IOException {
        assertFalse(result.passed());

        var outputFolder = new File("./build/test-images/%s".formatted(ImageComparisonTest.class.getName()));
        //noinspection ResultOfMethodCallIgnored
        outputFolder.mkdirs();

        var outputLocation = new File(outputFolder, "comparison-%s.png".formatted(outputFileName));

        ImageIO.write(result.comparisonImage(), "png", outputLocation);
    }

}
