package editor;

public class StackLink {
    protected TextNode lastItem;
    protected String action;
    protected TextNode before;

    public StackLink(TextNode before, TextNode lastItem, String action) {
        this.lastItem = lastItem;
        this.action = action;
        this.before = before;
    }

    public TextNode lastItem() {
        return lastItem;
    }

    public String action() {
        return action;
    }

    public TextNode before() {
        return before;
    }
}
