/*
 [The "BSD license"]
  Copyright (c) 2011 Terence Parr
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. The name of the author may not be used to endorse or promote products
     derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.antlr.v4;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.v4.analysis.AnalysisPipeline;
import org.antlr.v4.automata.ATNFactory;
import org.antlr.v4.automata.LexerATNFactory;
import org.antlr.v4.automata.ParserATNFactory;
import org.antlr.v4.codegen.CodeGenPipeline;
import org.antlr.v4.parse.ANTLRLexer;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.parse.GrammarASTAdaptor;
import org.antlr.v4.parse.ToolANTLRParser;
import org.antlr.v4.runtime.misc.LogManager;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.semantics.SemanticPipeline;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.antlr.v4.tool.DOTGenerator;
import org.antlr.v4.tool.DefaultToolListener;
import org.antlr.v4.tool.ErrorManager;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.GrammarTransformPipeline;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.GrammarASTErrorNode;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.stringtemplate.v4.STGroup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Tool {
	public String VERSION = "4.0-"+new Date();

	public static final String GRAMMAR_EXTENSION = ".g4";
	public static final String LEGACY_GRAMMAR_EXTENSION = ".g";

	public static final List<String> ALL_GRAMMAR_EXTENSIONS =
		Collections.unmodifiableList(Arrays.asList(GRAMMAR_EXTENSION, LEGACY_GRAMMAR_EXTENSION));

	public static enum OptionArgType { NONE, STRING } // NONE implies boolean
	public static class Option {
		String fieldName;
		String name;
		OptionArgType argType;
		String description;

		public Option(String fieldName, String name, String description) {
			this(fieldName, name, OptionArgType.NONE, description);
		}

		public Option(String fieldName, String name, OptionArgType argType, String description) {
			this.fieldName = fieldName;
			this.name = name;
			this.argType = argType;
			this.description = description;
		}
	}

	// fields set by option manager

	public String outputDirectory;
	public String libDirectory;
	public boolean report = false;
	public boolean printGrammar = false;
	public boolean debug = false;
	public boolean profile = false;
	public boolean trace = false;
	public boolean generate_ATN_dot = false;
	public String grammarEncoding = null; // use default locale's encoding
	public String msgFormat = "antlr";
	public boolean saveLexer = false;
	public boolean launch_ST_inspector = false;
    public boolean force_atn = false;
    public boolean log = false;
	public boolean verbose_dfa = false;
	public boolean gen_listener = true;
	public boolean gen_visitor = false;
	public boolean abstract_recognizer = false;
	public Map<String, String> grammarOptions = null;

    public static Option[] optionDefs = {
        new Option("outputDirectory",	"-o", OptionArgType.STRING, "specify output directory where all output is generated"),
        new Option("libDirectory",		"-lib", OptionArgType.STRING, "specify location of grammars, tokens files"),
        new Option("report",			"-report", "print out a report about the grammar(s) processed"),
        new Option("printGrammar",		"-print", "print out the grammar without actions"),
        new Option("debug",				"-debug", "generate a parser that emits debugging events"),
        new Option("profile",			"-profile", "generate a parser that computes profiling information"),
        new Option("generate_ATN_dot",	"-atn", "generate rule augmented transition network diagrams"),
		new Option("grammarEncoding",	"-encoding", OptionArgType.STRING, "specify grammar file encoding; e.g., euc-jp"),
		new Option("msgFormat",			"-message-format", OptionArgType.STRING, "specify output style for messages"),
		new Option("gen_listener",		"-listener", "generate parse tree listener (default)"),
		new Option("gen_listener",		"-no-listener", "don't generate parse tree listener"),
		new Option("gen_visitor",		"-visitor", "generate parse tree visitor"),
		new Option("gen_visitor",		"-no-visitor", "don't generate parse tree visitor (default)"),
		new Option("abstract_recognizer", "-abstract", "generate abstract recognizer classes"),
		new Option("-D<option>=value",	"-message-format", "set a grammar-level option"),


        new Option("saveLexer",			"-Xsave-lexer", "save temp lexer file created for combined grammars"),
        new Option("launch_ST_inspector", "-XdbgST", "launch StringTemplate visualizer on generated code"),
        new Option("force_atn",			"-Xforce-atn", "use the ATN simulator for all predictions"),
		new Option("log",   			"-Xlog", "dump lots of logging info to antlr-timestamp.log"),
		new Option("verbose_dfa",   	"-Xverbose-dfa", "add config set to DFA states"),
	};

	// helper vars for option management
	protected boolean haveOutputDir = false;
	protected boolean return_dont_exit = false;

    // The internal options are for my use on the command line during dev
    public static boolean internalOption_PrintGrammarTree = false;
    public static boolean internalOption_ShowATNConfigsInDFA = false;


	public final String[] args;

	protected List<String> grammarFiles = new ArrayList<String>();

	public ErrorManager errMgr = new ErrorManager(this);
    public LogManager logMgr = new LogManager();

	List<ANTLRToolListener> listeners = new CopyOnWriteArrayList<ANTLRToolListener>();

	/** Track separately so if someone adds a listener, it's the only one
	 *  instead of it and the default stderr listener.
	 */
	DefaultToolListener defaultListener = new DefaultToolListener(this);

	public static void main(String[] args) {
        Tool antlr = new Tool(args);
        if ( args.length == 0 ) { antlr.help(); antlr.exit(0); }

        try {
            antlr.processGrammarsOnCommandLine();
        }
        finally {
            if ( antlr.log ) {
                try {
                    String logname = antlr.logMgr.save();
                    System.out.println("wrote "+logname);
                }
                catch (IOException ioe) {
                    antlr.errMgr.toolError(ErrorType.INTERNAL_ERROR, ioe);
                }
            }
        }
		if ( antlr.return_dont_exit ) return;

		if (antlr.errMgr.getNumErrors() > 0) {
			antlr.exit(1);
		}
		antlr.exit(0);
	}

	public Tool() { this(null); }

	public Tool(String[] args) {
		this.args = args;
		handleArgs();
	}

	protected void handleArgs() {
		int i=0;
		while ( args!=null && i<args.length ) {
			String arg = args[i];
			i++;
			if ( arg.startsWith("-D") ) { // -Dlanguage=Java syntax
				handleOptionSetArg(arg);
				continue;
			}
			if ( arg.charAt(0)!='-' ) { // file name
				grammarFiles.add(arg);
				continue;
			}
			boolean found = false;
			for (Option o : optionDefs) {
				if ( arg.equals(o.name) ) {
					found = true;
					String argValue = null;
					if ( o.argType==OptionArgType.STRING ) {
						argValue = args[i];
						i++;
					}
					// use reflection to set field
					Class<? extends Tool> c = this.getClass();
					try {
						Field f = c.getField(o.fieldName);
						if ( argValue==null ) {
							if ( arg.startsWith("-no-") ) f.setBoolean(this, false);
							else f.setBoolean(this, true);
						}
						else f.set(this, argValue);
					}
					catch (Exception e) {
						errMgr.toolError(ErrorType.INTERNAL_ERROR, "can't access field "+o.fieldName);
					}
				}
			}
			if ( !found ) {
				errMgr.toolError(ErrorType.INVALID_CMDLINE_ARG, arg);
			}
		}
		if ( outputDirectory!=null ) {
			if (outputDirectory.endsWith("/") ||
				outputDirectory.endsWith("\\")) {
				outputDirectory =
					outputDirectory.substring(0, outputDirectory.length() - 1);
			}
			File outDir = new File(outputDirectory);
			haveOutputDir = true;
			if (outDir.exists() && !outDir.isDirectory()) {
				errMgr.toolError(ErrorType.OUTPUT_DIR_IS_FILE, outputDirectory);
				libDirectory = ".";
			}
		}
		else {
			outputDirectory = ".";
		}
		if ( libDirectory!=null ) {
			if (libDirectory.endsWith("/") ||
				libDirectory.endsWith("\\")) {
				libDirectory = libDirectory.substring(0, libDirectory.length() - 1);
			}
			File outDir = new File(libDirectory);
			if (!outDir.exists()) {
				errMgr.toolError(ErrorType.DIR_NOT_FOUND, libDirectory);
				libDirectory = ".";
			}
		}
		else {
			libDirectory = ".";
		}
		if ( launch_ST_inspector ) {
			STGroup.trackCreationEvents = true;
			return_dont_exit = true;
		}
	}

	protected void handleOptionSetArg(String arg) {
		int eq = arg.indexOf('=');
		if ( eq>0 && arg.length()>3 ) {
			String option = arg.substring("-D".length(), eq);
			String value = arg.substring(eq+1);
			if ( value.length()==0 ) {
				errMgr.toolError(ErrorType.BAD_OPTION_SET_SYNTAX, arg);
				return;
			}
			if ( Grammar.parserOptions.contains(option) ||
				 Grammar.lexerOptions.contains(option) )
			{
				if ( grammarOptions==null ) grammarOptions = new HashMap<String, String>();
				grammarOptions.put(option, value);
			}
			else {
				errMgr.grammarError(ErrorType.ILLEGAL_OPTION,
									null,
									null,
									option);
			}
		}
		else {
			errMgr.toolError(ErrorType.BAD_OPTION_SET_SYNTAX, arg);
		}
	}

	public void processGrammarsOnCommandLine() {
		for (String fileName : grammarFiles) {
			GrammarAST t = loadGrammar(fileName);
			if ( t==null || t instanceof GrammarASTErrorNode) return; // came back as error node
			if ( ((GrammarRootAST)t).hasErrors ) return;
			GrammarRootAST ast = (GrammarRootAST)t;

			final Grammar g = createGrammar(ast);
			g.fileName = fileName;
			process(g, true);
		}
	}

	/** To process a grammar, we load all of its imported grammars into
		subordinate grammar objects. Then we merge the imported rules
		into the root grammar. If a root grammar is a combined grammar,
		we have to extract the implicit lexer. Once all this is done, we
		process the lexer first, if present, and then the parser grammar
	 */
	public void process(Grammar g, boolean gencode) {
		g.loadImportedGrammars();


		GrammarTransformPipeline transform = new GrammarTransformPipeline(g, this);
		transform.process();

		LexerGrammar lexerg;
		GrammarRootAST lexerAST;
		if ( g.ast!=null && g.ast.grammarType== ANTLRParser.COMBINED &&
			 !g.ast.hasErrors )
		{
			lexerAST = transform.extractImplicitLexer(g); // alters g.ast
			if ( lexerAST!=null ) {
				lexerg = new LexerGrammar(this, lexerAST);
				lexerg.fileName = g.fileName;
				g.implicitLexer = lexerg;
				lexerg.implicitLexerOwner = g;
				processNonCombinedGrammar(lexerg, gencode);
//				System.out.println("lexer tokens="+lexerg.tokenNameToTypeMap);
//				System.out.println("lexer strings="+lexerg.stringLiteralToTypeMap);
			}
		}
		if ( g.implicitLexer!=null ) g.importVocab(g.implicitLexer);
//		System.out.println("tokens="+g.tokenNameToTypeMap);
//		System.out.println("strings="+g.stringLiteralToTypeMap);
		processNonCombinedGrammar(g, gencode);
	}

	public void processNonCombinedGrammar(Grammar g, boolean gencode) {
		if ( g.ast!=null && internalOption_PrintGrammarTree ) System.out.println(g.ast.toStringTree());
		//g.ast.inspect();

		if ( errMgr.getNumErrors()>0 ) return;

		// MAKE SURE GRAMMAR IS SEMANTICALLY CORRECT (FILL IN GRAMMAR OBJECT)
		SemanticPipeline sem = new SemanticPipeline(g);
		sem.process();

		if ( errMgr.getNumErrors()>0 ) return;

		// BUILD ATN FROM AST
		ATNFactory factory;
		if ( g.isLexer() ) factory = new LexerATNFactory((LexerGrammar)g);
		else factory = new ParserATNFactory(g);
		g.atn = factory.createATN();

		if ( generate_ATN_dot ) generateATNs(g);

		// PERFORM GRAMMAR ANALYSIS ON ATN: BUILD DECISION DFAs
		AnalysisPipeline anal = new AnalysisPipeline(g);
		anal.process();

		//if ( generate_DFA_dot ) generateDFAs(g);

		if ( g.tool.getNumErrors()>0 ) return;

		// GENERATE CODE
		if ( gencode ) {
			CodeGenPipeline gen = new CodeGenPipeline(g);
			gen.process();
		}
	}

	/** Given the raw AST of a grammar, create a grammar object
		associated with the AST. Once we have the grammar object, ensure
		that all nodes in tree referred to this grammar. Later, we will
		use it for error handling and generally knowing from where a rule
		comes from.
	 */
	public Grammar createGrammar(GrammarRootAST ast) {
		final Grammar g;
		if ( ast.grammarType==ANTLRParser.LEXER ) g = new LexerGrammar(this, ast);
		else g = new Grammar(this, ast);

		// ensure each node has pointer to surrounding grammar
		GrammarTransformPipeline.setGrammarPtr(g, ast);
		return g;
	}

	public GrammarRootAST loadGrammar(String fileName) {
		try {
			ANTLRFileStream in = new ANTLRFileStream(fileName, grammarEncoding);
			GrammarRootAST t = load(in);
			return t;
		}
		catch (IOException ioe) {
			errMgr.toolError(ErrorType.CANNOT_OPEN_FILE, ioe, fileName);
		}
		return null;
	}

	/**
	 * Try current dir then dir of g then lib dir
	 * @param g
	 * @param name The imported grammar name.
	 */
	public Grammar loadImportedGrammar(Grammar g, String name) throws IOException {
		g.tool.log("grammar", "load " + name + " from " + g.fileName);
		File importedFile = null;
		for (String extension : ALL_GRAMMAR_EXTENSIONS) {
			importedFile = getImportedGrammarFile(g, name + extension);
			if (importedFile != null) {
				break;
			}
		}

		if ( importedFile==null ) {
			errMgr.toolError(ErrorType.CANNOT_FIND_IMPORTED_GRAMMAR, name, g.fileName);
			return null;
		}

		ANTLRFileStream in = new ANTLRFileStream(importedFile.getAbsolutePath());
		GrammarRootAST root = load(in);
		Grammar imported = createGrammar(root);
		imported.fileName = importedFile.getAbsolutePath();
		return imported;
	}

	public GrammarRootAST loadFromString(String grammar) {
		return load(new ANTLRStringStream(grammar));
	}

	public GrammarRootAST load(CharStream in) {
		try {
			GrammarASTAdaptor adaptor = new GrammarASTAdaptor(in);
			ANTLRLexer lexer = new ANTLRLexer(in);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			lexer.tokens = tokens;
			ToolANTLRParser p = new ToolANTLRParser(tokens, this);
			p.setTreeAdaptor(adaptor);
			ParserRuleReturnScope r = p.grammarSpec();
			GrammarAST root = (GrammarAST)r.getTree();
			if ( root instanceof GrammarRootAST) {
				((GrammarRootAST)root).hasErrors = p.getNumberOfSyntaxErrors()>0;
				((GrammarRootAST)root).tokens = tokens;
				if ( grammarOptions!=null ) {
					((GrammarRootAST)root).cmdLineOptions = grammarOptions;
				}
				return ((GrammarRootAST)root);
			}
			return null;
		}
		catch (RecognitionException re) {
			// TODO: do we gen errors now?
			ErrorManager.internalError("can't generate this message at moment; antlr recovers");
		}
		return null;
	}

	public void generateATNs(Grammar g) {
		DOTGenerator dotGenerator = new DOTGenerator(g);
		List<Grammar> grammars = new ArrayList<Grammar>();
		grammars.add(g);
		List<Grammar> imported = g.getAllImportedGrammars();
		if ( imported!=null ) grammars.addAll(imported);
		for (Grammar ig : grammars) {
			for (Rule r : ig.rules.values()) {
				try {
					String dot = dotGenerator.getDOT(g.atn.ruleToStartState[r.index], g.isLexer());
					if (dot != null) {
						writeDOTFile(g, r, dot);
					}
				}
                catch (IOException ioe) {
					errMgr.toolError(ErrorType.CANNOT_WRITE_FILE, ioe);
				}
			}
		}
	}

	/** This method is used by all code generators to create new output
	 *  files. If the outputDir set by -o is not present it will be created.
	 *  The final filename is sensitive to the output directory and
	 *  the directory where the grammar file was found.  If -o is /tmp
	 *  and the original grammar file was foo/t.g4 then output files
	 *  go in /tmp/foo.
	 *
	 *  The output dir -o spec takes precedence if it's absolute.
	 *  E.g., if the grammar file dir is absolute the output dir is given
	 *  precendence. "-o /tmp /usr/lib/t.g4" results in "/tmp/T.java" as
	 *  output (assuming t.g4 holds T.java).
	 *
	 *  If no -o is specified, then just write to the directory where the
	 *  grammar file was found.
	 *
	 *  If outputDirectory==null then write a String.
	 */
	public Writer getOutputFileWriter(Grammar g, String fileName) throws IOException {
		if (outputDirectory == null) {
			return new StringWriter();
		}
		// output directory is a function of where the grammar file lives
		// for subdir/T.g4, you get subdir here.  Well, depends on -o etc...
		// But, if this is a .tokens file, then we force the output to
		// be the base output directory (or current directory if there is not a -o)
		//
		File outputDir;
		if ( fileName.endsWith(".tokens") ) {// CodeGenerator.VOCAB_FILE_EXTENSION)) {
			outputDir = new File(outputDirectory);
		}
		else {
			outputDir = getOutputDirectory(g.fileName);
		}
		File outputFile = new File(outputDir, fileName);

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		FileWriter fw = new FileWriter(outputFile);
		return new BufferedWriter(fw);
	}

	public File getImportedGrammarFile(Grammar g, String fileName) {
		File importedFile = new File(fileName);
		if ( !importedFile.exists() ) {
			File gfile = new File(g.fileName);
			String parentDir = gfile.getParent();
			importedFile = new File(parentDir, fileName);
			if ( !importedFile.exists() ) { // try in lib dir
				importedFile = new File(libDirectory, fileName);
				if ( !importedFile.exists() ) {
					return null;
				}
			}
		}
		return importedFile;
	}

	/**
	 * Return the location where ANTLR will generate output files for a given
	 * file. This is a base directory and output files will be relative to
	 * here in some cases such as when -o option is used and input files are
	 * given relative to the input directory.
	 *
	 * @param fileNameWithPath path to input source
	 * @return
	 */
	public File getOutputDirectory(String fileNameWithPath) {
		File outputDir;
		String fileDirectory;

		// Some files are given to us without a PATH but should should
		// still be written to the output directory in the relative path of
		// the output directory. The file directory is either the set of sub directories
		// or just or the relative path recorded for the parent grammar. This means
		// that when we write the tokens files, or the .java files for imported grammars
		// taht we will write them in the correct place.
		if (fileNameWithPath.lastIndexOf(File.separatorChar) == -1) {
			// No path is included in the file name, so make the file
			// directory the same as the parent grammar (which might sitll be just ""
			// but when it is not, we will write the file in the correct place.
			fileDirectory = ".";

		}
		else {
			fileDirectory = fileNameWithPath.substring(0, fileNameWithPath.lastIndexOf(File.separatorChar));
		}
		if ( haveOutputDir ) {
			// -o /tmp /var/lib/t.g4 => /tmp/T.java
			// -o subdir/output /usr/lib/t.g4 => subdir/output/T.java
			// -o . /usr/lib/t.g4 => ./T.java
			if (fileDirectory != null &&
				(new File(fileDirectory).isAbsolute() ||
				 fileDirectory.startsWith("~"))) { // isAbsolute doesn't count this :(
				// somebody set the dir, it takes precendence; write new file there
				outputDir = new File(outputDirectory);
			}
			else {
				// -o /tmp subdir/t.g4 => /tmp/subdir/t.g4
				if (fileDirectory != null) {
					outputDir = new File(outputDirectory, fileDirectory);
				}
				else {
					outputDir = new File(outputDirectory);
				}
			}
		}
		else {
			// they didn't specify a -o dir so just write to location
			// where grammar is, absolute or relative, this will only happen
			// with command line invocation as build tools will always
			// supply an output directory.
			outputDir = new File(fileDirectory);
		}
		return outputDir;
	}

	protected void writeDOTFile(Grammar g, Rule r, String dot) throws IOException {
		writeDOTFile(g, r.g.name + "." + r.name, dot);
	}

	protected void writeDOTFile(Grammar g, String name, String dot) throws IOException {
		Writer fw = getOutputFileWriter(g, name + ".dot");
		fw.write(dot);
		fw.close();
	}

	public void help() {
		info("ANTLR Parser Generator  Version " + new Tool().VERSION);
		for (Option o : optionDefs) {
			String name = o.name + (o.argType!=OptionArgType.NONE? " ___" : "");
			String s = String.format(" %-19s %s", name, o.description);
			info(s);
		}
	}

    public void log(@Nullable String component, String msg) { logMgr.log(component, msg); }
    public void log(String msg) { log(msg); }

	public int getNumErrors() { return errMgr.getNumErrors(); }

	public void addListener(ANTLRToolListener tl) {
		if ( tl!=null ) listeners.add(tl);
	}
	public void removeListener(ANTLRToolListener tl) { listeners.remove(tl); }
	public void removeListeners() { listeners.clear(); }
	public List<ANTLRToolListener> getListeners() { return listeners; }

	public void info(String msg) {
		if ( listeners.isEmpty() ) {
			defaultListener.info(msg);
			return;
		}
		for (ANTLRToolListener l : listeners) l.info(msg);
	}
	public void error(ANTLRMessage msg) {
		if ( listeners.isEmpty() ) {
			defaultListener.error(msg);
			return;
		}
		for (ANTLRToolListener l : listeners) l.error(msg);
	}
	public void warning(ANTLRMessage msg) {
		if ( listeners.isEmpty() ) {
			defaultListener.warning(msg);
			return;
		}
		for (ANTLRToolListener l : listeners) l.warning(msg);
	}

	public void version() {
		info("ANTLR Parser Generator  Version " + new Tool().VERSION);
	}

	public void exit(int e) { System.exit(e); }

	public void panic() { throw new Error("ANTLR panic"); }

}