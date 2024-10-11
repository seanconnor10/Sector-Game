package com.disector.gameworld.components;

import com.disector.renderer.sprites.FacingSprite;

public interface HasFacingSprite extends GetSpriteInterface {
   @Override FacingSprite getInfo();
}