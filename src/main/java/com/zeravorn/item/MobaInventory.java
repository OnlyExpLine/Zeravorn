package com.zeravorn.item;
import java.util.*;
public final class MobaInventory {
    public static final int NORMAL_CAPACITY=5; private final List<ItemDefinition> normal=new ArrayList<>(); private ItemDefinition boots;
    public List<ItemDefinition> normalItems(){return List.copyOf(normal);} public ItemDefinition boots(){return boots;}
    public boolean contains(String id){return (boots!=null&&boots.id().equals(id))||normal.stream().anyMatch(i->i.id().equals(id));}
    public boolean add(ItemDefinition item){Objects.requireNonNull(item); if(contains(item.id()))return false; if(item.slotType()==SlotType.BOOTS){if(boots!=null)return false;boots=item;return true;} if(normal.size()>=NORMAL_CAPACITY)return false;normal.add(item);return true;}
    public ItemDefinition removeNormal(int slot){if(slot<0||slot>=normal.size())return null;return normal.remove(slot);} public ItemDefinition removeBoots(){ItemDefinition result=boots;boots=null;return result;}
    public ItemStats totalStats(){ItemStats total=ItemStats.NONE;for(var i:normal)total=total.plus(i.stats());if(boots!=null)total=total.plus(boots.stats());return total;}
}
