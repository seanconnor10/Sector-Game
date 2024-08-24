package com.disector.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.ScreenUtils;

import com.disector.Application;
import com.disector.Wall;

class NewEditorMapRenderer {
    private final Application app;
    private final Editor editor;

    private final ShapeRenderer shape = new ShapeRenderer();
    FrameBuffer frame;

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

    // -------------------------------------------------

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

        shape.begin(ShapeRenderer.ShapeType.Line);

        drawGrid();
        drawWalls();

        //Draw Player1 Position
        shape.setColor(Color.TEAL);
        Vector4 playerPos = app.gameWorld.getPlayerPosition();
        drawCircle(playerPos.x, playerPos.y, app.gameWorld.getPlayerRadius());

        //Draw Editor-ViewRenderer Camera Position
        if ( editor.layout != Layouts.MAP )
            drawCameraWidget();

        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Filled);
        drawVertices();
        shape.end();

        frame.end();
    }

    public void drawWalls() {
        int highlightedWall = editor.selection.getWallHighlightIndex();
        for (int i=0; i<app.walls.size; i++) {
            Wall wall = app.walls.get(i);

            if (i == highlightedWall) {
                shape.setColor(new Color(1f, 0f, 0f, 1f).lerp(Color.LIME, editor.animationFactor));
            } else {
                shape.setColor(wall.isPortal ? Color.CORAL : Color.WHITE);
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
    }

    public void drawVertices() {
        shape.setColor(Color.RED);
        for (Wall wall : editor.walls) {
            drawSquarePoint(wall.x1, wall.y1, 8);
            drawSquarePoint(wall.x2, wall.y2, 8);
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

    // ----- Draw Primitives From World Coordinates -------------------

    private void drawCircle(float x, float y, float r) {
        shape.circle(halfWidth+zoom*(x-camX), halfHeight+zoom*(y-camY), r*zoom);
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
