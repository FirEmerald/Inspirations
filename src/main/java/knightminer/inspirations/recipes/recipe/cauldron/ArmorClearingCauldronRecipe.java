package knightminer.inspirations.recipes.recipe.cauldron;

import knightminer.inspirations.library.recipe.cauldron.ICauldronRecipe;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated
public class ArmorClearingCauldronRecipe implements ICauldronRecipe {

  private final ArmorMaterial material;

  public ArmorClearingCauldronRecipe(ArmorMaterial material) {
    this.material = material;
  }

  @Override
  public boolean matches(ItemStack stack, boolean boiling, int level, CauldronState state) {
    if (level == 0 || !state.isWater()) {
      return false;
    }
    Item item = stack.getItem();
    if (!(item instanceof DyeableArmorItem)) {
      return false;
    }

    // only color leather, and ensure we are changing the color
    DyeableArmorItem armor = (DyeableArmorItem)item;
    return armor.getArmorMaterial() == material && armor.hasColor(stack);
  }

  @Override
  public ItemStack getResult(ItemStack stack, boolean boiling, int level, CauldronState state) {
    stack = stack.copy();
    ((DyeableArmorItem)stack.getItem()).removeColor(stack);
    return stack;
  }

  @Override
  public int getLevel(int level) {
    return level - 1;
  }

  @Override
  public ItemStack getContainer(ItemStack stack) {
    return ItemStack.EMPTY;
  }
}
