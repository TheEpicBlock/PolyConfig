package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.KDLNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KDLUtil {
    public static List<KDLNode> getChildren(KDLNode node) {
        var childDoc = node.getChild();
        if (childDoc.isPresent()) {
            return childDoc.get().getNodes();
        } else {
            return Collections.emptyList();
        }
    }
}
