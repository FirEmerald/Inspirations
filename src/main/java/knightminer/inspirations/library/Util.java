package knightminer.inspirations.library;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Potion;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class Util {

  public static boolean clickedAABB(AxisAlignedBB aabb, Vector3d hit) {
    return aabb.minX <= hit.x && hit.x <= aabb.maxX
           && aabb.minY <= hit.y && hit.y <= aabb.maxY
           && aabb.minZ <= hit.z && hit.z <= aabb.maxZ;
  }

  /**
   * Compute a voxelshape, rotated by the provided yaw.
   */
  public static VoxelShape makeRotatedShape(Direction side, int x1, int y1, int z1, int x2, int y2, int z2) {
    float yaw = -(float)Math.PI / 2F * side.getHorizontalIndex();
    Vector3d min = new Vector3d(x1 - 8, y1 - 8, z1 - 8).rotateYaw(yaw);
    Vector3d max = new Vector3d(x2 - 8, y2 - 8, z2 - 8).rotateYaw(yaw);
    return VoxelShapes.create(
        0.5 + min.x / 16.0, 0.5 + min.y / 16.0, 0.5 + min.z / 16.0,
        0.5 + max.x / 16.0, 0.5 + max.y / 16.0, 0.5 + max.z / 16.0
                             );
  }

  // An item with Silk Touch, to make blocks drop their silk touch items if they have any.
  // Using a Stick makes sure it won't be damaged.
  private static ItemStack silkTouchItem = new ItemStack(Items.STICK);

  static {
    silkTouchItem.addEnchantment(Enchantments.SILK_TOUCH, 1);
  }

  /**
   * Gets an item stack from a block state. Uses Silk Touch drops
   * @param state Input state
   * @return ItemStack for the state, or ItemStack.EMPTY if a valid item cannot be found
   */
  public static ItemStack getStackFromState(ServerWorld world, @Nullable BlockState state) {
    if (state == null) {
      return ItemStack.EMPTY;
    }
    Block block = state.getBlock();

    // skip air
    if (block == Blocks.AIR) {
      return ItemStack.EMPTY;
    }

    // Fill a fake context in to get Silk Touch drops.
    // From LootParameterSets.Block,
    // BLOCK_STATE, POSITION and TOOL is required and
    // THIS_ENTITY, BLOCK_ENTITY and EXPLOSION_RADIUS are optional.
    // BLOCK_STATE is provided by getDrops().
    List<ItemStack> drops = state.getDrops(new LootContext.Builder(world)
                                               .withParameter(LootParameters.POSITION, new BlockPos(0, 0, 64))
                                               .withParameter(LootParameters.TOOL, silkTouchItem)
                                          );
    if (drops.size() > 0) {
      return drops.get(0);
    }

    // if it fails, do a fallback of item.getItemFromBlock
    InspirationsRegistry.log.error("Failed to get silk touch drop for {}, using fallback", state);

    // fallback, use item dropped
    Item item = Item.getItemFromBlock(block);
    if (item == Items.AIR) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(item);
  }

  /**
   * Creates a NonNullList from the specified elements, using the class as the type
   * @param elements Elements for the list
   * @return New NonNullList
   */
  @SafeVarargs
  @Deprecated
  public static <E> NonNullList<E> createNonNullList(E... elements) {
    NonNullList<E> list = NonNullList.create();
    list.addAll(Arrays.asList(elements));
    return list;
  }

  /**
   * Combines two colors
   * @param color1 First color
   * @param color2 Second color
   * @param scale  Determines how many times color2 is applied
   * @return Combined color
   */
  public static int combineColors(int color1, int color2, int scale) {
    if (scale == 0) {
      return color1;
    }
    int a = color1 >> 24 & 0xFF;
    int r = color1 >> 16 & 0xFF;
    int g = color1 >> 8 & 0xFF;
    int b = color1 & 0xFF;
    int a2 = color2 >> 24 & 0xFF;
    int r2 = color2 >> 16 & 0xFF;
    int g2 = color2 >> 8 & 0xFF;
    int b2 = color2 & 0xFF;

    for (int i = 0; i < scale; i++) {
      a = (int)Math.sqrt(a * a2);
      r = (int)Math.sqrt(r * r2);
      g = (int)Math.sqrt(g * g2);
      b = (int)Math.sqrt(b * b2);
    }
    return a << 24 | r << 16 | g << 8 | b;
  }

  /**
   * Merge three float color components between 0 and 1 into a hex color integer
   * @param component float color component array, must be length 3
   * @return Color integer value
   */
  public static int getColorInteger(float[] component) {
    return ((int)(component[0] * 255) & 0xFF) << 16
           | ((int)(component[1] * 255) & 0xFF) << 8
           | ((int)(component[2] * 255) & 0xFF);
  }

  /**
   * Adds the tooltips for the potion type into the given string list
   * @param potionType Potion type input
   * @param lores      List to add the tooltips into
   */
  public static void addPotionTooltip(Potion potionType, List<ITextComponent> lores) {
    List<EffectInstance> effects = potionType.getEffects();

    if (effects.isEmpty()) {
      lores.add(new TranslationTextComponent("effect.none").mergeStyle(TextFormatting.GRAY));
      return;
    }

    for (EffectInstance effect : effects) {
      IFormattableTextComponent effectString = new TranslationTextComponent(effect.getPotion().getName());
      Effect potion = effect.getPotion();

      if (effect.getAmplifier() > 0) {
        effectString.appendString(" ");
        effectString.append(new TranslationTextComponent("potion.potency." + effect.getAmplifier()));
      }
      if (effect.getDuration() > 20) {
        effectString.append(new StringTextComponent(" (" + EffectUtils.getPotionDurationString(effect, 1.0f) + ")"));
      }
      effectString.mergeStyle(potion.isBeneficial() ? TextFormatting.BLUE : TextFormatting.RED);
      lores.add(effectString);
    }
  }

  /**
   * Gets the dye color for the given color int
   * @param color Dye color input
   * @return EnumDyeColor matching, or null for no match
   */
  @Nullable
  public static DyeColor getDyeForColor(int color) {
    for (DyeColor dyeColor : DyeColor.values()) {
      if (dyeColor.getId() == color) {
        return dyeColor;
      }
    }
    return null;
  }
}
