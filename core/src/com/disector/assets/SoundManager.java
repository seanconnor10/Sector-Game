package com.disector.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    public static Sound SFX_Clink;
    public static Sound SFX_Boom;

    public static Vector4 ear_pos = new Vector4();

    private static Map<Sound, Float> lengths = new HashMap<>();
    private static Array<PositionableSound> active_positionables = new Array<>();

    private static class PositionableSound {
        private Sound snd; //Reference to sound obj
        private long inst; //Sound instance ID
        private Vector3 pos;
        private float range;
        private float ageSeconds;
        private float lifeSpan;

        private PositionableSound(Sound snd, Vector3 pos, float range) {
            this.snd = snd;
            this.pos = pos;
            this.range = range;
            this.lifeSpan = lengths.get(snd);
            this.inst = snd.play();
        }
    }

    public static void init() {
        lengths.clear();

        SFX_Clink = initWav("glass_clink.wav", 2);
        SFX_Boom = initWav("boom.wav", 5);
    }

    public static void update(float dt) {
        for (int i=0; i<active_positionables.size; i++) {
            PositionableSound sound = active_positionables.get(i);
            sound.ageSeconds += dt;
            if (sound.ageSeconds > sound.lifeSpan) {
                active_positionables.removeIndex(i);
                i--;
                continue;
            }
            updatePan(sound.snd, sound.inst, sound.pos);
        }
    }

    public static void playStaticPosition(Sound snd, Vector3 snd_pos, float range) {
        //if (snd == null) return;
        float vol = Math.max(0, Math.max(0, 1.0f - (snd_pos.dst( xyz(ear_pos) ) / range) ));
        long inst = snd.play(vol);
        snd.setPitch( inst, (float)(0.5 + Math.random()) );
        updatePan(snd, inst, snd_pos);
    }

    public static void playPosition(Sound snd, Vector3 pos, float range) {
        PositionableSound newSound = new PositionableSound(snd, pos, range);
        active_positionables.add(newSound);
        updatePan(newSound.snd, newSound.inst, newSound.pos);
    }

    // ------------------------------------------------------------------------------------

    private static void updatePan(Sound snd, long inst, Vector3 snd_pos) {
        if (snd_pos == null) return;
        double t_x = snd_pos.x - ear_pos.x;
        double t_y = snd_pos.y - ear_pos.y;
        double temp = t_x;
        t_x = t_x*Math.cos(ear_pos.w) - t_y*Math.sin(ear_pos.w);
        t_y = t_y*Math.cos(ear_pos.w) - temp*Math.sin(ear_pos.w);
        double angle = Math.atan2(t_y, t_x);
        snd.setPan(inst, 0.5f - (float) Math.sin(angle), 0.5f + (float) Math.sin(angle));
    }

    private static Sound initWav(String path, float length) {
        FileHandle file = Gdx.files.local("assets/wav/" + path );
        if (!file.exists())
            System.out.println("Sound file missing: " + file.path());
        Sound newSound = Gdx.audio.newSound(file);
        lengths.put(newSound, length);
        return newSound;
    }

    private static Vector3 xyz(Vector4 v) {
        return new Vector3(v.x, v.y, v.z);
    }
}
