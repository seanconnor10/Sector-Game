package com.disector.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;

import java.util.Scanner;


public class Pallette {
    Color[] colors;

    public Pallette(FileHandle file) {
        Scanner in = new Scanner(file.read());

        Array<Color> cols = new Array<>();
        while(in.hasNext()) {
            String line = in.nextLine();
            if (!line.startsWith(";")) {
                cols.add( Color.valueOf(line.substring(2) + "FF") );
            }
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
