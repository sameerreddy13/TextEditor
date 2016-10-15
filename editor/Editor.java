package editor;
import editor.TextList.Link;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import java.util.Stack;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ScrollBar;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javafx.geometry.Orientation;

public class Editor extends Application {
    private static ArrayList<Link> linePointers = new ArrayList<>();
    private static Group textRoot = new Group();
    private static double leftMargin;
    private static double rightMargin;
    private static double bottomMargin;
    private static int windowWidth;
    private static int windowHeight;
    private static String fontName = "Verdana";
    private static int fontSize = 12;
    private static TextList text = new TextList();
    private static TextNode charToAdd;
    private static Rectangle cursor = new Rectangle(0, 0);
    private static Stack<StackLink> undo = new Stack<>();
    private static Stack<StackLink> redo = new Stack<>();
    private static boolean open = false;
    private static String inputFileName = "";
    private static boolean debug = false;
    private static ScrollBar scrollbar = new ScrollBar();
    private static Group root = new Group();
    private static int scrollbarWidth;

    private class RectangleBlinkEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors = {Color.BLACK, Color.TRANSPARENT};

        RectangleBlinkEventHandler() {
            // Set the color to be the first color in the list.
            changeColor();
        }

        private void changeColor() {
            cursor.setFill(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }

        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }

    /** Makes the text bounding box change color periodically. */
    public void cursorBlink() {
        // Create a Timeline that will call the "handle" function of RectangleBlinkEventHandler
        // every 1 second.
        final Timeline timeline = new Timeline();
        // The rectangle should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        RectangleBlinkEventHandler cursorChange = new RectangleBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private class MouseClickEventHandler implements EventHandler<MouseEvent> {
        MouseClickEventHandler(Group root) {
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            double mousePressedX = mouseEvent.getX();
            double mousePressedY = mouseEvent.getY();
           
            if (text.size() == 0) {
                updateCursor();
            } else if (mousePressedY >= text.lowestY + text.charHeight) {
                mousePressedY = text.lowestY + text.charHeight();
                updateCursor(mousePressedX, mousePressedY);
            } else if (mousePressedY < linePointers.get(0).current().getY()) {
                mousePressedY = 0;
                mousePressedX = 0;
                updateCursor(mousePressedX, mousePressedY);
            } else {
                updateCursor(mousePressedX, mousePressedY);
            }
            updateText();
            
        }
    }

    private class KeyEventHandler implements EventHandler<KeyEvent> {
  
        KeyEventHandler(final Group root, int windowWidth, int windowHeight) {
            // Creates cursor at top left of the screen.
            updateCursor();
        }

        private void upArrow(double newX, double newY, double currX, double currY) {
            newX = currX;
            newY = currY - text.charHeight();
            if (newY < 0) {
                newY = 0;
            }
            updateCursor(newX, newY);
        }

        private void downArrow(double newX, double newY, double currX, double currY) {
            newX = currX;
            newY = currY + text.charHeight();
            if (newY > text.lowestY) {
                newY = currY;
            } else {
                updateCursor(newX, newY);
            }
        }

        private void leftArrow(double newX, double newY, double currX, double currY) {
            if (text.beforeCursor == text.front()) {
                updateCursor(currX, currY);
            } else {
                Link p = text.beforeCursor;
                TextNode prev = p.current();
                newX = leftSide(prev);
                newY = prev.getY();
                updateCursor(newX, newY);
            }   
        }

        private void rightArrow(double newX, double newY, double currX, double currY) {
            if (text.beforeCursor.next() == null) {
                // updateCursor(currX, currY);
            } else {
                Link nextLink = text.beforeCursor.next();
                TextNode next = nextLink.current();
                newX = rightSide(next);
                newY = next.getY();
                if (next.isEnter()) {
                    newX = leftMargin;
                    newY = newY + text.charHeight();
                    if (nextLink.next() == null) {
                        newX = rightMargin;
                    } 
                }
                if (next.isWhiteSpace() && nextLink.next() != null) {
                    if (nextLink.next().current().getY() > next.getY()) {
                        newX = leftMargin;
                        newY = newY + text.charHeight();
                    }
                }
                updateCursor(newX, newY);
            }
        }

