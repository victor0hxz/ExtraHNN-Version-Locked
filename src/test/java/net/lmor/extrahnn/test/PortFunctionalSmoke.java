package net.lmor.extrahnn.test;
import net.lmor.extrahnn.*;
import net.lmor.extrahnn.common.tile.*;
import net.lmor.extrahnn.common.item.ExtraDataModelItem;
import net.lmor.extrahnn.data.*;
import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.*;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
@EventBusSubscriber(modid="extrahnn")
public class PortFunctionalSmoke {
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);ExtraHostileNetworks.LOGGER.info("PORT PASS: {}",message);}
 @SubscribeEvent public static void run(ServerStartedEvent event){
  if(!Boolean.getBoolean("port.smoke"))return;
  var level=event.getServer().overworld();
  try{
   var holders=DataModelRegistry.INSTANCE.getKeys().stream().sorted().map(DataModelRegistry.INSTANCE::holder).filter(h->h.isBound() && h.get().input().test(new ItemStack(Hostile.Items.PREDICTION_MATRIX))).limit(4).toList();
   require(holders.size()==4,"four compatible models loaded");
   var combined=new ItemStack(ExtraHostile.Items.EXTRA_DATA_MODEL);ExtraDataModelItem.setStoredModels(combined,holders);ExtraDataModelItem.setData(combined,Integer.MAX_VALUE/2);
   require(new ExtraDataModelInstance(combined).isValid(),"combined model valid");
   BlockPos mergePos=new BlockPos(0,100,0);level.setBlockAndUpdate(mergePos,ExtraHostile.Blocks.MERGER_CAMERA.value().defaultBlockState());
   var merger=(MergerCameraTileEntity)level.getBlockEntity(mergePos);
   for(int i=0;i<4;i++){var model=new ItemStack(Hostile.Items.DATA_MODEL);DataModelItem.setStoredModel(model,holders.get(i));DataModelItem.setData(model,Integer.MAX_VALUE/2);merger.getInventory().setStackInSlot(i,model);}
   try(var tx=Transaction.openRoot()){require(merger.getInventory().insert(4,net.neoforged.neoforge.transfer.item.ItemResource.of(ExtraHostile.Items.BLANK_EXTRA_DATA_MODEL),1,tx)==1,"merger accepts blank model by modern capability");tx.commit();}
   merger.getEnergy().setEnergy(ExtraHostileConfig.mergerCameraPowerCap);
   for(int tick=0;tick<=ExtraHostileConfig.mergerCameraPowerDuration;tick++)merger.serverTick(level,mergePos,merger.getBlockState());
   require(new ExtraDataModelInstance(merger.getInventory().getStackInSlot(5)).isValid(),"merger creates combined model");
   for(int tier=1;tier<=4;tier++){
    BlockPos pos=new BlockPos(tier*4,100,0);var block=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("extrahnn","ultimate_sim_chamber_v"+tier));level.setBlockAndUpdate(pos,block.defaultBlockState());
    var sim=(UltimateSimChamberTileEntity)level.getBlockEntity(pos);sim.getInventory().setStackInSlot(0,combined.copy());sim.getInventory().setStackInSlot(1,new ItemStack(Hostile.Items.PREDICTION_MATRIX,64));sim.getEnergy().setEnergy(sim.CAP);
    for(int tick=0;tick<=sim.DURATION;tick++)sim.serverTick(level,pos,sim.getBlockState());
    int outputs=0;for(int slot=2;slot<sim.getInventory().getSlots();slot++)outputs+=sim.getInventory().getStackInSlot(slot).getCount();
    require(outputs>0,"simulator tier "+tier+" consumes matrix and produces drops");
    require(sim.getInventory().getStackInSlot(1).getCount()==64-4*(1<<(tier==1?0:tier)),"simulator tier "+tier+" input multiplier");
    var saved=sim.saveWithFullMetadata(level.registryAccess());var loaded=BlockEntity.loadStatic(pos,sim.getBlockState(),saved,level.registryAccess());
    require(loaded instanceof UltimateSimChamberTileEntity && ((UltimateSimChamberTileEntity)loaded).getInventory().getStackInSlot(0).getCount()==1,"simulator tier "+tier+" inventory persistence");
    BlockPos fabPos=pos.offset(0,0,4);level.setBlockAndUpdate(fabPos,BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("extrahnn","ultimate_loot_fabricator_v"+tier)).defaultBlockState());var fab=(UltimateLootFabTileEntity)level.getBlockEntity(fabPos);
    fab.setFixedDrop(holders.getFirst(),0);var prediction=holders.getFirst().get().getPredictionDrop();prediction.setCount(64);fab.getInventory().setStackInSlot(0,prediction);fab.getEnergy().setEnergy(fab.CAP);
    for(int tick=0;tick<fab.DURATION+3;tick++)fab.serverTick(level,fabPos,fab.getBlockState());
    outputs=0;for(int slot=1;slot<fab.getInventory().getSlots();slot++)outputs+=fab.getInventory().getStackInSlot(slot).getCount();require(outputs>0,"fabricator tier "+tier+" produces selected loot");
   }
   BlockPos trainPos=new BlockPos(24,100,0);level.setBlockAndUpdate(trainPos,ExtraHostile.Blocks.SIMULATOR_MODELING.value().defaultBlockState());var trainer=(SimulationModelingTileEntity)level.getBlockEntity(trainPos);var training=combined.copy();ExtraDataModelItem.setData(training,0);trainer.getInventory().setStackInSlot(0,training);trainer.getEnergy().setEnergy(ExtraHostileConfig.simulationModelingPowerCap);for(int i=0;i<ExtraHostileConfig.simulationModelingPowerDuration+3;i++)trainer.serverTick(level,trainPos,trainer.getBlockState());require(ExtraDataModelItem.getData(training)>0,"model training advances data");
   ExtraHostileNetworks.LOGGER.info("PORT FUNCTIONAL SMOKE COMPLETE");
  }catch(Throwable error){ExtraHostileNetworks.LOGGER.error("PORT FUNCTIONAL SMOKE FAILED",error);}
  finally {event.getServer().halt(false);}
 }
}
