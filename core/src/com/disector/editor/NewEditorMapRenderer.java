package com.disector.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ScreenUtils;

import com.disector.Application;
import com.disector.Wall;

import java.util.Arrays;

class NewEditorMapRenderer {
    private final Application app;
    private final Editor editor;

    private final ShapeRenderer shape = new ShapeRenderer();
    FrameBuffer frame;

    private final Array<FakeWall> fakeWalls = new Array<>();

    CameraMapDraw viewCamPosition = new CameraMapDraw(0,0,0,0);
    float camX = 0, camY = 0;
    float zoom = 1f;

    float halfWidth, halfHeight;

    NewEditorMapRenderer(Application app, Editor editor, Rectangle startDimensions) {
        this.app = app;
        this.editor = editor;
        frame = new FrameBuffer(app.pixelFormat, 1, 1, false);
        refreshPanelSize(startDimensions);
    }

    void refreshPanelSize(Rectangle r) {
        frame.dispose();
        int w = Math.max( (int) r.width,  1 );
        int h = Math.max( (int) r.height, 1 );
        frame = new FrameBuffer(app.pixelFormat, w, h, false);
        halfWidth = w/2f;
        halfHeight = h/2f;
        shape.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
    }

    // --------Draw Methods---------------------------

    public void render() {
        frame.begin();

        ScreenUtils.clear(Color.BLACK);

        //shape.begin(ShapeRenderer.ShapeType.Filled); {
        //    drawSectorFilled(editor.selection.highlightedSectorIndex);
        //shape.end(); }

        shape.begin(ShapeRenderer.ShapeType.Line); {
            drawGrid();
            drawWalls();

            //Draw Player1 Position
            shape.setColor(Color.TEAL);
            Vector4 playerPos = app.gameWorld.getPlayerEyesPosition();
            drawCircle(playerPos.x, playerPos.y, app.gameWorld.getPlayerRadius());

            //Draw Editor-ViewRenderer Camera Position
            if ( editor.layout != Layouts.MAP )
                drawCameraWidget();

        shape.end(); }

        shape.begin(ShapeRenderer.ShapeType.Filled); {
            drawVertices();
        shape.end(); }

        frame.end();

        fakeWalls.clear();
    }

    public void drawWalls() {
        //Attempt to get this in stack memory?
        IntArray selectedWallIndices = new IntArray(editor.selection.wallIndices);
        int highlightedWall = editor.selection.getWallHighlightIndex();
        float anim = editor.animationFactor;

        for (int i=0; i<app.walls.size; i++) {
            Wall wall = app.walls.get(i);

            if (i == highlightedWall) {
                shape.setColor(new Color(1f, 0f, 0f, 1f).lerp(Color.TEAL, anim));
            } else {
                if (selectedWallIndices.contains(i)) {
                    shape.setColor(wall.isPortal ? Color.YELLOW : Color.CYAN);
                    shape.setColor( shape.getColor().lerp(wall.isPortal ? Color.LIME : Color.PINK, anim*2) );
                }
                else {
                    shape.setColor(wall.isPortal ? Color.CORAL : Color.WHITE);
                }
            }

            drawLine(wall.x1, wall.y1, wall.x2, wall.y2);

            //Draw Normal Notch
            //if (wall.isPortal) continue;
            shape.getColor().a *= 0.6f;
            float centerX = (wall.x1 + wall.x2) / 2f;
            float centerY = (wall.y1 + wall.y2) / 2f;
            drawLine(
                    centerX,
                    centerY,
                    centerX + (float) ( Math.cos(wall.normalAngle) * Math.min(15.0f/zoom, 15.f) ),
                    centerY + (float) ( Math.sin(wall.normalAngle) * Math.min(15.0f/zoom, 15.f) )
            );
        }

        //Draw Fake Walls
        for (FakeWall walls : fakeWalls) {
            shape.setColor(Color.LIME);
            drawLine(walls.p1.x, walls.p1.y, walls.p2.x, walls.p2.y);
        }
    }

