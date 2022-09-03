package nl.theepicblock.polyconfig.util;

import nl.theepicblock.polyconfig.block.ConfigFormatException;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface ElementGroup {
    String getName();

    static <G extends Enum<G> & ElementGroup> G getGroupByName(String name, Class<G> groupClass, String elementTypeName) throws ConfigFormatException {
        for (G group : groupClass.getEnumConstants()) {
            if (group.getName().equals(name)) return group;
        }
        throw new ConfigFormatException(name+" is not a valid "+elementTypeName+" group.")
                .withHelp("Valid "+elementTypeName+" groups are: "+String.join(", ", Arrays.stream(groupClass.getEnumConstants()).map(ElementGroup::getName).collect(Collectors.joining(", ", "[", "]"))));
    }
}
