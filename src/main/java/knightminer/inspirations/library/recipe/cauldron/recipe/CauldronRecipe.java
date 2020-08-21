package knightminer.inspirations.library.recipe.cauldron.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import knightminer.inspirations.library.recipe.cauldron.CauldronContentTypes;
import knightminer.inspirations.library.recipe.cauldron.CauldronIngredients;
import knightminer.inspirations.library.recipe.cauldron.contents.EmptyCauldronContents;
import knightminer.inspirations.library.recipe.cauldron.contents.ICauldronContents;
import knightminer.inspirations.library.recipe.cauldron.ingredient.ICauldronIngredient;
import knightminer.inspirations.library.recipe.cauldron.inventory.ICauldronInventory;
import knightminer.inspirations.library.recipe.cauldron.inventory.IModifyableCauldronInventory;
import knightminer.inspirations.library.recipe.cauldron.util.ILevelPredicate;
import knightminer.inspirations.library.recipe.cauldron.util.ILevelUpdate;
import knightminer.inspirations.library.recipe.cauldron.util.TemperaturePredicate;
import knightminer.inspirations.recipes.InspirationsRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

/**
 * Base cauldron recipe implementation
 */
public class CauldronRecipe implements ICauldronRecipe {
  private final ResourceLocation id;
  private final String group;
  private final Ingredient input;
  private final int amount;
  private final ICauldronIngredient contents;
  private final ILevelPredicate level;
  private final TemperaturePredicate temperature;
  private final ItemStack output;
  private final ICauldronContents newContents;
  private final ILevelUpdate levelUpdate;
  @Nullable
  private final ItemStack container;

  /**
   * Creates a new cauldron recipe
   * @param id           Recipe ID
   * @param group        Recipe group
   * @param input        Ingredient input
   * @param amount       Number of input to match
   * @param contents     Cauldron contents
   * @param level        Input level
   * @param temperature  Predicate for required cauldron temperature
   * @param output       Output stack, use empty for no output
   * @param newContents  Output contents, use {@link EmptyCauldronContents#INSTANCE} to keep old contents
   * @param levelUpdate  Level updater
   * @param container    Container output. If null, fetches container from the item. If empty, no container
   */
  public CauldronRecipe(ResourceLocation id, String group, Ingredient input, int amount, ICauldronIngredient contents, ILevelPredicate level,
                        TemperaturePredicate temperature, ItemStack output, ICauldronContents newContents, ILevelUpdate levelUpdate, @Nullable ItemStack container) {
    this.id = id;
    this.group = group;
    this.input = input;
    this.amount = amount;
    this.contents = contents;
    this.level = level;
    this.temperature = temperature;
    this.output = output;
    this.newContents = newContents;
    this.levelUpdate = levelUpdate;
    this.container = container;
  }

  @Override
  public boolean matches(ICauldronInventory inv, World worldIn) {
    // boiling must match, must have right level, must have valid contents
    if (!temperature.test(inv.isBoiling()) || !level.test(inv.getLevel()) || !contents.test(inv.getContents())) {
      return false;
    }
    // stack must have enough items and match the ingredient
    ItemStack stack = inv.getStack();
    return stack.getCount() >= amount && input.test(stack);
  }

  @Override
  public void handleRecipe(IModifyableCauldronInventory inventory) {
    // update level
    // only update contents if the level is not empty and we have new contents
    if (!inventory.updateLevel(levelUpdate) && newContents != EmptyCauldronContents.INSTANCE) {
      inventory.setContents(newContents);
    }

    // determine container item if passed container is null
    ItemStack container = this.container;
    if (container == null) {
      container = inventory.getStack().getContainerItem().copy();
      if (!container.isEmpty()) {
        container.setCount(amount);
      }
    }

    // update hand item and container item
    ItemStack hand = inventory.shrinkStack(amount);
    if (!container.isEmpty()) {
      if (hand.isEmpty()) {
        inventory.setStack(container.copy());
      } else {
        inventory.giveStack(container.copy());
      }
    }

    // give output
    if (!output.isEmpty()) {
      inventory.giveStack(output.copy());
    }
  }

  @Override
  public ResourceLocation getId() {
    return id;
  }

  @Override
  public String getGroup() {
    return group;
  }

  @Override
  public ItemStack getRecipeOutput() {
    return output;
  }

  @Override
  public IRecipeSerializer<?> getSerializer() {
    return InspirationsRecipes.cauldronSerializer;
  }

