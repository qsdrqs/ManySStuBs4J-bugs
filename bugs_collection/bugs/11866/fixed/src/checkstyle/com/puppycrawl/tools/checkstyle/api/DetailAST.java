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
package com.puppycrawl.tools.checkstyle.api;

import java.util.Arrays;

import antlr.CommonAST;
import antlr.Token;
import antlr.collections.AST;

/**
 * An extension of the CommonAST that records the line and column number.
 * The idea was taken from http://www.jguru.com/jguru/faq/view.jsp?EID=62654.
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 * @author lkuehne
 * @version 1.0
 */
public class DetailAST
    extends CommonAST
{
    /** constant to indicate if not calculated the child count */
    private static final int NOT_INITIALIZED = Integer.MIN_VALUE;

    /** the line number **/
    private int mLineNo = NOT_INITIALIZED;
    /** the column number **/
    private int mColumnNo = NOT_INITIALIZED;

    /** number of children */
    private int mChildCount = NOT_INITIALIZED;
    /** the parent token */
    private DetailAST mParent;

    /**
     * All token types in this branch, sorted.
     *
     * Note: This is not a Set to avoid creating zillions of
     * Integer objects in branchContains().
     */
    private int[] mBranchTokenTypes = null;

    /** @see antlr.CommonAST **/
    public void initialize(Token aTok)
    {
        super.initialize(aTok);
        mLineNo = aTok.getLine();
        mColumnNo = aTok.getColumn() - 1; // expect columns to start @ 0
    }

    /** @see antlr.CommonAST **/
    public void initialize(AST aAST)
    {
        final DetailAST da = (DetailAST) aAST;
        setText(da.getText());
        setType(da.getType());
        mLineNo = da.getLineNo();
        mColumnNo = da.getColumnNo();
    }

    /**
     * Sets this AST's first Child
     * @param aAST the new first child
     */
    public void setFirstChild(AST aAST)
    {
        mChildCount = NOT_INITIALIZED;
        super.setFirstChild(aAST);
    }



    /**
     * Returns the number of child nodes one level below this node. That is is
     * does not recurse down the tree.
     * @return the number of child nodes
     */
    public int getChildCount()
    {
        // lazy init
        if (mChildCount == NOT_INITIALIZED) {
            mChildCount = 0;
            AST child = getFirstChild();

            while (child != null) {
                mChildCount += 1;
                child = child.getNextSibling();
            }
        }
        return mChildCount;
    }

    /**
     * Set the parent token.
     * @param aParent the parent token
     */
    public void setParent(DetailAST aParent)
    {
        // TODO: Check visibility, could be private
        // if set in setFirstChild() and friends
        mParent = aParent;
    }

    /**
     * Returns the parent token
     * @return the parent token
     */
    public DetailAST getParent()
    {
        return mParent;
    }

    /** @return the line number **/
    public int getLineNo()
    {
        if (mLineNo == NOT_INITIALIZED) {
            // an inner AST that has been initialized
            // with initialize(String text)
            DetailAST child = (DetailAST) getFirstChild();
            DetailAST sibling = (DetailAST) getNextSibling();
            if (child != null) {
                return child.getLineNo();
            }
            else if (sibling != null) {
                return sibling.getLineNo();
            }
        }
        return mLineNo;
    }

    /** @return the column number **/
    public int getColumnNo()
    {
        if (mColumnNo == NOT_INITIALIZED) {
            // an inner AST that has been initialized
            // with initialize(String text)
            DetailAST child = (DetailAST) getFirstChild();
            DetailAST sibling = (DetailAST) getNextSibling();
            if (child != null) {
                return child.getColumnNo();
            }
            else if (sibling != null) {
                return sibling.getColumnNo();
            }
        }
        return mColumnNo;
    }

    /** @return a string representation of the object **/
    public String toString()
    {
        return super.toString() + " {line = " + getLineNo() + ", col = "
            + getColumnNo() + "}";
    }

    /** @return the last child node */
    public DetailAST getLastChild()
    {
        AST ast = getFirstChild();
        while (ast != null && ast.getNextSibling() != null) {
            ast = ast.getNextSibling();
        }
        return (DetailAST) ast;
    }

    /**
     * @return the token types that occur in the branch as a sorted set.
     */
    private int[] getBranchTokenTypes()
    {
        // lazy init
        if (mBranchTokenTypes == null) {

            // TODO improve algorithm to avoid most array creation
            int[] bag = new int[] { getType() };

            // add union of all childs
            DetailAST child = (DetailAST) getFirstChild();
            while (child != null) {
                int[] childTypes = child.getBranchTokenTypes();
                int[] savedBag = bag;
                bag = new int[savedBag.length + childTypes.length];
                System.arraycopy(savedBag, 0, bag, 0, savedBag.length);
                System.arraycopy(childTypes, 0, bag, savedBag.length,
                        childTypes.length);
                child = (DetailAST) child.getNextSibling();
            }
            // TODO: remove duplicates to speed up searching
            mBranchTokenTypes = bag;
            Arrays.sort(mBranchTokenTypes);
        }
        return mBranchTokenTypes;
    }

    /**
     * Checks if this branch of the parse tree contains a token
     * of the provided type.
     * @param aType a TokenType
     * @return true if and only if this branch (including this node)
     * contains a token of type <code>aType</code>.
     */
    public boolean branchContains(int aType)
    {
        return Arrays.binarySearch(getBranchTokenTypes(), aType) >= 0;
    }

    /**
     * Returns the number of direct child tokens that have the specified type.
     * @param aType the token type to match
     * @return the number of matching token
     */
    public int getChildCount(int aType)
    {
        int count = 0;
        for (AST i = getFirstChild(); i != null; i = i.getNextSibling()) {
            if (i.getType() == aType) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the previous sibling or null if no such sibling exists.
     * @return the previous sibling or null if no such sibling exists.
     */
    public DetailAST getPreviousSibling()
    {
        DetailAST parent = getParent();
        if (parent == null) {
            return null;
        }

        AST ast = parent.getFirstChild();
        while (ast != null) {
            AST nextSibling = ast.getNextSibling();
            if (this == nextSibling) {
                return (DetailAST) ast;
            }
            ast = nextSibling;
        }
        return null;
    }

    /**
     * Returns the first child token that makes a specified type.
     * @param aType the token type to match
     * @return the matching token, or null if no match
     */
    public DetailAST findFirstToken(int aType)
    {
        DetailAST retVal = null;
        for (AST i = getFirstChild(); i != null; i = i.getNextSibling()) {
            if (i.getType() == aType) {
                retVal = (DetailAST) i;
                break;
            }
        }
        return retVal;
    }
}
