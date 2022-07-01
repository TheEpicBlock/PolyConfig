package nl.theepicblock.polyconfig;

import java.util.ArrayList;
import java.util.List;

public class ConfigFormatException extends Exception {
    public final List<String> helpMessages = new ArrayList<>();

    public ConfigFormatException(String message) {
        super(message);
    }

    public ConfigFormatException withHelp(String help) {
        if (help != null) {
            this.helpMessages.add(help);
        }
        return this;
    }
}
