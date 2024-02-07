package com.puppycrawl.tools.checkstyle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

import junit.framework.TestCase;
import org.apache.regexp.RESyntaxException;

public class CheckerTest
    extends TestCase
{
    /** a brief logger that only display info about errors */
    protected static class BriefLogger
        extends DefaultLogger
    {
        public BriefLogger(OutputStream out)
        {
            super(out, true);
        }
        public void auditStarted(AuditEvent evt) {}
        public void fileFinished(AuditEvent evt) {}
        public void fileStarted(AuditEvent evt) {}
    }

    private final ByteArrayOutputStream mBAOS = new ByteArrayOutputStream();
    private final PrintStream mStream = new PrintStream(mBAOS);
    private final Configuration mConfig = new Configuration();

    public CheckerTest(String name)
    {
        super(name);
    }

    protected void setUp()
        throws Exception
    {
        mConfig.setHeaderFile(getPath("java.header"));
        mConfig.setLeftCurlyOptionProperty(Defn.LCURLY_METHOD_PROP,
                                           LeftCurlyOption.NL);
        mConfig.setLeftCurlyOptionProperty(Defn.LCURLY_OTHER_PROP,
                                           LeftCurlyOption.NLOW);
        mConfig.setLeftCurlyOptionProperty(Defn.LCURLY_TYPE_PROP,
                                           LeftCurlyOption.NL);
        mConfig.setRCurly(RightCurlyOption.ALONE);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, true);
        mConfig.setStringProperty(Defn.LOCALE_COUNTRY_PROP,
                                  Locale.ENGLISH.getCountry());
        mConfig.setStringProperty(Defn.LOCALE_LANGUAGE_PROP,
                                  Locale.ENGLISH.getLanguage());
    }

    static String getPath(String aFilename)
        throws IOException
    {
        final File f = new File(System.getProperty("testinputs.dir"),
                                aFilename);
        return f.getCanonicalPath();
    }

    protected Checker createChecker()
        throws RESyntaxException
    {
        final AuditListener listener = new BriefLogger(mStream);
        final Checker c = new Checker(mConfig);
        c.addListener(listener);
        return c;
    }

    private void verify(Checker aC, String aFilename, String[] aExpected)
        throws Exception
    {
        mStream.flush();
        final int errs = aC.process(new String[] {aFilename});

        // process each of the lines
        final ByteArrayInputStream bais =
            new ByteArrayInputStream(mBAOS.toByteArray());
        final LineNumberReader lnr =
            new LineNumberReader(new InputStreamReader(bais));

        for (int i = 0; i < aExpected.length; i++) {
            assertEquals(aExpected[i], lnr.readLine());
        }
        assertEquals(aExpected.length, errs);
        aC.destroy();
    }


    public void testWhitespace()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_CAST_WHITESPACE_PROP, false);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, false);
        mConfig.setParenPadOption(PadOption.NOSPACE);
        mConfig.setBlockOptionProperty(Defn.TRY_BLOCK_PROP, BlockOption.IGNORE);
        mConfig.setBlockOptionProperty(Defn.CATCH_BLOCK_PROP,
                                       BlockOption.IGNORE);
        final Checker c = createChecker();
        final String filepath = getPath("InputWhitespace.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":5:12: '.' is preceeded with whitespace.",
            filepath + ":5:14: '.' is followed by whitespace.",
            filepath + ":13: Type Javadoc comment is missing an @author tag.",
            filepath + ":16:22: '=' is not preceeded with whitespace.",
            filepath + ":16:23: '=' is not followed by whitespace.",
            filepath + ":18:24: '=' is not followed by whitespace.",
            filepath + ":26:14: '=' is not preceeded with whitespace.",
            filepath + ":27:10: '=' is not preceeded with whitespace.",
            filepath + ":27:11: '=' is not followed by whitespace.",
            filepath + ":28:10: '+=' is not preceeded with whitespace.",
            filepath + ":28:12: '+=' is not followed by whitespace.",
            filepath + ":29:13: '-=' is not followed by whitespace.",
            filepath + ":29:14: '-' is followed by whitespace.",
            filepath + ":29:21: '+' is followed by whitespace.",
            filepath + ":30:14: '++' is preceeded with whitespace.",
            filepath + ":30:21: '--' is preceeded with whitespace.",
            filepath + ":31:15: '++' is followed by whitespace.",
            filepath + ":31:22: '--' is followed by whitespace.",
            filepath + ":37:21: 'synchronized' is not followed by whitespace.",
            filepath + ":39:12: 'try' is not followed by whitespace.",
            filepath + ":39:12: '{' is not preceeded with whitespace.",
            filepath + ":41:14: 'catch' is not followed by whitespace.",
            filepath + ":41:34: '{' is not preceeded with whitespace.",
            filepath + ":58:11: 'if' is not followed by whitespace.",
            filepath + ":58:12: '(' is followed by whitespace.",
            filepath + ":58:36: ')' is preceeded with whitespace.",
            filepath + ":59:9: '{' should be on the previous line.",
            filepath + ":63:9: '{' should be on the previous line.",
            filepath + ":74:13: '(' is followed by whitespace.",
            filepath + ":74:18: ')' is preceeded with whitespace.",
            filepath + ":75:9: '{' should be on the previous line.",
            filepath + ":76:19: 'return' is not followed by whitespace.",
            filepath + ":79:9: '{' should be on the previous line.",
            filepath + ":88:21: 'cast' is not followed by whitespace.",
            filepath + ":97:29: '?' is not preceeded with whitespace.",
            filepath + ":97:30: '?' is not followed by whitespace.",
            filepath + ":97:34: ':' is not preceeded with whitespace.",
            filepath + ":97:35: ':' is not followed by whitespace.",
            filepath + ":98:15: '==' is not preceeded with whitespace.",
            filepath + ":98:17: '==' is not followed by whitespace.",
            filepath + ":104:20: '*' is not followed by whitespace.",
            filepath + ":104:21: '*' is not preceeded with whitespace.",
            filepath + ":111:22: '!' is followed by whitespace.",
            filepath + ":112:23: '~' is followed by whitespace.",
            filepath + ":119:18: '%' is not preceeded with whitespace.",
            filepath + ":120:20: '%' is not followed by whitespace.",
            filepath + ":121:18: '%' is not preceeded with whitespace.",
            filepath + ":121:19: '%' is not followed by whitespace.",
            filepath + ":123:18: '/' is not preceeded with whitespace.",
            filepath + ":124:20: '/' is not followed by whitespace.",
            filepath + ":125:18: '/' is not preceeded with whitespace.",
            filepath + ":125:19: '/' is not followed by whitespace.",
            filepath + ":129:17: '.' is preceeded with whitespace.",
            filepath + ":129:24: '.' is followed by whitespace.",
            filepath + ":136:10: '.' is preceeded with whitespace.",
            filepath + ":136:12: '.' is followed by whitespace.",
            filepath + ":153:15: 'assert' is not followed by whitespace.",
            filepath + ":156:20: ':' is not preceeded with whitespace.",
            filepath + ":156:21: ':' is not followed by whitespace.",
        };
        verify(c, filepath, expected);
        c.destroy();
    }

    public void testWhitespaceCastParenOff()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_CAST_WHITESPACE_PROP, true);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, false);
        mConfig.setParenPadOption(PadOption.SPACE);
        mConfig.setBlockOptionProperty(Defn.TRY_BLOCK_PROP, BlockOption.IGNORE);
        mConfig.setBlockOptionProperty(Defn.CATCH_BLOCK_PROP,
                                       BlockOption.IGNORE);
        final Checker c = createChecker();
        final String filepath = getPath("InputWhitespace.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":5:12: '.' is preceeded with whitespace.",
            filepath + ":5:14: '.' is followed by whitespace.",
            filepath + ":13: Type Javadoc comment is missing an @author tag.",
            filepath + ":16:22: '=' is not preceeded with whitespace.",
            filepath + ":16:23: '=' is not followed by whitespace.",
            filepath + ":18:24: '=' is not followed by whitespace.",
            filepath + ":26:14: '=' is not preceeded with whitespace.",
            filepath + ":27:10: '=' is not preceeded with whitespace.",
            filepath + ":27:11: '=' is not followed by whitespace.",
            filepath + ":28:10: '+=' is not preceeded with whitespace.",
            filepath + ":28:12: '+=' is not followed by whitespace.",
            filepath + ":29:13: '-=' is not followed by whitespace.",
            filepath + ":29:14: '-' is followed by whitespace.",
            filepath + ":29:20: '(' is not followed by whitespace.",
            filepath + ":29:21: '+' is followed by whitespace.",
            filepath + ":29:22: ')' is not preceeded with whitespace.",
            filepath + ":30:14: '++' is preceeded with whitespace.",
            filepath + ":30:21: '--' is preceeded with whitespace.",
            filepath + ":31:15: '++' is followed by whitespace.",
            filepath + ":31:22: '--' is followed by whitespace.",
            filepath + ":37:21: 'synchronized' is not followed by whitespace.",
            filepath + ":37:22: '(' is not followed by whitespace.",
            filepath + ":37:25: ')' is not preceeded with whitespace.",
            filepath + ":39:12: 'try' is not followed by whitespace.",
            filepath + ":39:12: '{' is not preceeded with whitespace.",
            filepath + ":41:14: 'catch' is not followed by whitespace.",
            filepath + ":41:15: '(' is not followed by whitespace.",
            filepath + ":41:32: ')' is not preceeded with whitespace.",
            filepath + ":41:34: '{' is not preceeded with whitespace.",
            filepath + ":58:11: 'if' is not followed by whitespace.",
            filepath + ":59:9: '{' should be on the previous line.",
            filepath + ":63:9: '{' should be on the previous line.",
            filepath + ":75:9: '{' should be on the previous line.",
            filepath + ":76:19: 'return' is not followed by whitespace.",
            filepath + ":76:20: '(' is not followed by whitespace.",
            filepath + ":76:20: ')' is not preceeded with whitespace.",
            filepath + ":79:9: '{' should be on the previous line.",
            filepath + ":87:21: '(' is not followed by whitespace.",
            filepath + ":87:26: ')' is not preceeded with whitespace.",
            filepath + ":88:14: '(' is not followed by whitespace.",
            filepath + ":88:19: ')' is not preceeded with whitespace.",
            filepath + ":89:14: '(' is not followed by whitespace.",
            filepath + ":89:19: ')' is not preceeded with whitespace.",
            filepath + ":90:14: '(' is not followed by whitespace.",
            filepath + ":90:19: ')' is not preceeded with whitespace.",
            filepath + ":97:22: '(' is not followed by whitespace.",
            filepath + ":97:27: ')' is not preceeded with whitespace.",
            filepath + ":97:29: '?' is not preceeded with whitespace.",
            filepath + ":97:30: '?' is not followed by whitespace.",
            filepath + ":97:34: ':' is not preceeded with whitespace.",
            filepath + ":97:35: ':' is not followed by whitespace.",
            filepath + ":98:14: '(' is not followed by whitespace.",
            filepath + ":98:15: '==' is not preceeded with whitespace.",
            filepath + ":98:17: '==' is not followed by whitespace.",
            filepath + ":98:17: ')' is not preceeded with whitespace.",
            filepath + ":104:20: '*' is not followed by whitespace.",
            filepath + ":104:21: '*' is not preceeded with whitespace.",
            filepath + ":111:22: '!' is followed by whitespace.",
            filepath + ":112:23: '~' is followed by whitespace.",
            filepath + ":119:18: '%' is not preceeded with whitespace.",
            filepath + ":120:20: '%' is not followed by whitespace.",
            filepath + ":121:18: '%' is not preceeded with whitespace.",
            filepath + ":121:19: '%' is not followed by whitespace.",
            filepath + ":123:18: '/' is not preceeded with whitespace.",
            filepath + ":124:20: '/' is not followed by whitespace.",
            filepath + ":125:18: '/' is not preceeded with whitespace.",
            filepath + ":125:19: '/' is not followed by whitespace.",
            filepath + ":129:17: '.' is preceeded with whitespace.",
            filepath + ":129:24: '.' is followed by whitespace.",
            filepath + ":136:10: '.' is preceeded with whitespace.",
            filepath + ":136:12: '.' is followed by whitespace.",
            filepath + ":150:28: '(' is not followed by whitespace.",
            filepath + ":150:31: ')' is not preceeded with whitespace.",
            filepath + ":153:15: 'assert' is not followed by whitespace.",
            filepath + ":153:16: '(' is not followed by whitespace.",
            filepath + ":153:19: ')' is not preceeded with whitespace.",
            filepath + ":156:20: ':' is not preceeded with whitespace.",
            filepath + ":156:21: ':' is not followed by whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testWhitespaceOff()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, false);
        mConfig.setBlockOptionProperty(Defn.TRY_BLOCK_PROP, BlockOption.IGNORE);
        mConfig.setBlockOptionProperty(Defn.CATCH_BLOCK_PROP,
                                       BlockOption.IGNORE);
        final Checker c = createChecker();
        final String filepath = getPath("InputWhitespace.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":13: Type Javadoc comment is missing an @author tag.",
            filepath + ":59:9: '{' should be on the previous line.",
            filepath + ":63:9: '{' should be on the previous line.",
            filepath + ":75:9: '{' should be on the previous line.",
            filepath + ":79:9: '{' should be on the previous line.",
        };
        verify(c, filepath, expected);
    }

    public void testBraces()
        throws Exception
    {
        final Checker c = createChecker();
        final String filepath = getPath("InputBraces.java");
        final String[] expected = {
            filepath + ":29: 'do' construct must use '{}'s.",
            filepath + ":41: 'while' construct must use '{}'s.",
            filepath + ":41:14: 'while' is not followed by whitespace.",
            filepath + ":42: 'while' construct must use '{}'s.",
            filepath + ":44: 'while' construct must use '{}'s.",
            filepath + ":45: 'if' construct must use '{}'s.",
            filepath + ":58: 'for' construct must use '{}'s.",
            filepath + ":58:12: 'for' is not followed by whitespace.",
            filepath + ":58:23: ';' is not followed by whitespace.",
            filepath + ":58:29: ';' is not followed by whitespace.",
            filepath + ":59: 'for' construct must use '{}'s.",
            filepath + ":61: 'for' construct must use '{}'s.",
            filepath + ":63: 'if' construct must use '{}'s.",
            filepath + ":82: 'if' construct must use '{}'s.",
            filepath + ":83: 'if' construct must use '{}'s.",
            filepath + ":85: 'if' construct must use '{}'s.",
            filepath + ":87: 'else' construct must use '{}'s.",
            filepath + ":89: 'if' construct must use '{}'s.",
            filepath + ":97: 'else' construct must use '{}'s.",
            filepath + ":99: 'if' construct must use '{}'s.",
            filepath + ":100: 'if' construct must use '{}'s."
        };
        verify(c, filepath, expected);
    }

    public void testBracesOff()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_BRACES_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputBraces.java");
        final String[] expected = {
            filepath + ":41:14: 'while' is not followed by whitespace.",
            filepath + ":58:12: 'for' is not followed by whitespace.",
            filepath + ":58:23: ';' is not followed by whitespace.",
            filepath + ":58:29: ';' is not followed by whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testTags()
        throws Exception
    {
        final Checker c = createChecker();
        final String filepath = getPath("InputTags.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":8: Missing a Javadoc comment.",
            filepath + ":11:17: Missing a Javadoc comment.",
            filepath + ":14:5: Missing a Javadoc comment.",
            filepath + ":18: Unused @param tag for 'unused'.",
            filepath + ":24: Expected an @return tag.",
            filepath + ":33: Expected an @return tag.",
            filepath + ":40:16: Expected @throws tag for 'Exception'.",
            filepath + ":49:16: Expected @throws tag for 'Exception'.",
            filepath + ":53: Unused @throws tag for 'WrongException'.",
            filepath + ":55:16: Expected @throws tag for 'Exception'.",
            filepath + ":55:27: Expected @throws tag for 'NullPointerException'.",
            filepath + ":60:22: Expected @param tag for 'aOne'.",
            filepath + ":68:22: Expected @param tag for 'aOne'.",
            filepath + ":72: Unused @param tag for 'WrongParam'.",
            filepath + ":73:23: Expected @param tag for 'aOne'.",
            filepath + ":73:33: Expected @param tag for 'aTwo'.",
            filepath + ":78: Unused @param tag for 'Unneeded'.",
            filepath + ":79: Unused Javadoc tag.",
            filepath + ":87: Duplicate @return tag.",
            filepath + ":109:23: Expected @param tag for 'aOne'.",
            filepath + ":109:55: Expected @param tag for 'aFour'.",
            filepath + ":109:66: Expected @param tag for 'aFive'.",
            filepath + ":129:5: '{' should be on the previous line.",
            filepath + ":178: Unused @throws tag for 'ThreadDeath'.",
            filepath + ":179: Unused @throws tag for 'ArrayStoreException'.",
        };

        verify(c, filepath, expected);
    }

    public void testTagsWithResolver()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.JAVADOC_CHECK_UNUSED_THROWS_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputTags.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":8: Missing a Javadoc comment.",
            filepath + ":11:17: Missing a Javadoc comment.",
            filepath + ":14:5: Missing a Javadoc comment.",
            filepath + ":18: Unused @param tag for 'unused'.",
            filepath + ":24: Expected an @return tag.",
            filepath + ":33: Expected an @return tag.",
            filepath + ":40:16: Expected @throws tag for 'Exception'.",
            filepath + ":49:16: Expected @throws tag for 'Exception'.",
            filepath + ":53: Unable to get class information for @throws tag 'WrongException'.",
            filepath + ":53: Unused @throws tag for 'WrongException'.",
            filepath + ":55:16: Expected @throws tag for 'Exception'.",
            filepath + ":55:27: Expected @throws tag for 'NullPointerException'.",
            filepath + ":60:22: Expected @param tag for 'aOne'.",
            filepath + ":68:22: Expected @param tag for 'aOne'.",
            filepath + ":72: Unused @param tag for 'WrongParam'.",
            filepath + ":73:23: Expected @param tag for 'aOne'.",
            filepath + ":73:33: Expected @param tag for 'aTwo'.",
            filepath + ":78: Unused @param tag for 'Unneeded'.",
            filepath + ":79: Unused Javadoc tag.",
            filepath + ":87: Duplicate @return tag.",
            filepath + ":109:23: Expected @param tag for 'aOne'.",
            filepath + ":109:55: Expected @param tag for 'aFour'.",
            filepath + ":109:66: Expected @param tag for 'aFive'.",
            filepath + ":129:5: '{' should be on the previous line.",
        };

        verify(c, filepath, expected);
    }

    public void testInner()
        throws Exception
    {
        final Checker c = createChecker();
        final String filepath = getPath("InputInner.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":14: Missing a Javadoc comment.",
            filepath + ":17:20: Missing a Javadoc comment.",
            filepath + ":21: Missing a Javadoc comment.",
            filepath + ":24:16: Missing a Javadoc comment.",
            filepath + ":24:16: Name 'data' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'.",
            filepath + ":27: Missing a Javadoc comment.",
            filepath + ":30:24: Missing a Javadoc comment.",
            filepath + ":30:24: Variable 'rData' must be private and have accessor methods.",
            filepath + ":33:27: Variable 'protectedVariable' must be private and have accessor methods.",
            filepath + ":36:17: Variable 'packageVariable' must be private and have accessor methods.",
            filepath + ":41:29: Variable 'sWeird' must be private and have accessor methods.",
            filepath + ":43:19: Variable 'sWeird2' must be private and have accessor methods.",
        };
        verify(c, filepath, expected);
    }

    public void testIgnoreAccess()
        throws Exception
    {
        mConfig.setPatternProperty(Defn.PUBLIC_MEMBER_PATTERN_PROP, "^r[A-Z]");
        mConfig.setBooleanProperty(Defn.ALLOW_PROTECTED_PROP, true);
        mConfig.setBooleanProperty(Defn.ALLOW_PACKAGE_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputInner.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":14: Missing a Javadoc comment.",
            filepath + ":17:20: Missing a Javadoc comment.",
            filepath + ":17:20: Variable 'fData' must be private and have accessor methods.",
            filepath + ":21: Missing a Javadoc comment.",
            filepath + ":24:16: Missing a Javadoc comment.",
            filepath + ":24:16: Name 'data' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'.",
            filepath + ":27: Missing a Javadoc comment.",
            filepath + ":30:24: Missing a Javadoc comment.",
        };
        verify(c, filepath, expected);
    }

    public void testSimple()
        throws Exception
    {
        mConfig.setIntProperty(Defn.MAX_FILE_LENGTH_PROP, 20) ;
        mConfig.setIntProperty(Defn.MAX_METHOD_LENGTH_PROP, 19) ;
        mConfig.setIntProperty(Defn.MAX_CONSTRUCTOR_LENGTH_PROP, 9) ;
        mConfig.setPatternProperty(Defn.PARAMETER_PATTERN_PROP, "^a[A-Z][a-zA-Z0-9]*$");
        mConfig.setPatternProperty(Defn.STATIC_PATTERN_PROP, "^s[A-Z][a-zA-Z0-9]*$");
        mConfig.setPatternProperty(Defn.MEMBER_PATTERN_PROP, "^m[A-Z][a-zA-Z0-9]*$");
        mConfig.setPatternProperty(Defn.IGNORE_LINE_LENGTH_PATTERN_PROP,"^.*is OK.*regexp.*$");
        mConfig.setPatternProperty(Defn.TODO_PATTERN_PROP, "FIXME:");
        mConfig.setPatternProperty(Defn.LOCAL_FINAL_VAR_PATTERN_PROP, "[A-Z]+");
        final Checker c = createChecker();
        final String filepath = getPath("InputSimple.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":1: File length is 198 lines (max allowed is 20).",
            filepath + ":3: Line does not match expected header line of '// Created: 2001'.",
            filepath + ":18: Line is longer than 80 characters.",
            filepath + ":19:25: Line contains a tab character.",
            filepath + ":25:29: Name 'badConstant' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'.",
            filepath + ":30:24: Name 'badStatic' must match pattern '^s[A-Z][a-zA-Z0-9]*$'.",
            filepath + ":35:17: Name 'badMember' must match pattern '^m[A-Z][a-zA-Z0-9]*$'.",
            filepath + ":39:19: Variable 'mNumCreated2' must be private and have accessor methods.",
            filepath + ":42:40: ',' is not followed by whitespace.",
            filepath + ":49:23: Variable 'sTest1' must be private and have accessor methods.",
            filepath + ":51:26: Variable 'sTest3' must be private and have accessor methods.",
            filepath + ":53:16: Variable 'sTest2' must be private and have accessor methods.",
            filepath + ":56:9: Variable 'mTest1' must be private and have accessor methods.",
            filepath + ":58:16: Variable 'mTest2' must be private and have accessor methods.",
            filepath + ":71:19: Name 'badFormat1' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            filepath + ":71:30: ',' is not followed by whitespace.",
            filepath + ":71:34: Name 'badFormat2' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            filepath + ":72:25: Name 'badFormat3' must match pattern '^a[A-Z][a-zA-Z0-9]*$'.",
            filepath + ":80: Method length is 20 lines (max allowed is 19).",
            filepath + ":103: Constructor length is 10 lines (max allowed is 9).",
            filepath + ":119:13: Name 'ABC' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
            filepath + ":122:19: Name 'cde' must match pattern '[A-Z]+'.",
            filepath + ":127:9: '{' should be on the previous line.",
            filepath + ":130:18: Name 'I' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
            filepath + ":131:9: '{' should be on the previous line.",
            filepath + ":132:20: Name 'InnerBlockVariable' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
            filepath + ":137:10: Name 'ALL_UPPERCASE_METHOD' must match pattern '^[a-z][a-zA-Z0-9]*$'.",
            filepath + ":142:30: Name 'BAD__NAME' must match pattern '^[A-Z](_?[A-Z0-9]+)*$'.",
            filepath + ":145: Line is longer than 80 characters.",
            filepath + ":145:35: Line contains a tab character.",
            filepath + ":146:64: Line contains a tab character.",
            filepath + ":153:27: '=' is not followed by whitespace.",
            filepath + ":154:9: Line contains a tab character.",
            filepath + ":154:27: '=' is not followed by whitespace.",
            filepath + ":155:10: Line contains a tab character.",
            filepath + ":155:27: '=' is not followed by whitespace.",
            filepath + ":156:1: Line contains a tab character.",
            filepath + ":156:27: '=' is not followed by whitespace.",
            filepath + ":157:3: Line contains a tab character.",
            filepath + ":157:27: '=' is not followed by whitespace.",
            filepath + ":158:3: Line contains a tab character.",
            filepath + ":158:27: '=' is not followed by whitespace.",
            filepath + ":161: Comment matches to-do format 'FIXME:'.",
            filepath + ":162: Comment matches to-do format 'FIXME:'.",
            filepath + ":163: Comment matches to-do format 'FIXME:'.",
            filepath + ":167: Comment matches to-do format 'FIXME:'.",
            filepath + ":194:5: More than 7 parameters.",
        };
        verify(c, filepath, expected);
    }

    public void testModifierChecks()
        throws Exception
    {
        final Checker c = createChecker();
        final String filepath = getPath("InputModifier.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":14:10: 'final' modifier out of order with the JLS suggestions.",
            filepath + ":18:12: 'private' modifier out of order with the JLS suggestions.",
            filepath + ":24:14: 'private' modifier out of order with the JLS suggestions.",
            filepath + ":32:9: Redundant 'public' modifier.",
            filepath + ":38:9: Redundant 'abstract' modifier.",
        };
        verify(c, filepath, expected);
    }

    public void testStrictJavadoc()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputPublicOnly.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":7: Missing a Javadoc comment.",
            filepath + ":9: Missing a Javadoc comment.",
            filepath + ":11:16: Missing a Javadoc comment.",
            filepath + ":12:9: Missing a Javadoc comment.",
            filepath + ":14: Missing a Javadoc comment.",
            filepath + ":16:25: Missing a Javadoc comment.",
            filepath + ":18:13: Missing a Javadoc comment.",
            filepath + ":25:13: Missing a Javadoc comment.",
            filepath + ":34: Missing a Javadoc comment.",
            filepath + ":36:21: Missing a Javadoc comment.",
            filepath + ":38:9: Missing a Javadoc comment.",
            filepath + ":43:17: Missing a Javadoc comment.",
            filepath + ":44:9: Missing a Javadoc comment.",
            filepath + ":44:9: Variable 'mLen' must be private and have accessor methods.",
            filepath + ":45:19: Missing a Javadoc comment.",
            filepath + ":45:19: Variable 'mDeer' must be private and have accessor methods.",
            filepath + ":46:16: Missing a Javadoc comment.",
            filepath + ":46:16: Variable 'aFreddo' must be private and have accessor methods.",
            filepath + ":49:5: Missing a Javadoc comment.",
            filepath + ":54:5: Missing a Javadoc comment.",
            filepath + ":59:5: Missing a Javadoc comment.",
            filepath + ":64:5: Missing a Javadoc comment.",
            filepath + ":69:5: Missing a Javadoc comment.",
            filepath + ":74:5: Missing a Javadoc comment.",
            filepath + ":79:5: Missing a Javadoc comment.",
            filepath + ":84:5: Missing a Javadoc comment.",
            filepath + ":94:32: Expected @param tag for 'aA'."
        };
        verify(c, filepath, expected);
    }

    public void testNoJavadoc()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setJavadocScope(Scope.NOTHING);
        final Checker c = createChecker();
        final String filepath = getPath("InputPublicOnly.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":44:9: Variable 'mLen' must be private and have accessor methods.",
            filepath + ":45:19: Variable 'mDeer' must be private and have accessor methods.",
            filepath + ":46:16: Variable 'aFreddo' must be private and have accessor methods.",
        };
        verify(c, filepath, expected);
    }

    // pre 1.4 relaxed mode is roughly equivalent with check=protected
    public void testRelaxedJavadoc()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setJavadocScope(Scope.PROTECTED);
        final Checker c = createChecker();
        final String filepath = getPath("InputPublicOnly.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":7: Missing a Javadoc comment.",
            filepath + ":44:9: Variable 'mLen' must be private and have accessor methods.",
            filepath + ":45:19: Missing a Javadoc comment.",
            filepath + ":45:19: Variable 'mDeer' must be private and have accessor methods.",
            filepath + ":46:16: Missing a Javadoc comment.",
            filepath + ":46:16: Variable 'aFreddo' must be private and have accessor methods.",
            filepath + ":59:5: Missing a Javadoc comment.",
            filepath + ":64:5: Missing a Javadoc comment.",
            filepath + ":79:5: Missing a Javadoc comment.",
            filepath + ":84:5: Missing a Javadoc comment."
        };
        verify(c, filepath, expected);
    }


    public void testScopeInnerInterfacesPublic()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.PUBLIC);
        mConfig.setBooleanProperty(Defn.IGNORE_PUBLIC_IN_INTERFACE_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputScopeInnerInterfaces.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":7: Missing a Javadoc comment.",
            filepath + ":38: Missing a Javadoc comment.",
            filepath + ":40:23: Missing a Javadoc comment.",
            filepath + ":41:16: Missing a Javadoc comment.",
            filepath + ":43:9: Missing a Javadoc comment.",
            filepath + ":44:9: Missing a Javadoc comment."
        };
        verify(c, filepath, expected);
    }

    public void testScopeInnerClassesPackage()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.getInstance("package"));
        final Checker c = createChecker();
        final String filepath = getPath("InputScopeInnerClasses.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":18: Missing a Javadoc comment.",
            filepath + ":20: Missing a Javadoc comment.",
            filepath + ":22: Missing a Javadoc comment."
        };
        verify(c, filepath, expected);
    }

    public void testScopeInnerClassesPublic()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.PUBLIC);
        final Checker c = createChecker();
        final String filepath = getPath("InputScopeInnerClasses.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":18: Missing a Javadoc comment.",
        };
        verify(c, filepath, expected);
    }

    public void testScopeAnonInnerPrivate()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.PRIVATE);
        final Checker c = createChecker();
        final String filepath = getPath("InputScopeAnonInner.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":37:34: '(' is followed by whitespace.",
            filepath + ":39:42: '(' is followed by whitespace.",
            filepath + ":39:57: ')' is preceeded with whitespace.",
            filepath + ":43:14: ')' is preceeded with whitespace.",
            filepath + ":51:34: '(' is followed by whitespace.",
            filepath + ":53:42: '(' is followed by whitespace.",
            filepath + ":53:57: ')' is preceeded with whitespace.",
            filepath + ":57:14: ')' is preceeded with whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testScopeAnonInnerAnonInner()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.ANONINNER);
        final Checker c = createChecker();
        final String filepath = getPath("InputScopeAnonInner.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":26:9: Missing a Javadoc comment.",
            filepath + ":37:34: '(' is followed by whitespace.",
            filepath + ":39:17: Missing a Javadoc comment.",
            filepath + ":39:42: '(' is followed by whitespace.",
            filepath + ":39:57: ')' is preceeded with whitespace.",
            filepath + ":43:14: ')' is preceeded with whitespace.",
            filepath + ":51:34: '(' is followed by whitespace.",
            filepath + ":53:17: Missing a Javadoc comment.",
            filepath + ":53:42: '(' is followed by whitespace.",
            filepath + ":53:57: ')' is preceeded with whitespace.",
            filepath + ":57:14: ')' is preceeded with whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testHeader()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("inputHeader.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":1: Missing a header - not enough lines in file.",
            filepath + ":1: Missing a Javadoc comment.",
            filepath + ":1:48: Name 'inputHeader' must match pattern '^[A-Z][a-zA-Z0-9]*$'.",
        };
        verify(c, filepath, expected);
    }

    public void testRegexpHeader()
        throws Exception
    {
        final Checker c = createChecker();
        mConfig.setBooleanProperty(Defn.HEADER_LINES_REGEXP_PROP, true);
        mConfig.setHeaderFile(getPath("regexp.header"));
        mConfig.setHeaderIgnoreLines("4,5");
        final String filepath = getPath("InputScopeAnonInner.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":3: Line does not match expected header line of '// Created: 2002'.",
            filepath + ":37:34: '(' is followed by whitespace.",
            filepath + ":39:42: '(' is followed by whitespace.",
            filepath + ":39:57: ')' is preceeded with whitespace.",
            filepath + ":43:14: ')' is preceeded with whitespace.",
            filepath + ":51:34: '(' is followed by whitespace.",
            filepath + ":53:42: '(' is followed by whitespace.",
            filepath + ":53:57: ')' is preceeded with whitespace.",
            filepath + ":57:14: ')' is preceeded with whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testImport()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_IMPORT_LENGTH_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputImport.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":7: Avoid using the '.*' form of import.",
            filepath + ":7: Redundant import from the same package.",
            filepath + ":8: Redundant import from the same package.",
            filepath + ":9: Avoid using the '.*' form of import.",
            filepath + ":10: Avoid using the '.*' form of import.",
            filepath + ":10: Redundant import from the java.lang package.",
            filepath + ":11: Redundant import from the java.lang package.",
            filepath + ":13: Unused import - java.util.List.",
            filepath + ":14: Duplicate import to line 13.",
            filepath + ":14: Unused import - java.util.List.",
            filepath + ":15: Import from illegal package - sun.net.ftpclient.FtpClient.",
        };
        verify(c, filepath, expected);
    }

    public void testPackageHtml()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.REQUIRE_PACKAGE_HTML_PROP, true);
        mConfig.setJavadocScope(Scope.PRIVATE);
        final Checker c = createChecker();
        final String packageHtmlPath = getPath("package.html");
        final String filepath = getPath("InputScopeAnonInner.java");
        assertNotNull(c);
        final String[] expected = {
            packageHtmlPath + ":0: Missing package documentation file.",
            filepath + ":37:34: '(' is followed by whitespace.",
            filepath + ":39:42: '(' is followed by whitespace.",
            filepath + ":39:57: ')' is preceeded with whitespace.",
            filepath + ":43:14: ')' is preceeded with whitespace.",
            filepath + ":51:34: '(' is followed by whitespace.",
            filepath + ":53:42: '(' is followed by whitespace.",
            filepath + ":53:57: ')' is preceeded with whitespace.",
            filepath + ":57:14: ')' is preceeded with whitespace.",
        };
        verify(c, filepath, expected);
    }

    public void testLCurlyMethodIgnore()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setLeftCurlyOptionProperty(Defn.LCURLY_METHOD_PROP,
                                           LeftCurlyOption.IGNORE);
        mConfig.setJavadocScope(Scope.NOTHING);
        final Checker c = createChecker();
        final String filepath = getPath("InputLeftCurlyMethod.java");
        assertNotNull(c);
        final String[] expected = {
        };
        verify(c, filepath, expected);
    }

    public void testLCurlyMethodNL()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setLeftCurlyOptionProperty(Defn.LCURLY_METHOD_PROP,
                                           LeftCurlyOption.NL);
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setBooleanProperty(Defn.ALLOW_TABS_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputLeftCurlyMethod.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":14:39: '{' should be on a new line.",
            filepath + ":21:20: '{' should be on a new line.",
            filepath + ":34:31: '{' should be on a new line.",
        };
        verify(c, filepath, expected);
    }

    public void testLCurlyOther()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setRCurly(RightCurlyOption.SAME);
        final Checker c = createChecker();
        final String filepath = getPath("InputLeftCurlyOther.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":19:9: '{' should be on the previous line.",
            filepath + ":21:13: '{' should be on the previous line.",
            filepath + ":23:17: '{' should be on the previous line.",
            filepath + ":25:17: '}' should be on the same line.",
            filepath + ":28:17: '}' should be on the same line.",
            filepath + ":30:17: '{' should be on the previous line.",
            filepath + ":34:17: '{' should be on the previous line.",
            filepath + ":40:13: '}' should be on the same line.",
            filepath + ":42:13: '{' should be on the previous line.",
            filepath + ":44:13: '}' should be on the same line.",
            filepath + ":46:13: '{' should be on the previous line.",
            filepath + ":52:9: '{' should be on the previous line.",
            filepath + ":54:13: '{' should be on the previous line.",
        };
        verify(c, filepath, expected);
    }

    public void testAssertIdentifier()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.NOTHING);
        final Checker c = createChecker();
        final String filepath = getPath("InputAssertIdentifier.java");
        assertNotNull(c);
        final String[] expected = {
        };
        verify(c, filepath, expected);
    }

    public void testSemantic()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setBlockOptionProperty(Defn.TRY_BLOCK_PROP, BlockOption.STMT);
        mConfig.setBlockOptionProperty(Defn.CATCH_BLOCK_PROP, BlockOption.STMT);
        mConfig.setBlockOptionProperty(Defn.FINALLY_BLOCK_PROP, BlockOption.STMT);
        mConfig.setBooleanProperty(Defn.IGNORE_IMPORTS_PROP, true);
        mConfig.setBooleanProperty(Defn.IGNORE_LONG_ELL_PROP, false);
        mConfig.setStringSetProperty(
            Defn.ILLEGAL_INSTANTIATIONS_PROP,
            "java.lang.Boolean,"
            + "com.puppycrawl.tools.checkstyle.InputModifier,"
            + "java.io.File,"
            + "java.awt.Color");
        final Checker c = createChecker();
        final String filepath = getPath("InputSemantic.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":19:21: Avoid instantiation of java.lang.Boolean.",
            filepath + ":24:21: Avoid instantiation of java.lang.Boolean.",
            filepath + ":30:16: Avoid instantiation of java.lang.Boolean.",
            filepath + ":37:21: Avoid instantiation of " +
                               "com.puppycrawl.tools.checkstyle.InputModifier.",
            filepath + ":40:18: Avoid instantiation of java.io.File.",
            filepath + ":43:21: Avoid instantiation of java.awt.Color.",
            filepath + ":51:65: Must have at least one statement.",
            filepath + ":53:41: Must have at least one statement.",
            filepath + ":70:38: Must have at least one statement.",
            filepath + ":71:52: Must have at least one statement.",
            filepath + ":72:45: Must have at least one statement.",
            filepath + ":74:13: Must have at least one statement.",
            filepath + ":76:17: Must have at least one statement.",
            filepath + ":78:13: Must have at least one statement.",
            filepath + ":81:17: Must have at least one statement.",
            filepath + ":93:43: Should use uppercase 'L'.",
        };
        verify(c, filepath, expected);
    }

    public void testSemantic2()
        throws Exception
    {
        mConfig.setBooleanProperty(Defn.IGNORE_WHITESPACE_PROP, true);
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setBlockOptionProperty(Defn.TRY_BLOCK_PROP, BlockOption.TEXT);
        mConfig.setBlockOptionProperty(Defn.CATCH_BLOCK_PROP, BlockOption.TEXT);
        mConfig.setBlockOptionProperty(Defn.FINALLY_BLOCK_PROP, BlockOption.TEXT);
        mConfig.setBooleanProperty(Defn.IGNORE_IMPORTS_PROP, true);
        mConfig.setBooleanProperty(Defn.IGNORE_LONG_ELL_PROP, true);
        mConfig.setStringSetProperty(Defn.ILLEGAL_INSTANTIATIONS_PROP, "");
        final Checker c = createChecker();
        final String filepath = getPath("InputSemantic.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":51:65: Empty catch block.",
            filepath + ":71:52: Empty catch block.",
            filepath + ":72:45: Empty catch block.",
            filepath + ":74:13: Empty try block.",
            filepath + ":76:17: Empty finally block.",
        };
        verify(c, filepath, expected);
    }

    public void testOpWrapOn()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setWrapOpOption(WrapOpOption.NL);
        final Checker c = createChecker();
        final String filepath = getPath("InputOpWrap.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":15:19: '+' should be on a new line.",
            filepath + ":16:15: '-' should be on a new line.",
            filepath + ":24:18: '&&' should be on a new line.",
        };
        verify(c, filepath, expected);
    }

    public void testOpWrapOff()
        throws Exception
    {
        mConfig.setJavadocScope(Scope.NOTHING);
        mConfig.setWrapOpOption(WrapOpOption.IGNORE);
        final Checker c = createChecker();
        final String filepath = getPath("InputOpWrap.java");
        assertNotNull(c);
        final String[] expected = {
        };
        verify(c, filepath, expected);
    }

    public void testNoAuthor()
        throws Exception
    {
        mConfig.setWrapOpOption(WrapOpOption.NL);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, false);
        final Checker c = createChecker();
        final String filepath = getPath("InputJavadoc.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":11: Type Javadoc comment is missing an @author tag."
        };
        verify(c, filepath, expected);
    }

    public void testNoVersion()
        throws Exception
    {
        mConfig.setWrapOpOption(WrapOpOption.NL);
        mConfig.setBooleanProperty(Defn.ALLOW_NO_AUTHOR_PROP, true);
        mConfig.setBooleanProperty(Defn.REQUIRE_VERSION_PROP, true);
        final Checker c = createChecker();
        final String filepath = getPath("InputJavadoc.java");
        assertNotNull(c);
        final String[] expected = {
            filepath + ":11: Type Javadoc comment is missing an @version tag."
        };
        verify(c, filepath, expected);
    }
}
