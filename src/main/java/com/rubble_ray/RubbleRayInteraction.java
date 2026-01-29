package com.rubble_ray;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import javax.annotation.Nonnull;

public class RubbleRayInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<RubbleRayInteraction> CODEC =
            BuilderCodec.builder(
                    RubbleRayInteraction.class,
                    RubbleRayInteraction::new,
                    SimpleInstantInteraction.CODEC
            ).build();

    private static final HytaleLogger LOGGER =
            HytaleLogger.forEnclosingClass();

    // Configurable tunnel dimensions
    private final int width = 2;   // horizontal half-width
    private final int height = 3;  // vertical height
    private final int length = 40; // forward length

    private final boolean generateFloor = true;
    private final boolean generateRoof = true;
    private final boolean generateWalls = true;

    public RubbleRayInteraction() {}

    @Override
    protected void firstRun(
            @Nonnull InteractionType interactionType,
            @Nonnull InteractionContext interactionContext,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        var commandBuffer = interactionContext.getCommandBuffer();
        Ref<EntityStore> ref = interactionContext.getEntity();
        LivingEntity entity = (LivingEntity) EntityUtils.getEntity(ref, commandBuffer);

        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack itemStack = interactionContext.getHeldItem();
        if (itemStack == null) return;

        TransformComponent transform =
                commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        World world = commandBuffer.getExternalData().getWorld();

        // Start from player's eyes
        Vector3d startVec = transform.getPosition().add(0, 1.6, 0);

        Vector3d forward = getForwardVector(transform.getRotation()).normalize();

        // Determine dominant direction (X or Z) for tunnel
        int deltax, deltaz;
        if (Math.abs(forward.x) > Math.abs(forward.z)) {
            deltaz = 0;
            deltax = forward.x > 0 ? 1 : -1;
        } else {
            deltax = 0;
            deltaz = forward.z > 0 ? 1 : -1;
        }

        if (deltax == 0 && deltaz == 0) return;

        int baseY = (int) Math.floor(startVec.y);

        commandBuffer.run(store -> {
            for (int i = 0; i < length; i++) {           // forward
                for (int h = 0; h < height; h++) {      // vertical
                    for (int w = -width; w <= width; w++) { // horizontal
                        int x = (int) Math.floor(startVec.x) + i * deltax + w * deltaz;
                        int y = baseY + h;
                        int z = (int) Math.floor(startVec.z) + i * deltaz + w * deltax;

                        var blockType = world.getBlockType(x, y, z);
                        if (blockType != null && blockType != BlockType.EMPTY) {
                            world.breakBlock(x, y, z, 0);
                        }
                    }
                }
            }
        });

        relay(interactionContext, player, "Tunnel carved successfully", false);
    }

    private void relay(
            InteractionContext context,
            Player player,
            String message,
            boolean fail
    ) {
        LOGGER.atInfo().log("[RubbleRay] %s", message);
        if (player != null) {
            player.sendMessage(Message.raw("§6[RubbleRay] §f" + message));
        }
        if (fail) {
            context.getState().state = InteractionState.Failed;
        }
    }

    private Vector3d getForwardVector(Vector3f rotation) {
        float yawRad = (float) Math.toRadians(rotation.y);
        float pitchRad = (float) Math.toRadians(rotation.x);

        double x = Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vector3d(x, y, z);
    }
}
