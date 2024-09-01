package com.disector.editor;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.IntArray;
import com.disector.Physics;
import com.disector.Sector;
import com.disector.Wall;
import com.disector.editor.actions.EditAction;

import java.util.Arrays;

class STATE_BisectingSector extends EditorState {
    boolean started;

    Sector sector;
    Sector newSector;
    int newSectorInd;
    int secInd;
    int[] sec_wallIndices;
    Array<Wall> sec_walls;

    IntArray affectedIndices = new IntArray();
    Array<Wall> affectedWalls = new Array<>();
    Array<Wall> preexistingPortals = new Array<>();
    Array<Wall> newPortals = new Array<>();

    float start_x, start_y;
    float prev_x, prev_y;

    private float clickDistance = 8f;;

    /*
        When the first wall is a portal We're determining the sector were splitting depending on
        What side of the wall is first clicked in handleFirstClick...
        Really we should determine which side it depending on where the first new portal is
        (8/29/24)
     */

    STATE_BisectingSector(Editor editor, Panel panel) {
        super(editor, panel);
        ignoreEditorClick = true;
        visibleName = "Sector Bisection";
    }

    @Override
    void step() {
        if (panel.input.isJustPressed(Input.Keys.ESCAPE)) {
            shouldFinish = true;
        }

        if (shouldFinish) return;

        if (!started) return;


        for (Wall w : affectedWalls) {
            editor.mapRenderer.addFakeWall(w.x1, w.y1, w.x2, w.y2);
        }

        editor.mapRenderer.addFakeWall(prev_x, prev_y, xUnSnapped(), yUnSnapped());

    }

    @Override
    void click() {
        if (!started) {
            handleFirstClick();
        } else {
            handleClick();
        }
    }

    @Override
    void rightClick() {

    }

    @Override
    EditAction[] finish() {
        return new EditAction[0];
    }

    private void handleFirstClick() {
        Wall wall;
        int wInd;

        try {
            wInd = getPointsOnesAtMouseByIndex(clickDistance)[0];
            wall = editor.walls.get(wInd);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return;
        }

        //Find Sector we're splitting
        int secInd = -1;

        if (wall.isPortal) {
            if (Physics.containsPoint(wall.linkA, xUnSnapped(), yUnSnapped() ))
                secInd = wall.linkA;
            else if (Physics.containsPoint(wall.linkB, xUnSnapped(), yUnSnapped() ))
                secInd = wall.linkB;
            else
                return;
        } else {
            for (int i = 0; i < editor.sectors.size; i++) {
                if (editor.sectors.get(i).walls.contains(wInd)) {
                    secInd = i;
                    break;
                }
            }
        }

        if (secInd == -1) return;

        sector = editor.sectors.get(secInd);
        newSectorInd = editor.sectors.size;
        newSector = new Sector(sector, false);
        this.secInd = secInd;

        sec_wallIndices = Arrays.copyOf(sector.walls(), sector.walls.size);
        sec_walls = new Array<>();
        Arrays.stream(sec_wallIndices).forEach( (int item) -> {sec_walls.add(editor.walls.get(item));});

        started = true;

        start_x = wall.x1;
        start_y = wall.y1;
        prev_x = start_x;
        prev_y = start_y;
    }


    private void handleClick() {

        boolean addedPreexistingWall = false;

        for (int wInd : sec_wallIndices) {
            Wall w = editor.walls.get(wInd);

            if (affectedIndices.contains(wInd))
                continue;

            if ( //If clicking on the point two of the wall whose point one we started at
                squareCollision(prev_x, prev_y, w.x1, w.y1, clickDistance)
                &&
                squareCollision(xUnSnapped(), yUnSnapped(), w.x2, w.y2, clickDistance)
            ) {
                affectedIndices.add(wInd);
                affectedWalls.add(w);
                if (w.isPortal)
                    preexistingPortals.add(w);
                addedPreexistingWall = true;
                prev_x = w.x2;
                prev_y = w.y2;
                break;
            } else if ( //If clicking on the point one of the wall whose point two we started at
                    squareCollision(prev_x, prev_y, w.x2, w.y2, clickDistance)
                    &&
                    squareCollision(xUnSnapped(), yUnSnapped(), w.x1, w.y1, clickDistance)
            ) {
                affectedIndices.add(wInd);
                affectedWalls.add(w);
                if (w.isPortal)
                    preexistingPortals.add(w);
                addedPreexistingWall = true;
                prev_x = w.x1;
                prev_y = w.y1;
                break;
            }
        }

        if (!addedPreexistingWall) {
            float x = x(), y = y();

            for (Wall w : sec_walls) {
                if (squareCollision(w.x1, w.y1, xUnSnapped(), yUnSnapped(), clickDistance)) {
                    x = w.x1;
                    y = w.y1;
                    break;
                } else if (squareCollision(w.x1, w.y2, xUnSnapped(), yUnSnapped(), clickDistance)) {
                    x = w.x2;
                    y = w.y2;
                    break;
                }
            }

            Wall newPortal = new Wall(prev_x, prev_y, x, y);
            newPortal.isPortal = true;
            newPortal.linkA = this.secInd;
            newPortal.linkB = this.secInd;
            newPortals.add(newPortal);
            sector.walls.add(editor.walls.size);
            newSector.walls.add(editor.walls.size);
            affectedIndices.add(editor.walls.size);
            affectedWalls.add(newPortal);
            editor.walls.add(newPortal);

            prev_x = x;
            prev_y = y;
        }


        if (chainComplete())
            enact();

    }

    private boolean chainComplete() {
        if (affectedIndices.size < 2)
            return false;

        return (prev_x == start_x && prev_y == start_y);
    }

    private void enact() {
        boolean swapPortalLinks;
        Wall first = affectedWalls.get(0);
        swapPortalLinks = (first.x2 == start_x && first.y2 == start_y);

        for (int wInd : affectedIndices.toArray()) {
            Wall w = editor.walls.get(wInd);
            if (newPortals.contains(w, true)) {
                if (swapPortalLinks) {
                    w.linkA = secInd;
                    w.linkB = newSectorInd;
                } else {
                    w.linkA = newSectorInd;
                    w.linkB = secInd;
                }
            } else if (preexistingPortals.contains(w, true)) {
                sector.walls.removeValue(wInd);
                newSector.addWallSafely(wInd);
                if (w.linkA == secInd)
                    w.linkA = newSectorInd;
                if (w.linkB == secInd)
                    w.linkB = newSectorInd;
            } else {
                sector.walls.removeValue(wInd);
                newSector.addWallSafely(wInd);
            }
        }

        editor.sectors.add(newSector);

        shouldFinish = true;
    }

}
