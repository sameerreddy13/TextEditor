package editor;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.VPos;
/**
 * Class to create a Text Node. 
 */
public class TextNode extends Text {

    public TextNode(double x, double y, String letter, String fontName, int fontSize) {
        super(x, y, letter);
        setFont(Font.font(fontName, fontSize));
        setTextOrigin(VPos.TOP);
    }

    public TextNode(double x, double y, String letter) {
        super(x, y, letter);
        setFont(Font.font("Verdana", 12));
        setTextOrigin(VPos.TOP);
    }

    public double width() {
        return Math.round(getLayoutBounds().getWidth()); 
    }

    public double height() {
        return Math.round(getLayoutBounds().getHeight());
    }


    public boolean isWhiteSpace() {
        return getText().equals(" ");
    }

    public boolean isEnter() {
        return getText().equals("\n");
    }

}
