package com.disector.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.disector.*;
import com.disector.assets.PixmapContainer;

import java.util.Stack;

public class SoftwareRenderer extends DimensionalRenderer {
    private final Pixmap[] ERROR_TEXTURE;

    private int[] occlusionBottom;
    private int[] occlusionTop;
    private final Stack<Integer> drawnPortals = new Stack<>();
    //private HashSet<Integer> transformedWalls = new HashSet<>();
    //private HashSet<Integer> transformedSectors = new HashSet<>();

    private final Color depthFogColor = new Color(0.2f, 0.05f, 0.2f, 1f);
    private final Color darkColor = new Color(0x20_0A_10_FF);

    public SoftwareRenderer(Application app) {
        super(app);

        Texture temp = new Texture(Gdx.files.local("assets/img/error_tex.png"));
        temp.getTextureData().prepare();
        ERROR_TEXTURE = PixmapContainer.makeMipMapSeries(temp);
        temp.dispose();

        setFovFromDeg(Application.config.fov);
    }

    @Override
    public void renderWorld() {
        resetDrawData();
        buffer.fill();
        drawSector(camCurrentSector, 0, frameWidth-1);
    }

    @Override
    public void resizeFrame(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        float fovDeg = getDegFromFov();
        super.resizeFrame(w, h);
        setFovFromDeg(fovDeg);
        reInitDrawData(w);
    }

    @Override
    public void setFov(float val) {
        setFovFromDeg(val);
    }

    public void setFovFromDeg(float deg) {
        camFOV = (float) ( halfWidth / Math.tan(Math.toRadians(deg/2)) );
    }

    public float getDegFromFov() {
        return 2.f * (float) Math.toDegrees( Math.atan(halfWidth / camFOV) );
    }

    @Override
    public boolean screenHasEmptySpace() {
        return !spanFilled(0, frameWidth-1);
    }

    // --------------------------------------------------------------------------------------

    private void drawSector(int secInd, int spanStart, int spanEnd) {
        Sector sec = null;
        try {
            sec = sectors.get(secInd);
        } catch (IndexOutOfBoundsException indexException) {
            return;// false;
        }

        //Get all walls of the sector, finding their nearest point to the camera
        Array<WallInfoPack> wallsToDraw = new Array<>();
        for (int wInd : sec.walls.toArray()) {
            wallsToDraw.add( new WallInfoPack(walls.get(wInd), wInd, new Vector2(camX, camY)) );
        }

        wallsToDraw.sort( //Sort wallsToDraw with nearest to camera first
            (WallInfoPack o1, WallInfoPack o2) -> Float.compare(o1.distToNearest, o2.distToNearest)
        );

        for (WallInfoPack wallInfo : wallsToDraw) {
            drawWall(wallInfo.wInd, secInd, spanStart, spanEnd);
            if (spanFilled(spanStart, spanEnd)) return;// true;
        }

        //Returns false if the entire span isn't filled
        //If this instance of drawSector() is the original
        //where the span is the full screen AND we have returned false,
        //we can know that currentSectorIndex of the camera is
        //misplaced
        return;// false;
    }