        //If the linked list is empty, add typed character to the top left.
        // Otherwise update the character's postitions. If it will go past the margin  
        // we move it to the next line. If not we set it next to character 
        // beforqe the cursor.
        private void insertChar(TextNode charAdd) {
            root.getChildren().add(text.insert(charToAdd));
            updateText();
            updateCursor();
        }

        private void insertCharUndo(TextNode t, TextNode item) {
            root.getChildren().add(text.insertAt(t, item));
            updateText();
            updateCursor();
        }

        private TextNode deleteChar() {
            TextNode itemToRemove = text.delete();
            if (itemToRemove != null) {
                root.getChildren().remove(itemToRemove);
            }
            updateText();
            updateCursor();
            return itemToRemove;
        }

        private void deleteCharUndo(TextNode item) {
            TextNode itemToRemove = text.delete(item);
            if (itemToRemove != null) {
                root.getChildren().remove(itemToRemove);
            }
            updateText();
            updateCursor();
        }

        private void fontSizeUpdate() {
            TextNode charExample = new TextNode(0, 0, "", fontName, fontSize);
            text.setCharHeight(charExample.height());
            cursor.setHeight(text.charHeight());
            updateText();
            updateCursor();
        }

        private void cursorPrint() {
            int cursorX = (int) cursor.getX();
            int cursorY = (int) cursor.getY();
            System.out.println(cursor.getX() + ", " + cursor.getY());
        }

        private void reduceFontSize() {
            fontSize = fontSize - 4;
            if (fontSize <= 0) {
                fontSize = fontSize + 4;
            } else {
                fontSizeUpdate();
            }
        }

        private void increaseFontSize() {
            fontSize = fontSize + 4;
            fontSizeUpdate();
        }

