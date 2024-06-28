import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ImageComparison {

    private final float threshold;
    private final int searchDistance;

    private final int maxComparisonBoxWidth;
    private final int maxComparisonBoxHeight;
    private final int comparisonBoxRgb;

    public ImageComparison(ImageComparisonConfig config) {
        this.threshold = config.getPixelRgbThreshold();
        this.searchDistance = config.getMaxSearchDistance();
        this.maxComparisonBoxWidth = config.getMaxComparisonBoxWidth();
        this.maxComparisonBoxHeight = config.getMaxComparisonBoxHeight();
        this.comparisonBoxRgb = config.getComparisonBoxColor().getRGB();
    }

    public ImageComparisonResult compare(BufferedImage image1, BufferedImage image2) {

        if (imagesAreDifferentSize(image1, image2)) {
            return new ImageComparisonResult(false, null);
        }

        var comparisonImage = copyImage(image1);
        List<ComparisonBox> boxes = new ArrayList<>();

        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                var pixel = getPixelAt(image1, x, y);
                if (!anyPixelWithinRangeMeetsThreshold(image2, pixel)) {
                    addPointToBoxInListOrCreateNew(boxes, x, y);
                }
            }
        }

        if(!boxes.isEmpty()) {
            drawComparisonBoxesOnImage(comparisonImage, boxes);
            return new ImageComparisonResult(false, comparisonImage);
        }


        return new ImageComparisonResult(true, null);
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
            if (added) break;
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

        if(boxWouldExceedMaxValues(newX1, newY1, newX2, newY2)) return false;

        box.setNewPoints(newX1, newY1, newX2, newY2);

        return true;
    }

    private boolean boxWouldExceedMaxValues(int x1, int y1, int x2, int y2) {
        return x2 - x1 <= maxComparisonBoxWidth && y2 - y1 <= maxComparisonBoxHeight;
    }

    private boolean anyPixelWithinRangeMeetsThreshold(BufferedImage image, Pixel pixel) {
        int x1Bound = Math.max(0, pixel.x - searchDistance);
        int y1Bound = Math.max(0, pixel.y - searchDistance);
        int x2Bound = Math.min(pixel.x + searchDistance, image.getWidth() - 1);
        int y2Bound = Math.min(pixel.y + searchDistance, image.getHeight() - 1);
        for (int x = x1Bound; x <= x2Bound; x++) {
            for (int y = y1Bound; y <= y2Bound; y++) {
                var pixelInSearchImage = getPixelAt(image, x, y);
                if (pixelsAreWithinThresholdValuesOfEachOther(pixel, pixelInSearchImage)) {
                    return true;
                }
            }
        }
        return false;
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

    public record ImageComparisonResult(boolean passed, BufferedImage comparisonImage) {

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
}
