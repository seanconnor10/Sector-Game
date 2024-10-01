package com.disector.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.disector.Material;

import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;

public class PixmapContainer {
    private static final FileHandle imgDir = Gdx.files.local("assets/img");
    public static final int MIPMAP_COUNT = 5;

    private Pixmap[][] pixmaps;
    private TreeMap<String, Pixmap[]> pixmapsByName;

    public void loadArray(Array<Material> blankMaterials, Palette pall) {
        //Takes an Array of Materials without the Texture loaded,
        //Loads the texture and adds reference to this PixmapContainer
        long timeStamp = TimeUtils.millis();
        System.out.println("Loading Textures from Materials Array");

        clear();
        pixmaps = new Pixmap[blankMaterials.size][MIPMAP_COUNT];
        pixmapsByName = new TreeMap<>();

        HashSet<String> loadedImages = new HashSet<>();

        int i = 0;
        for (Material mat : blankMaterials) {
            FileHandle file = getFileHandleFromName(mat.nameReference);

            if (loadedImages.contains(mat.nameReference)) {
                mat.tex = get( mat.nameReference );
                continue; //Avoid loading same image twice, even if two different
            }

            Pixmap pixmap = new Pixmap(file);

            if (pall != null) pall.palletize(pixmap);

            //Temporary forced formatting
            /*Pixmap formattedPixmap = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA4444);
            formattedPixmap.drawPixmap(pixmap, 0, 0);
            pixmap = formattedPixmap;*/

            pixmaps[i] = makeMipMapSeries(pixmap);

            mat.tex = pixmaps[i];
            pixmapsByName.put(file.nameWithoutExtension().toUpperCase(), pixmaps[i]);

            loadedImages.add(file.toString());
            System.out.println("    " + i + ") " + file + pixmaps[i][0].getFormat().toString());
            i++;
        }

        System.out.println("    Done in " + TimeUtils.timeSinceMillis(timeStamp) + "ms" );

    }

    public void loadArray(Array<Material> blankMaterials) {
        loadArray(blankMaterials, null);
    }


    public Pixmap[] get(String name) {
        return pixmapsByName.getOrDefault(name, null);
    }

    public static Pixmap[] makeMipMapSeries(Pixmap pix) {
        Pixmap[] pixmaps = new Pixmap[MIPMAP_COUNT];
        pixmaps[0] = pix;
        for (int i = 1; i< MIPMAP_COUNT; i++) {
            pixmaps[i] = halvePixmap(pixmaps[i-1]);
        }
        return pixmaps;
    }

    // --------------------------------------------------------------------------------
    
    private static  Pixmap halvePixmap(Pixmap pix) {
        Pixmap newPix = new Pixmap(pix.getWidth()/2, pix.getHeight(), pix.getFormat());
        newPix.setFilter(Pixmap.Filter.BiLinear);
        newPix.drawPixmap(
            pix, 1, 1, pix.getWidth()-1, pix.getHeight(), //Source Pixmap The -1's seem to help it align
            0, 0, newPix.getWidth(), newPix.getHeight() //Destination Pixmap
        );
        return newPix;
    }

    private boolean handleIsImage(FileHandle handle) {
        if (handle.isDirectory())
            return false;

        String str = handle.toString().toLowerCase(Locale.ROOT);
        return str.endsWith(".png") || str.endsWith(".jpg") || str.endsWith(".bmp");
    }

    private FileHandle getFileHandleFromName(String name) {
        FileHandle handle = null;

        for (FileHandle file : imgDir.list()) {
            if (handleIsImage(file) && file.nameWithoutExtension().equalsIgnoreCase(name)) {
                handle = file;
                break;
            }
        }

        return handle;
    }

    private void clear() {
        if (pixmaps == null) return;
        for (Pixmap[] pp : pixmaps) {
            for (Pixmap p : pp) {
                if (p == null) continue;
                p.dispose();
            }
        }
        pixmapsByName.clear();

    }


}
