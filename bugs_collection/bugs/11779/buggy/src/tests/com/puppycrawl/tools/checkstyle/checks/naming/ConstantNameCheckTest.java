package com.puppycrawl.tools.checkstyle.checks.naming;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class ConstantNameCheckTest
    extends BaseCheckTestCase
{
    public void testIllegalRegexp()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ConstantNameCheck.class);
        checkConfig.addAttribute("format", "\\");
        try {
            createChecker(checkConfig);
            fail();
        }
        catch (CheckstyleException ex) {
            // expected exception
        }
    }

    public void testDefault()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ConstantNameCheck.class);
        final String[] expected = {
            "25:29: Name 'badConstant' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'.",
            "142:30: Name 'BAD__NAME' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'."
        };
        verify(checkConfig, getPath("InputSimple.java"), expected);
    }

    public void testInterface()
        throws Exception
    {
        final DefaultConfiguration checkConfig =
            createCheckConfig(ConstantNameCheck.class);
        final String[] expected = {
            "24:16: Name 'data' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'."
        };
        verify(checkConfig, getPath("InputInner.java"), expected);
    }
}
