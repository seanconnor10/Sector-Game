package com.disector.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import com.disector.Wall;
import com.disector.Sector;
import com.disector.Application;
import com.disector.Material;
import com.disector.assets.PixmapContainer;

public class EditingSoftwareRenderer extends SoftwareRenderer {

    /*
     * Allows the view to be clicked on. giving info about
     * which Wall or Sector Floor/Ceil was clicked On
     */

    private final Color highlightColorSector = new Color(0x10_D0_40_FF);
    private final Color highlightColorWall = new Color(0xD0_10_40_FF);

    public int wallHighLightIndex = -1;
    public int sectorHighlightIndex = -1;
    public float highLightStrength = 0;


    public enum CLICK_TYPE {FLOOR, CEIL, WALL_MAIN, WALL_UPPER, WALL_LOWER}

    public class ClickInfo {
        public int index = -1;
        public CLICK_TYPE type;

    };


    public ClickInfo[] clickInfo = new ClickInfo[frameWidth*frameHeight];

    public EditingSoftwareRenderer(Application app) {
        super(app);
    }


    @Override
    public void resizeFrame(int w, int h) {
        clickInfo = new ClickInfo[w*h];

        for (int i=0; i<clickInfo.length; i++) {
            clickInfo[i] = new ClickInfo();
        }

        super.resizeFrame(w, h);
    }

    public ClickInfo getClickInfo(int x, int y) {
        int loc = x + (y*frameWidth);
        if (loc < 0) loc = 0;
        if (loc >= clickInfo.length) loc = clickInfo.length - 1;
        return clickInfo[loc];
    }

    public int getWidth() {
        return frameWidth;
    }

    public int getHeight() {
        return frameHeight;
    }


    // --------------------------------------------------------------------------------------

    @Override
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

        float leftEdgePrecise = Math.max(0, Math.min(p2_plotX,p1_plotX) );
        float rightEdgePrecise = Math.min( Math.max(p2_plotX,p1_plotX), frameWidth-1);

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

            try {
                texturesLow = materials.get(w.matLower).tex;
            } catch (Exception e) {
                texturesLow = ERROR_TEXTURE;
            }

            try {
                texturesHigh = materials.get(w.matUpper).tex;
            } catch (Exception e) {
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

            float fog;
            {
                float screenXProgress = (drawX-leftEdgePrecise) / (rightEdgePrecise-leftEdgePrecise);
                fog = getFogFactor(x1 + Math.max(0, Math.min(1.0f, screenXProgress))*(x2-x1) );
            }

            float light = fullBright ? 1.0f : w.light;

            rasterBottom = Math.max( (int) quadBottom, occlusionBottom[drawX]);
            rasterTop = Math.min( (int) quadTop, occlusionTop[drawX]);

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


                float pixU;

                float yOff, yScale;
                if (!isPortal) {
                    yOff = w.yOffset;
                    yScale = w.yScale;
                    pixU = texX;
                } else if (v<lowerWallCutoffV) {
                    yOff = w.Lower_yOffset;
                    yScale = w.Lower_yScale;
                    pixU = texX_Lower;
                } else if (v<upperWallCutoffV) {
                    //Middle of Portal
                    continue;
                    /*yOff = w.yOffset;
                    yScale = w.yScale;
                    pixU = texX;*/
                } else {
                    yOff = w.Upper_yOffset;
                    yScale = w.Upper_yScale;
                    pixU = texX_Upper;
                }

                float tempYOff = yOff < 0 ? 1.f - Math.abs(yOff) % 1.f : yOff;
                float texV = (tempYOff + v * yScale) % 1.0f;

                Color drawColor;
                CLICK_TYPE type;

                if (!w.isPortal) {
                    drawColor = grabColor(tex, pixU, texV);
                    type = CLICK_TYPE.WALL_MAIN;
                } else if (v <= lowerWallCutoffV) {
                    drawColor = grabColor(texLower, pixU, texV);
                    type = CLICK_TYPE.WALL_LOWER;
                } else if (v <= upperWallCutoffV){
                    type = CLICK_TYPE.WALL_MAIN;
                    drawColor = grabColor(tex, pixU, texV);
                } else {
                    type = CLICK_TYPE.WALL_UPPER;
                    drawColor = grabColor(texUpper, pixU, texV);
                }

                drawColor.lerp(depthFogColor,fog);
                drawColor.lerp(darkColor, 1.f - light);

                if (wallHighLightIndex == wInd) {
                    drawColor.lerp(highlightColorWall, highLightStrength);
                }

                setPixel(drawX, drawY, drawColor);

                ClickInfo info = getClickInfo(drawX, drawY);
                info.index = wInd;
                info.type = type;

            } //End Per Pixel Loop

            //Floor and Ceiling
            if (occlusionBottom[drawX] < quadBottom && camZ > currentSector.floorZ)
                drawFloor(w, currentSector.matFloor, drawX, fov, rasterBottom, secFloorZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightFloor, currentSectorIndex);

            if (occlusionTop[drawX] > rasterTop && camZ < currentSector.ceilZ)
                drawCeiling(w, currentSector.matCeil, drawX, fov, rasterTop, secCeilZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightCeil, currentSectorIndex);

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


    protected void drawFloor(Wall w, int texInd, int drawX, float fov, int rasterBottom, float secFloorZ, float playerSin, float playerCos, float light, int secInd) {
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

                if (sectorHighlightIndex == secInd) {
                    drawColor.lerp(highlightColorSector, highLightStrength);
                }

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );

                ClickInfo info = getClickInfo(drawX, drawY - vOffset);
                info.index = secInd;
                info.type = CLICK_TYPE.FLOOR;

            }
        }
    }

    protected void drawCeiling(Wall w, int texInd, int drawX, float fov, int rasterTop, float secCeilZ, float playerSin, float playerCos, float light, int secInd) {
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

                if (sectorHighlightIndex == secInd) {
                    drawColor.lerp(highlightColorSector, highLightStrength);
                }

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );


            } else { //If isSky
                Color drawColor = grabColor(tex, centerScreenSkyU - (drawX-halfWidth)*portionImgToDraw/frameWidth, drawY/(float)tex.getHeight());

                if (sectorHighlightIndex == secInd) {
                    drawColor.lerp(highlightColorSector, highLightStrength);
                }

                buffer.drawPixel(drawX, drawY - vOffset, Color.rgba8888(drawColor) );
            }

            ClickInfo info = getClickInfo(drawX, drawY - vOffset);
            info.index = secInd;
            info.type  = CLICK_TYPE.CEIL;

        }

    }

    @Override
    protected void resetDrawData() {
        super.resetDrawData();

        for (ClickInfo item : clickInfo)
            item.index = -1;

    }

}