  /**
   * Gets the boiling predicate for the given JSON
   * @param json  Parent json object
   * @param key   Key in json
   * @return  Boiling predicate
   * @throws JsonSyntaxException  If the value is invalid
   */
  public static TemperaturePredicate getBoiling(JsonObject json, String key) {
    String name = JSONUtils.getString(json, key, "any");
    TemperaturePredicate boiling = TemperaturePredicate.byName(name);
    if (boiling == null) {
      throw new JsonSyntaxException("Invalid boiling predicate '" + name + "'");
    }
    return boiling;
  }

  public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CauldronRecipe> {
    @Override
    public CauldronRecipe read(ResourceLocation id, JsonObject json) {
      String group = JSONUtils.getString(json, "group", "");

      // parse inputs
      JsonObject inputJson = JSONUtils.getJsonObject(json, "input");
      Ingredient input = Ingredient.EMPTY;
      int amount = 0;
      if (inputJson.has("item")) {
        input = CraftingHelper.getIngredient(json.get("item"));
        amount = JSONUtils.getInt(json, "amount", 1);
      }
      ICauldronIngredient contents = CauldronIngredients.read(JSONUtils.getJsonObject(inputJson, "contents"));
      ILevelPredicate levels = ILevelPredicate.read(JSONUtils.getJsonObject(inputJson, "level"));
      TemperaturePredicate temperature = getBoiling(inputJson, "temperature");

      // parse outputs
      JsonObject outputJson = JSONUtils.getJsonObject(json, "output");
      ItemStack output = ItemStack.EMPTY;
      if (outputJson.has("item")) {
        output = CraftingHelper.getItemStack(JSONUtils.getJsonObject(outputJson, "item"), true);
      }
      ICauldronContents newContents = EmptyCauldronContents.INSTANCE;
      if (outputJson.has("contents")) {
        newContents = CauldronContentTypes.read(JSONUtils.getJsonObject(outputJson, "contents"));
      }
      ILevelUpdate levelUpdate = ILevelUpdate.IDENTITY;
      if (outputJson.has("level")) {
        levelUpdate = ILevelUpdate.read(JSONUtils.getJsonObject(outputJson, "level"));
      }
      ItemStack container = null;
      if (outputJson.has("container")) {
        JsonObject data = JSONUtils.getJsonObject(outputJson, "item");
        // special case for empty
        boolean empty = JSONUtils.getBoolean(data, "empty", false);
        if (empty) {
          container = ItemStack.EMPTY;
        } else {
          container = CraftingHelper.getItemStack(json, true);
        }
      }

      // finally, after all that return the recipe
      return new CauldronRecipe(id, group, input, amount, contents, levels, temperature, output, newContents, levelUpdate, container);
    }

    @Override
    public void write(PacketBuffer buffer, CauldronRecipe recipe) {
      buffer.writeString(recipe.group);
      recipe.input.write(buffer);
      buffer.writeVarInt(recipe.amount);
      CauldronIngredients.write(recipe.contents, buffer);
      recipe.level.write(buffer);
      buffer.writeEnumValue(recipe.temperature);
      buffer.writeItemStack(recipe.output);
      CauldronContentTypes.write(recipe.newContents, buffer);
      recipe.levelUpdate.write(buffer);
      if (recipe.container == null) {
        buffer.writeBoolean(false);
      } else {
        buffer.writeBoolean(true);
        buffer.writeItemStack(recipe.container);
      }
    }

    @Nullable
    @Override
    public CauldronRecipe read(ResourceLocation id, PacketBuffer buffer) {
      String group = buffer.readString();
      Ingredient input = Ingredient.read(buffer);
      int amount = buffer.readVarInt();
      ICauldronIngredient contents = CauldronIngredients.read(buffer);
      ILevelPredicate levels = ILevelPredicate.read(buffer);
      TemperaturePredicate boiling = buffer.readEnumValue(TemperaturePredicate.class);
      ItemStack output = buffer.readItemStack();
      ICauldronContents newContents = CauldronContentTypes.read(buffer);
      ILevelUpdate levelUpdate = ILevelUpdate.read(buffer);
      ItemStack container = null;
      if (buffer.readBoolean()) {
        container = buffer.readItemStack();
      }

      // finally, after all that return the recipe
      return new CauldronRecipe(id, group, input, amount, contents, levels, boiling, output, newContents, levelUpdate, container);
    }
  }
}
