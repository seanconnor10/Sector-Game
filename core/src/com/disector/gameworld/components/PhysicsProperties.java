package com.disector.gameworld.components;

public class PhysicsProperties {
    public float elasticity;
    public float restitution;
    public float zElasticity;
    public float zFriction;
    public float zBounceLimit;

    public PhysicsProperties(float elasticity, float restitution, float zElasticity, float zFriction, float zBounceLimit) {
        this.elasticity = elasticity;
        this.restitution = restitution;
        this.zElasticity = zElasticity;
        this.zFriction = zFriction;
        this.zBounceLimit = zBounceLimit;
    }

    public PhysicsProperties() {
    }
}
