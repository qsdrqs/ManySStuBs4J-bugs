package eu.stratosphere.sopremo.cleansing.TransitiveClosure;

import junit.framework.Assert;

import org.junit.Test;

import eu.stratosphere.sopremo.SopremoTest;
import eu.stratosphere.sopremo.cleansing.transitiveClosure.BinarySparseMatrix;
import eu.stratosphere.sopremo.cleansing.transitiveClosure.TransitiveClosure;
import eu.stratosphere.sopremo.jsondatamodel.IntNode;
import eu.stratosphere.sopremo.jsondatamodel.JsonNode;
import eu.stratosphere.sopremo.testing.SopremoTestPlan;

public class ParallelTransitiveClosureTest {

	@Test
	public void shouldDoSimplePartitioningAndFindTransitiveClosureInIt() {
		final TestTransitiveClosure transitiveClosure = new TestTransitiveClosure();
		transitiveClosure.setPhase(1);
		
		final SopremoTestPlan sopremoTestPlan = new SopremoTestPlan(transitiveClosure);
		String nullInput = SopremoTest.getResourcePath("null.json");

		String input = SopremoTest.getResourcePath("phase1.json");
		
		sopremoTestPlan.getInput(1).load(nullInput);
		sopremoTestPlan
			.getInput(0).load(input);

		String output = SopremoTest.getResourcePath("phase1Result.json");
		sopremoTestPlan.getExpectedOutput(0).load(output);
		sopremoTestPlan.trace();

		sopremoTestPlan.run();
	}
	
	@Test	
	public void shouldFindTransitiveClosureWithinRowsAndColumns() {
		final TestTransitiveClosure transitiveClosure = new TestTransitiveClosure();
		transitiveClosure.setPhase(2);
		transitiveClosure.setNumberOfPartitions(2);

		
		final SopremoTestPlan sopremoTestPlan = new SopremoTestPlan(transitiveClosure);
		String nullInput = SopremoTest.getResourcePath("null.json");
		String input = SopremoTest.getResourcePath("phase2.json");

		sopremoTestPlan
			.getInput(0).load(input);
		sopremoTestPlan.getInput(1).load(nullInput);
		
		String output = SopremoTest.getResourcePath("phase2Result.json");
		sopremoTestPlan.getExpectedOutput(0).load(output);

		sopremoTestPlan.trace();
		sopremoTestPlan.run();
	}
	
	@Test
	public void shouldFindTransitiveClosureInWholeMatrix() {
		final TestTransitiveClosure transitiveClosure = new TestTransitiveClosure();
		transitiveClosure.setPhase(3);
		transitiveClosure.setNumberOfPartitions(3);
		
		final SopremoTestPlan sopremoTestPlan = new SopremoTestPlan(transitiveClosure);
		String nullInput = SopremoTest.getResourcePath("null.json");
		String input = SopremoTest.getResourcePath("phase3.json");

		sopremoTestPlan.getInput(0).load(input);
		sopremoTestPlan.getInput(1).load(nullInput);

		String output = SopremoTest.getResourcePath("phase3Result.json");
		sopremoTestPlan.getExpectedOutput(0).load(output);

		sopremoTestPlan.trace();
		sopremoTestPlan.toString();
		sopremoTestPlan.run();
	}

	/**
	 * Tests the algorithm only
	 */
	@Test
	public void testWarshall() {
		final BinarySparseMatrix matrix = new BinarySparseMatrix();
		final JsonNode[] nodes = new JsonNode[6];
		for (int index = 0; index < nodes.length; index++)
			nodes[index] = IntNode.valueOf(index);

		matrix.set(nodes[0], nodes[1]);
		matrix.set(nodes[1], nodes[2]);
		matrix.set(nodes[2], nodes[3]);

		matrix.set(nodes[4], nodes[5]);
		matrix.makeSymmetric();

		TransitiveClosure.warshall(matrix);

		final BinarySparseMatrix expected = new BinarySparseMatrix();
		expected.set(nodes[0], nodes[1]);
		expected.set(nodes[1], nodes[2]);
		expected.set(nodes[2], nodes[3]);
		// newly found pairs
		expected.set(nodes[0], nodes[2]);
		expected.set(nodes[0], nodes[3]);
		expected.set(nodes[1], nodes[3]);

		expected.set(nodes[4], nodes[5]);
		expected.makeSymmetric();

		Assert.assertEquals(expected, matrix);
	}
}
