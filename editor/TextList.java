package editor;

/**
 * Linked List of Text Nodes. Not circularly linked.
*/
public class TextList {
    private Link sentinel;
    private int size;
    protected double lowestY = 0;
    protected double charHeight = 15.0;
    //private double width = 0;
    protected Link beforeCursor;
    // private Link afterCursor;
   
    protected class Link {
        private Link prev;
        private TextNode current;
        private Link next;

        public Link(TextNode x, Link y) {
            prev = sentinel;
            current = x;
            next = y;
        }

        public Link prev() {
            return prev;
        }

        public Link next() {
            return next;
        }

        public TextNode current() {
            return current;
        }
    }

    
    /* Create empty list. */
    public TextList() {
        size = 0;
        sentinel = new Link(null, null);
        sentinel.prev = sentinel;
        beforeCursor = sentinel;
    }

    public void textList(TextNode item) {
        size = 1;
        sentinel = new Link(null, sentinel);
        sentinel.next = new Link(item, null);
        sentinel.next.prev = sentinel;
        beforeCursor = sentinel.next;
        beforeCursor.prev = sentinel;
    }

    
    public void sizeZero(TextNode item) {
        textList(item);
    }

    public Link getLastLink() {
        return sentinel.prev;
    }

    public Link getFirstLink() {
        return sentinel.next;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public Link front() {
        return sentinel;
    }

    public TextNode peekLast() {
        if (isEmpty()) {
            return null;
        }

        return getLastLink().current;
    }

    public double charHeight() {
        return charHeight;
    }

    public void setCharHeight(double x) {
        charHeight = x;
    }

    public TextNode insert(TextNode item) {
        if (isEmpty()) {
            sizeZero(item);
        } else {
            Link next = beforeCursor.next;
            beforeCursor.next = new Link(item, next);
            if (beforeCursor.next.next != null) {
                beforeCursor.next.next.prev = beforeCursor.next;
            }
            beforeCursor.next.prev = beforeCursor;
            beforeCursor = beforeCursor.next;
            size++;
        }

        return item;
        
    }

    public TextNode delete() {
        if (beforeCursor != sentinel) {
            TextNode item = beforeCursor.current();
            if (beforeCursor.next == null) {
                Link before = beforeCursor.prev;
                before.next = null;
                beforeCursor = before;
            } else {
                Link before = beforeCursor.prev;
                before.next = beforeCursor.next;
                before.next.prev = before;
                beforeCursor = before;
            }
            size--;
            return item;
        }
        return null;
    }

    public TextNode delete(TextNode item) {
        Link ptr = sentinel;
        ptr = ptr.next;
        while (ptr != null) {
            if (ptr.current() == item) {
                beforeCursor = ptr;
                delete();
                return item;
            }
            ptr = ptr.next;
        }
        return item;
    }

    public TextNode insertAt(TextNode t, TextNode item) {
        Link ptr = sentinel;
        while (ptr != null) {
            if (ptr.current() == t) {
                beforeCursor = ptr;
                insert(item);
                return item;
            }
            ptr = ptr.next;
        }
        return item;
    }

}