        @Override
        public void handle(KeyEvent keyEvent) {
            KeyCode code;
            // Shortcuts
            if (keyEvent.isShortcutDown()) {
                code = keyEvent.getCode();
                if (code == KeyCode.P) {
                   cursorPrint();
                }
                if (code == KeyCode.MINUS) {
                    reduceFontSize();
                } 
                if (code == KeyCode.PLUS || code == KeyCode.EQUALS) {
                    increaseFontSize();
                }
                if (code == KeyCode.Z) {
                    if (undo.size() > 0) {
                        StackLink newAction = undo.pop();
                        redo.add(newAction);
                        if (newAction.action().equals("delete")) {
                            deleteCharUndo(newAction.lastItem());
                        } else {
                            insertCharUndo(newAction.before(), newAction.lastItem());
                        }
                    }
                }
                if (code == KeyCode.Y) {
                    if (redo.size() > 0) {
                        StackLink newAction = redo.pop();
                        undo.add(newAction);
                        if (newAction.action().equals("delete")) {
                            insertCharUndo(newAction.before(), newAction.lastItem());
                        } else {
                            deleteCharUndo(newAction.lastItem());
                        }
                    }
                }

                if (code == KeyCode.S) {
                    try {
                        File inputFile = new File(inputFileName);
                        if (!inputFile.exists()) {
                            inputFile.createNewFile();
                            System.out.print("New file created: " + inputFileName);
                        }
                        FileWriter writer = new FileWriter(inputFile);
                        writer.flush();
                        Link front = text.front();
                        front = front.next();
                        while (front != null) {
                            String curr = front.current().getText();
                            if (curr.equals("\n")
                                || curr.equals("\r\n")) {
                                writer.write("\n");
                            } else {
                                writer.write(curr);
                            }
                            front = front.next();
                        }
                        writer.close();
                    } catch (FileNotFoundException e) {
                        System.out.println("Unable to open file " + inputFileName);
                    } catch (IOException ioException) {
                        System.out.println("Unable to write file " + inputFileName);
                    }
                }
                keyEvent.consume();
            // Typing letters
            } else if (keyEvent.getEventType() == KeyEvent.KEY_TYPED) {
                String charTyped = keyEvent.getCharacter();
                charToAdd = new TextNode(leftMargin, 0, charTyped);
                keyEvent.consume();
                if (charTyped.length() > 0 && charTyped.charAt(0) != 8) {
                    if (charTyped.equals("\r")) {
                        charToAdd.setText("\n");
                        root.getChildren().add(text.insert(charToAdd));
                        updateText();
                        updateCursorEnter();
                    } else {
                        insertChar(charToAdd);
                    }
                    redo.clear();
                    StackLink newUndo = new StackLink(text.beforeCursor.prev().current(), charToAdd, "delete");
                    undo.push(newUndo);
                    if (undo.size() > 100) {
                        undo.remove(0);
                    }
                }
            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                code = keyEvent.getCode();
                keyEvent.consume();
                // Deleting letters
                if (code == KeyCode.BACK_SPACE) {
                    redo.clear();
                    TextNode item = deleteChar();
                    if (item != null) {
                        StackLink newUndo = new StackLink(text.beforeCursor.current(), item, "insert");
                        undo.push(newUndo);
                        if (undo.size() > 100) {
                            undo.remove(0);
                        }
                    }
                }

                double currX = cursor.getX();
                double currY = cursor.getY();
                double newX = 0;
                double newY = 0;
                // UP, DOWN, RIGHT, and LEFT, arrow keys
                if (code == KeyCode.UP || code == KeyCode.DOWN 
                    || code == KeyCode.RIGHT || code == KeyCode.LEFT) {
                    updateText();
                    if (text.size() == 0) {
                        updateCursor();
                    } else if (code == KeyCode.DOWN) {
                        downArrow(newX, newY, currX, currY);
                    } else if (code == KeyCode.UP) {
                        upArrow(newX, newY, currX, currY);
                    } else if (code == KeyCode.RIGHT) {
                        rightArrow(newX, newY, currX, currY);
                    } else if (code == KeyCode.LEFT) {
                        leftArrow(newX, newY, currX, currY);
                    }
                }
            }
        }
    }

    // Handling pressing enter.
    private void updateCursorEnter() {
        double newX;
        double newY;
        newX = leftMargin;
        newY = cursor.getY() + text.charHeight();
        cursor.setX(newX);
        cursor.setY(newY);
        text.lowestY = newY;
    }

    //Used to update cursor for insertion and deletion operations.
    private void updateCursor() {
        if (text.beforeCursor == text.front()) {
            cursor.setX(leftMargin);
            cursor.setHeight(text.charHeight());
            cursor.setWidth(1);
        } else {
            double cursorX = rightSide(text.beforeCursor.current());
            double cursorY = text.beforeCursor.current().getY();
            if (text.beforeCursor.current().isEnter()) {
                cursorX = leftMargin;
                cursorY = cursorY + text.charHeight();
            }
            cursor.setX(cursorX);
            cursor.setY(cursorY);
        }
    }

