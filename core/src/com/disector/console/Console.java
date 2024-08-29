package com.disector.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;

import com.disector.inputrecorder.InputChainInterface;
import com.disector.inputrecorder.InputChainNode;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Console {
    private final InputChainNode input;

    private final CommandExecutor executor;

    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final Matrix4 renderTransform;

    private String currentIn;
    private final Array<String> textLines;
    private final List<String> pastEntries = new ArrayList<>();
    private int pastEntriesIndex = -1;

    private boolean visible;
    private boolean active;
    private float y;
    private final float lineHeight;
    private int lineScroll;
    private final float screenPercentage = 0.65f;
    private final int xBorder = 42;

    private BitmapFont font = new BitmapFont( Gdx.files.local("assets/font/fira.fnt") );
    private final Color backgroundColor = new Color(0.2f, 0.4f, 0.6f, 0.7f);

    public Console(CommandExecutor executor, InputChainInterface inputParent) {
        active = false;
        visible = false;
        y=Gdx.graphics.getHeight();
        lineHeight = font.getLineHeight()+8;
        lineScroll = 0;

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
        renderTransform = new Matrix4();
        font.setColor(Color.WHITE);

        textLines = new Array<>(25);
        currentIn = "";
        textLines.add("    -= Welcome =-");

        this.executor = executor;

        input = new InputChainNode(inputParent, "Console") {
            @Override
            public boolean keyTyped (char character) {
                if (active &&
                        character != '\b' &&
                        character != '\n' &&
                        character != '\t' &&
                        character != '\r' &&
                        character != '\f' &&
                        character != '\\' &&
                        character != '`' &&
                        character != '~' )
                    currentIn += character;

                if (character == '\b') { //Backspace
                    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                        currentIn = removeLastWord(currentIn);
                    else
                        currentIn = currentIn.substring(0, Math.max( 0, (currentIn+"MARK!MARK%MARK?").indexOf("MARK!MARK%MARK?")-1) );

                } else if (character == '\t') {
                    currentIn = autoComplete(currentIn);
                }

                return true;
            }

            @Override
            public boolean keyDown (int keycode) {
                if (active) {
                    final int LINES_PER_PAGE = (int) (Gdx.graphics.getHeight()*screenPercentage/lineHeight);
                    final int TOP_OF_PAGE = Math.max(0, textLines.size + 2 - LINES_PER_PAGE);

                    switch(keycode) {
                    case Input.Keys.FORWARD_DEL: //'Delete' Key
                        currentIn = ""; //Bug fix to actually do this in update
                        pastEntriesIndex = -1;
                        break;
                    case Input.Keys.ENTER:
                        lineScroll = 0;
                        executeEntry();
                        break;
                    case Input.Keys.PAGE_DOWN:
                        lineScroll = Math.max(0, lineScroll - LINES_PER_PAGE );
                        break;
                    case Input.Keys.PAGE_UP:
                        lineScroll = Math.min(TOP_OF_PAGE, lineScroll + LINES_PER_PAGE );
                        break;
                    case Input.Keys.END:
                        lineScroll = 0;
                        break;
                    case Input.Keys.HOME:
                        lineScroll = TOP_OF_PAGE;
                        break;
                    case Input.Keys.UP:
                        if (pastEntries.isEmpty()) break;
                        pastEntriesIndex++;
                        if (pastEntriesIndex >= pastEntries.size())
                            pastEntriesIndex = pastEntries.size()-1;
                        currentIn = pastEntries.get(pastEntriesIndex);
                        break;
                    case Input.Keys.DOWN:
                        if (pastEntries.isEmpty()) break;
                        pastEntriesIndex--;
                        if (pastEntriesIndex < -1)
                            pastEntriesIndex = pastEntries.size()-1;
                        else if (pastEntriesIndex == -1)
                            pastEntriesIndex = 0;
                        currentIn = pastEntries.get(pastEntriesIndex);
                        break;
                    default:
                        break;
                    }
                }

                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                lineScroll -= Math.round(amountY);
                if (lineScroll < 0) lineScroll = 0;

                float maxUp = (float)(textLines.size+3) - screenPercentage*Gdx.graphics.getHeight()/lineHeight;
                maxUp = Math.max(0.0f, maxUp);
                if (lineScroll > (int) maxUp) lineScroll = (int) maxUp;

                return true;
            }
        };
    }

    public void insertText(String str) {
        textLines.insert(0, str);
    }

    public InputChainNode getInputAdapter() {
        return input;
    }

    public void toggle() {
        active = !active;
    }

    public void updateAndDraw(float delta) {
        //Fix bug where commands don't register currently after delete was pressed ?
        if (active && Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) ) {
            currentIn = "";
        }

        //Slide console up or down
        float yGoal = Gdx.graphics.getHeight() * (1.0f - screenPercentage);
        if (active) {
            visible = true;
            if (y != yGoal)
                y =  Math.max(y-1500.0f*delta, yGoal);
        } else {
            if (y != Gdx.graphics.getHeight())
                y = Math.min(y+1500.0f*delta, Gdx.graphics.getHeight());
            if ( Math.round(y) == Gdx.graphics.getHeight())
                visible = false;
        }

        //Draw when not hidden
        if (active || y != Gdx.graphics.getHeight()) {
            draw();
        }
    }

    public void updateTransform() {
        renderTransform.setToOrtho2D(0.0f,0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
    }

    private void executeEntry() {
        insertText(">> " + currentIn);

        String response = executor.execute(currentIn);

        if (response == null || !response.equals(CommandExecutor.NO_MATCH_TEXT)) {
            pastEntriesIndex = -1;
            pastEntries.add(0, currentIn);
        }

        currentIn = "";

        if (response == null)
            return;

        String[] responses = response.split("\n");

        for (String line : responses)
            insertText(line);
    }

    private void draw() {
        if (!visible) return;

        updateTransform();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.setProjectionMatrix(renderTransform);
        //Green Back
        shape.setColor(backgroundColor);
        shape.rect(0.0f,  y, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()-y );
        //Entry Field
        shape.rect(0.0f,  y, Gdx.graphics.getWidth(), lineHeight );
        //Thin Line
        shape.setColor(1.0f, 1.0f, 0.9f, 0.5f);
        shape.rect(0.0f,  y-1, Gdx.graphics.getWidth(), 1 );
        shape.end();

        batch.begin();
        batch.setProjectionMatrix(renderTransform);

        //Draw Fps
        font.draw(batch, "FPS: " + (int)(1.0f/Gdx.graphics.getDeltaTime()), Gdx.graphics.getWidth()-175.0f, y+lineHeight-10.0f);

        //Draw Console Input
        font.draw(batch, currentIn, xBorder, y - 5 + lineHeight);

        //Draw Console Log
        for (int i = lineScroll; i < textLines.size; i++) {
            font.draw(batch, textLines.get(i), xBorder, y + (i+2-lineScroll)*lineHeight);
        }
        batch.end();
    }

    private String autoComplete(String str) {
        /*
         * Absolutely the nastiest garbage ever written
         */

        if (str == null || str.replaceAll("\n|\t|\b|\r", "").isEmpty() )
            return "";

        String[] names = executor.getCommandNames();

        Object[] filteredNames = Arrays.stream(names).filter(
                (String name) -> (!name.equalsIgnoreCase(str) && name.toLowerCase().startsWith( str.toLowerCase() ))
        ).toArray();

        int size = filteredNames.length;
        if (size == 0)
            return str;
        else if (size == 1)
            return (String) filteredNames[0];

        String current = str;
        while (size > 1) {
            int strLength = current.length()+1;
            if (strLength >= ((String)filteredNames[0]).length())
                return (String) filteredNames[0];
            current = ((String) filteredNames[0]).substring(0, current.length()+1);
            final String currentFinalized = current;
            filteredNames = Arrays.stream(names).filter(
                (String name) -> (!name.equalsIgnoreCase(currentFinalized) && name.toLowerCase().startsWith( currentFinalized.toLowerCase() ))
            ).toArray();
            size = filteredNames.length;
        }
        return current.substring(0, current.length()-1);
    }

    private String removeLastWord(String str) {
        //If last char is ' ' remove it so we don't do nothing
        if (str.lastIndexOf(' ') == str.length()-1)
            str = str.substring(0, str.length()-1);

        return str.substring(0, Math.max(1, str.lastIndexOf(' ') + 1) );
    }
}
