package com.puppycrawl.tools.checkstyle.checks.whitespace;

import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class NoWhitespaceBeforeCheckTest
    extends BaseCheckTestCase
{
    private DefaultConfiguration checkConfig;

    public void setUp() {
        checkConfig = createCheckConfig(NoWhitespaceBeforeCheck.class);
    }

    public void testDefault() throws Exception
    {
        final String[] expected = {
            "30:14: '++' is preceded with whitespace.",
            "30:21: '--' is preceded with whitespace.",
            "176:18: ';' is preceded with whitespace.",
            "178:23: ';' is preceded with whitespace.",
        };
        verify(checkConfig, getPath("InputWhitespace.java"), expected);
    }

    public void testDot() throws Exception
    {
        checkConfig.addAttribute("tokens", "DOT");
        final String[] expected = {
            "5:12: '.' is preceded with whitespace.",
            "6:4: '.' is preceded with whitespace.",
            "129:17: '.' is preceded with whitespace.",
            "135:12: '.' is preceded with whitespace.",
            "136:10: '.' is preceded with whitespace.",
        };
        verify(checkConfig, getPath("InputWhitespace.java"), expected);
    }


    public void testDotAllowLineBreaks() throws Exception
    {
        checkConfig.addAttribute("tokens", "DOT");
        checkConfig.addAttribute("allowLineBreaks", "yes");
        final String[] expected = {
            "5:12: '.' is preceded with whitespace.",
            "129:17: '.' is preceded with whitespace.",
            "136:10: '.' is preceded with whitespace.",
        };
        verify(checkConfig, getPath("InputWhitespace.java"), expected);
    }

}
