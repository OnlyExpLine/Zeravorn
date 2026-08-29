package com.zeravorn.item;
import com.zeravorn.hero.*; import com.zeravorn.team.TeamId; import java.util.UUID; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class ShopServiceTest {
 private HeroRuntime hero(String id){return new HeroRuntime(UUID.randomUUID(),TeamId.BLUE,HeroDefinitions.byId(id));}
 @Test void buySellAndSlots(){var h=hero("jason");h.addGold(2000);var shop=new ShopService(new ItemCatalog());assertTrue(shop.buy(h,"iron_blade").success());assertEquals(700,h.gold());assertFalse(shop.buy(h,"iron_blade").success());assertTrue(shop.sellNormal(h,0).success());assertEquals(1780,h.gold());assertTrue(shop.buy(h,"traveler_boots").success());assertTrue(shop.sellBoots(h).success());assertEquals(1420,h.gold());}
 @Test void classRestrictionsAndDeadBuy(){var h=hero("amelia");h.addGold(2000);h.setAlive(false);var shop=new ShopService(new ItemCatalog());assertTrue(shop.buy(h,"frost_crystal").success());assertEquals("CLASS_RESTRICTED",shop.buy(h,"iron_blade").errorCode());}
 @Test void goldAndXpServices(){var h=hero("jason");var g=new com.zeravorn.economy.GoldService();assertEquals(9,g.grantPassive(h,600,660));assertEquals(509,h.gold());var x=new com.zeravorn.economy.ExperienceService();assertEquals(1,x.grant(h,180,com.zeravorn.economy.ExperienceReason.LANE));assertEquals(2,h.level());}
}
