package prestocloud.topology.task;

/**
 * Task when topology is empty.
 */
public class EmptyTask extends AbstractTask {
    public EmptyTask() {
        super(TaskCode.EMPTY);
    }
}