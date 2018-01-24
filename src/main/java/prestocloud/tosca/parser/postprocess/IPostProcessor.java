package prestocloud.tosca.parser.postprocess;

import java.util.function.Consumer;

/**
 * Specific consumer that can be managed by the post processor aspect to enable/disable it based on the current version of the tosca spec.
 */
public interface IPostProcessor<T> extends Consumer<T> {
    /**
     * Post process a context result.
     *
     * @param instance The instance to post process.
     */
    void process(T instance);

    @Override
    default void accept(T t) {
        process(t);
    }
}