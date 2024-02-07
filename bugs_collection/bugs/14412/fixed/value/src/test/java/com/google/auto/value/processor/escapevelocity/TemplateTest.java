/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.auto.value.processor.escapevelocity;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author emcmanus@google.com (Éamonn McManus)
 */
@RunWith(JUnit4.class)
public class TemplateTest {
  @Rule public TestName testName = new TestName();

  private RuntimeInstance velocityRuntimeInstance;

  @Before
  public void setUp() {
    velocityRuntimeInstance = new RuntimeInstance();

    // Ensure that $undefinedvar will produce an exception rather than outputting $undefinedvar.
    velocityRuntimeInstance.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "true");
    velocityRuntimeInstance.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        new NullLogChute());

    // Disable any logging that Velocity might otherwise see fit to do.
    velocityRuntimeInstance.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());

    velocityRuntimeInstance.init();
  }

  private void compare(String template) {
    compare(template, ImmutableMap.<String, Object>of());
  }

  private void compare(String template, Map<String, Object> vars) {
    compare(template, Suppliers.ofInstance(vars));
  }

  /**
   * Checks that the given template and the given variables produce identical results with
   * Velocity and EscapeVelocity. This uses a {@code Supplier} to define the variables to cover
   * test cases that involve modifying the values of the variables. Otherwise the run using
   * Velocity would change those values so that the run using EscapeVelocity would not be starting
   * from the same point.
   */
  private void compare(String template, Supplier<Map<String, Object>> varsSupplier) {
    Map<String, Object> velocityVars = varsSupplier.get();
    String velocityRendered = velocityRender(template, velocityVars);
    Map<String, Object> escapeVelocityVars = varsSupplier.get();
    String escapeVelocityRendered;
    try {
      escapeVelocityRendered =
          Template.parseFrom(new StringReader(template)).evaluate(escapeVelocityVars);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    String failure = "from velocity: <" + velocityRendered + ">\n"
        + "from escape velocity: <" + escapeVelocityRendered + ">\n";
    assert_().withFailureMessage(failure).that(escapeVelocityRendered).isEqualTo(velocityRendered);
  }

  private String velocityRender(String template, Map<String, Object> vars) {
    VelocityContext velocityContext = new VelocityContext(new TreeMap<String, Object>(vars));
    StringWriter writer = new StringWriter();
    SimpleNode parsedTemplate;
    try {
      parsedTemplate = velocityRuntimeInstance.parse(
          new StringReader(template), testName.getMethodName());
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
    boolean rendered = velocityRuntimeInstance.render(
        velocityContext, writer, parsedTemplate.getTemplateName(), parsedTemplate);
    assertThat(rendered).isTrue();
    return writer.toString();
  }

  @Test
  public void empty() {
    compare("");
  }

  @Test
  public void literalOnly() {
    compare("In the reign of James the Second \n It was generally reckoned\n");
  }

  @Test
  public void comment() {
    compare("line 1 ##\n  line 2");
  }

  @Test
  public void substituteNoBraces() {
    compare(" $x ", ImmutableMap.of("x", (Object) 1729));
    compare(" ! $x ! ", ImmutableMap.of("x", (Object) 1729));
  }

  @Test
  public void substituteWithBraces() {
    compare("a${x}\nb", ImmutableMap.of("x", (Object) "1729"));
  }

  @Test
  public void substitutePropertyNoBraces() {
    compare("=$t.name=", ImmutableMap.of("t", (Object) Thread.currentThread()));
  }

  @Test
  public void substitutePropertyWithBraces() {
    compare("=${t.name}=", ImmutableMap.of("t", (Object) Thread.currentThread()));
  }

  @Test
  public void substituteNestedProperty() {
    compare("\n$t.name.empty\n", ImmutableMap.of("t", (Object) Thread.currentThread()));
  }

  @Test
  public void substituteMethodNoArgs() {
    compare("<$c.size()>", ImmutableMap.of("c", (Object) ImmutableMap.of()));
  }

  @Test
  public void substituteMethodOneArg() {
    compare("<$list.get(0)>", ImmutableMap.of("list", (Object) ImmutableList.of("foo")));
  }

  @Test
  public void substituteMethodTwoArgs() {
    compare("\n$s.indexOf(\"bar\", 2)\n", ImmutableMap.of("s", (Object) "barbarbar"));
  }

  @Test
  public void substituteMethodNoSynthetic() {
    // If we aren't careful, we'll see both the inherited `Set<K> keySet()` from Map
    // and the overridden `ImmutableSet<K> keySet()` in ImmutableMap.
    compare("$map.keySet()", ImmutableMap.of("map", (Object) ImmutableMap.of("foo", "bar")));
  }

  @Test
  public void substituteIndexNoBraces() {
    compare("<$map[\"x\"]>", ImmutableMap.of("map", (Object) ImmutableMap.of("x", "y")));
  }

  @Test
  public void substituteIndexWithBraces() {
    compare("<${map[\"x\"]}>", ImmutableMap.of("map", (Object) ImmutableMap.of("x", "y")));
  }

  @Test
  public void substituteIndexThenProperty() {
    compare("<$map[2].name>", ImmutableMap.of("map", (Object) ImmutableMap.of(2, getClass())));
  }

  @Test
  public void variableNameCantStartWithNonAscii() {
    compare("<$Éamonn>", ImmutableMap.<String, Object>of());
  }

  @Test
  public void variableNamesAreAscii() {
    compare("<$Pádraig>", ImmutableMap.of("P", (Object) "(P)"));
  }

  @Test
  public void variableNameCharacters() {
    compare("<AZaz-foo_bar23>", ImmutableMap.of("AZaz-foo_bar23", (Object) "(P)"));
  }

  public static class Indexable {
    public String get(String y) {
      return "[" + y + "]";
    }
  }

  @Test
  public void substituteExoticIndex() {
    // Any class with a get(X) method can be used with $x[i]
    compare("<$x[\"foo\"]>", ImmutableMap.of("x", (Object) new Indexable()));
  }
}
