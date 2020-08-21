package knightminer.inspirations.library.recipe.cauldron.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.IntPredicate;

/**
 * Predicate to match a cauldron level
 */
public interface ILevelPredicate extends IntPredicate {
  String KEY_MIN = "min";
  String KEY_MAX = "max";

  /**
   * Writes this to the packet buffer
   * @param buffer  Buffer instance
   */
  void write(PacketBuffer buffer);

  /**
   * Writes this to the packet buffer
   * @param json  Json object
   */
  void write(JsonObject json);

  /**
   * Writes this to JSON
   */
  default JsonObject toJson() {
    JsonObject object = new JsonObject();
    write(object);
    return object;
  }

  /**
   * Reads a level predicate from JSON
   * @param json  JSON object
   * @return  Level predicate
   */
  static ILevelPredicate read(JsonObject json) {
    Integer min = json.has(KEY_MIN) ? JSONUtils.getInt(json, KEY_MIN) : null;
    Integer max = json.has(KEY_MAX) ? JSONUtils.getInt(json, KEY_MAX) : null;
    if (min != null) {
      if (max != null) {
        return new Range(min, max);
      }
      return new Min(min);
    } if (max != null) {
      return new Max(max);
    }
    throw new JsonSyntaxException("Must specify 'min' or 'max' for input");
  }

  /**
   * Reads a level predicate from the packet buffer
   * @param buffer  Buffer instance
   * @return  Level predicate
   */
  static ILevelPredicate read(PacketBuffer buffer) {
    Type type = buffer.readEnumValue(Type.class);
    int i = buffer.readVarInt();
    switch (type) {
      case MIN: return new Min(i);
      case MAX: return new Max(i);
      case RANGE: return new Range(i, buffer.readVarInt());
    }
    throw new DecoderException("Got null type, this should not be possible");
  }

  /**
   * Predicate to match a minimum level or higher
   */
  class Min implements ILevelPredicate {
    private final int min;

    public Min(int min) {
      this.min = min;
    }

    @Override
    public boolean test(int value) {
      return value >= min;
    }

    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.MIN);
      buffer.writeVarInt(min);
    }

    @Override
    public void write(JsonObject json) {
      json.addProperty(KEY_MIN, min);
    }
  }

  /**
   * Predicate to match a maximum level or lower
   */
  class Max implements ILevelPredicate {
    private final int max;

    public Max(int max) {
      this.max = max;
    }

    @Override
    public boolean test(int value) {
      return value <= max;
    }

    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.MAX);
      buffer.writeVarInt(max);
    }

    @Override
    public void write(JsonObject json) {
      json.addProperty(KEY_MAX, max);
    }
  }

  /**
   * Predicate to match a value between two numbers. Really no idea why you would want this, but included for completion
   */
  class Range implements ILevelPredicate {
    private final int min, max;

    public Range(int min, int max) {
      this.min = min;
      this.max = max;
    }

    @Override
    public boolean test(int value) {
      return value <= max && value >= min;
    }

    @Override
    public void write(PacketBuffer buffer) {
      buffer.writeEnumValue(Type.MAX);
      buffer.writeVarInt(min);
      buffer.writeVarInt(max);
    }

    @Override
    public void write(JsonObject json) {
      json.addProperty(KEY_MIN, min);
      json.addProperty(KEY_MAX, max);
    }
  }

  /** All valid level predicate types */
  enum Type {
    MIN,
    MAX,
    RANGE;

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