    // Used to update cursor for cursor operations, 
    // updating the text's beforeCursor pointer as well.
    private void updateCursor(double x, double y) {
        double newX = 5;
        double newY = 0;

        // Find the front of the line that is closest to entered y,
        /// and set Y to this line's Y coordinate.
        int lineIndex = ((int) y) / ((int) text.charHeight());
        if (lineIndex == linePointers.size()) {
            lineIndex = lineIndex - 1;
        }
        Link currLink = linePointers.get(lineIndex);
        TextNode currText;
        newY = currLink.current().getY();
        Link nextLine;
        if (lineIndex + 1 == linePointers.size()) {
            nextLine = null;
        } else {
            nextLine = linePointers.get(lineIndex + 1);
        }

        if (x <= leftMargin) {
            newX = leftMargin;
            text.beforeCursor = currLink.prev();
        } else {
            while (currLink != nextLine) {
                currText = currLink.current();
                if (currText.isEnter()) {
                    if (currLink == linePointers.get(lineIndex)) {
                        text.beforeCursor = currLink.prev();
                    }
                    currLink = currLink.next();
                } else {
                    double left = leftSide(currText);
                    double right = rightSide(currText);
                    double leftDist = x - left;
                    double rightDist = x - right;
                    if (rightDist <= 0 && leftDist > 0) {
                        double rightMagnitude = Math.abs(rightDist);
                        double leftMagnitude = leftDist;
                        double dist = Math.min(leftMagnitude, rightMagnitude);
                        if (dist == leftMagnitude) {
                            newX = left;
                            text.beforeCursor = currLink.prev();
                        } else {
                            newX = right;
                            text.beforeCursor = currLink;
                        }  
                    } else if (x > right) {
                        newX = right;
                        text.beforeCursor = currLink;
                        // if (currLink.current().isWhiteSpace()) {
                        //     newX = leftSide(currLink.current());
                        // }
                    }
                    currLink = currLink.next();
                }
            }
        }
        cursor.setX(newX);
        cursor.setY(newY);
    }

    private void updateText() {
        linePointers.clear();
        Link front = text.front();
        if (front.next() != null) {
            linePointers.add(front.next());
        }
        front = front.next();
        while (front != null) {
            TextNode currentText = front.current();
            if (front.prev() == text.front()) {
                currentText.setX(leftMargin);
                currentText.setY(0);
                currentText.setFont(Font.font(fontName, fontSize));
            } else {
                TextNode prevText = front.prev().current();
                double newX;
                double newY;
                currentText.setFont(Font.font(fontName, fontSize));   
                double widthToAdd = currentText.width();
                double prevRightX = rightSide(prevText);
                if ((prevRightX + widthToAdd > rightMargin) || prevText.isEnter()) {
                    newX = leftMargin;
                    newY = prevText.getY() + text.charHeight();
                    if (!prevText.isEnter()) {
                        int i = ((int) prevText.getY()) / ((int) text.charHeight());
                        Link newFront = wordWrap(front, i);
            
                        front = newFront;
                        currentText = front.current();
                    }
                    currentText.setX(newX);
                    currentText.setY(newY);
                    linePointers.add(front);
                } else {
                    newX = rightSide(prevText);
                    newY = prevText.getY();
                }
                currentText.setX(newX);
                currentText.setY(newY);
                double currLowY = (linePointers.size() - 1) * text.charHeight();  
                if (currentText.getY() > currLowY) {
                    text.lowestY = currentText.getY();
                } else {
                    text.lowestY = currLowY;
                }
            }
            front = front.next();
        }
        if (debug) {
            beforeCursorPrint();
            linePointersPrint();
        }
        // updateScrollBar();
    }

    // private void updateScrollBar() {
    //     double scrollbarY = text.lowestY + text.charHeight() - windowHeight;
    //     if (scrollbarY <= 0) {
    //         root.setLayoutY(0);
    //         scrollbar.setMax(0);
    //     } else {
    //         scrollbar.setMax(scrollbarY);
    //         if (!lastAction.equals("scroll")) {
    //             if (cursor.getY() < scrollbar.getValue()) {
    //                 scrollbar.setValue(cursor.getY());
    //             } else if (cursor.getY() + text.charHeight() >= scrollbar.getValue()) {
    //                 double i = cursor.getY() - windowHeight + text.charHeight();
    //                 scrollbar.setValue(i);                   
    //             }
    //         }
    //     }
    // }
    private Link wordWrap(Link x, int lineI) {
        Link holder = x;
        Link end = linePointers.get(lineI);
        boolean reachedSpace = false;
        while (x != end && !reachedSpace) {
            x = x.prev();
            if (x.current().isWhiteSpace()) {
                x = x.next();
                reachedSpace = true;
                return x;
            }
        }
        return holder;
    }

    private double rightSide(TextNode item) {
        return item.getX() + item.width();
    }

