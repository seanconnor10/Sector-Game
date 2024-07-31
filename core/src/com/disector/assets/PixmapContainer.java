package com.disector.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;

public class PixmapContainer {
    private static final FileHandle imgDir = Gdx.files.local("assets/img");
    public static final int MipMapNumber = 5;

    public Pixmap[][] pixmaps;
    private TreeMap<String, Pixmap[]> pixmapsByName;

    public void loadFolder(String path) {
        Array<FileHandle> imgFiles = new Array<>();

        System.out.println("Pixmap Container Loading  All Images In " + imgDir);

        for (FileHandle file : imgDir.child(path) .list()) {
            if (handleIsImage(file))
                imgFiles.add(file);
            System.out.println(handleIsImage(file) ? "    " + file : "    REJECTED " + file);
        }

        //Make 'pixmaps' 2D-Array
        pixmaps = new Pixmap[imgFiles.size][MipMapNumber];
        for (int i=0; i<imgFiles.size; i++) {
            Texture temp = new Texture(imgFiles.get(i), Pixmap.Format.RGBA8888, false);
            if (!temp.getTextureData().isPrepared()) temp.getTextureData().prepare();
            pixmaps[i][0] = temp.getTextureData().consumePixmap();
            for (int k=1; k<MipMapNumber; k++) {
                pixmaps[i][k] = halvePixmap(pixmaps[i][k-1]);
            }
            temp.dispose();
        }

        //Make 'pixmaps2' String/Img Map
        pixmapsByName = new TreeMap<>();
        for (int i=0; i<imgFiles.size; i++) {
            Texture temp = new Texture(imgFiles.get(i), Pixmap.Format.RGBA8888, false);
            if (!temp.getTextureData().isPrepared()) temp.getTextureData().prepare();
            Pixmap[] thisImgMips = new Pixmap[MipMapNumber];
            thisImgMips[0] = temp.getTextureData().consumePixmap();
            for (int k=1; k<MipMapNumber; k++) {
                thisImgMips[k] = halvePixmap(thisImgMips[k-1]);
            }
            pixmapsByName.put(imgFiles.get(i).nameWithoutExtension().toUpperCase(), thisImgMips);
            temp.dispose();
        }
    }

    public void loadArray(Array<Material> blankMaterials) {
        //Takes an Array of Materials without the Texture loaded,
        //Loads the texture and adds reference to this PixmapContainer
        System.out.println("Loading Textures from Materials Array");
        pixmaps = new Pixmap[blankMaterials.size][MipMapNumber];
        pixmapsByName = new TreeMap<>();

        HashSet<String> loadedImages = new HashSet<>();

        int i = 0;
        for (Material mat : blankMaterials) {
            FileHandle file = getFileHandleFromName(mat.nameReference);

            if (loadedImages.contains(mat.nameReference)) {
                mat.tex = get( mat.nameReference );
                continue; //Avoid loading same image twice, even if two different
            }

            Texture temp = new Texture(file, Pixmap.Format.RGBA8888, false);
            if (!temp.getTextureData().isPrepared()) temp.getTextureData().prepare();

            pixmaps[i] = makeMipMapSeries(temp);

            mat.tex = pixmaps[i];
            pixmapsByName.put(file.nameWithoutExtension().toUpperCase(), pixmaps[i]);

            temp.dispose();
            loadedImages.add(file.toString());
            System.out.println("    " + i + ") " + file);
            i++;
        }

    }

    public Pixmap[] get(String name) {
        return pixmapsByName.getOrDefault(name, null);
    }

    public static Pixmap[] makeMipMapSeries(Texture tex) {
        Pixmap[] pixmaps = new Pixmap[MipMapNumber];
        pixmaps[0] = tex.getTextureData().consumePixmap();
        for (int i=1; i<MipMapNumber; i++) {
            pixmaps[i] = halvePixmap(pixmaps[i-1]);
        }
        return pixmaps;
    }

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

}
