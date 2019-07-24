package prestocloud.btrplace.cost;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Unit tests for {@link Cost}.
 */
public class CostTest {

  public static final int[] distances = {684, 734, 1901, 1454, 1028, 2314, 797, 1028, 734, 1092, 432};

  public static final double[] costs = {0.112, 0.115, 0.102, 0.107, 0.111, 0.104, 0.104, 0.122, 0.122, 0.104, 0.133};

  @Test
  public void testDeliverableValues() {

    double[] expected ={1.341, 1.413, 2.700, 2.207, 1.734, 3.188, 1.432, 1.788, 1.448, 1.774, 1.152};
    for (int i = 0; i < distances.length; i++) {
      Cost cc = new Cost(costs[i], distances[i], 1, 1, 1);
      Assert.assertEquals(expected[i], cc.compute(IntStream.of(distances).min().getAsInt(), DoubleStream.of(costs).min().getAsDouble()), 1E-2);
    }
  }

  @Test
  public void testWithDifferentWeights() {
    double[] expected = {1.179, 1.223, 1.567, 1.435, 1.303, 1.742, 1.157, 1.393, 1.280, 1.271, 1.253};
    for (int i = 0; i < distances.length; i++) {
      Cost cc = new Cost(costs[i], distances[i], 1, 1, 5);
      Assert.assertEquals(expected[i], cc.compute(IntStream.of(distances).min().getAsInt(), DoubleStream.of(costs).min().getAsDouble()), 1E-2);

      cc = new Cost(costs[i], distances[i], 2, 1, 5);
      Assert.assertEquals(expected[i] / 2, cc.compute(IntStream.of(distances).min().getAsInt(), DoubleStream.of(costs).min().getAsDouble()), 1E-2);
    }
  }
}
