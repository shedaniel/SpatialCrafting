package fudge.spatialcrafting.debug;

import fudge.spatialcrafting.SpatialCrafting;
import fudge.spatialcrafting.common.data.CraftersData;
import fudge.spatialcrafting.common.data.SharedData;
import fudge.spatialcrafting.common.data.WorldSavedDataCrafters;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber
public final class Debug {

    private Debug() {}

    public static String getAllCrafterData(World world) {
        List<SharedData> allData = WorldSavedDataCrafters.getAllData(world);

        StringBuilder string = new StringBuilder("\n");


        allData.forEach(dataG -> {
            CraftersData data = (CraftersData) dataG;
            String worldType = world.isRemote ? "CLIENT" : "SERVER";
            string.append(String.format("%n**** Data for crafter multiblock at masterPos = %s in %s %s****%n",
                    data.getMasterPos(),
                    worldType,
                    world));
            string.append(String.format("Craft End Time = %s %n", data.getCraftTime() != 0 ? data.getCraftTime() : "not crafting"));
            string.append(String.format("Recipe = %s %n", data.getRecipe() != null ? data.getRecipe() : "No help recipe active"));
            string.append(String.format("Energy = %d %n", data.getEnergyHandler().getEnergyStored()));

        });

        return string.toString();
    }


    @SubscribeEvent
    public static void debugOnWorldLoad(WorldEvent.Load event) {
        if (SpatialCrafting.isDebugActive() && !event.getWorld().isRemote) {
            String debugInfo = getAllCrafterData(event.getWorld());
            if (!debugInfo.equals("")) {
                SpatialCrafting.LOGGER.info(getAllCrafterData(event.getWorld()));
            }
        }
    }


}
