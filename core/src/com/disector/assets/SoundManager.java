package com.disector.assets;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class SoundManager {
    public static Sound SFX_Clink;

//    private static boolean InitHasFailed;

    Array<PositionableSound> active = new Array<>();

    public static void init() {
        SFX_Clink = initWav("glass_clink.wav");
    }

    private static class PositionableSound {
        private long snd_instance;
        private Vector3 pos;
        private float range;

        private PositionableSound(Vector3 pos, Sound snd) {
            this.pos = pos;
            this.range =
            this.snd_instance = snd.play();
        }
    }

    public static void playPosStatic(Sound snd, Vector3 snd_pos, Vector3 ear_pos, float ear_facing_angle, float range) {
        if (snd == null) return;
        float vol = Math.max(0, 1.0f - (snd_pos.dst(ear_pos) / range));

        snd.play(vol);
    }

    private static Sound initWav(String path) {
        FileHandle file = Gdx.files.local("assets/wav/" + path );
        if (!file.exists())
            System.out.println("Sound file missing: " + file.path());
        return Gdx.audio.newSound(file);
    }
}
