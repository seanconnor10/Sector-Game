package com.disector.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;

import java.util.Scanner;


public class Palette {
    Color[] colors;

    public Palette(FileHandle file) {
        load(file);
    }

    private void load(FileHandle file) {
        Array<Color> cols = new Array<>();

        boolean beginsWithAlpha = file.extension().equalsIgnoreCase("txt");

        Scanner in = new Scanner(file.read());
        while(in.hasNext()) {
            String line = in.nextLine();

            if (line.startsWith(";") || line.startsWith("//"))
                continue;

            line = line.replace("#", "");

            if (beginsWithAlpha)
                line = line.substring(2);

            if (line.length() == 6)
                line = line + "FF";

            cols.add( Color.valueOf(line) );
        }

        colors = cols.toArray(Color.class);
    }


    public void palletize(Pixmap pix) {
        int w = pix.getWidth();
        int h = pix.getHeight();
        for (int x=0; x<w; x++) {
            for (int y=0; y<h; y++) {
                Color col = new Color(pix.getPixel(x,y));
                setToNearest(col);
                pix.drawPixel(x,y, Color.rgba8888(col));
            }
        }
    }

    public Color getNearestCopy(Color in) {
        Color closest = null;
        float smallestDistance = 3;
        float thisDist = 3;

        for (Color c : colors) {
            thisDist = Math.abs(in.r - c.r) + Math.abs(in.g - c.g) + Math.abs(in.b - c.b);
            if (thisDist < smallestDistance) {
                closest = c;
                smallestDistance = thisDist;
            }
        }

        return closest;
    }

    public void setToNearest(Color in) {
        Color closest = null;
        float smallestDistance = 3;
        float thisDist = 3;

        for (Color c : colors) {
            thisDist = Math.abs(in.r - c.r) + Math.abs(in.g - c.g) + Math.abs(in.b - c.b);
            if (thisDist < smallestDistance) {
                closest = c;
                smallestDistance = thisDist;
            }
        }

        in.set(closest);
    }


}
