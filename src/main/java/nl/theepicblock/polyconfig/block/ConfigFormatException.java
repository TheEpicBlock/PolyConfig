package nl.theepicblock.polyconfig.block;

import java.util.ArrayList;
import java.util.List;

public class ConfigFormatException extends Exception {
    public final List<String> helpMessages = new ArrayList<>();
    public List<ConfigFormatException> subExceptions = new ArrayList<>();

    public ConfigFormatException(String message) {
        super(message);
    }

    public ConfigFormatException withHelp(String help) {
        if (help != null) {
            this.helpMessages.add(help);
        }
        return this;
    }

    public ConfigFormatException withSubExceptions(List<ConfigFormatException> subExceptions) {
        this.subExceptions = subExceptions;
        return this;
    }

    @Override
    public String toString() {
        var b = new StringBuilder();
        b.append(this.getMessage());
        appendTrailingStuff(b, "");
        return b.toString();
    }

    public void appendTrailingStuff(StringBuilder b, String indent) {
        for (var help : helpMessages) {
            b.append("\n").append(indent).append("help: ").append(help);
        }
        for (var subException : subExceptions) {
            b.append("\n").append(indent);
            b.append("SUB ERROR: "+subException.getMessage());
            subException.appendTrailingStuff(b, indent+"    ");
        }
    }
}
