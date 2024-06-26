package com.disector.maploader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.IntArray;
import com.disector.Sector;
import com.disector.Wall;
import com.disector.gameworld.GameWorld;

import java.util.Scanner;

public class TextFileMapLoader implements MapLoader {
    private Array<Sector> sectors;
    private Array<Wall> walls;
    private GameWorld world;

    public TextFileMapLoader(Array<Sector> sectors, Array<Wall> walls, GameWorld world) {
        this.sectors = sectors;
        this.walls = walls;
        this.world = world;
    }

    @Override
    public boolean load(String path) {
        String mode = "NONE"; //"SECTOR" "WALL" "OBJECT"
        String subMode = "NONE";

        FileHandle file = Gdx.files.local(path);
        Scanner in = new Scanner(file.readString());
        String next = "";

        Array<Wall> newWalls = new Array<>();
        Array<Sector> newSectors = new Array<>();

        Sector sectorBuild = null;
        Wall wallBuild = null;

        while (in.hasNext()) {
            next = in.next().trim().toUpperCase();
            if (isObjectKeyword(next)) {
                //Finalize Previous Mode
                switch (mode) {
                    case "SECTOR":
                        newSectors.add(sectorBuild);
                        sectorBuild = null;
                        break;
                    case "WALL":
                        newWalls.add(wallBuild);
                        wallBuild = null;
                        break;
                    default:
                        break;
                }
                subMode = "NONE";
                mode = next;
                //Init New Mode
                switch (mode) {
                    case "SECTOR":
                        sectorBuild = new Sector();
                        break;
                    case "WALL":
                        wallBuild = new Wall();
                        break;
                    default:
                        break;
                }
            } else { //If not ObjectKeyword...
                switch (mode) {
                    case "NONE":
                        if (!isObjectKeyword(next)) {
                            System.out.printf("Token %s is not Object Keyword\n", next);
                        }
                        break;
                    case "SECTOR":
                        if (isSectorKeyword(next)) {
                            subMode = next;
                        } else if (next.equals("::")) {
                            subMode = "NONE";
                        } else {
                            switch (subMode) {
                                case "HAS":
                                    sectorBuild.walls.add(Integer.parseInt(next));
                                    break;
                                case "HEIGHT":
                                    sectorBuild.floorZ = Float.parseFloat(next);
                                    sectorBuild.ceilZ = Float.parseFloat(in.next());
                                    subMode = "NONE";
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    case "WALL":
                        if (isWallKeyword(next)) {
                            subMode = next;
                        } else if (next.equals("::")) {
                            subMode = "NONE";
                        } else {
                            switch (subMode) {
                                case "POS":
                                    wallBuild.x1 = Float.parseFloat(next);
                                    wallBuild.y1 = Float.parseFloat(in.next());
                                    wallBuild.x2 = Float.parseFloat(in.next());
                                    wallBuild.y2 = Float.parseFloat(in.next());
                                    subMode = "NONE";
                                    break;
                                case "PORT":
                                    wallBuild.isPortal = true;
                                    wallBuild.linkA = Integer.parseInt(next);
                                    in.next(); //Absorb '->' or whatever is there
                                    wallBuild.linkB = Integer.parseInt(in.next());
                                    subMode = "NONE";
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        //Finalize Open Wall or Sector Builds
        if (sectorBuild != null)
            newSectors.add(sectorBuild);
        if (wallBuild != null)
            newWalls.add(wallBuild);

        //Validate..
        int errors = 0;
        for (Sector s : newSectors) {
            for (int wInd : s.walls.toArray()) {
                if (wInd >= newWalls.size) {
                    errors++;
                    System.out.printf("Wall index %d does not exist\n", wInd);
                }
            }
        }

        if (errors != 0) {
            System.out.println("MAP DATA BAD");
            return false;
        }

        //Copy To Applications' Sector and Wall Lists
        sectors.clear();
        for (Sector s : newSectors) {
            sectors.add(new Sector(s));
        }

        walls.clear();
        for (Wall w : newWalls) {
            walls.add(new Wall(w));
        }

        return true;
    }

    @Override
    public boolean save(String path) {
        FileHandle file = Gdx.files.local(path);

        file.writeString("", false); //Clear File

        StringBuilder allSectorsString = new StringBuilder();
        int sInd = 0;
        for (Sector s : sectors) {
            allSectorsString.append(sectorToText(s, "" + sInd));
            sInd++;
        }
        file.writeString(allSectorsString.toString(), true);

        int wInd = 0;
        StringBuilder allWallsString = new StringBuilder();
        for (Wall w : walls) {
            allWallsString.append(wallToText(w, "" + wInd));
            wInd++;
        }
        file.writeString(allWallsString.toString(), true);

        return true;
    }

    private String sectorToText(Sector s, String note) {
        StringBuilder str = new StringBuilder("SECTOR ");
        if (note != null && !note.isEmpty()) {
            str.append("(").append(note).append(") ");
        }

        str.append(":: ");

        //Wall Indices
        str.append("HAS ");
        for (int wInd : s.walls.toArray()) {
            str.append(wInd).append(" ");
        }
        str.append(":: ");

        //Height
        str.append("HEIGHT ").append(s.floorZ).append(" ").append(s.ceilZ).append(" ");

        str.append("::\n");

        return str.toString();
    }

    private String wallToText(Wall w, String note) {
        StringBuilder str = new StringBuilder("WALL ");

        if (note != null && !note.isEmpty()) {
            str.append("(").append(note).append(") ");
        }

        str.append(":: ");

        str.append("POS ");
        str.append(form(w.x1)).append(" ");
        str.append(form(w.y1)).append(" ");
        str.append(form(w.x2)).append(" ");
        str.append(form(w.y2)).append(" ");
        str.append(":: ");

        if (w.isPortal) {
            str.append(String.format("PORT %d -> %d :: ", w.linkA, w.linkB));
        }

        str.append("\n");

        return str.toString();
    }

    private String form(double num) {
        //Prints a double as integer if no partial value
        return num % 1 == 0 ? ("" + (int) num) : ("" + num);
    }

    private boolean isObjectKeyword(String str) {
        return enumContains(str, ObjectKeyword.class);
    }

    private boolean isSectorKeyword(String str) {
        return enumContains(str, SectorKeyword.class);
    }

    private boolean isWallKeyword(String str) {
        return enumContains(str, WallKeyword.class);
    }

    private <E extends Enum<E>> boolean enumContains(String str, Class<E> enumClass) {
        E[] keywords = enumClass.getEnumConstants();

        // Alternative method
        // return Arrays.toString(keywords).toUpperCase().contains(str.toUpperCase());

        for (E word : keywords) {
            String wordName = word.toString();
            if (wordName.equals(str)) return true;
        }

        return false;
    }

}