    private double leftSide(TextNode item) {
        return item.getX();
    }

    private void linePointersPrint() {
        String first;
        int i = 0;
        for (Link l: linePointers) {
            TextNode t = l.current();
            if (t.isEnter()) {
                first = "/n";
            } else {
                first = t.getText();
            }   
            System.out.println("Line " + i + ": "  + first);
            i++;
        }
    }

    private void beforeCursorPrint() {
        if (text.beforeCursor.current() == null) {
            System.out.println("No text before cursor");
        } else {
            String t = text.beforeCursor.current().getText();
            if (t.equals("\n")) {
                System.out.println("Text before cursor: new line character");
            } else {
                System.out.println("Text before cursor: " + t);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Create a Node that will be the parent of all things displayed on the screen.
        // The Scene represents the window: its height and width will be the height and width
        // of the window displayed.
        // root.getChildren().add(textRoot);
        windowWidth = 500;
        windowHeight = 500;
        leftMargin = 5;
        //bottomMargin = windowHeight;
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);
        // To get information about what keys the user is pressing, create an EventHandler.
        // EventHandler subclasses must override the "handle" function, which will be called
        // by javafx.
        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(root, windowWidth, windowHeight);

        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(new MouseClickEventHandler(root));
        root.getChildren().add(cursor);
        cursorBlink();

        scrollbarWidth = (int) scrollbar.getLayoutBounds().getWidth();
        windowWidth = windowWidth - scrollbarWidth;
        rightMargin = windowWidth - 5;
        scrollbar.setOrientation(Orientation.VERTICAL);
        scrollbar.setPrefHeight(windowHeight);
        scrollbar.setMin(0);
        scrollbar.setMax(0);
        scrollbar.setValue(5);
        scrollbar.setLayoutX(windowWidth);
        root.getChildren().add(scrollbar);

        if (open) {
            updateText();
            text.beforeCursor = text.front();
            updateCursor();
        }
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                windowWidth = (int) newScreenWidth.doubleValue();
                windowWidth = windowWidth - scrollbarWidth;
                scrollbar.setLayoutX(windowWidth);
                rightMargin = windowWidth - 5;
                updateText();
                updateCursor();
                //render();
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                windowHeight = (int) newScreenHeight.doubleValue();
                updateText();
                updateCursor();
                //render();
            }
        });
        // scrollbar.valueProperty().addListener(new ChangeListener<Number>() {
        //     public void changed(
        //             ObservableValue<? extends Number> observableValue,
        //             Number oldValue,
        //             Number newValue) {
        //         textRoot.setLayoutY(textRoot.getLayoutY() - (newValue.intValue() - oldValue.intValue()));
        //         updateText();
        //         updateScrollBar();
        //     }
        // });

        // Register the event handler to be called for all KEY_PRESSED and KEY_TYPED events.
        
        
        primaryStage.setTitle("Editor");
        primaryStage.setScene(scene);
        primaryStage.show();    
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Filename not entered.");
            System.exit(1);
        } else if (args.length == 2) {
            if (!args[1].equals("debug")) {
                System.out.println("Second argument must be 'debug'");
                System.exit(1);
            } else {
                debug = true;
            }
        }
        inputFileName = args[0];
        try {
            File inputFile = new File(inputFileName);
            if (!inputFile.exists()) {
                System.out.println("File not found.");
                launch(args);
                return;
            } else {
                open = true;
                FileReader reader = new FileReader(inputFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
                int intRead = -1;
                while ((intRead = bufferedReader.read()) != -1) {
                    char charRead = (char) intRead;
                    String stringRead = String.valueOf(charRead);
                    TextNode textRead = new TextNode(0, 0, stringRead);
                    text.insert(textRead);
                    root.getChildren().add(textRead);
                }
            }
            launch(args);
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open file " + inputFileName);
        } catch (IOException ioException) {
            System.out.println("Unable to write file " + inputFileName);
        }
        
    }
}
