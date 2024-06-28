import java.awt.*;

public class ImageComparisonConfig {

    private float pixelRgbThreshold = 0.2f;
    private int maxSearchDistance = 5;
    private int maxComparisonBoxWidth = 100;
    private int maxComparisonBoxHeight = 50;
    private Color comparisonBoxColor = Color.RED;

    public float getPixelRgbThreshold() {
        return pixelRgbThreshold;
    }

    public ImageComparisonConfig setPixelRgbThreshold(float pixelRgbThreshold) {
        this.pixelRgbThreshold = pixelRgbThreshold;
        return this;
    }

    public int getMaxSearchDistance() {
        return maxSearchDistance;
    }

    public ImageComparisonConfig setMaxSearchDistance(int maxSearchDistance) {
        this.maxSearchDistance = maxSearchDistance;
        return this;
    }

    public int getMaxComparisonBoxWidth() {
        return maxComparisonBoxWidth;
    }

    public ImageComparisonConfig setMaxComparisonBoxWidth(int maxComparisonBoxWidth) {
        this.maxComparisonBoxWidth = maxComparisonBoxWidth;
        return this;
    }

    public int getMaxComparisonBoxHeight() {
        return maxComparisonBoxHeight;
    }

    public ImageComparisonConfig setMaxComparisonBoxHeight(int maxComparisonBoxHeight) {
        this.maxComparisonBoxHeight = maxComparisonBoxHeight;
        return this;
    }

    public Color getComparisonBoxColor() {
        return comparisonBoxColor;
    }

    public ImageComparisonConfig setComparisonBoxColor(Color comparisonBoxColor) {
        this.comparisonBoxColor = comparisonBoxColor;
        return this;
    }
}
