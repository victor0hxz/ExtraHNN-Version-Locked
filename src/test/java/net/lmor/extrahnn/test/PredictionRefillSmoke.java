package net.lmor.extrahnn.test;

import net.lmor.extrahnn.*;
import net.lmor.extrahnn.common.tile.*;
import net.lmor.extrahnn.common.item.ExtraDataModelItem;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@EventBusSubscriber(modid="extrahnn")
public class PredictionRefillSmoke {
    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        ExtraHostileNetworks.LOGGER.info("REFILL PASS: {}", label);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("port.refillSmoke")) return;
        var level=event.getServer().overworld();
        try {
            var holders=DataModelRegistry.INSTANCE.getKeys().stream().sorted()
                .map(DataModelRegistry.INSTANCE::holder)
                .filter(h->h.isBound() && h.get().input().test(new ItemStack(Hostile.Items.PREDICTION_MATRIX)))
                .limit(4).toList();
            require(holders.size()==4,"four compatible models loaded");
            var combined=new ItemStack(ExtraHostile.Items.EXTRA_DATA_MODEL);
            ExtraDataModelItem.setStoredModels(combined,holders);
            ExtraDataModelItem.setData(combined,Integer.MAX_VALUE/2);
            for(int tier=1;tier<=4;tier++) {
                var pos=new BlockPos(tier*4,110,0);
                var block=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("extrahnn","ultimate_sim_chamber_v"+tier));
                level.removeBlock(pos, false);
                level.setBlockAndUpdate(pos,block.defaultBlockState());
                var sim=(UltimateSimChamberTileEntity)level.getBlockEntity(pos);
                sim.getInventory().setStackInSlot(0,combined.copy());
                sim.getInventory().setStackInSlot(1,new ItemStack(Hostile.Items.PREDICTION_MATRIX,64));
                sim.getEnergy().setEnergy(sim.CAP);
                sim.serverTick(level,pos,sim.getBlockState());
                int batch=4*(tier==1?1:1<<tier);
                require(sim.getRuntime()==sim.DURATION,"tier "+tier+" starts one cycle");
                require(sim.getInventory().getStackInSlot(1).getCount()==64-batch,"tier "+tier+" charges input once");
                for(int i=0;i<sim.DURATION/3;i++)sim.serverTick(level,pos,sim.getBlockState());
                int remaining=sim.getRuntime();
                int before=sim.getInventory().getStackInSlot(1).getCount();
                var originalModel=sim.getInventory().getStackInSlot(0);
                try(var tx=Transaction.openRoot()) {
                    require(sim.getInventory().insert(1,ItemResource.of(Hostile.Items.PREDICTION_MATRIX),batch,tx)==batch,
                        "tier "+tier+" previews a refill");
                }
                require(sim.getInventory().getStackInSlot(0)==originalModel,"tier "+tier+" rollback preserves untouched model identity");
                require(sim.getInventory().getStackInSlot(1).getCount()==before,"tier "+tier+" abort restores input");
                sim.serverTick(level,pos,sim.getBlockState());
                require(sim.getRuntime()==remaining-1,"tier "+tier+" rollback does not restart paid cycle");
                require(sim.getInventory().getStackInSlot(1).getCount()==before,"tier "+tier+" rollback does not double-consume");
                remaining=sim.getRuntime();
                try(var tx=Transaction.openRoot()) {
                    require(sim.getInventory().insert(1,ItemResource.of(Hostile.Items.PREDICTION_MATRIX),batch,tx)==batch,
                        "tier "+tier+" commits refill during cycle");tx.commit();
                }
                sim.serverTick(level,pos,sim.getBlockState());
                require(sim.getRuntime()==remaining-1,"tier "+tier+" committed refill keeps progress");
                require(sim.getInventory().getStackInSlot(1).getCount()==64,"tier "+tier+" new inputs await next cycle");
                remaining=sim.getRuntime();
                sim.getInventory().setStackInSlot(0,sim.getInventory().getStackInSlot(0).copy());
                sim.serverTick(level,pos,sim.getBlockState());
                require(sim.getRuntime()==remaining-1,"tier "+tier+" equivalent copied model keeps progress");
                remaining=sim.getRuntime();
                for(int i=0;i<remaining;i++)sim.serverTick(level,pos,sim.getBlockState());
                require(sim.getRuntime()==0,"tier "+tier+" finishes its original cycle");
                int outputs=0;for(int slot=2;slot<sim.getInventory().getSlots();slot++)outputs+=sim.getInventory().getStackInSlot(slot).getCount();
                require(outputs>=batch,"tier "+tier+" delivers expected simulation loot");
                require(sim.getInventory().getStackInSlot(1).getCount()==64,"tier "+tier+" finishing does not consume refills");
            }
            ExtraHostileNetworks.LOGGER.info("REFILL FUNCTIONAL COMPLETE");
        }catch(Throwable error){ExtraHostileNetworks.LOGGER.error("REFILL FUNCTIONAL FAILED",error);}
        finally {event.getServer().halt(false);}
    }
}
