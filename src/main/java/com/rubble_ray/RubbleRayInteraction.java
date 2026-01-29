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

    /* ==============================
       TUNNEL CONFIG
       ============================== */

    private final int WIDTH  = 5;
    private final int HEIGHT = 5;
    private final int LENGTH = 64;

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

        if (!(entity instanceof Player player)) return;

        ItemStack stack = interactionContext.getHeldItem();
        if (stack == null) return;

        TransformComponent transform =
                commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        World world = commandBuffer.getExternalData().getWorld();

        /* ==============================
           POSITION
           ============================== */

        Vector3d start = transform.getPosition().add(0, 0.6, 0);

        /* ==============================
           FACING DIRECTION (CORRECT)
           ============================== */

        Vector3f axis = transform.getTransform().getAxisDirection().toVector3f();

        int deltax;
        int deltaz;

        if (Math.abs(axis.x) > Math.abs(axis.z)) {
            deltaz = 0;
            deltax = axis.x > 0 ? 1 : -1;
        } else {
            deltax = 0;
            deltaz = axis.z > 0 ? 1 : -1;
        }

        int baseX = (int) Math.floor(start.x);
        int baseY = (int) Math.floor(start.y);
        int baseZ = (int) Math.floor(start.z);

        /* ==============================
           TUNNEL EXECUTION
           ============================== */

        commandBuffer.run(store -> {

            for (int k = 0; k < LENGTH; k++) {
                for (int h = 0; h < HEIGHT; h++) {
                    for (int w = -WIDTH; w <= WIDTH; w++) {

                        int x = baseX + k * deltax + w * deltaz;
                        int y = baseY + h;
                        int z = baseZ + k * deltaz + w * deltax;

                        BlockType block = world.getBlockType(x, y, z);

                        if (block != null && block != BlockType.EMPTY && !block.getId().contains("Ore_")) {
                            world.breakBlock(x, y, z, 0);
                        }
                    }
                }
            }
        });

    }

    /* ==============================
       HELPERS
       ============================== */

    private void relay(
            InteractionContext context,
            Player player,
            String message,
            boolean fail
    ) {
        LOGGER.atInfo().log("[RubbleRay] %s", message);
        if (player != null) {
            player.sendMessage(Message.raw("[RubbleRay] " + message));
        }
        if (fail) {
            context.getState().state = InteractionState.Failed;
        }
    }
}