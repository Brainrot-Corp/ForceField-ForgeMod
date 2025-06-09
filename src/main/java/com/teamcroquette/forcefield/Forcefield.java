package com.teamcroquette.forcefield;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Forcefield.MODID)
public class Forcefield {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "forcefield";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String NBT_KEY_LAST_POS_X = "ForceField_LastPosX";
    public static final String NBT_KEY_LAST_POS_Y = "ForceField_LastPosY";
    public static final String NBT_KEY_LAST_POS_Z = "ForceField_LastPosZ";

    public static final String NBT_KEY_ACTIVE = "ForceField_Active";

    public Forcefield() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("ForceField is initialized!");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (!entity.acceptsFailure()) { return; }

        if (!(entity instanceof Player player)) {
            return;
        }
        player.addTag(NBT_KEY_ACTIVE);
        LOGGER.info("Setting NBT TAG for ForceField");
    }

    // --- 1. Prevent all Status Effects (Potions) from being applied ---
    @SubscribeEvent
    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();

        // Apply this only to players
        if (entity instanceof Player) {
            if (event.getEntity().getTags().contains(NBT_KEY_ACTIVE)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    // --- 2. Prevent all Status Effects (Potions) from being applied ---
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player) {
            if (event.getEntity().getTags().contains(NBT_KEY_ACTIVE)) {
                event.getEntity().clearFire();
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            CompoundTag playerData = player.getPersistentData();

            if (!player.getTags().contains(NBT_KEY_ACTIVE)) {return;}

            double currentX = player.getX();
            double currentY = player.getY();
            double currentZ = player.getZ();

            double lastX = playerData.contains(NBT_KEY_LAST_POS_X) ? playerData.getDouble(NBT_KEY_LAST_POS_X) : currentX;
            double lastY = playerData.contains(NBT_KEY_LAST_POS_Y) ? playerData.getDouble(NBT_KEY_LAST_POS_Y) : currentY;
            double lastZ = playerData.contains(NBT_KEY_LAST_POS_Z) ? playerData.getDouble(NBT_KEY_LAST_POS_Z) : currentZ;

            double movementThresholdSq = 0.001;

            if (player.distanceToSqr(lastX, lastY, lastZ) > movementThresholdSq) {
                // *** The player has moved! ***
                player.removeTag(NBT_KEY_ACTIVE);

                // 2. Clear all status effects the player currently has
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);

                // Clear the playerData
                playerData.remove(NBT_KEY_LAST_POS_X);
                playerData.remove(NBT_KEY_LAST_POS_Y);
                playerData.remove(NBT_KEY_LAST_POS_Z);
                return;
            }

            // Always update the player's last known position for the next tick's comparison.
            playerData.putDouble(NBT_KEY_LAST_POS_X, currentX);
            playerData.putDouble(NBT_KEY_LAST_POS_Y, currentY);
            playerData.putDouble(NBT_KEY_LAST_POS_Z, currentZ);
        }
    }
}