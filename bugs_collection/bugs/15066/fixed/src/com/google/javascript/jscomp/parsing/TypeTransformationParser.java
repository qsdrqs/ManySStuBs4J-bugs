/*
 * Copyright 2014 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp.parsing;

import com.google.common.math.DoubleMath;
import com.google.javascript.jscomp.parsing.Config.LanguageMode;
import com.google.javascript.jscomp.parsing.ParserRunner.ParseResult;
import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.SimpleErrorReporter;
import com.google.javascript.rhino.jstype.StaticSourceFile;

import java.util.HashSet;

/**
 * A parser for the type transformation expressions (TTL-Exp) as in
 * @template T := TTL-Exp =:
 *
 */
public final class TypeTransformationParser {

  private String typeTransformationString;
  private Node typeTransformationAst;
  private StaticSourceFile sourceFile;
  private ErrorReporter errorReporter;
  private int templateLineno, templateCharno;

  private static final int VAR_ARGS = Integer.MAX_VALUE - 1;

  /** The classification of the keywords */
  public static enum OperationKind {
    TYPE_CONSTRUCTOR,
    OPERATION,
    BOOLEAN_PREDICATE
  }

  /** Keywords of the type transformation language */
  public static enum Keywords {
    TYPE("type", 2, VAR_ARGS, OperationKind.TYPE_CONSTRUCTOR),
    UNION("union", 2, VAR_ARGS, OperationKind.TYPE_CONSTRUCTOR),
    COND("cond", 3, 3, OperationKind.OPERATION),
    MAPUNION("mapunion", 2, 2, OperationKind.OPERATION),
    EQ("eq", 2, 2, OperationKind.BOOLEAN_PREDICATE),
    SUB("sub", 2, 2, OperationKind.BOOLEAN_PREDICATE),
    NONE("none", 0, 0, OperationKind.TYPE_CONSTRUCTOR),
    RAWTYPEOF("rawTypeOf", 1, 1, OperationKind.TYPE_CONSTRUCTOR),
    TEMPLATETYPEOF("templateTypeOf", 2, 2, OperationKind.TYPE_CONSTRUCTOR),
    RECORD("record", 1, 1, OperationKind.TYPE_CONSTRUCTOR);

    public final String name;
    public final int minParamCount, maxParamCount;
    public final OperationKind kind;

    Keywords(String name, int minParamCount, int maxParamCount,
        OperationKind kind) {
      this.name = name;
      this.minParamCount = minParamCount;
      this.maxParamCount = maxParamCount;
      this.kind = kind;
    }
  }

  public TypeTransformationParser(String typeTransformationString,
      StaticSourceFile sourceFile, ErrorReporter errorReporter,
      int templateLineno, int templateCharno) {
    this.typeTransformationString = typeTransformationString;
    this.sourceFile = sourceFile;
    this.errorReporter = errorReporter;
    this.templateLineno = templateLineno;
    this.templateCharno = templateCharno;
  }

  public Node getTypeTransformationAst() {
    return typeTransformationAst;
  }

  private void addNewWarning(String messageId, String messageArg, Node nodeWarning) {
    // TODO(lpino): Use the exact lineno and charno, it is currently using
    // the lineno and charno of the parent @template
    errorReporter.warning(
        "Bad type annotation. "
            + SimpleErrorReporter.getMessage1(messageId, messageArg),
            sourceFile.getName(),
            templateLineno,
            templateCharno);
  }

  private Keywords nameToKeyword(String s) {
    return Keywords.valueOf(s.toUpperCase());
  }

