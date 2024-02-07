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

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.ScopeUtils;

/**
 * Checks that classes are designed for inheritance.
 *
 * <p>
 * More specifically, it enforces a programming style
 * where superclasses provide empty "hooks" that can be
 * implemented by subclasses.
 * </p>
 *
 * <p>
 * The exact rule is that nonprivate methods in
 * nonfinal classes (or classes that do not
 * only have private constructors) must either be
 * <ul>
 * <li>abstract or</li>
 * <li>final or</li>
 * <li>have an empty implementation</li>
 * </ul>
 * </p>
 *
 * <p>
 * This protects superclasses against beeing broken by
 * subclasses. The downside is that subclasses are limited
 * in their flexibility, in particular they cannot prevent
 * execution of code in the superclass, but that also
 * means that subclasses can't forget to call their super
 * method.
 * </p>
 *
 * @author lkuehne
 * @version $Revision: 1.4 $
 */
public class DesignForInheritanceCheck extends Check
{
    /** @see Check */
    public int[] getDefaultTokens()
    {
        return new int[] {TokenTypes.METHOD_DEF};
    }

    /** @see Check */
    public void visitToken(DetailAST aAST)
    {
        // nothing to do for Interfaces
        if (ScopeUtils.inInterfaceBlock(aAST)) {
            return;
        }

        // method is ok if it is private or abstract or final
        DetailAST modifiers = aAST.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers.branchContains(TokenTypes.LITERAL_PRIVATE)
            || modifiers.branchContains(TokenTypes.ABSTRACT)
            || modifiers.branchContains(TokenTypes.FINAL)
            || modifiers.branchContains(TokenTypes.LITERAL_STATIC))
        {
            return;
        }

        // method is ok if it is empty
        DetailAST implemetation = aAST.findFirstToken(TokenTypes.SLIST);
        if (implemetation.getFirstChild().getType() == TokenTypes.RCURLY) {
            return;
        }

        // check if the containing class can be subclassed
        DetailAST classDef = findContainingClass(aAST);
        DetailAST classMods = classDef.findFirstToken(TokenTypes.MODIFIERS);
        if (classMods.branchContains(TokenTypes.FINAL)) {
            return;
        }

        // check if subclassing is prevented by having only private ctors
        DetailAST objBlock = classDef.findFirstToken(TokenTypes.OBJBLOCK);

        boolean hasDefaultConstructor = true;
        boolean hasExplNonPrivateCtor = false;

        DetailAST candidate = (DetailAST) objBlock.getFirstChild();

        while (candidate != null) {
            if (candidate.getType() == TokenTypes.CTOR_DEF) {
                hasDefaultConstructor = false;

                DetailAST ctorMods =
                    candidate.findFirstToken(TokenTypes.MODIFIERS);
                if (!ctorMods.branchContains(TokenTypes.LITERAL_PRIVATE)) {
                    hasExplNonPrivateCtor = true;
                    break;
                }
            }
            candidate = (DetailAST) candidate.getNextSibling();
        }

        if (hasDefaultConstructor || hasExplNonPrivateCtor) {
            String name = aAST.findFirstToken(TokenTypes.IDENT).getText();
            log(aAST.getLineNo(), aAST.getColumnNo(),
                "design.forInheritance", name);
        }



    }

    /**
     * Searches the tree towards the root until it finds a CLASS_DEF node.
     * @param aAST the start node for searching
     * @return the CLASS_DEF node.
     */
    private DetailAST findContainingClass(DetailAST aAST)
    {
        DetailAST searchAST = aAST;
        while (searchAST.getType() != TokenTypes.CLASS_DEF) {
            searchAST = searchAST.getParent();
        }
        return searchAST;
    }
}
