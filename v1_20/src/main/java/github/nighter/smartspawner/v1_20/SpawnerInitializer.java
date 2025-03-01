package github.nighter.smartspawner.v1_20;

import github.nighter.smartspawner.nms.SpawnerWrapper;
import java.util.Arrays;

public class SpawnerInitializer {
    public static void init() {
        SpawnerWrapper.SUPPORTED_MOBS = Arrays.asList(
                "BLAZE", "CAVE_SPIDER", "CHICKEN", "COW", "CREEPER",
                "DROWNED", "ENDERMAN", "EVOKER", "GHAST", "GLOW_SQUID", "GUARDIAN",
                "HOGLIN", "HUSK", "IRON_GOLEM", "MAGMA_CUBE", "MUSHROOM_COW", "PIG",
                "PIGLIN", "PIGLIN_BRUTE", "PILLAGER", "PUFFERFISH", "RABBIT", "RAVAGER",
                "SALMON", "SHEEP", "SHULKER", "SKELETON", "SLIME", "SPIDER", "SQUID",
                "STRAY", "STRIDER", "TROPICAL_FISH", "VINDICATOR", "WITCH",
                "WITHER_SKELETON", "ZOGLIN", "ZOMBIE", "ZOMBIE_VILLAGER", "ZOMBIFIED_PIGLIN"
        );
    }
}