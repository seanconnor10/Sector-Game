package com.disector.gameworld.components;

public class PhysicsProperties {
    public float elasticity;
    public float restitution;
    public float zElasticity;
    public float zFriction;
    public float gravFactor = 1f;
    public float zBounceLimit;

    public PhysicsProperties(
        float elasticity,
        float restitution,
        float zElasticity,
        float zFriction,
        float zBounceLimit,
        float gravFactor
    ) {
        this.elasticity = elasticity;
        this.restitution = restitution;
        this.zElasticity = zElasticity;
        this.zFriction = zFriction;
        this.zBounceLimit = zBounceLimit;
        this.gravFactor = gravFactor;
    }

    public PhysicsProperties() {
    }
}