    public void drawSectorFilled(int index) {
        if (index == -1) return;

        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        float anim = editor.animationFactor;
        shape.setColor(1f, 0.5f, 0.5f, 0.1f);
        shape.getColor().a=0.1f;

        Vertex[] verts;

        try {
            verts = Arrays.stream(editor.sectors.get(index).walls.toArray())
                    .mapToObj(editor.walls::get)
                    .map(wall -> new Vertex(wall.x1, wall.y1))
                    .toArray(Vertex[]::new);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return;
        }

        Vertex first = verts[0];

        for (int i=2; i<verts.length; i++) {
            drawTri(first.x, first.y, verts[i].x, verts[i].y, verts[i-1].x, verts[i-1].y);
        }
    }

    private static class Vertex {
        float x, y;
        Vertex(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class FakeWall {
        Vertex p1, p2;
        FakeWall(float x1, float y1, float x2, float y2) {
            this.p1 = new Vertex(x1, y1);
            this.p2 = new Vertex(x2, y2);
        }
    }

    public void drawVertices() {
        shape.setColor(Color.RED); // Transparent Red
        for (Wall wall : editor.walls) {
            drawSquarePoint(wall.x1, wall.y1, 6);
            drawSquarePoint(wall.x2, wall.y2, 6);
        }
    }

    public void drawGrid() {
        if (!editor.isGridSnapping) return;

        shape.setColor(0, 0.2f, 0.1f, 0.5f);

        int gridSize = editor.gridSize;

        for (float worldX = gridSize*(int)((camX-(halfWidth/zoom))/gridSize); worldX<camX+( (halfWidth+gridSize) /zoom); worldX+=gridSize) {
            drawLine(worldX, camY-halfHeight/zoom, worldX, camY+halfHeight/zoom);
        }
        for (float worldY = gridSize*(int)((camY-(halfHeight/zoom))/gridSize); worldY<camY+(halfHeight/zoom); worldY+=gridSize) {
            drawLine(camX-halfWidth/zoom, worldY, camX+halfWidth/zoom, worldY);
        }

        shape.setColor(0.1f, 0.2f, 0.3f, 0.8f);
        drawLine(-10000,0, 10000, 0);
        drawLine(0, -10000, 0, 10000);
    }

    public void drawCameraWidget() {
        float x = viewCamPosition.x;
        float y = viewCamPosition.y;
        float r = viewCamPosition.r;
        float hFov = (float) Math.toRadians(viewCamPosition.halfFov);
        final float LENGTH = 150.f;
        float sideLength = LENGTH / (float) Math.cos(hFov);
        float lx = x + sideLength * (float) Math.cos(r+hFov);
        float ly = y + sideLength * (float) Math.sin(r+hFov);
        float rx = x + sideLength * (float) Math.cos(r-hFov);
        float ry = y + sideLength * (float) Math.sin(r-hFov);
        shape.setColor(Color.CYAN);
        drawCircle(viewCamPosition.x, viewCamPosition.y, 3);
        shape.getColor().a = 0.5f;
        drawLine(x, y, lx, ly);
        drawLine(x, y, rx, ry);drawLine(lx, ly, rx, ry);
    }

    public void addFakeWall(float x1, float y1, float x2, float y2) {
        fakeWalls.add(new FakeWall(x1, y1, x2, y2));
    }

    // ----- Draw Primitives From World Coordinates -------------------

    private void drawCircle(float x, float y, float r) {
        shape.circle(halfWidth+zoom*(x-camX), halfHeight+zoom*(y-camY), r*zoom);
    }

    private void drawTri(float x, float y, float x2, float y2, float x3, float y3) {
        shape.triangle(
                halfWidth+zoom*(x-camX),
                halfHeight+zoom*(y-camY),
                halfWidth+zoom*(x2-camX),
                halfHeight+zoom*(y2-camY),
                halfWidth+zoom*(x3-camX),
                halfHeight+zoom*(y3-camY)
        );
    }

    private void drawLine(float x, float y, float x2, float y2) {
        shape.line(
                halfWidth+zoom*(x-camX),
                halfHeight+zoom*(y-camY),
                halfWidth+zoom*(x2-camX),
                halfHeight+zoom*(y2-camY)
        );
    }

    private void drawSquarePoint(float x, float y, float length) {
        shape.rect(
                halfWidth+zoom*(x-camX)-(length/2f),
                halfHeight+zoom*(y-camY)-(length/2f),
                length,
                length
        );
    }
}
