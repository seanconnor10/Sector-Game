package com.disector.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import com.disector.Wall;
import com.disector.Sector;
import com.disector.Application;
import com.disector.Material;
import com.disector.assets.PixmapContainer;
import com.disector.editor.Editor;

import java.nio.ShortBuffer;
import java.util.Arrays;

public class EditingSoftwareRenderer extends SoftwareRenderer {

    /*
     * Allows the view to be clicked on. giving info about
     * which Wall or Sector Floor/Ceil was clicked On
     */

    private final Color highlightColorSector = new Color(Color.GOLDENROD);
    private final Color highlightColorWall = new Color(Color.TEAL);

    private final Color wallSelectedColor = new Color(Color.PINK);

    public int wallHighLightIndex = -1;
    public int sectorHighlightIndex = -1;
    public float highLightStrength = 0;

    private final Editor editor;

    public enum CLICK_TYPE {FLOOR, CEIL, WALL_MAIN, WALL_UPPER, WALL_LOWER}

    public class ClickInfo {
        public int index = -1;
        public CLICK_TYPE type;
    };

    public ClickInfo[] clickInfo = new ClickInfo[frameWidth*frameHeight];

    public EditingSoftwareRenderer(Application app, Editor editor) {
        super(app);
        this.editor = editor;
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
    protected void drawWall(int wInd, int currentSectorIndex, int spanStart, int spanEnd, float[] floorDistances, float[] ceilDistances) {
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

        Pixmap floorPixels, ceilPixels;
        boolean ceilIsSky = false;
        try {
            floorPixels = materials.get(currentSector.matFloor).tex[0];
            Material ceilMat = materials.get(currentSector.matCeil);
            ceilPixels = ceilMat.tex[0];
            ceilIsSky = ceilMat.isSky;
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            floorPixels = ERROR_TEXTURE[0];
            ceilPixels = ERROR_TEXTURE[0];
        }

        int texHeightMid, texHeightLow, texHeightUpper;
        texHeightMid = textures[0].getHeight();
        texHeightLow = texturesLow[0].getHeight();
        texHeightUpper = texturesHigh[0].getHeight();

        float lightMiddle, lightLower, lightUpper;
        if (fullBright) {
            lightMiddle = 1f;
            lightLower = 1f;
            lightUpper = 1f;
        } else {
            lightLower = w.lightLower;
            lightMiddle = w.light;
            lightUpper = w.lightUpper;
        }

        float secHeight = secCeilZ - secFloorZ;

        boolean wallSelected = editor.selection.getWalls().contains(w, true);

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

            float texU=0;
            int pixmap_ind=0;
            {
                texU = w.xOffset + u * (wallLength / (float)textures[0].getWidth() / w.xScale);
                float texX_PlusOne = uPlus1 * (wallLength / (float)textures[0].getWidth() / w.xScale);
                float pixWidth = (texX_PlusOne - texU);
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                tex = textures[pixmap_ind];
                texLower = tex;
                texUpper = tex;

                texU %= 1;
            }

            float texX_Lower=0, texX_Upper=0;
            if (isPortal){
                texX_Lower = w.Lower_xOffset + u * (wallLength / (float)texturesLow[0].getWidth() / w.Lower_xScale);
                float texX_PlusOne = uPlus1 * (wallLength / (float)texturesLow[0].getWidth() / w.Lower_xScale);
                float pixWidth = (texX_PlusOne - texX_Lower);
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                texLower = texturesLow[pixmap_ind];
                texX_Lower %= 1;

                texX_Upper = w.Upper_xOffset + u * (wallLength / (float)texturesLow[0].getWidth() / w.Upper_xScale);
                texX_PlusOne = uPlus1 * (wallLength / (float)texturesLow[0].getWidth() / w.Upper_xScale);
                pixWidth = (texX_PlusOne - texX_Upper);
                pixmap_ind = Math.max(0, Math.min(MAX_MIP_IND, Math.round(
                        pixWidth - (AGGRESSIVE_MIPMAPS ? 0 : 1)
                )));
                texUpper = texturesHigh[pixmap_ind];
                texX_Upper %= 1;
            }

            float deltaV = 1 / quadHeight;
            float v = (rasterBottom - quadBottom) / quadHeight;

            for (int drawY = rasterBottom; drawY < rasterTop; drawY++) { //Per Pixel draw loop

                if (isPortal && (v > lowerWallCutoffV && v < upperWallCutoffV) ) {
                    v += deltaV;
                    continue;
                }

                float selected_texU;
                float yOff, yScale, light;
                float texHeight;
                Pixmap pickedTex;

                CLICK_TYPE type;

                if (!isPortal) {
                    yOff = w.yOffset;
                    yScale = w.yScale;
                    selected_texU = texU;
                    light = lightMiddle;
                    texHeight = texHeightMid;
                    pickedTex = tex;
                    type = CLICK_TYPE.WALL_MAIN;
                } else if (v<lowerWallCutoffV) {
                    yOff = w.Lower_yOffset;
                    yScale = w.Lower_yScale;
                    selected_texU = texX_Lower;
                    light = lightLower;
                    texHeight = texHeightLow;
                    pickedTex = texLower;
                    type = CLICK_TYPE.WALL_LOWER;
                } else if (v<upperWallCutoffV) {
                    yOff = w.yOffset;
                    yScale = w.yScale;
                    selected_texU = texU;
                    light = lightMiddle;
                    texHeight = texHeightMid;
                    pickedTex = tex;
                    type = CLICK_TYPE.WALL_MAIN;
                } else {
                    yOff = w.Upper_yOffset;
                    yScale = w.Upper_yScale;
                    selected_texU = texX_Upper;
                    light = lightUpper;
                    texHeight = texHeightUpper;
                    pickedTex = texUpper;
                    type = CLICK_TYPE.WALL_UPPER;
                }

                float texV = ( yOff + v*secHeight/texHeight/yScale ) % 1;

                int drawColor = pickedTex.getPixel((int)(selected_texU* pickedTex.getWidth()), (int)(texV*texHeight));

                int r = drawColor >> 24 & 0xFF;
                int g = drawColor >> 16 & 0xFF;
                int b = drawColor >> 8  & 0xFF;

                r *= light;
                g *= light;
                b *= light;

                r = (int) ( r + fog * (fogR_int - r) );
                g = (int) ( g + fog * (fogG_int - g) );
                b = (int) ( b + fog * (fogB_int - b) );

                //Draw Highlight
                if (wallHighLightIndex == wInd) {
                    Color c = highlightColorWall;
                    float f = highLightStrength;
                    float r2 = r / 255f;
                    float g2 = g / 255f;
                    float b2 = b / 255f;
                    r2 = r2 + (c.r - r2) * f;
                    g2 = g2 + (c.g - g2) * f;
                    b2 = b2 + (c.b - b2) * f;
                    r = (int)(r2*255f) & 0xFF;
                    g = (int)(g2*255f) & 0xFF;
                    b = (int)(b2*255f) & 0xFF;
                }
                if (wallSelected) {
                    Color c = wallSelectedColor;
                    final float f = 0.75f;
                    float r2 = r / 255f;
                    float g2 = g / 255f;
                    float b2 = b / 255f;
                    r2 = r2 + (c.r - r2) * f;
                    g2 = g2 + (c.g - g2) * f;
                    b2 = b2 + (c.b - b2) * f;
                    r = (int)(r2*255f) & 0xFF;
                    g = (int)(g2*255f) & 0xFF;
                    b = (int)(b2*255f) & 0xFF;
                }

                //8BitInt RGB to 4Bit
                short drawColor4bit = (short) (
                        (b >> 4 & 0xF) << 12 |
                                0xF            <<  8 |
                                (r >> 4 & 0xF) <<  4 |
                                (g >> 4 & 0xF)
                );

                ShortBuffer ints = buffer.getPixels().asShortBuffer();
                ints.put(drawX + drawY*frameWidth, drawColor4bit);

                ClickInfo info = getClickInfo(drawX, drawY);
                info.index = wInd;
                info.type  = type;

                v += deltaV;

            } //End Per Pixel Loop

            //Floor and Ceiling
            if (occlusionBottom[drawX] < quadBottom && camZ > currentSector.floorZ)
                drawFloor(floorPixels, drawX, fov, rasterBottom, secFloorZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightFloor, currentSectorIndex);

            if (occlusionTop[drawX] > rasterTop && camZ < currentSector.ceilZ)
                drawCeiling(ceilPixels, ceilIsSky, drawX, fov, rasterTop, secCeilZ, playerSin, playerCos, fullBright ? 1.f : currentSector.lightCeil, currentSectorIndex);

            //Update Occlusion Matrix
            updateOcclusion(isPortal, drawX, quadTop, quadBottom, quadHeight, upperWallCutoffV, lowerWallCutoffV);

        } //End Per Column Loop

        //Render Through Portal
        if (isPortal) {
            drawSector(portalDestIndex, leftEdgeX, rightEdgeX);
            drawnPortals.pop();
        }

    }

