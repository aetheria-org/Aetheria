package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

@Getter
@Setter
public class RenderEntityModelEvent extends AetheriaEvent {

    private EntityLivingBase entity;
    private float limbSwing;
    private float limbSwingAmount;
    private float ageInTicks;
    private float headYaw;
    private float headPitch;
    private float scaleFactor;
    private ModelBase model;

    public RenderEntityModelEvent(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scaleFactor, ModelBase model) {
        this.entity = entity;
        this.limbSwing = limbSwing;
        this.limbSwingAmount = limbSwingAmount;
        this.ageInTicks = ageInTicks;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.scaleFactor = scaleFactor;
        this.model = model;
    }
}