  private boolean isValidKeyword(String name) {
    for (Keywords k : Keywords.values()) {
      if (k.name.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean isOperationKind(String name, OperationKind kind) {
    return isValidKeyword(name) ? nameToKeyword(name).kind == kind : false;
  }

  private boolean isValidTypeConstructor(String name) {
    return isOperationKind(name, OperationKind.TYPE_CONSTRUCTOR);
  }

  private boolean isValidBooleanPredicate(String name) {
    return isOperationKind(name, OperationKind.BOOLEAN_PREDICATE);
  }

  private boolean isUnion(String name) {
    return Keywords.UNION.name.equals(name);
  }

  private boolean isTemplateType(String name) {
    return Keywords.TYPE.name.equals(name);
  }

  private Node getCallArgument(Node n, int i) {
    return n.isCall() ? n.getChildAtIndex(i + 1) : null;
  }

  private boolean isTypeVar(Node n) {
    return n.isName();
  }

  private boolean isTypeName(Node n) {
    return n.isString();
  }

  private boolean isOperation(Node n) {
    return n.isCall();
  }

  /**
   * A valid expression is either:
   * - NAME for a type variable
   * - STRING for a type name
   * - CALL for the other expressions
   */
  private boolean isValidExpression(Node e) {
    return isTypeVar(e) || isTypeName(e) || isOperation(e);
  }

  private void warnInvalid(String msg, Node e) {
    addNewWarning("msg.jsdoc.typetransformation.invalid", msg, e);
  }

  private void warnInvalidExpression(String msg, Node e) {
    addNewWarning("msg.jsdoc.typetransformation.invalid.expression", msg, e);
  }

  private void warnMissingParam(String msg, Node e) {
    addNewWarning("msg.jsdoc.typetransformation.missing.param", msg, e);
  }

  private void warnExtraParam(String msg, Node e) {
    addNewWarning("msg.jsdoc.typetransformation.extra.param", msg, e);
  }

  private void warnInvalidInside(String msg, Node e) {
    addNewWarning("msg.jsdoc.typetransformation.invalid.inside", msg, e);
  }

  private boolean checkParameterCount(Node expr, Keywords keyword) {
    int numParams = expr.getChildCount();
    if (numParams < 1 + keyword.minParamCount) {
      warnMissingParam(keyword.name, expr);
      return false;
    }
    if (numParams > 1 + keyword.maxParamCount) {
      warnExtraParam(keyword.name, expr);
      return false;
    }
    return true;
  }

  /**
   * Takes a type transformation expression, transforms it to an AST using
   * the ParserRunner of the JSCompiler and then verifies that it is a valid
   * AST.
   * @return true if the parsing was successful otherwise it returns false and
   * at least one warning is reported
   */
  public boolean parseTypeTransformation() {
    Config config = new Config(new HashSet<String>(),
        new HashSet<String>(), true, true, LanguageMode.ECMASCRIPT6, false);
    // TODO(lpino): ParserRunner reports errors if the expression is not
    // ES6 valid. We need to abort the validation of the type transformation
    // whenever an error is reported.
    ParseResult result = ParserRunner.parse(
        sourceFile, typeTransformationString, config, errorReporter);
    Node ast = result.ast;
    // Check that the expression is a script with an expression result
    if (!ast.isScript() || !ast.getFirstChild().isExprResult()) {
      warnInvalidExpression("type transformation", ast);
      return false;
    }

    Node expr = ast.getFirstChild().getFirstChild();
    // The AST of the type transformation must correspond to a valid expression
    if (!validTypeTransformationExpression(expr)) {
      // No need to add a new warning because the validation does it
      return false;
    }
    // Store the result if the AST is valid
    typeTransformationAst = expr;
    return true;
  }

  /**
   * A template type expression must be a type variable or
   * a type(typename, TypeExp...) expression
   */
  private boolean validTemplateTypeExpression(Node expr) {
    // The expression must have at least three children the type keyword,
    // a type name (or type variable) and a type expression
    if (!checkParameterCount(expr, Keywords.TYPE)) {
      return false;
    }
    int numParams = expr.getChildCount() - 1;
    // The first parameter must be a type variable or a type name
    Node firstParam = getCallArgument(expr, 0);
    if (!isTypeVar(firstParam) && !isTypeName(firstParam)) {
      warnInvalid("type name or type variable", expr);
      warnInvalidInside("template type operation", expr);
      return false;
    }
    // The rest of the parameters must be valid type expressions
    for (int i = 1; i < numParams; i++) {
      if (!validTypeExpression(getCallArgument(expr, i))) {
        warnInvalidInside("template type operation", expr);
        return false;
      }
    }
    return true;
  }

  /**
   * A Union type expression must be a valid type variable or
   * a union(Uniontype-Exp, Uniontype-Exp, ...)
   */
  private boolean validUnionTypeExpression(Node expr) {
    if (!isTypeVar(expr) && !isOperation(expr)) {
      warnInvalidExpression("union type", expr);
      return false;
    }
    if (isTypeVar(expr)) {
      return true;
    }
    // It must start with union keyword
    String name = expr.getFirstChild().getString();
    if (!isUnion(name)) {
      warnInvalidExpression("union type", expr);
      return false;
    }
    // The expression must have at least three children: The union keyword and
    // two type expressions
    if (!checkParameterCount(expr, Keywords.UNION)) {
      return false;
    }
    int numParams = expr.getChildCount() - 1;
    // Check if each of the members of the union is a valid type expression
    for (int i = 0; i < numParams; i++) {
      if (!validTypeExpression(expr.getChildAtIndex(i))) {
        warnInvalidInside("union type", expr);
        return false;
      }
    }
    return true;
  }

  /**
   * A none type expression must be of the form: none()
   */
  private boolean validNoneTypeExpression(Node expr) {
    // The expression must have no children
    return checkParameterCount(expr, Keywords.NONE);
  }

  /**
   * A raw type expression must be of the form rawTypeOf(TemplateType)
   */
  private boolean validRawTypeOfTypeExpression(Node expr) {
    // The expression must have two children. The rawTypeOf keyword and the
    // parameter
    if (!checkParameterCount(expr, Keywords.RAWTYPEOF)) {
      return false;
    }
    // The parameter must be a valid type expression
    if (!validTypeExpression(getCallArgument(expr, 0))) {
      warnInvalidInside("rawTypeOf", expr);
      return false;
    }
    return true;
  }

  /**
   * A template type of expression must be of the form
   * templateTypeOf(TemplateType, index)
   */
  private boolean validTemplateTypeOfExpression(Node expr) {
    // The expression must have three children. The templateTypeOf keyword, a
    // templatized type and an index
    if (!checkParameterCount(expr, Keywords.TEMPLATETYPEOF)) {
      return false;
    }
    // The parameter must be a valid type expression
    if (!validTypeExpression(getCallArgument(expr, 0))) {
      warnInvalidInside("templateTypeOf", expr);
      return false;
    }
    if (!getCallArgument(expr, 1).isNumber()) {
      warnInvalid("index", expr);
      warnInvalidInside("templateTypeOf", expr);
      return false;
    }
    double index = getCallArgument(expr, 1).getDouble();
    if (!DoubleMath.isMathematicalInteger(index) || index < 0) {
      warnInvalid("index", expr);
      warnInvalidInside("templateTypeOf", expr);
      return false;
    }
    return true;
  }

  private boolean validRecordTypeExpression(Node expr) {
    // The expression must have two children. The record keyword and
    // a record expression
    if (!checkParameterCount(expr, Keywords.RECORD)) {
      return false;
    }
    // A record expression must be an object literal with at least one property
    Node record = getCallArgument(expr, 0);
    if (!record.isObjectLit()) {
      warnInvalid("record expression", record);
      return false;
    }
    if (record.getChildCount() < 1) {
      warnMissingParam("record expression", record);
      return false;
    }
    // Each value of a property must be a valid type expression
    for (Node prop : record.children()) {
      if (!prop.hasChildren()) {
        warnInvalid("property, missing type", prop);
        warnInvalidInside("record", prop);
        return false;
      } else if (!validTypeExpression(prop.getFirstChild())) {
        warnInvalidInside("record", prop);
        return false;
      }
    }
    return true;
  }

  /**
   * A TTL type expression must be a type variable, a basic type expression
   * or a union type expression
   */
  private boolean validTypeExpression(Node expr) {
    if (!isValidExpression(expr)) {
      warnInvalidExpression("type", expr);
      return false;
    }
    if (isTypeVar(expr) || isTypeName(expr)) {
      return true;
    }
    // If it is an operation we can safely move one level down
    Node operation = expr.getFirstChild();
    String name = operation.getString();
    // Check for valid type operations
    if (!isValidTypeConstructor(name)) {
      warnInvalidExpression("type", expr);
      return false;
    }
    Keywords keyword = nameToKeyword(name);
    // Use the right verifier
    if (keyword == Keywords.TYPE) {
      return validTemplateTypeExpression(expr);
    }
    if (keyword == Keywords.UNION) {
      return validUnionTypeExpression(expr);
    }
    if (keyword == Keywords.NONE) {
      return validNoneTypeExpression(expr);
    }
    if (keyword == Keywords.RAWTYPEOF) {
      return validRawTypeOfTypeExpression(expr);
    }
    if (keyword == Keywords.TEMPLATETYPEOF) {
      return validTemplateTypeOfExpression(expr);
    }
    if (keyword == Keywords.RECORD) {
      return validRecordTypeExpression(expr);
    }
    throw new IllegalStateException("Invalid type expression");
  }

  /**
   * A boolean expression (Bool-Exp) must follow the syntax:
   * Bool-Exp := eq(Type-Exp, Type-Exp) | sub(Type-Exp, Type-Exp)
   */
  private boolean validBooleanTypeExpression(Node expr) {
    if (!isOperation(expr)) {
      warnInvalidExpression("boolean", expr);
      return false;
    }
    String predicate = expr.getFirstChild().getString();
    if (!isValidBooleanPredicate(predicate)) {
      warnInvalid("boolean predicate", expr);
      return false;
    }
    if (!checkParameterCount(expr, Keywords.EQ)) {
      return false;
    }
    // Both input types must be valid type expressions
    if (!validTypeTransformationExpression(getCallArgument(expr, 0))
        || !validTypeTransformationExpression(getCallArgument(expr, 1))) {
      warnInvalidInside("boolean", expr);
      return false;
    }
    return true;
  }

  /**
   * A conditional type transformation expression must be of the
   * form cond(Bool-Exp, TTL-Exp, TTL-Exp)
   */
  private boolean validConditionalExpression(Node expr) {
    // The expression must have four children:
    // - The cond keyword
    // - A boolean expression
    // - A type transformation expression with the 'if' branch
    // - A type transformation expression with the 'else' branch
    if (!checkParameterCount(expr, Keywords.COND)) {
      return false;
    }
    // Check for the validity of the boolean and the expressions
    if (!validBooleanTypeExpression(getCallArgument(expr, 0))) {
      warnInvalidInside("conditional", expr);
      return false;
    }
    if (!validTypeTransformationExpression(getCallArgument(expr, 1))) {
      warnInvalidInside("conditional", expr);
      return false;
    }
    if (!validTypeTransformationExpression(getCallArgument(expr, 2))) {
      warnInvalidInside("conditional", expr);
      return false;
    }
    return true;
  }

  /**
   * A mapunion type transformation expression must be of the form
   * mapunion(Uniontype-Exp, (typevar) => TTL-Exp).
   */
  private boolean validMapunionExpression(Node expr) {
    // The expression must have four children:
    // - The mapunion keyword
    // - A union type expression
    // - A map function
    if (!checkParameterCount(expr, Keywords.MAPUNION)) {
      return false;
    }
    // The second child must be a valid union type expression
    if (!validUnionTypeExpression(getCallArgument(expr, 0))) {
      warnInvalidInside("mapunion", getCallArgument(expr, 0));
      return false;
    }
    // The third child must be a function
    if (!getCallArgument(expr, 1).isFunction()) {
      warnInvalid("map function", getCallArgument(expr, 1));
      return false;
    }
    Node mapFn = getCallArgument(expr, 1);
    // The map function must have only one parameter
    Node mapFnParam = mapFn.getChildAtIndex(1);
    if (!mapFnParam.hasChildren()) {
      warnMissingParam("map function", mapFnParam);
      return false;
    }
    if (!mapFnParam.hasOneChild()) {
      warnExtraParam("map function", mapFnParam);
      return false;
    }
    // The body must be a valid type transformation expression
    Node mapFnBody = mapFn.getChildAtIndex(2);
    if (!validTypeTransformationExpression(mapFnBody)) {
      warnInvalidInside("map function body", mapFnBody);
      return false;
    }
    return true;
  }

  /**
   * Checks the structure of the AST of a type transformation expression
   * in @template T := expression =:
   */
  private boolean validTypeTransformationExpression(Node expr) {
    if (!isValidExpression(expr)) {
      warnInvalidExpression("type transformation", expr);
      return false;
    }
    // If the expression is a type variable or a type name then return
    if (isTypeVar(expr) || isTypeName(expr)) {
      return true;
    }
    // If it is a CALL we can safely move one level down
    String name = expr.getFirstChild().getString();
    // Check for valid operations
    if (!isValidKeyword(name)) {
      warnInvalidExpression("type transformation", expr);
      return false;
    }
    Keywords keyword = nameToKeyword(name);
    // Check the rest of the expression depending on the operation
    if (keyword.kind == OperationKind.TYPE_CONSTRUCTOR) {
      return validTypeExpression(expr);
    }
    if (keyword == Keywords.COND) {
      return validConditionalExpression(expr);
    }
    if (keyword == Keywords.MAPUNION) {
      return validMapunionExpression(expr);
    }
    throw new IllegalStateException("Invalid type transformation expression");
  }
}