    protected void drawFloor(Pixmap pix, int drawX, float fov, int rasterBottom, float secFloorZ, float playerSin, float playerCos, float light, int secInd) {
        final float scaleFactor = 32.f;
        float floorXOffset = camX/scaleFactor, floorYOffset = camY/scaleFactor;
        int vOffset = (int) camVLook;

        if (occlusionBottom[drawX] < rasterBottom) {
            float heightOffset = (camZ - secFloorZ) / scaleFactor;
            int floorEndScreenY = Math.min(rasterBottom, occlusionTop[drawX]);
            ShortBuffer shortBuffer = buffer.getPixels().asShortBuffer();

            final int max_pix_ind = frameHeight * frameWidth - 1;

            for (int drawY = occlusionBottom[drawX] + vOffset; drawY<=floorEndScreenY + vOffset; drawY++) {
                float floorX = heightOffset * (drawX-halfWidth) / (drawY-halfHeight);
                float floorY = heightOffset * fov / (drawY-halfHeight);

                float rotFloorX = Math.abs( floorX*playerSin - floorY*playerCos + floorXOffset );
                float rotFloorY = Math.abs( floorX*playerCos + floorY*playerSin + floorYOffset );

                rotFloorX /= 2;
                rotFloorY /= 2;

                rotFloorX = rotFloorX%1;
                rotFloorY = rotFloorY%1;

                //float horizonScreenDistVert = halfHeight - drawY;
                //float angleOfScreenRow = (float) Math.atan(horizonScreenDistVert / fov);
                //float dist = (camZ - secFloorZ) / (float) Math.sin(angleOfScreenRow);

                int drawColor = pix.getPixel( (int)(rotFloorX*pix.getWidth()), (int)((1.f-rotFloorY)*pix.getHeight()) );

                int r_8 = drawColor >> 24 & 0xFF;
                int g_8 = drawColor >> 16 & 0xFF;
                int b_8 = drawColor >> 8 & 0xFF;

                r_8 *= light;
                g_8 *= light;
                b_8 *= light;

                if (sectorHighlightIndex == secInd) {
                    Color c = highlightColorSector;
                    float f = highLightStrength;
                    float r2 = r_8 / 255f;
                    float g2 = g_8 / 255f;
                    float b2 = b_8 / 255f;
                    r2 = r2 + (c.r - r2) * f;
                    g2 = g2 + (c.g - g2) * f;
                    b2 = b2 + (c.b - b2) * f;
                    r_8 = (int)(r2*255f) & 0xFF;
                    g_8 = (int)(g2*255f) & 0xFF;
                    b_8 = (int)(b2*255f) & 0xFF;
                }

                short drawColor4bit = (short) (//Convert from RGBA8888 to RGBA4444
                        (b_8 >> 4 & 0xF) << 12 |
                                0xF              <<  8 |
                                (r_8 >> 4 & 0xF) <<  4 |
                                (g_8 >> 4 & 0xF)
                );

                int i = /*Math.clamp( */drawX + (drawY-vOffset)*frameWidth;//, 0, -1+frameWidth*frameHeight);
                if (i<0) i=0;
                if (i>max_pix_ind) i = max_pix_ind;
                shortBuffer.put(i, drawColor4bit);

                ClickInfo info = getClickInfo(drawX, drawY - vOffset);
                info.index = secInd;
                info.type  = CLICK_TYPE.CEIL;

            }
        }
    }