    private void drawWall(int wInd, int currentSectorIndex, int spanStart, int spanEnd) {
        Wall w = walls.get(wInd);
        boolean isPortal = w.isPortal;

        if (w.isPortal && drawnPortals.contains(wInd))
            return;

        float x1, y1, x2, y2; //Transform wall points relative to camera and store on stack
        if (isPortal && w.linkA == currentSectorIndex) {
            x1 = w.x2 - camX;
            y1 = w.y2 - camY;
            x2 = w.x1 - camX;
            y2 = w.y1 - camY;
        } else {
            x1 = w.x1 - camX;
            y1 = w.y1 - camY;
            x2 = w.x2 - camX;
            y2 = w.y2 - camY;
        }
        float playerCos = (float) Math.cos(-camR) , playerSin = (float) Math.sin(-camR);
        float tempX = x1;
        x1 = x1 * playerCos - y1 * playerSin;
        y1 = y1 * playerCos + tempX * playerSin;
        tempX = x2;
        x2 = x2 * playerCos - y2 * playerSin;
        y2 = y2 * playerCos + tempX * playerSin;

        if (x1 < 0 && x2 < 0) return; //Avoid drawing if totally behind camera

        float leftClipU = 0.f, rightClipU = 1.f;
        float wallLength = w.length();

        if (x1 < 0) { //If on-screen-left edge of wall is on camera, clip wall to point at edge of frame
            float slope = (y2-y1) / (x2-x1);
            float yAxisIntersect = y1 - slope*x1;
            leftClipU = (float) Math.sqrt( x1*x1 + (yAxisIntersect-y1)*(yAxisIntersect-y1) ) / wallLength;
            x1 = 0.001f; //Avoid dividing by zero
            y1 = yAxisIntersect;
        }

        if (x2 < 0) { //Now for right edge of wall and right edge of frame
            float slope = (y2-y1) / (x2-x1);
            float yAxisIntersect = y1 - slope*x1;
            rightClipU = 1.f - (float) ( Math.sqrt( x2*x2 + (yAxisIntersect-y2)*(yAxisIntersect-y2) ) / wallLength );
            x2 = 0.001f; //Avoid dividing by zero
            y2 = yAxisIntersect;
        }

        //if (x1 < 0.001f) x1 = 0.001f; //Avoid future division by very small values
        //if (x2 < 0.001f) x2 = 0.001f; //Well this is already set above ^^ And if

        float fov = camFOV;

        float p1_plotX = halfWidth - fov*y1/x1; //Plot edges of wall onto screen space
        float p2_plotX = halfWidth - fov*y2/x2;

        if (!isPortal && p2_plotX < p1_plotX) return; //Avoid drawing backside of non portal wall

        int leftEdgeX = Math.max(0, Math.min((int)p2_plotX,(int)p1_plotX) );
		int rightEdgeX = Math.min( Math.max((int)p2_plotX,(int)p1_plotX), frameWidth-1);

        if (leftEdgeX > spanEnd) return; //Avoid more processing if out of span
        if (rightEdgeX < spanStart) return;
        if (spanFilled(spanStart, spanEnd)) return;

        if (leftEdgeX < spanStart) leftEdgeX = spanStart;
        if (rightEdgeX > spanEnd) rightEdgeX = spanEnd;

        Sector currentSector = sectors.get(currentSectorIndex);
        float secFloorZ = currentSector.floorZ, secCeilZ = currentSector.ceilZ;

        float p1_plotLow = halfHeight - camVLook + fov*(secFloorZ-camZ)/x1; //Plot wall points vertically
        float p1_plotHigh = halfHeight - camVLook + fov*(secCeilZ-camZ)/x1;
        float p2_plotLow = halfHeight - camVLook + fov*(secFloorZ-camZ)/x2;
        float p2_plotHigh = halfHeight - camVLook + fov*(secCeilZ-camZ)/x2;

        float hProgress; //Horizontal per-pixel progress for this wall
        float quadBottom, quadTop, quadHeight; //Stores the top and bottom of the wall for each pixel column
        int rasterBottom, rasterTop; //Where to stop and start drawing for this pixel column

        //Variables needed if portal
        int portalDestIndex = (w.linkA == currentSectorIndex) ? w.linkB : w.linkA;
        float destCeiling = 100.f, destFloor = 0.f, upperWallCutoffV = 1.001f, lowerWallCutoffV = -0.001f;

        Pixmap[] textures, texturesLow, texturesHigh;
        try {
            textures = materials.get(w.mat).tex;
        } catch (Exception e) {
            //System.out.println("SoftwareRenderer: Caught Exception When grabbing texture");
            textures = ERROR_TEXTURE;
        }
        texturesLow = textures;
        texturesHigh = textures;

        if (isPortal) {
            drawnPortals.push(wInd); // !!
            destCeiling = sectors.get(portalDestIndex).ceilZ;
            destFloor = sectors.get(portalDestIndex).floorZ;
            float thisSectorCeilingHeight = secCeilZ - secFloorZ;
            if (destCeiling < secCeilZ)
                upperWallCutoffV = (destCeiling - secFloorZ) / thisSectorCeilingHeight;
            if (destFloor > secFloorZ)
                lowerWallCutoffV = (destFloor - secFloorZ) / thisSectorCeilingHeight;
            texturesLow = materials.get(w.matLower).tex;
            texturesHigh = materials.get(w.matUpper).tex;
        }

        for (int drawX = leftEdgeX; drawX <= rightEdgeX; drawX++) { //Per draw column loop
            if (occlusionTop[drawX] -1 <= occlusionBottom[drawX] ) continue;

            hProgress = (drawX-p1_plotX) / (p2_plotX-p1_plotX);

            if (hProgress<0) hProgress = 0.0f;
            if (hProgress>1.0) hProgress = 1.0f;

            quadBottom = p1_plotLow + hProgress*(p2_plotLow-p1_plotLow);
            quadTop = p1_plotHigh + hProgress*(p2_plotHigh-p1_plotHigh);
            quadHeight = quadTop - quadBottom;

            float fog = getFogFactor(x1 + hProgress*(x2-x1));

            float light = fullBright ? 1.0f : w.light;

            rasterBottom = Math.max( (int) quadBottom, occlusionBottom[drawX]);
            rasterTop = Math.min( (int) quadTop, occlusionTop[drawX]);

            float u =  ((1 - hProgress)*(leftClipU/x1) + hProgress*(rightClipU/x2)) / ( (1-hProgress)*(1/x1) + hProgress*(1/x2));
            //if (u<0.01f) u = 0.01f; if (u>0.99) u = 0.99f;

            Pixmap tex, texLower, texUpper;
            {
                final int mipMapCount = app.textures.pixmaps[0].length;
                final float mipMapResistanceFactor = 1f;
                float hProgressPlusOne = (drawX+1-p1_plotX) / (p2_plotX-p1_plotX);
                float uPlus1 = ((1 - hProgressPlusOne) * (leftClipU / x1) + hProgressPlusOne * (rightClipU / x2)) / ((1 - hProgressPlusOne) * (1 / x1) + hProgressPlusOne * (1 / x2));
                float texPixelWidth = Math.abs( textures[0].getWidth() * (uPlus1-u) );
                int mipMapIndex = Math.max(0, Math.min( (int)( (int)(Math.sqrt(texPixelWidth))/mipMapResistanceFactor ), mipMapCount-1));
                tex = textures[mipMapIndex];
                texLower = texturesLow[mipMapIndex];
                texUpper = texturesHigh[mipMapIndex];
            }

            for (int drawY = rasterBottom; drawY < rasterTop; drawY++) { //Per Pixel draw loop
                float v = (drawY - quadBottom) /quadHeight;
                //if (v<0.01f) v = 0.01f; if (v>0.99) v = 0.99f;

                if (isPortal && (v > lowerWallCutoffV && v < upperWallCutoffV) )
                    continue;

                Color drawColor;
                if (/*Draw Textures*/ true) {
                    if (!w.isPortal)
                        drawColor = grabColor(tex, u, v);
                    else if (v <= lowerWallCutoffV)
                        drawColor = grabColor(texLower, u, v);
                    else
                        drawColor = grabColor(texUpper, u, v);

                    drawColor.lerp(depthFogColor,fog);
                    drawColor.lerp(darkColor, 1.f - light);
                } else {
                    drawColor = getCheckerboardColor(u,v);
                }

                buffer.drawPixel(drawX, drawY, Color.rgba8888(drawColor) );

            } //End Per Pixel Loop

            //Floor and Ceiling
            if (occlusionBottom[drawX] < quadBottom && camZ > currentSector.floorZ)
                drawFloor(w, currentSector.matFloor, drawX, fov, rasterBottom, secFloorZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightFloor);

            if (occlusionTop[drawX] > rasterTop && camZ < currentSector.ceilZ)
                drawCeiling(w, currentSector.matCeil, drawX, fov, rasterTop, secCeilZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightCeil);

            //Update Occlusion Matrix
            updateOcclusion(isPortal, drawX, quadTop, quadBottom, quadHeight, upperWallCutoffV, lowerWallCutoffV);

        } //End Per Column Loop

        //Render Through Portal
        if (isPortal) {
            //drawSector(portalDestIndex, Math.max(leftEdgeX, spanStart), Math.min(rightEdgeX, spanEnd));
            drawSector(portalDestIndex, leftEdgeX, rightEdgeX);
            drawnPortals.pop();
        }

    }

