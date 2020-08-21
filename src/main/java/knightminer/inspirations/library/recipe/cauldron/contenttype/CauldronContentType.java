package knightminer.inspirations.library.recipe.cauldron.contenttype;

import com.google.gson.JsonObject;
import knightminer.inspirations.library.recipe.cauldron.CauldronContentTypes;
import knightminer.inspirations.library.recipe.cauldron.contents.ICauldronContents;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a type of contents that can be stored in the cauldron
 * @param <C>  {@link ICauldronContents} implementation for this type
 */
public abstract class CauldronContentType<C extends ICauldronContents> {
  private final Map<ICauldronContents, Supplier<C>> equivalencies = new HashMap<>();

  /**
   * Adds an equivalency to this type, allowing it to be used in place of the other, I.E. otherContents is thisContents, but not necessarily the other way around, I.E. thisContents may not be otherContents
   * @param thisContents The contents of this type to match to
   * @param otherContents The contents to be matched with
   */
  public final void addEquivalency(Supplier<C> thisContents, ICauldronContents otherContents) {
	  equivalencies.put(otherContents, thisContents);
  }

  @SuppressWarnings("unchecked")
  public final <T extends ICauldronContents >void addBiEquivalency(C thisContents, T otherContents) {
	  addEquivalency(() -> thisContents, otherContents);
	  ((CauldronContentType<T>) otherContents.getType()).addEquivalency(() -> otherContents, thisContents);
  }

  /**
   * Gets the contents as this type
   * @param contents  Contents to fetch
   * @return  Type to get
   * @return Optional of the this type, empty if no equivalence is found
   */
  @SuppressWarnings("unchecked")
  public Optional<C> of(ICauldronContents contents) {
	  if (contents.is(this)) return Optional.of((C) contents);
	  else {
		  Supplier<C> supplier = equivalencies.get(contents);
		  return supplier == null ? Optional.empty() : Optional.of(supplier.get());
	  }
  }

  /**
   * Creates a new instance
   */
  protected CauldronContentType() {}

  /**
   * Reads the given type from NBT
   * @param tag  NBT tag
   * @return  Read value
   */
  @Nullable
  public abstract C read(CompoundNBT tag);

  /**
   * Reads the given type from JSON
   * @param json  JSON object
   * @return  Read value=
   * @throws com.google.gson.JsonSyntaxException if the JSON is invalid
   */
  public abstract C read(JsonObject json);

  /**
   * Reads the given type from the packet buffer
   * @param buffer  Packet buffer
   * @return  Read value
   * @throws io.netty.handler.codec.DecoderException if the type is invalid
   */
  public abstract C read(PacketBuffer buffer);

  /**
   * Writes the given type to NBT
   * @param contents  Contents to write
   * @param tag       NBT tag
   */
  public abstract void write(C contents, CompoundNBT tag);

  /**
   * Writes the given type to JSON
   * @param contents  Contents to write
   * @param json      JSON object
   */
  public abstract void write(C contents, JsonObject json);

  /**
   * Writes the given type to the packet buffer
   * @param contents  Contents to write
   * @param buffer    Packet buffer
   */
  public abstract void write(C contents, PacketBuffer buffer);

  @Override
  public String toString() {
    return String.format("CauldronContentType[%s]", CauldronContentTypes.getName(this));
  }
}
