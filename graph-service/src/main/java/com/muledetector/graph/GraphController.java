package com.muledetector.graph;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class GraphController {

    private final GraphAnalysis graph;

    public GraphController(GraphAnalysis graph) {
        this.graph = graph;
    }

    /** Suspected mules after suspicion has followed the money, with the signals behind each. */
    @GetMapping("/suspects")
    public Map<String, Set<String>> suspects() {
        return graph.suspects();
    }

    /** "Follow the money": the hops one transfer's money took next. */
    @GetMapping("/trail/{transferId}")
    public List<GraphAnalysis.Hop> trail(@PathVariable long transferId) {
        return graph.trail(transferId);
    }

    /** Runs Louvain and returns account number to community id. */
    @PostMapping("/communities")
    public Map<String, Long> communities() {
        return graph.communities();
    }
}
