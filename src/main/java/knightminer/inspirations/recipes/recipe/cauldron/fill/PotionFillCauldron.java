package knightminer.inspirations.recipes.recipe.cauldron.fill;

import knightminer.inspirations.common.Config;
import knightminer.inspirations.library.recipe.cauldron.ICauldronRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

@Deprecated
public class PotionFillCauldron implements ICauldronRecipe {

  private ItemStack bottle;
  private Item potion;

  public PotionFillCauldron(Item potion, ItemStack bottle) {
    this.bottle = bottle;
    this.potion = potion;
  }

  @Override
  public boolean matches(ItemStack stack, boolean boiling, int level, CauldronState state) {
    return level < Config.getCauldronMax() && (level == 0 || state.getPotion() != Potions.EMPTY)
           && stack.getItem() == potion;
  }

  @Override
  public ItemStack getResult(ItemStack stack, boolean boiling, int level, CauldronState state) {
    return bottle.copy();
  }

  @Override
  public int getLevel(int level) {
    return level + 1;
  }

  @Override
  public CauldronState getState(ItemStack stack, boolean boiling, int level, CauldronState state) {
    // if the level was 0, we need to add the potion state
    Potion inputType = PotionUtils.getPotionFromItem(stack);
    if (level == 0) {
      return CauldronState.potion(inputType);
    }
    // if the types differ, it just turns black
    if (state.getPotion() != inputType) {
      return CauldronState.potion(Potions.THICK);
    }

    return state;
  }

  @Override
  public SoundEvent getSound(ItemStack stack, boolean boiling, int level, CauldronState state) {
    if (level > 0 && PotionUtils.getPotionFromItem(stack) != state.getPotion()) {
      return SoundEvents.BLOCK_FIRE_EXTINGUISH;
    }

    return SoundEvents.ITEM_BOTTLE_EMPTY;
  }

  @Override
  public ItemStack getContainer(ItemStack stack) {
    return ItemStack.EMPTY;
  }
}
