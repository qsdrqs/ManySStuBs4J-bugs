////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2002  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.puppycrawl.tools.checkstyle.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Checks that package names conform to a format specified
 * by the format property. The format is a
 * <a href="http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html">
 * regular expression</a>
 * and defaults to
 * <strong>^[a-z]+(\.[a-zA-Z_][a-zA-Z_0-9]*)*$</strong>.
 * </p>
 * The default format has been chosen to match the requirements in the
 * <a
 * href="http://java.sun.com/docs/books/jls/second_edition/html/packages.doc.html#40169">
 * Java Language specification</a> and the Sun coding conventions.
 * However both underscores and uppercase letters are rather uncommon,
 * so most projects should probably use
 * <strong>^[a-z]+(\.[a-z][a-z0-9]*)*$</strong>.
 * </p>
 * <p>
 * An example of how to configure the check is:
 * </p>
 * <pre>
 * &lt;module name="PackageName"/&gt;
 * </pre> 
 * <p>
 * An example of how to configure the check for package names that begin with
 * <code>com.puppycrawl.tools.checkstyle</code> is:
 * </p>
 * <pre>
 * &lt;module name="PackageName"&gt;
 *    &lt;property name="format"
 *              value="^com\.puppycrawl\.tools\.checkstyle(\\.[a-zA-Z_][a-zA-Z_0-9]*)*$"/&gt;
 * &lt;/module&gt;
 * </pre>
 *
 * @author <a href="mailto:checkstyle@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
public class PackageNameCheck
    extends AbstractFormatCheck
{
    /**
     * Creates a new <code>PackageNameCheck</code> instance.
     */
    public PackageNameCheck()
    {
        // Uppercase letters seem rather uncommon, but they're allowed in
        // http://java.sun.com/docs/books/jls/
        //   second_edition/html/packages.doc.html#40169
        super("^[a-z]+(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$");
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.PACKAGE_DEF};
    }

    /** @see com.puppycrawl.tools.checkstyle.api.Check */
    public void visitToken(DetailAST aAST)
    {
        final DetailAST nameAST = (DetailAST) aAST.getFirstChild();
        final FullIdent full = FullIdent.createFullIdent(nameAST);
        if (!getRegexp().match(full.getText())) {
            log(full.getLineNo(),
                full.getColumnNo(),
                "name.invalidPattern",
                full.getText(),
                getFormat());
        }
    }
}
