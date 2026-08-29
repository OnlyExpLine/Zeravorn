package com.zeravorn.item;
import com.zeravorn.hero.*;
public final class ShopService {
    private final ItemCatalog catalog; public ShopService(ItemCatalog catalog){this.catalog=catalog;}
    public ShopResult buy(HeroRuntime hero,String itemId){var item=catalog.find(itemId);if(item==null)return ShopResult.reject("INVALID_ITEM",hero.gold());if(!allowed(hero,item))return ShopResult.reject("CLASS_RESTRICTED",hero.gold());if(hero.inventory().contains(item.id()))return ShopResult.reject("DUPLICATE_ITEM",hero.gold());if(hero.gold()<item.price())return ShopResult.reject("NO_GOLD",hero.gold());if(item.slotType()==SlotType.BOOTS&&hero.inventory().boots()!=null)return ShopResult.reject("WRONG_SLOT",hero.gold());if(item.slotType()==SlotType.NORMAL&&hero.inventory().normalItems().size()>=MobaInventory.NORMAL_CAPACITY)return ShopResult.reject("INVENTORY_FULL",hero.gold());hero.spendGold(item.price());hero.inventory().add(item);return new ShopResult(true,"",hero.gold());}
    public ShopResult sellNormal(HeroRuntime hero,int slot){var item=hero.inventory().removeNormal(slot);if(item==null)return ShopResult.reject("WRONG_SLOT",hero.gold());hero.addGold((int)Math.floor(item.price()*.60));return new ShopResult(true,"",hero.gold());}
    public ShopResult sellBoots(HeroRuntime hero){var item=hero.inventory().removeBoots();if(item==null)return ShopResult.reject("WRONG_SLOT",hero.gold());hero.addGold((int)Math.floor(item.price()*.60));return new ShopResult(true,"",hero.gold());}
    private boolean allowed(HeroRuntime h,ItemDefinition i){return i.category()==ItemCategory.DEFENSE||i.category()==ItemCategory.BOOTS||i.category()==ItemCategory.PHYSICAL&&h.definition().damageType()!=DamageType.MAGICAL||i.category()==ItemCategory.MAGICAL&&h.definition().damageType()!=DamageType.PHYSICAL;}
}
