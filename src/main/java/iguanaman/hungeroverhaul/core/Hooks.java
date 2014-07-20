package iguanaman.hungeroverhaul.core;

import java.util.Random;

import iguanaman.hungeroverhaul.HungerOverhaul;
import iguanaman.hungeroverhaul.IguanaConfig;
import iguanaman.hungeroverhaul.api.FoodEvent;
import iguanaman.hungeroverhaul.api.FoodValues;
import mods.natura.blocks.crops.NetherBerryBush;
import mods.natura.blocks.trees.SaguaroBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockReed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;

public class Hooks
{
    /**
     * Hooks into ItemStack-aware FoodStats.addStats method
     * @param foodStats The food stats being added to
     * @param itemFood The item of food that is being eaten
     * @param itemStack The ItemStack of the food that is being eaten
     * @param player The player eating the food
     * @return The modified food values or null if the default code should be executed
     */
    public static FoodValues onFoodStatsAdded(FoodStats foodStats, ItemFood itemFood, ItemStack itemStack, EntityPlayer player)
    {
        return FoodValues.getPlayerSpecific(itemFood, itemStack, player);
    }

    public static void onPostFoodStatsAdded(FoodStats foodStats, ItemFood itemFood, ItemStack itemStack, FoodValues foodValues, int hungerAdded, float saturationAdded, EntityPlayer player)
    {
        MinecraftForge.EVENT_BUS.post(new FoodEvent.FoodEaten(player, itemFood, itemStack, foodValues, hungerAdded, saturationAdded));
    }

    public static int getModifiedFoodEatingSpeed(ItemFood itemFood, ItemStack itemStack)
    {
        return FoodValues.getModified(itemFood, itemStack).hunger * 8 + 8;
    }

    public static float getMaxExhaustion(EntityPlayer player)
    {
        EnumDifficulty difficulty = player.worldObj.difficultySetting;
        float hungerLossRate = 3f;
        if (IguanaConfig.difficultyScaling && IguanaConfig.difficultyScalingHunger)
        {
            if (difficulty == EnumDifficulty.PEACEFUL)
                hungerLossRate = 5F;
            else if (difficulty == EnumDifficulty.EASY)
                hungerLossRate = 4F;
        }

        return hungerLossRate / (IguanaConfig.hungerLossRatePercentage / 100F);
    }

    public static float getHealthRegenPeriod(EntityPlayer player)
    {
        float wellfedModifier = 1.0F;
        if (player.isPotionActive(HungerOverhaul.potionWellFed))
            wellfedModifier = 0.75F;

        EnumDifficulty difficulty = player.worldObj.difficultySetting;
        float difficultyModifierHealing = 1.0F;
        if (IguanaConfig.difficultyScaling && IguanaConfig.difficultyScalingHealing)
        {
            if (difficulty.getDifficultyId() <= EnumDifficulty.EASY.getDifficultyId())
                difficultyModifierHealing = 0.75F;
            else if (difficulty == EnumDifficulty.HARD)
                difficultyModifierHealing = 1.5F;
        }

        float lowHealthModifier = player.getMaxHealth() - player.getHealth();
        lowHealthModifier *= IguanaConfig.lowHealthRegenRateModifier / 100F;
        lowHealthModifier *= difficultyModifierHealing;
        lowHealthModifier = (float) Math.pow(lowHealthModifier + 1F, 1.5F);

        return 80.0F * difficultyModifierHealing * wellfedModifier * lowHealthModifier
                / (IguanaConfig.healthRegenRatePercentage / 100F);
    }

    // TODO: Abstract this logic out to a growth modifier/handler
    public static boolean shouldUpdateTick(Block block, World world, int x, int y, int z, Random rand)
    {
        if (block instanceof NetherBerryBush)
            return shouldNetherBerryBushUpdateTick(world, x, y, z, rand);
        else if (block instanceof SaguaroBlock)
            return shouldSaguaroUpdateTick(world, x, y, z, rand);
        else if (block instanceof BlockReed)
            return shouldReedUpdateTick(world, x, y, z, rand);

        int sunlightModifier = world.isDaytime() && world.canBlockSeeTheSky(x, y, z) ? 1 : IguanaConfig.noSunlightRegrowthMultiplier;
        if (sunlightModifier == 0)
            return false;

        // biome modifier
        int biomeModifier = IguanaConfig.wrongBiomeRegrowthMultiplier;
        try
        {
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(x, z);
            for (BiomeDictionary.Type biomeType : new BiomeDictionary.Type[]{BiomeDictionary.Type.FOREST, BiomeDictionary.Type.PLAINS})
                if (BiomeDictionary.isBiomeOfType(biome, biomeType))
                {
                    biomeModifier = 1;
                    break;
                }
        }
        catch (Exception var5)
        {
            biomeModifier = 1;
        }
        if (biomeModifier == 0)
            return false;

        if (rand.nextInt(IguanaConfig.cropRegrowthMultiplier * biomeModifier) != 0)
            return false;

        return true;
    }

    public static boolean shouldNetherBerryBushUpdateTick(World world, int x, int y, int z, Random rand)
    {
        // biome modifier
        int biomeModifier = IguanaConfig.wrongBiomeRegrowthMultiplier;
        try
        {
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(x, z);
            if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.NETHER))
                biomeModifier = 1;
        }
        catch (Exception var5)
        {
            biomeModifier = 1;
        }

        if (rand.nextInt(IguanaConfig.cropRegrowthMultiplier * biomeModifier) != 0)
            return false;

        return true;
    }

    public static boolean shouldSaguaroUpdateTick(World world, int x, int y, int z, Random rand)
    {
        // biome modifier
        int biomeModifier = IguanaConfig.wrongBiomeRegrowthMultiplier;
        try
        {
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(x, z);
            if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SANDY))
                biomeModifier = 1;
        }
        catch (Exception var5)
        {
            biomeModifier = 1;
        }

        if (rand.nextInt(IguanaConfig.cactusRegrowthMultiplier * biomeModifier) != 0)
            return false;

        return true;
    }

    public static boolean shouldReedUpdateTick(World world, int x, int y, int z, Random rand)
    {
        int sunlightModifier = world.isDaytime() && world.canBlockSeeTheSky(x, y, z) ? 1 : IguanaConfig.noSunlightRegrowthMultiplier;
        if (sunlightModifier == 0)
            return false;

        // biome modifier
        int biomeModifier = IguanaConfig.wrongBiomeRegrowthMultiplierSugarcane;
        try
        {
            BiomeGenBase biome = world.getWorldChunkManager().getBiomeGenAt(x, z);
            for (BiomeDictionary.Type biomeType : new BiomeDictionary.Type[]{BiomeDictionary.Type.JUNGLE, BiomeDictionary.Type.SWAMP})
            {
                if (BiomeDictionary.isBiomeOfType(biome, biomeType))
                {
                    biomeModifier = 1;
                    break;
                }
            }
        }
        catch (Exception var5)
        {
            biomeModifier = 1;
        }
        if (biomeModifier == 0)
            return false;

        if (rand.nextInt(IguanaConfig.sugarcaneRegrowthMultiplier * biomeModifier) != 0)
            return false;

        return true;
    }
}
