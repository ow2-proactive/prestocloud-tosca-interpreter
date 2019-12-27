package prestocloud.model;

import java.util.concurrent.atomic.AtomicInteger;

public class ParsedFragmentRegistration {
    private String fragmentName;
    private AtomicInteger edgeOccurence;
    private AtomicInteger cloudOccurence;

    public ParsedFragmentRegistration(String fragmentName) {
        this.fragmentName = fragmentName;
        this.edgeOccurence = new AtomicInteger(0);
        this.cloudOccurence = new AtomicInteger(0);
    }

    public void appendEdge() {
        edgeOccurence.incrementAndGet();
    }

    public void appendCloud() {
        cloudOccurence.incrementAndGet();
    }

    public String toString() {
        return String.format("%s;%s;%s;%s", fragmentName, edgeOccurence.get() + cloudOccurence.get(), cloudOccurence.get(), edgeOccurence.get());
    }
}
