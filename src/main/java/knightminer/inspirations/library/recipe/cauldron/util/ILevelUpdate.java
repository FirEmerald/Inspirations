package knightminer.inspirations.library.recipe.cauldron.util;

import com.google.gson.JsonObject;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

/**
 * Logic to update the level to a new value
 */
public interface ILevelUpdate extends IntUnaryOperator {
  String KEY_ADD = "add";
  String KEY_SET = "set";

  /** Level update that returns the input */
  ILevelUpdate IDENTITY = new ILevelUpdate() {
    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.IDENTITY);
    }

    @Override
    public int applyAsInt(int original) {
      return original;
    }

    @Override
    public JsonObject toJson() {
      return new JsonObject();
    }
  };

  /**
   * Writes this to the packet buffer
   * @param buffer  Buffer instance
   */
  void write(PacketBuffer buffer);

  /**
   * Writes this to the packet buffer
   * @param json  Json object
   */
  default void write(JsonObject json) {}

  /**
   * Writes this to JSON
   */
  default JsonObject toJson() {
    JsonObject object = new JsonObject();
    write(object);
    return object;
  }

  /**
   * Reads a level update from JSON
   * @param json  JSON object
   * @return  Level predicate
   */
  static ILevelUpdate read(JsonObject json) {
    if (json.has(KEY_ADD)) {
      return new Add(JSONUtils.getInt(json, KEY_ADD));
    }
    if (json.has(KEY_SET)) {
      return new Set(JSONUtils.getInt(json, KEY_SET));
    }

    // neither? means identity
    return IDENTITY;
  }

  /**
   * Reads a level update from the packet buffer
   * @param buffer  Buffer instance
   * @return  Level predicate
   */
  static ILevelUpdate read(PacketBuffer buffer) {
    Type type = buffer.readEnumValue(Type.class);
    switch (type) {
      case IDENTITY: return IDENTITY;
      case SET: return new Set(buffer.readVarInt());
      case ADD: return new Add(buffer.readVarInt());
    }
    throw new DecoderException("Got null type, this should not be possible");
  }

  /**
   * Updater that sets the amount
   */
  class Set implements ILevelUpdate {
    private final int amount;
    public Set(int amount) {
      this.amount = amount;
    }

    @Override
    public int applyAsInt(int original) {
      return amount;
    }

    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.SET);
      buffer.writeVarInt(amount);
    }

    @Override
    public void write(JsonObject json) {
      json.addProperty(KEY_SET, amount);
    }
  }

  /**
   * Updater that adds to the amount
   */
  class Add implements ILevelUpdate {
    private final int amount;
    public Add(int amount) {
      this.amount = amount;
    }

    @Override
    public int applyAsInt(int original) {
      return MathHelper.clamp(original + amount, 0, 3);
    }

    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.ADD);
      buffer.writeVarInt(amount);
    }

    @Override
    public void write(JsonObject json) {
      json.addProperty(KEY_ADD, amount);
    }
  }

  /** All valid level update types */
  enum Type {
    IDENTITY,
    SET,
    ADD;

    private final String name = name().toLowerCase(Locale.US);

    /**
     * Gets the name of this type
     * @return  Type name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets a predicate type for the given name
     * @param name  Name to check
     * @return  Value, or null if missing
     */
    @Nullable
    public static Type byName(String name) {
      for (Type type : values()) {
        if (type.getName().equals(name)) {
          return type;
        }
      }
      return null;
    }
  }
}
