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

package org.antlr.v4.runtime.tree;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.gui.TreePostScriptGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A set of utility routines useful for all kinds of ANTLR trees */
public class Trees {

	public static String getPS(Tree t, Parser recog,
							   String fontName, int fontSize)
	{
		TreePostScriptGenerator psgen =
			new TreePostScriptGenerator(recog, t, fontName, fontSize);
		return psgen.getPS();
	}

	public static String getPS(Tree t, Parser recog) {
		return getPS(t, recog, "Helvetica", 11);
	}

	public static void writePS(Tree t, Parser recog,
							   String fileName,
							   String fontName, int fontSize)
		throws IOException
	{
		String ps = getPS(t, recog, fontName, fontSize);
		FileWriter f = new FileWriter(fileName);
		BufferedWriter bw = new BufferedWriter(f);
		bw.write(ps);
		bw.close();
	}

	public static void writePS(Tree t, Parser recog, String fileName)
		throws IOException
	{
		writePS(t, recog, fileName, "Helvetica", 11);
	}

	/** Print out a whole tree in LISP form. getNodeText is used on the
	 *  node payloads to get the text for the nodes.  Detect
	 *  parse trees and extract data appropriately.
	 */
	public static String toStringTree(Tree t, Parser recog) {
		String s = Utils.escapeWhitespace(getNodeText(t, recog), false);
		if ( t.getChildCount()==0 ) return s;
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		s = Utils.escapeWhitespace(getNodeText(t, recog), false);
		buf.append(s);
		buf.append(' ');
		for (int i = 0; i<t.getChildCount(); i++) {
			if ( i>0 ) buf.append(' ');
			buf.append(toStringTree(t.getChild(i), recog));
		}
		buf.append(")");
		return buf.toString();
	}

	public static String getNodeText(Tree t, Parser recog) {
		if ( recog!=null ) {
			if ( t instanceof ParseTree.RuleNode ) {
				int ruleIndex = ((ParseTree.RuleNode)t).getRuleContext().getRuleIndex();
				String ruleName = recog.getRuleNames()[ruleIndex];
				return ruleName;
			}
			else if ( t instanceof ParseTree.ErrorNodeImpl) {
				return t.toString();
			}
			else if ( t instanceof ParseTree.TerminalNode) {
				Object symbol = ((ParseTree.TerminalNode<?>)t).getSymbol();
				if (symbol instanceof Token) {
					String s = ((Token)symbol).getText();
					return s;
				}
			}
		}
		// no recog for rule names
		Object payload = t.getPayload();
		if ( payload instanceof Token ) {
			return ((Token)payload).getText();
		}
		return t.getPayload().toString();
	}

	/** Return a list of all ancestors of this node.  The first node of
	 *  list is the root and the last is the parent of this node.
	 */
	@NotNull
	public static List<? extends Tree> getAncestors(@NotNull Tree t) {
		if ( t.getParent()==null ) return Collections.emptyList();
		List<Tree> ancestors = new ArrayList<Tree>();
		t = t.getParent();
		while ( t!=null ) {
			ancestors.add(0, t); // insert at start
			t = t.getParent();
		}
		return ancestors;
	}

}