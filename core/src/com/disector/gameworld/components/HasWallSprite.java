package com.disector.gameworld.components;

import com.disector.renderer.sprites.WallSprite;

public interface HasWallSprite extends GetSpriteInterface {
    @Override WallSprite getInfo();
}
