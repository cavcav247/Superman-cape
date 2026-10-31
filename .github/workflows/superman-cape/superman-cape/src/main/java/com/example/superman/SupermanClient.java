package com.example.superman;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class SupermanClient implements ClientModInitializer {
    /** Speed in blocks per tick (1.2 = 24 blocks/sec). */
    private static final double SPEED = 1.2;

    private static KeyMapping toggleKey;

    /** X pressed: next Space press starts flying. */
    private boolean armed = false;
    private boolean flying = false;
    private int groundTicks = 0;
    private int landingGrace = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.supermancape.fly",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            armed = false;
            flying = false;
            return;
        }

        while (toggleKey.consumeClick()) {
            armed = !armed;
            if (!armed) {
                stopFlying();
            }
            player.displayClientMessage(Component.literal(armed
                    ? "Superman: ARMED - press Space to fly"
                    : "Superman: OFF"), true);
        }

        if (armed && !flying && mc.options.keyJump.isDown() && !player.onGround()) {
            flying = true;
            groundTicks = 0;
        }
        // Start from the ground too: jump, then Space again in the air is enough,
        // but allow takeoff straight from the ground with a hop.
        if (armed && !flying && mc.options.keyJump.isDown() && player.onGround()) {
            player.jumpFromGround();
            flying = true;
            groundTicks = -5;
        }

        if (flying) {
            if (player.isPassenger() || player.isInWater() || player.isInLava()
                    || mc.options.keyShift.isDown()) {
                stopFlying();
            } else {
                Vec3 look = player.getLookAngle();
                double speed = mc.options.keySprint.isDown() ? SPEED * 1.8 : SPEED;
                player.setDeltaMovement(look.scale(speed));
                player.setPose(Pose.SWIMMING);
                player.resetFallDistance();
                resetServerFall(mc, player);

                if (player.onGround()) {
                    groundTicks++;
                    if (groundTicks > 3) {
                        stopFlying();
                    }
                } else if (groundTicks > 0) {
                    groundTicks = 0;
                }
            }
        }

        if (landingGrace > 0) {
            landingGrace--;
            player.resetFallDistance();
            resetServerFall(mc, player);
        }
    }

    private void stopFlying() {
        if (flying) {
            flying = false;
            landingGrace = 40;
        }
    }

    /** In singleplayer, the integrated server computes fall damage itself, so reset it there too. */
    private static void resetServerFall(Minecraft mc, LocalPlayer player) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(player.getUUID());
            if (sp != null) {
                sp.resetFallDistance();
            }
        });
    }
}
