package net.lmor.extrahnn.compat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.*;
/** Transaction bridge retaining the machines' original slot restrictions. */
public class InternalItemHandler extends ItemStackHandler implements ResourceHandler<ItemResource> {
 public InternalItemHandler(int size) { super(size); }
 public ItemStack insertItemInternal(int slot,ItemStack stack,boolean simulate) { return super.insertItem(slot,stack,simulate); }
 public ItemStack extractItemInternal(int slot,int amount,boolean simulate) { return super.extractItem(slot,amount,simulate); }
 private final SnapshotJournal<NonNullList<ItemStack>> journal=new SnapshotJournal<>() {
  protected NonNullList<ItemStack> createSnapshot() { var copy=NonNullList.withSize(stacks.size(),ItemStack.EMPTY);for(int i=0;i<stacks.size();i++)copy.set(i,stacks.get(i).copy());return copy; }
  protected void revertToSnapshot(NonNullList<ItemStack> snapshot) { for(int i=0;i<snapshot.size();i++)if (!ItemStack.matches(stacks.get(i), snapshot.get(i))) stacks.set(i,snapshot.get(i)); }
  protected void onRootCommit(NonNullList<ItemStack> snapshot) { for(int i=0;i<stacks.size();i++)if (!ItemStack.matches(stacks.get(i), snapshot.get(i))) onContentsChanged(i); }
 };
 public int size(){return getSlots();}
 public ItemResource getResource(int slot){return ItemResource.of(getStackInSlot(slot));}
 public long getAmountAsLong(int slot){return getStackInSlot(slot).getCount();}
 public long getCapacityAsLong(int slot,ItemResource resource){return getStackLimit(slot,resource.toStack());}
 public boolean isValid(int slot,ItemResource resource){return isItemValid(slot,resource.toStack());}
 public int insert(int slot,ItemResource resource,int amount,TransactionContext tx){if(resource.isEmpty()||amount<=0)return 0;var stack=resource.toStack(amount);int accepted=amount-insertItem(slot,stack,true).getCount();if(accepted>0){journal.updateSnapshots(tx);return amount-insertItem(slot,stack,false).getCount();}return 0;}
 public int extract(int slot,ItemResource resource,int amount,TransactionContext tx){if(!resource.equals(getResource(slot))||amount<=0)return 0;int extracted=extractItem(slot,amount,true).getCount();if(extracted>0){journal.updateSnapshots(tx);return extractItem(slot,amount,false).getCount();}return 0;}
}
