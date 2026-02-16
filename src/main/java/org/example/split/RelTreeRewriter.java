package org.example.split;

import org.apache.calcite.rel.RelNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic tree rewriter: replace a target node with a replacement node.
 */
public class RelTreeRewriter {

    public static RelNode replace(RelNode root, RelNode target, RelNode replacement) {
        if (root == target) return replacement;

        List<RelNode> inputs = root.getInputs();
        if (inputs == null || inputs.isEmpty()) return root;

        List<RelNode> newInputs = inputs.stream()
                .map(in -> replace(in, target, replacement))
                .collect(Collectors.toList());

        // If nothing changed, avoid copying
        boolean changed = false;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i) != newInputs.get(i)) { changed = true; break; }
        }
        if (!changed) return root;

        return root.copy(root.getTraitSet(), newInputs);
    }
}
