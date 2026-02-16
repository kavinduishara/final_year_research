package org.example.rl;

import org.apache.calcite.rel.RelNode;

import java.util.List;

public interface Policy {
    Action choose(List<RelNode> legalCutPoints);
}