    private void drawFloor(Wall w, int texInd, int drawX, float fov, int rasterBottom, float secFloorZ, float playerSin, float playerCos, float light) {
        final float scaleFactor = 32.f;
        float floorXOffset = camX/scaleFactor, floorYOffset = camY/scaleFactor;
        int vOffset = (int) camVLook;
        if (occlusionBottom[drawX] < rasterBottom) {
            float heightOffset = (camZ - secFloorZ) / scaleFactor;
            int floorEndScreenY = Math.min(rasterBottom, occlusionTop[drawX]);
            Pixmap tex = materials.get(texInd).tex[0];
            for (int drawY = occlusionBottom[drawX] + vOffset; drawY<=floorEndScreenY + vOffset; drawY++) {
                float floorX = heightOffset * (drawX-halfWidth) / (drawY-halfHeight);
                float floorY = heightOffset * fov / (drawY-halfHeight);

                float rotFloorX = floorX*playerSin - floorY*playerCos + floorXOffset;
                float rotFloorY = floorX*playerCos + floorY*playerSin + floorYOffset;

                if (rotFloorX<=0) rotFloorX = -rotFloorX;
                if (rotFloorY<0) rotFloorY = -rotFloorY;

                rotFloorX /= 4;
                rotFloorY /= 4;

                rotFloorX = rotFloorX%1;
                rotFloorY = rotFloorY%1;

                while(rotFloorX<0.0) rotFloorX+=1.0f;
                while(rotFloorX>1.0f) rotFloorX-=1.0f;
                while(rotFloorY<0.0) rotFloorY+=1.0f;
                while(rotFloorY>1.0f) rotFloorY-=1.0f;

                /* CHECKERBOARD
                boolean checkerBoard = ( (int)(rotFloorX*8%2) == (int)(rotFloorY*8%2) );
                Color drawColor = new Color( checkerBoard ? 0xFFD08010 : 0xFF10D080 );
                float floorFogValue = 1.f - ((halfHeight-heightOffset-drawY)/(halfHeight-heightOffset));
                floorFogValue = Math.min(1.f, Math.max(0.f,floorFogValue));
                drawColor.lerp(0.1f,0f,0.2f,1f, floorFogValue);
                buffer.drawPixel(drawX, drawY - vOffset, drawColor.toIntBits() );*/

                float horizonScreenDistVert = halfHeight - drawY;
                float angleOfScreenRow = (float) Math.atan(horizonScreenDistVert / fov);
                float dist = (camZ - secFloorZ) / (float) Math.sin(angleOfScreenRow);

                Color drawColor = grabColor(tex, rotFloorX, rotFloorY);

                drawColor.lerp(depthFogColor, getFogFactor(dist));
                drawColor.lerp(darkColor, 1.0f - light);

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );

            }
        }
    }

    private void drawCeiling(Wall w, int texInd, int drawX, float fov, int rasterTop, float secCeilZ, float playerSin, float playerCos, float light) {
        Pixmap tex = materials.get(texInd).tex[0];
        boolean isSky = drawParallax && materials.get(texInd).isSky;

        final float scaleFactor = 32.f;

        float floorXOffset = 0f, floorYOffset = 0f, heightOffset = 0f;
        float portionImgToDraw = 0f, centerScreenSkyU = 0f;

        if (!isSky) {
            floorXOffset = camX / scaleFactor;
            floorYOffset = camY / scaleFactor;
            heightOffset = (secCeilZ-camZ) / scaleFactor;
        } else {
            portionImgToDraw = getDegFromFov() / 360f;
            float angle = (float) Math.toDegrees(camR);
            while(angle < 0) angle += 360;
            angle = angle%360;
            centerScreenSkyU = angle / 360f;
        }

        int vOffset = (int) camVLook;
        int ceilEndScreenY = occlusionTop[drawX] + vOffset;

        for (int drawY = Math.max(rasterTop, occlusionBottom[drawX]) + vOffset; drawY <= ceilEndScreenY; drawY++) {

            if (!isSky) {

                float ceilX = heightOffset * (drawX - halfWidth) / (drawY - halfHeight);
                float ceilY = heightOffset * fov / (drawY - halfHeight);

                float rotX = ceilX * playerSin - ceilY * playerCos - floorXOffset;
                float rotY = ceilX * playerCos + ceilY * playerSin - floorYOffset;

                if (rotX <= 0) rotX = -rotX;
                if (rotY < 0) rotY = -rotY;

                rotX /= 4f;
                rotY /= 4f;

                rotX = rotX % 1;
                rotY = rotY % 1;

                while (rotX < 0.0) rotX += 1.0f;
                while (rotX > 1.0f) rotX -= 1.0f;
                while (rotY < 0.0) rotY += 1.0f;
                while (rotY > 1.0f) rotY -= 1.0f;

                /*boolean checkerBoard = ( (int)(rotX*8%2) == (int)(rotY*8%2) );
                Color drawColor = new Color( checkerBoard ? 0xFF_A0_20_50 : 0xFF_20_50_A0 );
                float ceilFogValue = 1.0f - ( ((drawY-halfHeight) / halfHeight) );
                ceilFogValue = (float) Math.min(1.f, Math.max(0.f,ceilFogValue));
                drawColor.lerp(0.1f,0f,0.2f,1f, ceilFogValue);
                buffer.drawPixel(drawX, drawY - vOffset, drawColor.toIntBits() );*/

                float horizonScreenDistVert = -halfHeight + drawY;
                float angleOfScreenRow = (float) Math.atan(horizonScreenDistVert / fov);
                float dist = (secCeilZ - camZ) / (float) Math.sin(angleOfScreenRow);

                Color drawColor = grabColor(tex, rotX, rotY);

                drawColor.lerp(0.1f, 0f, 0.2f, 1f, getFogFactor(dist));
                drawColor.lerp(darkColor, 1.0f - light);
                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );
            } else { //If isSky
                Color drawColor = grabColor(tex, centerScreenSkyU - (drawX-halfWidth)*portionImgToDraw/frameWidth, drawY/(float)tex.getHeight());
                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );
            }

        }

    }

    private void updateOcclusion(boolean isPortal, int drawX, float quadTop, float quadBottom, float quadHeight, float upperCutoff, float lowerCutoff) {
        if (!isPortal) {
            if (occlusionBottom[drawX] < quadTop) occlusionBottom[drawX] = (int) quadTop;
            if (occlusionTop[drawX] > quadBottom) occlusionTop[drawX] = (int) quadBottom;
        } else {
            occlusionTop[drawX] = (int) Math.min(quadBottom + (quadHeight * upperCutoff), occlusionTop[drawX]);
            occlusionBottom[drawX] = (int) Math.max(quadBottom + (quadHeight * lowerCutoff), occlusionBottom[drawX]);
        }
    }

    private void reInitDrawData(int newFrameWidth) {
        occlusionBottom = new int[newFrameWidth];
        occlusionTop = new int[newFrameWidth];
    }

    private void resetDrawData() {
        //transformedWalls.clear();
        //transformedSectors.clear();
        drawnPortals.clear();

        for (int i=0; i<frameWidth; i++) {
            occlusionBottom[i] = 0;
            occlusionTop[i] = frameHeight;
        }
    }

    private float getFogFactor(float dist) {
        if (!drawFog) return 0f;
        final float fogDistance = 400f;
        return Math.max(0, Math.min(fogDistance, dist) ) / fogDistance;
    }

    private boolean spanFilled(int spanStart, int spanEnd) {
        for (int i=spanStart; i<spanEnd; i++) {
            if (occlusionBottom[i] < occlusionTop[i]-1)
                return false;
        }
        return true;
    }

    private Color grabColor(Pixmap tex, float u, float v) {
        u = u - (int)u;
        v = v - (int)v;
        return new Color(tex.getPixel( (int)(u*tex.getWidth()), (int)((1.f-v)*tex.getHeight()) ));
    }

    private Color getCheckerboardColor(float u, float v) {
        boolean checker = ( (int)(u*8)%2 == (int)(v*8)%2 );
        return new Color(checker ? 0xB0_20_30_FF : 0xA0_A0_A0_FF);
    }
}