    protected void drawCeiling(Pixmap tex, boolean isSky, int drawX, float fov, int rasterTop, float secCeilZ, float playerSin, float playerCos, float light, int secInd) {

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

        ShortBuffer shortBuffer = buffer.getPixels().asShortBuffer();

        for (int drawY = Math.max(rasterTop, occlusionBottom[drawX]) + vOffset; drawY <= ceilEndScreenY; drawY++) {

            if (!isSky) {

                float ceilX = heightOffset * (drawX - halfWidth) / (drawY - halfHeight);
                float ceilY = heightOffset * fov / (drawY - halfHeight);

                float rotX = Math.abs( ceilX * playerSin - ceilY * playerCos - floorXOffset );
                float rotY = Math.abs( ceilX * playerCos + ceilY * playerSin - floorYOffset );

                rotX /= 2f;
                rotY /= 2f;

                rotX = rotX % 1;
                rotY = rotY % 1;

                //float horizonScreenDistVert = -halfHeight + drawY;
                //float angleOfScreenRow = (float) Math.atan(horizonScreenDistVert / fov);
                //float dist = (secCeilZ - camZ) / (float) Math.sin(angleOfScreenRow);

                int drawColor = tex.getPixel((int)(rotX*tex.getWidth()), (int)(rotY*tex.getHeight()));

                int r = drawColor >> 24 & 0xFF;
                int g = drawColor >> 16 & 0xFF;
                int b = drawColor >> 8 & 0xFF;

                r *= light;
                g *= light;
                b *= light;

                if (sectorHighlightIndex == secInd) {
                    Color c = highlightColorSector;
                    float f = highLightStrength;
                    float r2 = r / 255f;
                    float g2 = g / 255f;
                    float b2 = b / 255f;
                    r2 = r2 + (c.r - r2) * f;
                    g2 = g2 + (c.g - g2) * f;
                    b2 = b2 + (c.b - b2) * f;
                    r = (int)(r2*255f) & 0xFF;
                    g = (int)(g2*255f) & 0xFF;
                    b = (int)(b2*255f) & 0xFF;
                }

                short drawColor4bit = (short) (//Convert from RGBA8888 to RGBA4444
                        (b >> 4 & 0xF) << 12 |
                                0xF            <<  8 |
                                (r >> 4 & 0xF) <<  4 |
                                (g >> 4 & 0xF)
                );

                int i = Math.clamp( drawX + (drawY-vOffset)*frameWidth, 0, -1+frameWidth*frameHeight);
                shortBuffer.put(i, drawColor4bit);

            } else { //If isSky

                int drawColor = tex.getPixel(
                        (int)((centerScreenSkyU - (drawX-halfWidth)*portionImgToDraw/frameWidth)*tex.getWidth()),
                        (int)((1.f - (drawY/(float)tex.getHeight()))*tex.getHeight())
                );

                int r = drawColor >> 24 & 0xFF;
                int g = drawColor >> 16 & 0xFF;
                int b = drawColor >> 8 & 0xFF;

                short drawColor4bit = (short) (//Convert from RGBA8888 to RGBA4444
                        (b >> 4 & 0xF) << 12 |
                                0xF            <<  8 |
                                (r >> 4 & 0xF) <<  4 |
                                (g >> 4 & 0xF)
                );

                int i = Math.clamp( drawX + (drawY-vOffset)*frameWidth, 0, -1+frameWidth*frameHeight);
                shortBuffer.put(i, drawColor4bit);
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