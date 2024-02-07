package edu.stanford.nlp.trees;

import junit.framework.TestCase;

/**
 * Tests some of the various operations performed by the QPTreeTransformer.
 *
 * @author John Bauer
 */
public class QPTreeTransformerTest extends TestCase {
  public void testMoneyOrMore() {
    String input = "(ROOT (S (NP (DT This)) (VP (VBZ costs) (NP (QP ($ $) (CD 1) (CD million)) (QP (CC or) (JJR more)))) (. .)))";
    // First it gets flattened, then the CC gets broken up
    // TODO: the end result of NP on the left side should be QP with internal structure
    // TODO: NP for the right?
    String output = "(ROOT (S (NP (DT This)) (VP (VBZ costs) (NP (QP (NP ($ $) (CD 1) (CD million)) (CC or) (NP (JJR more))))) (. .)))";
    runTest(input, output);
  }

  public void testCompoundModifiers() {
    String input = "(ROOT (S (NP (NP (DT a) (NN stake)) (PP (IN of) (NP (QP (RB just) (IN under) (CD 30)) (NN %))))))";
    String output = "(ROOT (S (NP (NP (DT a) (NN stake)) (PP (IN of) (NP (QP (XS (RB just) (IN under)) (CD 30)) (NN %))))))";
    runTest(input, output);
  }


  public void outputResults(String input, String output) {
    Tree inputTree = Tree.valueOf(input);
    System.err.println(inputTree);
    Tree outputTree = QPTreeTransformer.QPtransform(inputTree);
    System.err.println(outputTree);
    System.err.println(output);
  }

  public void runTest(String input, String output) {
    Tree inputTree = Tree.valueOf(input);
    Tree outputTree = QPTreeTransformer.QPtransform(inputTree);
    assertEquals(output, outputTree.toString());
  }
}
