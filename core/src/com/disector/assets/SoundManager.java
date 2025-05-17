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
    public static Sound SFX_LampSpeech;
    public static Sound SFX_Mechanical;
    public static Sound SFX_MechanicalThud;
    public static Sound SFX_DoorLatch;
    public static Sound SFX_MetalDoorThud;

    public static Vector4 ear_pos = new Vector4();

    private static Map<Sound, Float> lengths = new HashMap<>();
    private static Array<PositionableSound> active_positionables = new Array<>();
    private static Array<PositionableSound> active_looping_positionables = new Array<>();

    public static class PositionableSound {
        private Sound snd; //Reference to sound obj
        private long inst; //Sound instance ID
        private Vector3 pos;
        private float range;
        private float ageSeconds;
        private float lifeSpan;

        private PositionableSound(Sound snd, Vector3 pos, float range, boolean loop) {
            this.snd = snd;
            this.pos = pos;
            this.range = range;
            this.lifeSpan = lengths.get(snd);
            this.inst = loop ? snd.loop() : snd.play();
        }
    }

    public static void init() {
        lengths.clear();

        SFX_Clink = initWav("glass_clink.wav", 2);
        SFX_Boom = initWav("boom.wav", 5);
        SFX_LampSpeech = initWav("lamp.wav", 10);
        SFX_Mechanical = initWav("mechanical.wav", 14);
        SFX_MechanicalThud = initWav("mechanical_thud.wav", 2);
        SFX_DoorLatch = initWav("latch_open.wav", 1);
        SFX_MetalDoorThud = initWav("metal_door_close.wav", 2);
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
            updatePan(sound.snd, sound.inst, sound.pos, sound.range);
        }

        for (int i=0; i<active_looping_positionables.size; i++) {
            PositionableSound sound = active_looping_positionables.get(i);
            updatePan(sound.snd, sound.inst, sound.pos, sound.range);
        }
    }

    public static void playStaticPosition(Sound snd, Vector3 snd_pos, float range) {
        //if (snd == null) return;
        float vol = Math.max(0, Math.max(0, 1.0f - (snd_pos.dst( xyz(ear_pos) ) / range) ));
        long inst = snd.play(vol);
        snd.setPitch( inst, (float)(0.8f + Math.random()*0.4f) );
        updatePan(snd, inst, snd_pos, range);
    }

    public static void playPosition(Sound snd, Vector3 pos, float range) {
        PositionableSound newSound = new PositionableSound(snd, pos, range, false);
        active_positionables.add(newSound);
        updatePan(newSound.snd, newSound.inst, newSound.pos, range);
    }

    public static PositionableSound loopPosition(Sound snd, Vector3 pos, float range) {
        PositionableSound newSound = new PositionableSound(snd, pos, range, true);
        active_looping_positionables.add(newSound);
        updatePan(newSound.snd, newSound.inst, newSound.pos, range);
        return newSound;
    }

    public static void killLoopingPositionable(PositionableSound reference) {
        reference.snd.stop();
        active_looping_positionables.removeValue(reference, true);
    }

    public static void killLoops() {
        for (PositionableSound snd : active_looping_positionables) {
            killLoopingPositionable(snd);
        }
    }

    // ------------------------------------------------------------------------------------

    private static void updatePan(Sound snd, long inst, Vector3 snd_pos, float range) {
        if (snd_pos == null) return;
        double t_x = snd_pos.x - ear_pos.x;
        double t_y = snd_pos.y - ear_pos.y;

        //double temp = t_x;
        //t_x = t_x*Math.cos(ear_pos.w) - t_y*Math.sin(ear_pos.w);
        //t_y = t_y*Math.cos(ear_pos.w) - temp*Math.sin(ear_pos.w);
        //double angle = Math.atan2(t_y, t_x);
        //snd.setPan(inst, 0.5f - (float) Math.sin(angle), 0.5f + (float) Math.sin(angle));

        //Pan didn't work just volume right now..
        float dist = (float) Math.sqrt(t_x*t_x + t_y*t_y);
        float newVol = 1.f - Math.min(1f, dist/range);
        snd.setVolume(inst, newVol);
        System.out.println("Volume:" + newVol);
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
