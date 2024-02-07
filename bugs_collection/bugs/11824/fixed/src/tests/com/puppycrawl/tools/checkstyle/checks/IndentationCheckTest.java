package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.checks.IndentationCheck;
import com.puppycrawl.tools.checkstyle.BaseCheckTestCase;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.Checker;

/**
 *
 * @author  jrichard
 */
public class IndentationCheckTest extends BaseCheckTestCase {


    public void testInvalidLabel()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final String[] expected = {
            "20: label child at indentation level 10 not at correct indentation, 8",
            "24: label child at indentation level 16 not at correct indentation, 12",
            "29: label child at indentation level 2 not at correct indentation, 4",
            "32: label child at indentation level 18 not at correct indentation, 8",
            "33: ctor def child at indentation level 18 not at correct indentation, 8",
            "35: label child at indentation level 6 not at correct indentation, 8",
            "35: method call child at indentation level 6 not at correct indentation, 8",
            "37: label child at indentation level 6 not at correct indentation, 8",
            "37: method call child at indentation level 6 not at correct indentation, 8",
        };
        verify(checkConfig, getPath("indentation/InputInvalidLabelIndent.java"), expected);
    }



    public void testValidLabel()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final String[] expected = {
        };
        verify(checkConfig, getPath("indentation/InputValidLabelIndent.java"), expected);
    }


     public void testValidIfWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidIfIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }


    public void testValidDotWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidDotIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }


    public void testValidMethodWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidMethodIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }


    public void testInvalidMethodWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidMethodIndent.java");
        final String[] expected = {
            "17: ctor def rcurly at indentation level 6 not at correct indentation, 4",
            "20: ctor def modifier at indentation level 6 not at correct indentation, 4",
            "21: ctor def lcurly at indentation level 2 not at correct indentation, 4",
            "22: ctor def rcurly at indentation level 6 not at correct indentation, 4",
            "25: method def modifier at indentation level 2 not at correct indentation, 4",
            "26: method def rcurly at indentation level 6 not at correct indentation, 4",
            "63: method def modifier at indentation level 5 not at correct indentation, 4",
            "64: method def modifier at indentation level 5 not at correct indentation, 4",
            "65: method def return type at indentation level 5 not at correct indentation, 4",
            "74: method def modifier at indentation level 3 not at correct indentation, 4",
            "75: method def modifier at indentation level 3 not at correct indentation, 4",
            "76: method def return type at indentation level 3 not at correct indentation, 4",
            "77: method def at indentation level 5 not at correct indentation, 4",
            "87: ctor def child at indentation level 4 not at correct indentation, 8",
            "87: method call child at indentation level 4 not at correct indentation, 8",
            "92: method def child at indentation level 6 not at correct indentation, 8",
            "93: if at indentation level 6 not at correct indentation, 8",
            "94: if child at indentation level 10 not at correct indentation, 12",
            "94: method call child at indentation level 10 not at correct indentation, 12",
            "95: if rcurly at indentation level 6 not at correct indentation, 8",
            "98: method call child at indentation level 10 not at correct indentation, 12",
            "104: method call child at indentation level 14 not at correct indentation, 16",
            "107: method call child at indentation level 10 not at correct indentation, 12",
            "112: method call child at indentation level 14 not at correct indentation, 16",
            "116: method call child at indentation level 10 not at correct indentation, 12",
            "120: method call child at indentation level 10 not at correct indentation, 12",
            "121: method call rparen at indentation level 6 not at correct indentation, 8",
            "125: method call rparen at indentation level 6 not at correct indentation, 8",
            "139: method call child at indentation level 10 not at correct indentation, 12",
            "142: method call child at indentation level 10 not at correct indentation, 12",
            // todo: 152 -- 8 should be 12
            "152: method call child at indentation level 6 not at correct indentation, 8",
            "158: method def throws at indentation level 6 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }




    public void testInvalidSwitchWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidSwitchIndent.java");
        final String[] expected = {
            "26: switch at indentation level 6 not at correct indentation, 8",
            "28: case child at indentation level 10 not at correct indentation, 12",
            "29: block child at indentation level 14 not at correct indentation, 16",
            "29: method call child at indentation level 14 not at correct indentation, 16",
            "33: block child at indentation level 14 not at correct indentation, 16",
            "35: case child at indentation level 14 not at correct indentation, 12",
            "36: case child at indentation level 10 not at correct indentation, 12",
            "39: case child at indentation level 10 not at correct indentation, 12",
            "40: block child at indentation level 14 not at correct indentation, 16",
            "40: method call child at indentation level 14 not at correct indentation, 16",
            "41: block child at indentation level 14 not at correct indentation, 16",
            "49: block child at indentation level 14 not at correct indentation, 16",
            "49: method call child at indentation level 14 not at correct indentation, 16",
            "50: block child at indentation level 18 not at correct indentation, 16",
            "51: block rcurly at indentation level 10 not at correct indentation, 12",
            "55: block lcurly at indentation level 10 not at correct indentation, 12",
            "58: block rcurly at indentation level 14 not at correct indentation, 12",
            "62: block lcurly at indentation level 14 not at correct indentation, 12",
            "65: block rcurly at indentation level 10 not at correct indentation, 12",
            "72: case child at indentation level 14 not at correct indentation, 16",
            "77: case child at indentation level 14 not at correct indentation, 16",
            "85: switch rcurly at indentation level 6 not at correct indentation, 8",
            "88: switch lcurly at indentation level 6 not at correct indentation, 8",
            "89: switch rcurly at indentation level 10 not at correct indentation, 8",
            "91: switch lcurly at indentation level 10 not at correct indentation, 8",
            "92: switch rcurly at indentation level 6 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }

    public void testValidSwitchWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidSwitchIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }



    public void testValidArrayInitWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidArrayInitIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testInvalidArrayInitWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidArrayInitIndent.java");
        final String[] expected = {
            "17: class def child at indentation level 2 not at correct indentation, 4",
            "18: class def child at indentation level 6 not at correct indentation, 4",
            "20: class def child at indentation level 2 not at correct indentation, 4",
            "22: array initialization rcurly at indentation level 6 not at correct indentation, 2",
            "24: class def child at indentation level 6 not at correct indentation, 4",
            "25: array initialization child at indentation level 8 not at correct indentation, 10",
            "26: array initialization rcurly at indentation level 4 not at correct indentation, 6",
            "29: array initialization child at indentation level 9 not at correct indentation, 8",
            "30: array initialization child at indentation level 7 not at correct indentation, 8",
            "31: array initialization child at indentation level 9 not at correct indentation, 8",
            "36: array initialization lcurly at indentation level 2 not at correct indentation, 4",
            "40: array initialization rcurly at indentation level 6 not at correct indentation, 4",
            "44: array initialization lcurly at indentation level 2 not at correct indentation, 4",
            "48: array initialization child at indentation level 20 not at correct indentation, [8, 31]",
            "49: array initialization child at indentation level 4 not at correct indentation, [8, 31]",
            "54: array initialization child at indentation level 6 not at correct indentation, 8",
            "59: class def child at indentation level 2 not at correct indentation, 4",
            "61: class def child at indentation level 6 not at correct indentation, 4",
            "62: array initialization rcurly at indentation level 2 not at correct indentation, 6",
            "65: array initialization child at indentation level 6 not at correct indentation, 8",
            "72: array initialization child at indentation level 10 not at correct indentation, 12",
            "85: array initialization child at indentation level 8 not at correct indentation, 12",
            "96: array initialization child at indentation level 10 not at correct indentation, 12",
            "97: array initialization child at indentation level 14 not at correct indentation, 12",
            "100: array initialization child at indentation level 10 not at correct indentation, 12",
            "101: array initialization child at indentation level 14 not at correct indentation, 12",
            "102: array initialization rcurly at indentation level 6 not at correct indentation, 8",
            "105: array initialization lcurly at indentation level 6 not at correct indentation, 8",
            "106: array initialization child at indentation level 14 not at correct indentation, 12",
            "107: array initialization child at indentation level 10 not at correct indentation, 12",
            "108: array initialization rcurly at indentation level 6 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }




    public void testValidTryWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidTryIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testInvalidTryWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidTryIndent.java");
        final String[] expected = {
            "21: try at indentation level 9 not at correct indentation, 8",
            "22: try rcurly at indentation level 7 not at correct indentation, 8",
            "24: catch rcurly at indentation level 7 not at correct indentation, 8",
            "26: try at indentation level 4 not at correct indentation, 8",
            "27: method call child at indentation level 8 not at correct indentation, 12",
            "27: try child at indentation level 8 not at correct indentation, 12",
            "28: try rcurly at indentation level 4 not at correct indentation, 8",
            "29: finally child at indentation level 8 not at correct indentation, 12",
            "29: method call child at indentation level 8 not at correct indentation, 12",
            "34: catch child at indentation level 8 not at correct indentation, 12",
            "34: method call child at indentation level 8 not at correct indentation, 12",
            "39: try rcurly at indentation level 10 not at correct indentation, 8",
            "41: catch rcurly at indentation level 6 not at correct indentation, 8",
            "48: catch rcurly at indentation level 5 not at correct indentation, 8",
            "55: catch child at indentation level 10 not at correct indentation, 12",
            "55: method call child at indentation level 10 not at correct indentation, 12",
            "56: catch child at indentation level 14 not at correct indentation, 12",
            "57: catch child at indentation level 10 not at correct indentation, 12",
            "57: method call child at indentation level 10 not at correct indentation, 12",
            "59: catch at indentation level 6 not at correct indentation, 8",
            "66: try lcurly at indentation level 10 not at correct indentation, 8",
            "68: try rcurly at indentation level 10 not at correct indentation, 8",
            "70: catch lcurly at indentation level 6 not at correct indentation, 8",
            "73: catch rcurly at indentation level 10 not at correct indentation, 8",
            "76: catch child at indentation level 10 not at correct indentation, 12",
            "76: method call child at indentation level 10 not at correct indentation, 12",
        };
        verify(c, fname, expected);
    }


    // TODO: needs to be finished
    public void testInvalidClassDefWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidClassDefIndent.java");
        final String[] expected = {
            "19: class def ident at indentation level 2 not at correct indentation, 0",
            "19: class def modifier at indentation level 2 not at correct indentation, 0",
            "25: class def lcurly at indentation level 2 not at correct indentation, 0",
            "28: class def rcurly at indentation level 2 not at correct indentation, 0",
            "31: class def ident at indentation level 2 not at correct indentation, 0",
            "35: class def rcurly at indentation level 2 not at correct indentation, 0",
            "40: class def child at indentation level 2 not at correct indentation, 4",
            "41: class def child at indentation level 2 not at correct indentation, 4",
            "47: class def child at indentation level 2 not at correct indentation, 4",
            "55: class def child at indentation level 2 not at correct indentation, 4",
            "56: class def child at indentation level 2 not at correct indentation, 4",
            "61: class def ident at indentation level 2 not at correct indentation, 0",
            "61: class def modifier at indentation level 2 not at correct indentation, 0",
            "62: class def lcurly at indentation level 2 not at correct indentation, 0",
            "70: class def rcurly at indentation level 2 not at correct indentation, 0",
            "74: class def child at indentation level 2 not at correct indentation, 4",
            "83: class def child at indentation level 2 not at correct indentation, 4",
            "83: class def ident at indentation level 2 not at correct indentation, 4",
            "85: class def ident at indentation level 6 not at correct indentation, 4",
            "88: class def child at indentation level 2 not at correct indentation, 4",
            "88: class def ident at indentation level 2 not at correct indentation, 4",
            "92: class def child at indentation level 6 not at correct indentation, 8",
            "98: class def child at indentation level 10 not at correct indentation, 12",
            "103: class def child at indentation level 6 not at correct indentation, 8",
            "108: class def rcurly at indentation level 6 not at correct indentation, 4",
            "110: class def ident at indentation level 6 not at correct indentation, 4",
            "116: class def child at indentation level 6 not at correct indentation, 8",
            "116: class def ident at indentation level 6 not at correct indentation, 8",
            "119: class def ident at indentation level 10 not at correct indentation, 8",
            "121: class def rcurly at indentation level 10 not at correct indentation, 8",
            "124: class def child at indentation level 10 not at correct indentation, 12",
            "129: method def child at indentation level 10 not at correct indentation, 8",
            "130: object def lcurly at indentation level 8 not at correct indentation, 10",
            "131: method def modifier at indentation level 12 not at correct indentation, 14",
            "133: method def rcurly at indentation level 12 not at correct indentation, 14",
            "134: object def rcurly at indentation level 8 not at correct indentation, 10",
            "138: object def lcurly at indentation level 6 not at correct indentation, 8",
            "142: object def rcurly at indentation level 6 not at correct indentation, 8",
            "147: method def modifier at indentation level 10 not at correct indentation, 12",
            "149: method def rcurly at indentation level 10 not at correct indentation, 12",
            // TODO: extends and implements need to be added
        };
        verify(c, fname, expected);
    }


    public void testInvalidBlockWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidBlockIndent.java");
        final String[] expected = {
            "22: block lcurly at indentation level 7 not at correct indentation, 8",
            "23: block lcurly at indentation level 9 not at correct indentation, 8",
            "25: block lcurly at indentation level 9 not at correct indentation, 8",
            "26: block rcurly at indentation level 7 not at correct indentation, 8",
            "28: block lcurly at indentation level 6 not at correct indentation, 8",
            "30: block rcurly at indentation level 6 not at correct indentation, 8",
            "31: block lcurly at indentation level 6 not at correct indentation, 8",
            "34: block lcurly at indentation level 9 not at correct indentation, 8",
            "35: block child at indentation level 13 not at correct indentation, 12",
            "37: block child at indentation level 13 not at correct indentation, 12",
            "38: block rcurly at indentation level 9 not at correct indentation, 8",
            "41: block lcurly at indentation level 6 not at correct indentation, 8",
            "42: block child at indentation level 10 not at correct indentation, 12",
            "44: block child at indentation level 10 not at correct indentation, 12",
            "45: block rcurly at indentation level 6 not at correct indentation, 8",
            "48: block lcurly at indentation level 6 not at correct indentation, 8",
            "51: block child at indentation level 10 not at correct indentation, 12",
            "55: block lcurly at indentation level 10 not at correct indentation, 12",
            "59: block rcurly at indentation level 10 not at correct indentation, 12",
            "64: block child at indentation level 10 not at correct indentation, 12",
            "66: block lcurly at indentation level 10 not at correct indentation, 12",
            "67: block child at indentation level 14 not at correct indentation, 16",
            "82: block rcurly at indentation level 10 not at correct indentation, 12",
// TODO: static gives back wrong colno            "87: block static at indentation level 2 not at correct indentation, 4",
            "119: static initialization child at indentation level 6 not at correct indentation, 8",
            "124: static initialization child at indentation level 4 not at correct indentation, 8",
            "125: static initialization rcurly at indentation level 2 not at correct indentation, 4",
            "130: static initialization rcurly at indentation level 6 not at correct indentation, 4",
            "133: block lcurly at indentation level 2 not at correct indentation, 4",
            "134: block lcurly at indentation level 6 not at correct indentation, 4",
            "137: block lcurly at indentation level 2 not at correct indentation, 4",
            "139: block rcurly at indentation level 6 not at correct indentation, 4",
            "141: block lcurly at indentation level 6 not at correct indentation, 4",
            "143: block rcurly at indentation level 2 not at correct indentation, 4",
            "146: block child at indentation level 6 not at correct indentation, 8",

        };
        verify(c, fname, expected);
    }

    public void testInvalidIfWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidIfIndent.java");
        final String[] expected = {
            "50: if at indentation level 1 not at correct indentation, 8",
            "55: if at indentation level 9 not at correct indentation, 8",
            "56: if lcurly at indentation level 9 not at correct indentation, 8",
            "57: if rcurly at indentation level 7 not at correct indentation, 8",
            "59: if at indentation level 6 not at correct indentation, 8",
            "60: if lcurly at indentation level 5 not at correct indentation, 8",
            "61: if rcurly at indentation level 5 not at correct indentation, 8",
            "65: if rcurly at indentation level 10 not at correct indentation, 8",
//TODO: add this back? -- } else is already checked by if            "65: else at indentation level 10 not at correct indentation, 8",
            "66: else rcurly at indentation level 7 not at correct indentation, 8",
            "69: if at indentation level 9 not at correct indentation, 8",
            "70: if lcurly at indentation level 7 not at correct indentation, 8",
            "72: else at indentation level 9 not at correct indentation, 8",
            "74: else rcurly at indentation level 9 not at correct indentation, 8",
            "77: if at indentation level 10 not at correct indentation, 8",
            "78: if rcurly at indentation level 7 not at correct indentation, 8",
            "79: else at indentation level 9 not at correct indentation, 8",
            "80: else lcurly at indentation level 7 not at correct indentation, 8",
            "81: else rcurly at indentation level 9 not at correct indentation, 8",
            "85: if at indentation level 9 not at correct indentation, 8",
            "86: if lcurly at indentation level 9 not at correct indentation, 8",
            "87: if rcurly at indentation level 9 not at correct indentation, 8",
//TODO: same here            "87: else at indentation level 9 not at correct indentation, 8",
            "88: else lcurly at indentation level 7 not at correct indentation, 8",
            "89: else rcurly at indentation level 10 not at correct indentation, 8",
            "92: if at indentation level 6 not at correct indentation, 8",
            "93: if lcurly at indentation level 10 not at correct indentation, 8",
            "94: if rcurly at indentation level 10 not at correct indentation, 8",
//TODO: same here            "94: else at indentation level 10 not at correct indentation, 8",
            "95: else rcurly at indentation level 7 not at correct indentation, 8",
            "98: if at indentation level 5 not at correct indentation, 8",
            "99: if rcurly at indentation level 11 not at correct indentation, 8",
            "100: else at indentation level 5 not at correct indentation, 8",
            "101: else rcurly at indentation level 11 not at correct indentation, 8",
//            "110: dot left side \"System\" at indentation level 14 not at correct indentation, 12",
            "121: if child at indentation level 14 not at correct indentation, 12",
            "126: if lcurly at indentation level 10 not at correct indentation, 8",
            "127: if child at indentation level 10 not at correct indentation, 12",
            "127: method call child at indentation level 10 not at correct indentation, 12",
            "143: if child at indentation level 16 not at correct indentation, 12",
            "144: if rcurly at indentation level 9 not at correct indentation, 8",
            "147: else child at indentation level 16 not at correct indentation, 12",
            "153: if child at indentation level 0 not at correct indentation, 12",
            "153: method call child at indentation level 0 not at correct indentation, 12",
            "157: else child at indentation level 40 not at correct indentation, 12",
            "164: if child at indentation level 14 not at correct indentation, 12",
            "167: else child at indentation level 14 not at correct indentation, 12",
            "173: if child at indentation level 10 not at correct indentation, 12",
            "173: method call child at indentation level 10 not at correct indentation, 12",
            "175: else child at indentation level 10 not at correct indentation, 12",
            "175: method call child at indentation level 10 not at correct indentation, 12",
            "179: if at indentation level 10 not at correct indentation, 8",
            "180: if child at indentation level 14 not at correct indentation, 12",
            "181: if rcurly at indentation level 10 not at correct indentation, 8",
            "182: else at indentation level 10 not at correct indentation, 8",
            "183: else child at indentation level 14 not at correct indentation, 12",
            "184: else rcurly at indentation level 10 not at correct indentation, 8",
            "187: if child at indentation level 9 not at correct indentation, 12",
            "188: if child at indentation level 11 not at correct indentation, 12",
            "192: if child at indentation level 10 not at correct indentation, 12",
            "195: if rcurly at indentation level 7 not at correct indentation, 8",
            "202: if child at indentation level 10 not at correct indentation, 12",
            "202: method call child at indentation level 10 not at correct indentation, 12",
            "204: if child at indentation level 10 not at correct indentation, 12",
            "204: method call child at indentation level 10 not at correct indentation, 12",
            "211: method call child at indentation level 10 not at correct indentation, 12",
            "220: if at indentation level 10 not at correct indentation, 12",
            "224: if child at indentation level 18 not at correct indentation, 20",
            "224: method call child at indentation level 18 not at correct indentation, 20",
            "228: if rcurly at indentation level 40 not at correct indentation, 8",
            "235: if rparen at indentation level 10 not at correct indentation, 8",
            "240: if rparen at indentation level 6 not at correct indentation, 8",
            "246: if lparen at indentation level 6 not at correct indentation, 8",
            "248: if rparen at indentation level 6 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }

    public void testInvalidWhileWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidWhileIndent.java");
        final String[] expected = {
            "21: while at indentation level 9 not at correct indentation, 8",
            "22: while rcurly at indentation level 7 not at correct indentation, 8",
            "24: while at indentation level 7 not at correct indentation, 8",
            "25: while lcurly at indentation level 9 not at correct indentation, 8",
            "26: while rcurly at indentation level 9 not at correct indentation, 8",
            "28: while at indentation level 9 not at correct indentation, 8",
            "29: while lcurly at indentation level 6 not at correct indentation, 8",
            "30: while child at indentation level 14 not at correct indentation, 12",
            "31: while rcurly at indentation level 6 not at correct indentation, 8",
            "33: while at indentation level 10 not at correct indentation, 8",
            "35: while rcurly at indentation level 10 not at correct indentation, 8",
            "37: while at indentation level 10 not at correct indentation, 8",
            "40: while rcurly at indentation level 10 not at correct indentation, 8",
            "42: while at indentation level 6 not at correct indentation, 8",
            "43: while lcurly at indentation level 10 not at correct indentation, 8",
            "46: while rcurly at indentation level 6 not at correct indentation, 8",
            "49: if at indentation level 14 not at correct indentation, 12",
            "50: if child at indentation level 18 not at correct indentation, 16",
            "51: if rcurly at indentation level 14 not at correct indentation, 12",
            "52: while child at indentation level 14 not at correct indentation, 12",
            "53: while rcurly at indentation level 10 not at correct indentation, 8",
            "56: while child at indentation level 10 not at correct indentation, 12",
            "62: while child at indentation level 10 not at correct indentation, 12",
            "67: while child at indentation level 10 not at correct indentation, 12",
            "74: while rparen at indentation level 5 not at correct indentation, 8",
            "81: while rparen at indentation level 10 not at correct indentation, 8",
            "88: while rparen at indentation level 10 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }

    public void testInvalidForWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputInvalidForIndent.java");
        final String[] expected = {
            "22: for at indentation level 6 not at correct indentation, 8",
            "23: for rcurly at indentation level 10 not at correct indentation, 8",
            "25: for at indentation level 9 not at correct indentation, 8",
            "26: for lcurly at indentation level 6 not at correct indentation, 8",
            "27: for rcurly at indentation level 6 not at correct indentation, 8",
            "31: for child at indentation level 10 not at correct indentation, 12",
            "31: method call child at indentation level 10 not at correct indentation, 12",
            "32: for rcurly at indentation level 10 not at correct indentation, 8",
            "35: for lcurly at indentation level 10 not at correct indentation, 8",
            "36: for child at indentation level 10 not at correct indentation, 12",
            "44: for child at indentation level 10 not at correct indentation, 12",
            "50: for at indentation level 7 not at correct indentation, 8",
            "51: for child at indentation level 10 not at correct indentation, 12",
            "60: for child at indentation level 7 not at correct indentation, 12",
            "65: for at indentation level 6 not at correct indentation, 8",
            "66: for child at indentation level 10 not at correct indentation, 12",
            "67: for child at indentation level 14 not at correct indentation, 16",
            "68: for child at indentation level 10 not at correct indentation, 12",
            "73: for rcurly at indentation level 39 not at correct indentation, 8",
            "77: for rparen at indentation level 12 not at correct indentation, 8",
        };
        verify(c, fname, expected);
    }

    public void testValidForWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidForIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testValidDoWhileWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidDoWhileIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testValidBlockWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidBlockIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }


    public void testValidWhileWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidWhileIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testValidClassDefWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidClassDefIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testValidInterfaceDefWithChecker()
        throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidInterfaceDefIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testValidCommaWithChecker()
        throws Exception
    {

        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        final Checker c = createChecker(checkConfig);
        final String fname = getPath("indentation/InputValidCommaIndent.java");
        final String[] expected = {
        };
        verify(c, fname, expected);
    }

    public void testTabs()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        checkConfig.addAttribute("basicOffset", new Integer(4).toString());
        checkConfig.addAttribute("tabWidth", new Integer(4).toString());
        final String[] expected = {
            "25: ctor def child at indentation level 9 not at correct indentation, 8",
        };
        verify(checkConfig, getPath("indentation/InputUseTabs.java"), expected);
    }

    public void testIndentationLevel()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        checkConfig.addAttribute("basicOffset", new Integer(2).toString());
        final String[] expected = {
            "25: ctor def child at indentation level 5 not at correct indentation, 4",
        };
        verify(checkConfig, getPath("indentation/InputUseTwoSpaces.java"), expected);
    }

    public void testCaseLevel()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        checkConfig.addAttribute("caseIndent", new Integer(0).toString());
        final String[] expected = {
            "23: case child at indentation level 10 not at correct indentation, 8",
        };
        verify(checkConfig, getPath("indentation/InputCaseLevel.java"), expected);
    }

    public void testBraceAdjustment()
            throws Exception
    {
        final DefaultConfiguration checkConfig = createCheckConfig(IndentationCheck.class);
        checkConfig.addAttribute("braceAdjustment", new Integer(2).toString());
        final String[] expected = {
            "24: if rcurly at indentation level 8 not at correct indentation, 10",
        };
        verify(checkConfig, getPath("indentation/InputBraceAdjustment.java"), expected);
    }


}
