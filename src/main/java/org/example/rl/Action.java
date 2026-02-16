package org.example.rl;

public record Action(int cutNodeId) {
    @Override
    public String toString() {
        return "Action{cutNodeId=" + cutNodeId + "}";
    }
}
