package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface PropertyFilter<T extends Comparable<T>> extends Predicate<T> {
    static PropertyWithFilter<?> get(String propertyName, KDLValue<?> valueRange, Block moddedBlock) throws ConfigFormatException {
        // The input will be something like age="1..5". With "age being passed to `propertyName` and "1..5" to `valueRange`
        var property = moddedBlock.getStateManager().getProperty(propertyName);
        if (property == null) throw propertyNotFound(propertyName, moddedBlock);

        if (valueRange instanceof KDLNull) {
            throw new ConfigFormatException("null is not a valid range of properties");
        } else if (valueRange instanceof KDLBoolean bValueRange) {
            if (property instanceof BooleanProperty booleanProperty) {
                return new PropertyWithFilter<>(booleanProperty, v -> v == bValueRange.getValue());
            } else {
                // Try it again but using "true" or "false" as if it were a string
                valueRange = KDLString.from(String.valueOf(valueRange.getValue()));
            }
        } else if (valueRange instanceof KDLNumber nValueRange) {
            if (property instanceof IntProperty intProperty) {
                var valueInt = nValueRange.getValue().intValue();
                if (intProperty.getValues().contains(valueInt)) {
                    return new PropertyWithFilter<>(intProperty, v -> v == valueInt);
                } else {
                    // Try it again but using the number as a string
                    valueRange = KDLString.from(String.valueOf(valueRange.getValue()));
                }
            }
        } else if (valueRange instanceof KDLString sValueRange) {
            var string = sValueRange.getValue();
            if (string.contains("..")) {
                var split = string.split("\\.\\.", 2);
                var left = split[0].equals("") ? KDLNumber.from(Integer.MIN_VALUE) : KDLNumber.from(split[0]).orElse(null);
                var right = split[1].equals("") ? KDLNumber.from(Integer.MAX_VALUE) : KDLNumber.from(split[1]).orElse(null);

                if (left == null) throw new ConfigFormatException("Invalid number "+split[0]+" in range "+string);
                if (right == null) throw new ConfigFormatException("Invalid number "+split[1]+" in range "+string);

                var leftInt = left.getValue().intValue();
                var rightInt = right.getValue().intValue();
                if (rightInt < leftInt) throw new ConfigFormatException("Right value is bigger than left value in range "+string);

                if (property instanceof IntProperty intProperty) {
                    return new PropertyWithFilter<>(intProperty, v -> v >= leftInt && v <= rightInt);
                }
            } else if (string.equals("*")) {
                return new PropertyWithFilter<>(property, block -> true);
            } else {
                var regex = Pattern.compile(string).asMatchPredicate();
                return new PropertyWithFilter<>(property, v -> regex.test(v.toString()));
            }
        }
        throw new IllegalStateException("This should not happen");
    }

    record PropertyWithFilter<T extends Comparable<T>>(Property<T> property, PropertyFilter<T> filter) {
        public boolean testState(BlockState state) {
            return this.filter().test(state.get(this.property()));
        }
    }

    static ConfigFormatException propertyNotFound(String name, Block block) {
        return new ConfigFormatException(block.getTranslationKey()+" does not have the property '"+name+"'");
    }
}
