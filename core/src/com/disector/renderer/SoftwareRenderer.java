package com.disector.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.disector.*;
import com.disector.Material;
import com.disector.assets.PixmapContainer;
import com.disector.renderer.sprites.FacingSprite;
import com.disector.renderer.sprites.Sprite;
import com.disector.renderer.sprites.WallSprite;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class SoftwareRenderer extends DimensionalRenderer {
    protected final Pixmap[] ERROR_TEXTURE;

    protected int[] occlusionBottom;
    protected int[] occlusionTop;
    protected final Deque<Integer> drawnPortals = new ArrayDeque<>();
    protected float[] depth;
    //private HashSet<Integer> transformedWalls = new HashSet<>();
    //private HashSet<Integer> transformedSectors = new HashSet<>();

    protected Array<Sprite> sprites = new Array<>();
    private static final Pixmap TEST_SPRITE_IMG = new Pixmap(Gdx.files.local("assets/img/lamp.png"));

    protected final Color depthFogColor = new Color(0.1f, 0.05f, 0.2f, 1f);
    protected final Color darkColor = new Color(0x20_0A_00_FF);

    protected final boolean AGGRESSIVE_MIPMAPS = false;

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
        drawSprites();

    }

    @Override
    public void resizeFrame(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        float fovDeg = getDegFromFov();
        super.resizeFrame(w, h);
        setFovFromDeg(fovDeg);
        reInitDrawData(w, h);
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

    // ----- World Geometry Drawing Methods -----------------------------

    protected void drawSector(int secInd, int spanStart, int spanEnd) {
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

    protected void drawWall(int wInd, int currentSectorIndex, int spanStart, int spanEnd) {
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
            x1 = 0.01f; //Avoid dividing by zero
            y1 = yAxisIntersect;
        }

        if (x2 < 0) { //Now for right edge of wall and right edge of frame
            float slope = (y2-y1) / (x2-x1);
            float yAxisIntersect = y1 - slope*x1;
            rightClipU = 1.f - (float) ( Math.sqrt( x2*x2 + (yAxisIntersect-y2)*(yAxisIntersect-y2) ) / wallLength );
            x2 = 0.01f; //Avoid dividing by zero
            y2 = yAxisIntersect;
        }

        //if (x1 < 0.001f) x1 = 0.001f; //Avoid future division by very small values
        //if (x2 < 0.001f) x2 = 0.001f; //Well this is already set above ^^ And if

        float fov = camFOV;

        float p1_plotX = halfWidth - fov*y1/x1; //Plot edges of wall onto screen space
        float p2_plotX = halfWidth - fov*y2/x2;

        if (!isPortal && p2_plotX < p1_plotX) return; //Avoid drawing backside of non portal wall

        float leftEdgePrecise = Math.max(0, Math.min(p2_plotX,p1_plotX) );
        float rightEdgePrecise = Math.min( Math.max(p2_plotX,p1_plotX), frameWidth-1);

        int leftEdgeX = (int) leftEdgePrecise;//Math.max(0, Math.min((int)p2_plotX,(int)p1_plotX) );
		int rightEdgeX = (int) rightEdgePrecise; //Math.min( Math.max((int)p2_plotX,(int)p1_plotX), frameWidth-1);

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
            if (textures == null)
                throw new NullPointerException("Material Exists yet Pixmap is null");
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
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

            try {
                texturesLow = materials.get(w.matLower).tex;
                if (texturesLow == null)
                    throw new NullPointerException("Lower Material Exists yet Pixmap is null");
            } catch (NullPointerException | ArrayIndexOutOfBoundsException e)  {
                texturesLow = ERROR_TEXTURE;
            }

            try {
                texturesHigh = materials.get(w.matUpper).tex;
                if (texturesHigh == null)
                    throw new NullPointerException("Upper Material Exists yet Pixmap is null");
            } catch (NullPointerException | ArrayIndexOutOfBoundsException e)  {
                texturesHigh = ERROR_TEXTURE;
            }

        }

        for (int drawX = leftEdgeX; drawX <= rightEdgeX; drawX++) { //Per draw column loop
            if (occlusionTop[drawX] -1 <= occlusionBottom[drawX] ) continue;

            hProgress = (drawX-p1_plotX) / (p2_plotX-p1_plotX);

            if (hProgress<0) hProgress = 0.0f;
            if (hProgress>1.0) hProgress = 1.0f;

            quadBottom = p1_plotLow + hProgress*(p2_plotLow-p1_plotLow);
            quadTop = p1_plotHigh + hProgress*(p2_plotHigh-p1_plotHigh);
            quadHeight = quadTop - quadBottom;

            float dist, fog; //= getFogFactor( (x1 + hProgress*(x2-x1)) );
            {
                float screenXProgress = (drawX-leftEdgePrecise) / (rightEdgePrecise-leftEdgePrecise);
                dist = x1 + screenXProgress*(x2-x1);
                fog = getFogFactor(dist);
            }

            float light = fullBright ? 1.0f : w.light;

            rasterBottom = Math.max( (int) quadBottom, occlusionBottom[drawX]);
            rasterTop = Math.min( (int) quadTop, occlusionTop[drawX]);

            //Fill Depth Array where appropriate
            try {
                Arrays.fill(
                        depth,
                        drawX * frameHeight + Math.min(occlusionBottom[drawX], rasterBottom),
                        drawX * frameHeight + Math.max(occlusionTop[drawX], rasterTop),
                        dist
                );
            } catch (IllegalArgumentException e) {}

            float u =  ((1 - hProgress)*(leftClipU/x1) + hProgress*(rightClipU/x2)) / ( (1-hProgress)*(1/x1) + hProgress*(1/x2));
            //if (u<0.01f) u = 0.01f; if (u>0.99) u = 0.99f;

            Pixmap tex, texLower, texUpper;

            final int MAX_MIP_IND = PixmapContainer.MIPMAP_COUNT - 1;

            float hProgressPlusOne = (drawX+1-p1_plotX) / (p2_plotX-p1_plotX);
            float uPlus1 = ((1 - hProgressPlusOne) * (leftClipU / x1) + hProgressPlusOne * (rightClipU / x2)) / ((1 - hProgressPlusOne) * (1 / x1) + hProgressPlusOne * (1 / x2));

            float texX=0;
            int pixmap_ind=0;
            {
                texX = w.xOffset + u * (wallLength / (float)textures[0].getWidth() / w.xScale);
                float texX_PlusOne = uPlus1 * (wallLength / (float)textures[0].getWidth() / w.xScale);
                float pixWidth = (texX_PlusOne - texX) * textures[0].getWidth();
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                tex = textures[pixmap_ind];
                texLower = tex;
                texUpper = tex;
            }

            float texX_Lower=0, texX_Upper=0;
            if (isPortal){
                texX_Lower = w.Lower_xOffset + u * (wallLength / (float)texturesLow[0].getWidth() / w.Lower_xScale);
                float texX_PlusOne = uPlus1 * (wallLength / (float)texturesLow[0].getWidth() / w.Lower_xScale);
                float pixWidth = (texX_PlusOne - texX_Lower) * texturesLow[0].getWidth();
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                texLower = texturesLow[pixmap_ind];
           
                texX_Upper = w.Upper_xOffset + u * (wallLength / (float)texturesLow[0].getWidth() / w.Upper_xScale);
                texX_PlusOne = uPlus1 * (wallLength / (float)texturesLow[0].getWidth() / w.Upper_xScale);
                pixWidth = (texX_PlusOne - texX_Upper) * texturesLow[0].getWidth();
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                texUpper = texturesHigh[pixmap_ind];
            }

            for (int drawY = rasterBottom; drawY < rasterTop; drawY++) { //Per Pixel draw loop
                float v = (drawY - quadBottom) / quadHeight;

                if (isPortal && (v > lowerWallCutoffV && v < upperWallCutoffV) )
                    continue;

                float pixX;
                
                float yOff, yScale;
                if (!isPortal) {
                    yOff = w.yOffset;
                    yScale = w.yScale;
                    pixX = texX;
                } else if (v<lowerWallCutoffV) {
                    yOff = w.Lower_yOffset;
                    yScale = w.Lower_yScale;
                    pixX = texX_Lower;
                } else if (v<upperWallCutoffV) {
                    yOff = w.yOffset;
                    yScale = w.yScale;
                    pixX = texX;
                } else {
                    yOff = w.Upper_yOffset;
                    yScale = w.Upper_yScale;
                    pixX = texX_Upper;
                }

                float tempYOff = yOff < 0 ? 1.f - Math.abs(yOff) % 1.f : yOff;
                float texV = (tempYOff + v * yScale) % 1.0f;

                Color drawColor;
                    if (!w.isPortal)
                        drawColor = grabColor(tex, pixX, texV);
                    else if (v <= lowerWallCutoffV)
                        drawColor = grabColor(texLower, pixX, texV);
                    else
                        drawColor = grabColor(texUpper, pixX, texV);

                drawColor.lerp(depthFogColor,fog);
                drawColor.lerp(darkColor, 1.f - light);

                buffer.drawPixel(drawX, drawY, Color.rgba8888(drawColor));

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

    protected void drawFloor(Wall w, int texInd, int drawX, float fov, int rasterBottom, float secFloorZ, float playerSin, float playerCos, float light) {
        final float scaleFactor = 32.f;
        float floorXOffset = camX/scaleFactor, floorYOffset = camY/scaleFactor;
        int vOffset = (int) camVLook;

        if (occlusionBottom[drawX] < rasterBottom) {
            float heightOffset = (camZ - secFloorZ) / scaleFactor;
            int floorEndScreenY = Math.min(rasterBottom, occlusionTop[drawX]);
            Pixmap tex;
            try {
                tex = materials.get(texInd).tex[0];
            } catch (Exception e) {
                //System.out.println("SoftwareRenderer: Caught Exception When grabbing texture");
                tex = ERROR_TEXTURE[0];
            }
            for (int drawY = occlusionBottom[drawX] + vOffset; drawY<=floorEndScreenY + vOffset; drawY++) {
                float floorX = heightOffset * (drawX-halfWidth) / (drawY-halfHeight);
                float floorY = heightOffset * fov / (drawY-halfHeight);

                float rotFloorX = Math.abs( floorX*playerSin - floorY*playerCos + floorXOffset );
                float rotFloorY = Math.abs( floorX*playerCos + floorY*playerSin + floorYOffset );

                rotFloorX /= 4;
                rotFloorY /= 4;

                rotFloorX = rotFloorX%1;
                rotFloorY = rotFloorY%1;

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

                //Color drawColor = grabColor(tex, rotFloorX, rotFloorY);
                Color drawColor = new Color(tex.getPixel( (int)(rotFloorX*tex.getWidth()), (int)((1.f-rotFloorY)*tex.getHeight()) ));

                drawColor.lerp(depthFogColor, getFogFactor(dist));
                drawColor.lerp(darkColor, 1.0f - light);

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor));

            }
        }
    }

    protected void drawCeiling(Wall w, int texInd, int drawX, float fov, int rasterTop, float secCeilZ, float playerSin, float playerCos, float light) {
        Pixmap tex;
        boolean isSky = false;
        try {
            Material mat = materials.get(texInd);
            tex = mat.tex[0];
            isSky = drawParallax && mat.isSky;
        } catch (Exception e) {
            //System.out.println("SoftwareRenderer: Caught Exception When grabbing texture");
            tex = ERROR_TEXTURE[0];
        }

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

                float rotX = Math.abs( ceilX * playerSin - ceilY * playerCos - floorXOffset );
                float rotY = Math.abs( ceilX * playerCos + ceilY * playerSin - floorYOffset );

                rotX /= 4f;
                rotY /= 4f;

                rotX = rotX % 1;
                rotY = rotY % 1;

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

                drawColor.lerp(depthFogColor, getFogFactor(dist));
                drawColor.lerp(darkColor, 1.0f - light);

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor));

            } else { //If isSky
                Color drawColor = grabColor(tex, centerScreenSkyU - (drawX-halfWidth)*portionImgToDraw/frameWidth, drawY/(float)tex.getHeight());
                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor));

            }

        }

    }

    protected void updateOcclusion(boolean isPortal, int drawX, float quadTop, float quadBottom, float quadHeight, float upperCutoff, float lowerCutoff) {
        if (!isPortal) {
            if (occlusionBottom[drawX] < quadTop) occlusionBottom[drawX] = (int) quadTop;
            if (occlusionTop[drawX] > quadBottom) occlusionTop[drawX] = (int) quadBottom;
        } else {
            occlusionTop[drawX] = (int) Math.min(quadBottom + (quadHeight * upperCutoff), occlusionTop[drawX]);
            occlusionBottom[drawX] = (int) Math.max(quadBottom + (quadHeight * lowerCutoff), occlusionBottom[drawX]);
        }
    }

    // ---- Sprite Drawing Methods ---------------------------------------

    protected void drawSprites() {
        //Transform Co-Ords
        float playerCos = (float) Math.cos(-camR) , playerSin = (float) Math.sin(-camR);
        float tempvar;
        for (Sprite spr : sprites) {
            spr.x -= camX;
            spr.y -= camY;

            tempvar = spr.x;
            spr.x = spr.x * playerCos - spr.y * playerSin;
            spr.y = spr.y * playerCos + tempvar * playerSin;
            spr.depth = spr.x;

            switch (spr.type) {
                case WALL:
                    WallSprite wSpr = (WallSprite) spr;
                    wSpr.x2 -= camX;
                    wSpr.y2 -= camY;

                    tempvar = wSpr.x2;
                    wSpr.x2 = wSpr.x2 * playerCos - wSpr.y2 * playerSin;
                    wSpr.y2 = wSpr.y2 * playerCos + tempvar * playerSin;
                    //spr.depth = spr.x;
                    break;
                default:
            }

        }

        //Sort by Depth
        sprites.sort( (o1, o2) -> {
            return Float.compare(o2.depth, o1.depth);
        } );

        //Call Draw Function
        for (Sprite spr : sprites) {
            switch (spr.type) {
                case FACING:
                    drawFacingSprite((FacingSprite) spr);
                    break;
                case WALL:
                    drawWalLSprite((WallSprite) spr);
                    break;
                default:
                    break;
            }
        }
    }

    protected void drawFacingSprite(FacingSprite spr) {
        float x = spr.x;
        if (x < 0) return;
        float y = spr.y;
        float z = spr.z;

        float width = spr.width;
        float height = spr.height;

        float fovDivX = camFOV / x;

        //Center of billboard spr on screen
        float xPlot = halfWidth - y * fovDivX;

        //Half of sprite's width on screen space
        float spr_h_w = fovDivX * (width/2f);

        //Where the edges on actually plotted on screen space
        float leftEdgePlot = xPlot - spr_h_w;
        float rightEdgePlot = xPlot + spr_h_w;

        float bottomEdgePlot = halfHeight - camVLook + (z-camZ) * fovDivX;
        float topEdgePlot = halfHeight - camVLook + (z+height-camZ) * fovDivX;

        //Where we will begin drawing in screen space
        int rasterLeft = (int) Math.max(0, leftEdgePlot);
        int rasterRight = (int) Math.min(frameWidth-1, rightEdgePlot);

        int rasterBottom = (int) Math.max(0, bottomEdgePlot);
        int rasterTop = (int) Math.min(frameHeight-1, topEdgePlot);

        float u, v, startV, du, dv; // 'du' = delta U ...
        u = (rasterLeft-leftEdgePlot) / (rightEdgePlot - leftEdgePlot);
        v = 1.f - (rasterBottom-bottomEdgePlot) / (topEdgePlot - bottomEdgePlot);
        startV = v;
        du = ( (rasterLeft+1 - leftEdgePlot) / (rightEdgePlot - leftEdgePlot) ) - u;
        dv = (1.f - (rasterBottom+1 - bottomEdgePlot) / (topEdgePlot - bottomEdgePlot)) - v;

        Pixmap img = spr.img;
        float imgW = img.getWidth();
        float imgH = img.getHeight();

        for (int dx = rasterLeft; dx < rasterRight; dx++) {

            for (int dy = rasterBottom; dy < rasterTop; dy++) {
                if (depth[dx * frameHeight + dy] < x)
                    continue;

                buffer.drawPixel(dx, dy, img.getPixel((int)(u*imgW), (int)(v*imgH)));

                v += dv;
            }

            v = startV;
            u += du;
        }
    }

    protected void drawWalLSprite(WallSprite spr) {
        float x1 = spr.x, y1 = spr.y, x2 = spr.x2, y2 = spr.y2, z = spr.z, height = spr.height;
        float fov = camFOV;

        //Clip wall if an edge is behind camera
        float leftClipU = 0.f, rightClipU = 1.f;
        float length = (float) Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) );

        if (x1 < 0) { //If on-screen-left edge of wall is on camera, clip wall to point at edge of frame
            float slope = (y2-y1) / (x2-x1);
            float yAxisIntersect = y1 - slope*x1;
            leftClipU = (float) Math.sqrt( x1*x1 + (yAxisIntersect-y1)*(yAxisIntersect-y1) ) / length;
            x1 = 0.01f; //Avoid dividing by zero
            y1 = yAxisIntersect;
        }

        if (x2 < 0) { //Now for right edge of wall and right edge of frame
            float slope = (y2-y1) / (x2-x1);
            float yAxisIntersect = y1 - slope*x1;
            rightClipU = 1.f - (float) ( Math.sqrt( x2*x2 + (yAxisIntersect-y2)*(yAxisIntersect-y2) ) / length );
            x2 = 0.01f; //Avoid dividing by zero
            y2 = yAxisIntersect;
        }

        float p1_plot = halfWidth - y1 * fov / x1;
        float p2_plot = halfWidth - y2 * fov / x2;

        if (p2_plot < p1_plot) return;
        if (p2_plot < 0 && p1_plot < 0) return;

        float midline = halfHeight - camVLook;
        float p1_plot_low  = midline + ( (z-camZ) * fov / x1 );
        float p1_plot_high = midline + ( (z+height-camZ) * fov / x1 );
        float p2_plot_low  = midline + ( (z-camZ) * fov / x2 );
        float p2_plot_high = midline + ( (z+height-camZ) * fov / x2 );

        Pixmap img = spr.img;
        float imgW = img.getWidth();
        float imgH = img.getHeight();

        float startX = Math.clamp(p1_plot, 0, frameWidth);
        float endX = Math.clamp(p2_plot, 0, frameWidth);

        for (float dx = startX; dx < endX; dx++) {
            float hProgress = (dx - p1_plot) / (p2_plot- p1_plot);

            float u = ((1 - hProgress)*(leftClipU/x1) + hProgress*(rightClipU/x2)) / ( (1-hProgress)*(1/x1) + hProgress*(1/x2));

            float startY = p1_plot_low + hProgress*(p2_plot_low-p1_plot_low);
            float endY = p1_plot_high + hProgress*(p2_plot_high-p1_plot_high);

            for (float dy = startY; dy < endY; dy++) {
                float v = (dy-startY) / (endY-startY);
                buffer.drawPixel((int)dx, (int)dy, img.getPixel((int)(u*imgW), (int)(v*imgH)));
            }
        }
    }

    // --------------------------------------------------------------------

    protected void reInitDrawData(int newFrameWidth, int newFrameHeight) {
        occlusionBottom = new int[newFrameWidth];
        occlusionTop = new int[newFrameWidth];
        depth = new float[newFrameWidth * newFrameHeight];
    }

    protected void resetDrawData() {
        //transformedWalls.clear();
        //transformedSectors.clear();
        drawnPortals.clear();

        for (int i=0; i<frameWidth; i++) {
            occlusionBottom[i] = 0;
            occlusionTop[i] = frameHeight;
        }
        Arrays.fill(depth, Float.MAX_VALUE);

        sprites.clear();
        try {
            sprites.add(new FacingSprite(TEST_SPRITE_IMG, 10, 5, 0, 16, 64));
            sprites.add(new FacingSprite(TEST_SPRITE_IMG, 30, 5, 0, 16, 64));
            sprites.add(new FacingSprite(TEST_SPRITE_IMG, 25, 25, 0, 16, 64));
            sprites.add(new WallSprite(TEST_SPRITE_IMG, 30, 30, 0, 30, 60, 64));

        } catch (Exception e) {

        }
    }

    // ----------------------------------------------------------------------

    protected float getFogFactor(float dist) {
        if (!drawFog) return 0f;
        final float fogDistance = 600;
        return Math.max(0, Math.min(fogDistance, dist) ) / fogDistance;
    }

    protected boolean spanFilled(int spanStart, int spanEnd) {
        for (int i=spanStart; i<spanEnd; i++) {
            if (occlusionBottom[i] < occlusionTop[i]-1)
                return false;
        }
        return true;
    }

    protected Color grabColor(Pixmap tex, float u, float v) {
        u = u - (int)u;
        v = v - (int)v;
        return new Color(tex.getPixel( (int)(u*tex.getWidth()), (int)((1.f-v)*tex.getHeight()) ));
    }

    protected Color getCheckerboardColor(float u, float v) {
        boolean checker = ( (int)(u*8)%2 == (int)(v*8)%2 );
        return new Color(checker ? 0xB0_20_30_FF : 0xA0_A0_A0_FF);
    }

    protected void setPixel(int x, int y, Color color) {
        buffer.drawPixel(x, y, Color.rgba8888(color));
    }

    protected void drawDepthOverlay() {
        for (int i=0; i<depth.length; i++) {
//            i = 401  x = 1 y = 0
            int x = i/frameHeight;
            int y = i%frameHeight;
            Color col = new Color( buffer.getPixel(x, y) );
            col.lerp(Color.RED, depth[i]/500f);
            buffer.drawPixel(x, y, Color.rgba8888(col)) ;
        }
    }
}