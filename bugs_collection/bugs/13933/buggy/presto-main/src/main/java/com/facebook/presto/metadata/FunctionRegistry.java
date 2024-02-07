/*
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
package com.facebook.presto.metadata;

import com.facebook.presto.operator.aggregation.ApproximateAverageAggregations;
import com.facebook.presto.operator.aggregation.ApproximateCountColumnAggregations;
import com.facebook.presto.operator.aggregation.ApproximateCountDistinctAggregations;
import com.facebook.presto.operator.aggregation.ApproximateDoublePercentileAggregations;
import com.facebook.presto.operator.aggregation.ApproximateLongPercentileAggregations;
import com.facebook.presto.operator.aggregation.ApproximateSetAggregation;
import com.facebook.presto.operator.aggregation.ApproximateSumAggregations;
import com.facebook.presto.operator.aggregation.AverageAggregations;
import com.facebook.presto.operator.aggregation.BooleanMaxAggregation;
import com.facebook.presto.operator.aggregation.BooleanMinAggregation;
import com.facebook.presto.operator.aggregation.CountColumnAggregations;
import com.facebook.presto.operator.aggregation.CountIfAggregation;
import com.facebook.presto.operator.aggregation.DoubleMaxAggregation;
import com.facebook.presto.operator.aggregation.DoubleMinAggregation;
import com.facebook.presto.operator.aggregation.DoubleSumAggregation;
import com.facebook.presto.operator.aggregation.LongMaxAggregation;
import com.facebook.presto.operator.aggregation.LongMinAggregation;
import com.facebook.presto.operator.aggregation.LongSumAggregation;
import com.facebook.presto.operator.aggregation.MaxByAggregations;
import com.facebook.presto.operator.aggregation.MergeHyperLogLogAggregation;
import com.facebook.presto.operator.aggregation.VarBinaryMaxAggregation;
import com.facebook.presto.operator.aggregation.VarBinaryMinAggregation;
import com.facebook.presto.operator.aggregation.VarianceAggregation;
import com.facebook.presto.operator.scalar.ColorFunctions;
import com.facebook.presto.operator.scalar.DateTimeFunctions;
import com.facebook.presto.operator.scalar.HyperLogLogFunctions;
import com.facebook.presto.operator.scalar.JsonFunctions;
import com.facebook.presto.operator.scalar.MathFunctions;
import com.facebook.presto.operator.scalar.RegexpFunctions;
import com.facebook.presto.operator.scalar.StringFunctions;
import com.facebook.presto.operator.scalar.UrlFunctions;
import com.facebook.presto.operator.scalar.VarbinaryFunctions;
import com.facebook.presto.operator.window.CumulativeDistributionFunction;
import com.facebook.presto.operator.window.DenseRankFunction;
import com.facebook.presto.operator.window.FirstValueFunction.BigintFirstValueFunction;
import com.facebook.presto.operator.window.FirstValueFunction.BooleanFirstValueFunction;
import com.facebook.presto.operator.window.FirstValueFunction.DoubleFirstValueFunction;
import com.facebook.presto.operator.window.FirstValueFunction.VarcharFirstValueFunction;
import com.facebook.presto.operator.window.LagFunction.BigintLagFunction;
import com.facebook.presto.operator.window.LagFunction.BooleanLagFunction;
import com.facebook.presto.operator.window.LagFunction.DoubleLagFunction;
import com.facebook.presto.operator.window.LagFunction.VarcharLagFunction;
import com.facebook.presto.operator.window.LastValueFunction.BigintLastValueFunction;
import com.facebook.presto.operator.window.LastValueFunction.BooleanLastValueFunction;
import com.facebook.presto.operator.window.LastValueFunction.DoubleLastValueFunction;
import com.facebook.presto.operator.window.LastValueFunction.VarcharLastValueFunction;
import com.facebook.presto.operator.window.LeadFunction.BigintLeadFunction;
import com.facebook.presto.operator.window.LeadFunction.BooleanLeadFunction;
import com.facebook.presto.operator.window.LeadFunction.DoubleLeadFunction;
import com.facebook.presto.operator.window.LeadFunction.VarcharLeadFunction;
import com.facebook.presto.operator.window.NthValueFunction.BigintNthValueFunction;
import com.facebook.presto.operator.window.NthValueFunction.BooleanNthValueFunction;
import com.facebook.presto.operator.window.NthValueFunction.DoubleNthValueFunction;
import com.facebook.presto.operator.window.NthValueFunction.VarcharNthValueFunction;
import com.facebook.presto.operator.window.PercentRankFunction;
import com.facebook.presto.operator.window.RankFunction;
import com.facebook.presto.operator.window.RowNumberFunction;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.StandardErrorCode;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.TypeManager;
import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.type.BigintOperators;
import com.facebook.presto.type.BooleanOperators;
import com.facebook.presto.type.DateOperators;
import com.facebook.presto.type.DateTimeOperators;
import com.facebook.presto.type.DoubleOperators;
import com.facebook.presto.type.HyperLogLogOperators;
import com.facebook.presto.type.IntervalDayTimeOperators;
import com.facebook.presto.type.IntervalYearMonthOperators;
import com.facebook.presto.type.LikeFunctions;
import com.facebook.presto.type.TimeOperators;
import com.facebook.presto.type.TimeWithTimeZoneOperators;
import com.facebook.presto.type.TimestampOperators;
import com.facebook.presto.type.TimestampWithTimeZoneOperators;
import com.facebook.presto.type.TypeUtils;
import com.facebook.presto.type.VarbinaryOperators;
import com.facebook.presto.type.VarcharOperators;
import com.facebook.presto.util.IterableTransformer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Primitives;
import io.airlift.slice.Slice;

import javax.annotation.concurrent.ThreadSafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.presto.metadata.FunctionInfo.isAggregationPredicate;
import static com.facebook.presto.metadata.FunctionInfo.isHiddenPredicate;
import static com.facebook.presto.operator.aggregation.ApproximateCountAggregation.APPROXIMATE_COUNT_AGGREGATION;
import static com.facebook.presto.operator.aggregation.CountAggregation.COUNT;
import static com.facebook.presto.spi.type.BigintType.BIGINT;
import static com.facebook.presto.spi.type.BooleanType.BOOLEAN;
import static com.facebook.presto.spi.type.DateType.DATE;
import static com.facebook.presto.spi.type.DoubleType.DOUBLE;
import static com.facebook.presto.spi.type.TimeType.TIME;
import static com.facebook.presto.spi.type.TimeWithTimeZoneType.TIME_WITH_TIME_ZONE;
import static com.facebook.presto.spi.type.TimestampType.TIMESTAMP;
import static com.facebook.presto.spi.type.TimestampWithTimeZoneType.TIMESTAMP_WITH_TIME_ZONE;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static com.facebook.presto.type.JsonPathType.JSON_PATH;
import static com.facebook.presto.type.LikePatternType.LIKE_PATTERN;
import static com.facebook.presto.type.RegexpType.REGEXP;
import static com.facebook.presto.type.TypeUtils.nameGetter;
import static com.facebook.presto.type.UnknownType.UNKNOWN;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static java.lang.String.format;

@ThreadSafe
public class FunctionRegistry
{
    private static final String MAGIC_LITERAL_FUNCTION_PREFIX = "$literal$";

    // hack: java classes for types that can be used with magic literals
    private static final Set<Class<?>> SUPPORTED_LITERAL_TYPES = ImmutableSet.<Class<?>>of(long.class, double.class, Slice.class, boolean.class);

    private final TypeManager typeManager;
    private volatile FunctionMap functions = new FunctionMap();

    public FunctionRegistry(TypeManager typeManager, boolean experimentalSyntaxEnabled)
    {
        this.typeManager = checkNotNull(typeManager, "typeManager is null");

        FunctionListBuilder builder = new FunctionListBuilder(typeManager)
                .window("row_number", BIGINT, ImmutableList.<Type>of(), RowNumberFunction.class)
                .window("rank", BIGINT, ImmutableList.<Type>of(), RankFunction.class)
                .window("dense_rank", BIGINT, ImmutableList.<Type>of(), DenseRankFunction.class)
                .window("percent_rank", DOUBLE, ImmutableList.<Type>of(), PercentRankFunction.class)
                .window("cume_dist", DOUBLE, ImmutableList.<Type>of(), CumulativeDistributionFunction.class)
                .window("first_value", BIGINT, ImmutableList.<Type>of(BIGINT), BigintFirstValueFunction.class)
                .window("first_value", DOUBLE, ImmutableList.<Type>of(DOUBLE), DoubleFirstValueFunction.class)
                .window("first_value", BOOLEAN, ImmutableList.<Type>of(BOOLEAN), BooleanFirstValueFunction.class)
                .window("first_value", VARCHAR, ImmutableList.<Type>of(VARCHAR), VarcharFirstValueFunction.class)
                .window("last_value", BIGINT, ImmutableList.<Type>of(BIGINT), BigintLastValueFunction.class)
                .window("last_value", DOUBLE, ImmutableList.<Type>of(DOUBLE), DoubleLastValueFunction.class)
                .window("last_value", BOOLEAN, ImmutableList.<Type>of(BOOLEAN), BooleanLastValueFunction.class)
                .window("last_value", VARCHAR, ImmutableList.<Type>of(VARCHAR), VarcharLastValueFunction.class)
                .window("nth_value", BIGINT, ImmutableList.<Type>of(BIGINT, BIGINT), BigintNthValueFunction.class)
                .window("nth_value", DOUBLE, ImmutableList.<Type>of(DOUBLE, BIGINT), DoubleNthValueFunction.class)
                .window("nth_value", BOOLEAN, ImmutableList.<Type>of(BOOLEAN, BIGINT), BooleanNthValueFunction.class)
                .window("nth_value", VARCHAR, ImmutableList.<Type>of(VARCHAR, BIGINT), VarcharNthValueFunction.class)
                .window("lag", BIGINT, ImmutableList.<Type>of(BIGINT), BigintLagFunction.class)
                .window("lag", BIGINT, ImmutableList.<Type>of(BIGINT, BIGINT), BigintLagFunction.class)
                .window("lag", BIGINT, ImmutableList.<Type>of(BIGINT, BIGINT, BIGINT), BigintLagFunction.class)
                .window("lag", DOUBLE, ImmutableList.<Type>of(DOUBLE), DoubleLagFunction.class)
                .window("lag", DOUBLE, ImmutableList.<Type>of(DOUBLE, BIGINT), DoubleLagFunction.class)
                .window("lag", DOUBLE, ImmutableList.<Type>of(DOUBLE, BIGINT, DOUBLE), DoubleLagFunction.class)
                .window("lag", BOOLEAN, ImmutableList.<Type>of(BOOLEAN), BooleanLagFunction.class)
                .window("lag", BOOLEAN, ImmutableList.<Type>of(BOOLEAN, BIGINT), BooleanLagFunction.class)
                .window("lag", BOOLEAN, ImmutableList.<Type>of(BOOLEAN, BIGINT, BOOLEAN), BooleanLagFunction.class)
                .window("lag", VARCHAR, ImmutableList.<Type>of(VARCHAR), VarcharLagFunction.class)
                .window("lag", VARCHAR, ImmutableList.<Type>of(VARCHAR, BIGINT), VarcharLagFunction.class)
                .window("lag", VARCHAR, ImmutableList.<Type>of(VARCHAR, BIGINT, VARCHAR), VarcharLagFunction.class)
                .window("lead", BIGINT, ImmutableList.<Type>of(BIGINT), BigintLeadFunction.class)
                .window("lead", BIGINT, ImmutableList.<Type>of(BIGINT, BIGINT), BigintLeadFunction.class)
                .window("lead", BIGINT, ImmutableList.<Type>of(BIGINT, BIGINT, BIGINT), BigintLeadFunction.class)
                .window("lead", DOUBLE, ImmutableList.<Type>of(DOUBLE), DoubleLeadFunction.class)
                .window("lead", DOUBLE, ImmutableList.<Type>of(DOUBLE, BIGINT), DoubleLeadFunction.class)
                .window("lead", DOUBLE, ImmutableList.<Type>of(DOUBLE, BIGINT, DOUBLE), DoubleLeadFunction.class)
                .window("lead", BOOLEAN, ImmutableList.<Type>of(BOOLEAN), BooleanLeadFunction.class)
                .window("lead", BOOLEAN, ImmutableList.<Type>of(BOOLEAN, BIGINT), BooleanLeadFunction.class)
                .window("lead", BOOLEAN, ImmutableList.<Type>of(BOOLEAN, BIGINT, BOOLEAN), BooleanLeadFunction.class)
                .window("lead", VARCHAR, ImmutableList.<Type>of(VARCHAR), VarcharLeadFunction.class)
                .window("lead", VARCHAR, ImmutableList.<Type>of(VARCHAR, BIGINT), VarcharLeadFunction.class)
                .window("lead", VARCHAR, ImmutableList.<Type>of(VARCHAR, BIGINT, VARCHAR), VarcharLeadFunction.class)
                .aggregate(COUNT)
                .aggregate(VarianceAggregation.class)
                .aggregate(ApproximateLongPercentileAggregations.class)
                .aggregate(ApproximateDoublePercentileAggregations.class)
                .aggregate(CountIfAggregation.class)
                .aggregate(CountColumnAggregations.class)
                .aggregate(BooleanMinAggregation.class)
                .aggregate(BooleanMaxAggregation.class)
                .aggregate(DoubleMinAggregation.class)
                .aggregate(DoubleMaxAggregation.class)
                .aggregate(LongMinAggregation.class)
                .aggregate(LongMaxAggregation.class)
                .aggregate(VarBinaryMinAggregation.class)
                .aggregate(VarBinaryMaxAggregation.class)
                .aggregate(DoubleSumAggregation.class)
                .aggregate(LongSumAggregation.class)
                .aggregate(AverageAggregations.class)
                .aggregate(ApproximateCountDistinctAggregations.class)
                .aggregate(MergeHyperLogLogAggregation.class)
                .aggregate(ApproximateSetAggregation.class)
                .aggregate(MaxByAggregations.getAggregations(typeManager))
                .scalar(StringFunctions.class)
                .scalar(VarbinaryFunctions.class)
                .scalar(RegexpFunctions.class)
                .scalar(UrlFunctions.class)
                .scalar(MathFunctions.class)
                .scalar(DateTimeFunctions.class)
                .scalar(JsonFunctions.class)
                .scalar(ColorFunctions.class)
                .scalar(HyperLogLogFunctions.class)
                .scalar(BooleanOperators.class)
                .scalar(BigintOperators.class)
                .scalar(DoubleOperators.class)
                .scalar(VarcharOperators.class)
                .scalar(VarbinaryOperators.class)
                .scalar(DateOperators.class)
                .scalar(TimeOperators.class)
                .scalar(TimestampOperators.class)
                .scalar(IntervalDayTimeOperators.class)
                .scalar(IntervalYearMonthOperators.class)
                .scalar(TimeWithTimeZoneOperators.class)
                .scalar(TimestampWithTimeZoneOperators.class)
                .scalar(DateTimeOperators.class)
                .scalar(HyperLogLogOperators.class)
                .scalar(LikeFunctions.class);

        if (experimentalSyntaxEnabled) {
            builder.aggregate(ApproximateAverageAggregations.class)
                    .aggregate(ApproximateSumAggregations.class)
                    .aggregate(APPROXIMATE_COUNT_AGGREGATION)
                    .aggregate(ApproximateCountColumnAggregations.class);
        }

        addFunctions(builder.getFunctions(), builder.getOperators());
    }

    public final synchronized void addFunctions(List<FunctionInfo> functions, Multimap<OperatorType, FunctionInfo> operators)
    {
        for (FunctionInfo function : functions) {
            checkArgument(this.functions.get(function.getSignature()) == null,
                    "Function already registered: %s", function.getSignature());
        }

        this.functions = new FunctionMap(this.functions, functions, operators);
    }

    public List<FunctionInfo> list()
    {
        return FluentIterable.from(functions.list())
                .filter(not(isHiddenPredicate()))
                .toList();
    }

    public boolean isAggregationFunction(QualifiedName name)
    {
        return Iterables.any(functions.get(name), isAggregationPredicate());
    }

    public FunctionInfo resolveFunction(QualifiedName name, List<String> parameterTypes, final boolean approximate)
    {
        List<FunctionInfo> candidates = IterableTransformer.on(functions.get(name)).select(new Predicate<FunctionInfo>() {
            @Override
            public boolean apply(FunctionInfo input)
            {
                return input.isScalar() || input.isApproximate() == approximate;
            }
        }).list();

        // search for exact match
        for (FunctionInfo functionInfo : candidates) {
            if (functionInfo.getArgumentTypes().equals(parameterTypes)) {
                return functionInfo;
            }
        }

        // search for coerced match
        for (FunctionInfo functionInfo : candidates) {
            if (canCoerce(TypeUtils.resolveTypes(parameterTypes, typeManager), TypeUtils.resolveTypes(functionInfo.getArgumentTypes(), typeManager))) {
                return functionInfo;
            }
        }

        List<String> expectedParameters = new ArrayList<>();
        for (FunctionInfo functionInfo : candidates) {
            expectedParameters.add(format("%s(%s)", name, Joiner.on(", ").join(functionInfo.getArgumentTypes())));
        }
        String parameters = Joiner.on(", ").join(parameterTypes);
        String message = format("Function %s not registered", name);
        if (!expectedParameters.isEmpty()) {
            String expected = Joiner.on(", ").join(expectedParameters);
            message = format("Unexpected parameters (%s) for function %s. Expected: %s", parameters, name, expected);
        }

        if (name.getSuffix().startsWith(MAGIC_LITERAL_FUNCTION_PREFIX)) {
            // extract type from function name
            String typeName = name.getSuffix().substring(MAGIC_LITERAL_FUNCTION_PREFIX.length());

            // lookup the type
            Type type = typeManager.getType(typeName);
            checkArgument(type != null, "Type %s not registered", typeName);

            // verify we have one parameter of the proper type
            checkArgument(parameterTypes.size() == 1, "Expected one argument to literal function, but got %s", parameterTypes);
            Type parameterType = typeManager.getType(parameterTypes.get(0));
            checkNotNull(parameterType, "Type %s not foudn", parameterTypes.get(0));
            checkArgument(parameterType.getJavaType() == type.getJavaType(),
                    "Expected type %s to use Java type %s, but Java type is %s",
                    type,
                    parameterType.getJavaType(),
                    type.getJavaType());

            MethodHandle identity = MethodHandles.identity(parameterType.getJavaType());
            return new FunctionInfo(
                    getMagicLiteralFunctionSignature(type),
                    null,
                    true,
                    identity,
                    true,
                    false,
                    ImmutableList.of(false));
        }

        throw new PrestoException(StandardErrorCode.FUNCTION_NOT_FOUND.toErrorCode(), message);
    }

    public FunctionInfo getExactFunction(Signature signature)
    {
        return functions.get(signature);
    }

    @VisibleForTesting
    public List<FunctionInfo> listOperators()
    {
        return ImmutableList.copyOf(functions.byOperator.values());
    }

    public FunctionInfo resolveOperator(OperatorType operatorType, List<? extends Type> argumentTypes)
            throws OperatorNotFoundException
    {
        Iterable<FunctionInfo> candidates = functions.getOperators(operatorType);

        // search for exact match
        for (FunctionInfo operatorInfo : candidates) {
            if (operatorInfo.getArgumentTypes().equals(Lists.transform(argumentTypes, nameGetter()))) {
                return operatorInfo;
            }
        }

        // search for coerced match
        for (FunctionInfo operatorInfo : candidates) {
            if (canCoerce(argumentTypes, TypeUtils.resolveTypes(operatorInfo.getArgumentTypes(), typeManager))) {
                return operatorInfo;
            }
        }

        throw new OperatorNotFoundException(operatorType, argumentTypes);
    }

    public static List<Type> resolveTypes(List<String> typeNames, final TypeManager typeManager)
    {
        return FluentIterable.from(typeNames).transform(new Function<String, Type>() {
            @Override
            public Type apply(String type)
            {
                return checkNotNull(typeManager.getType(type), "Type '%s' not found", type);
            }
        }).toList();
    }

    public FunctionInfo getCoercion(Type fromType, Type toType)
    {
        return getExactOperator(OperatorType.CAST, ImmutableList.of(fromType), toType);
    }

    public FunctionInfo getExactOperator(OperatorType operatorType, List<? extends Type> argumentTypes, Type returnType)
            throws OperatorNotFoundException
    {
        Iterable<FunctionInfo> candidates = functions.getOperators(operatorType);

        List<String> argumentTypeNames = FluentIterable.from(argumentTypes).transform(nameGetter()).toList();

        // search for exact match
        for (FunctionInfo operatorInfo : candidates) {
            if (operatorInfo.getReturnType().equals(returnType.getName()) && operatorInfo.getArgumentTypes().equals(argumentTypeNames)) {
                return operatorInfo;
            }
        }

        // if identity cast, return a custom operator info
        if ((operatorType == OperatorType.CAST) && (argumentTypes.size() == 1) && argumentTypes.get(0).equals(returnType)) {
            MethodHandle identity = MethodHandles.identity(returnType.getJavaType());
            return operatorInfo(OperatorType.CAST, returnType.getName(), FluentIterable.from(argumentTypes).transform(nameGetter()).toList(), identity, false, ImmutableList.of(false));
        }

        throw new OperatorNotFoundException(operatorType, argumentTypes, returnType);
    }

    public static boolean canCoerce(List<? extends Type> actualTypes, List<Type> expectedTypes)
    {
        if (actualTypes.size() != expectedTypes.size()) {
            return false;
        }
        for (int i = 0; i < expectedTypes.size(); i++) {
            Type expectedType = expectedTypes.get(i);
            Type actualType = actualTypes.get(i);
            if (!canCoerce(actualType, expectedType)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canCoerce(Type actualType, Type expectedType)
    {
        // are types the same
        if (expectedType.equals(actualType)) {
            return true;
        }
        // null can be cast to anything
        if (actualType.equals(UNKNOWN)) {
            return true;
        }
        // widen bigint to double
        if (actualType.equals(BIGINT) && expectedType.equals(DOUBLE)) {
            return true;
        }
        // widen date to timestamp
        if (actualType.equals(DATE) && expectedType.equals(TIMESTAMP)) {
            return true;
        }
        // widen date to timestamp with time zone
        if (actualType.equals(DATE) && expectedType.equals(TIMESTAMP_WITH_TIME_ZONE)) {
            return true;
        }
        // widen time to time with time zone
        if (actualType.equals(TIME) && expectedType.equals(TIME_WITH_TIME_ZONE)) {
            return true;
        }
        // widen timestamp to timestamp with time zone
        if (actualType.equals(TIMESTAMP) && expectedType.equals(TIMESTAMP_WITH_TIME_ZONE)) {
            return true;
        }

        if (actualType.equals(VARCHAR) && expectedType.equals(REGEXP)) {
            return true;
        }

        if (actualType.equals(VARCHAR) && expectedType.equals(LIKE_PATTERN)) {
            return true;
        }

        if (actualType.equals(VARCHAR) && expectedType.equals(JSON_PATH)) {
            return true;
        }

        return false;
    }

    public static Optional<Type> getCommonSuperType(Type firstType, Type secondType)
    {
        if (firstType.equals(UNKNOWN)) {
            return Optional.of(secondType);
        }

        if (secondType.equals(UNKNOWN)) {
            return Optional.of(firstType);
        }

        if (firstType.equals(secondType)) {
            return Optional.of(firstType);
        }

        if ((firstType.equals(BIGINT) || firstType.equals(DOUBLE)) && (secondType.equals(BIGINT) || secondType.equals(DOUBLE))) {
            return Optional.<Type>of(DOUBLE);
        }

        if ((firstType.equals(TIME) || firstType.equals(TIME_WITH_TIME_ZONE)) && (secondType.equals(TIME) || secondType.equals(TIME_WITH_TIME_ZONE))) {
            return Optional.<Type>of(TIME_WITH_TIME_ZONE);
        }

        if ((firstType.equals(TIMESTAMP) || firstType.equals(TIMESTAMP_WITH_TIME_ZONE)) && (secondType.equals(TIMESTAMP) || secondType.equals(TIMESTAMP_WITH_TIME_ZONE))) {
            return Optional.<Type>of(TIMESTAMP_WITH_TIME_ZONE);
        }

        return Optional.absent();
    }

    public static Type type(Class<?> clazz)
    {
        clazz = Primitives.unwrap(clazz);
        if (clazz == long.class) {
            return BIGINT;
        }
        if (clazz == double.class) {
            return DOUBLE;
        }
        if (clazz == Slice.class) {
            return VARCHAR;
        }
        if (clazz == boolean.class) {
            return BOOLEAN;
        }
        throw new IllegalArgumentException("Unhandled Java type: " + clazz.getName());
    }

    public static Signature getMagicLiteralFunctionSignature(Type type)
    {
        return new Signature(MAGIC_LITERAL_FUNCTION_PREFIX + type.getName(),
                type.getName(),
                Lists.transform(ImmutableList.of(type(type.getJavaType())), nameGetter()));
    }

    public static boolean isSupportedLiteralType(Type type)
    {
        return SUPPORTED_LITERAL_TYPES.contains(type.getJavaType());
    }

    public static FunctionInfo operatorInfo(OperatorType operatorType, String returnType, List<String> argumentTypes, MethodHandle method, boolean nullable, List<Boolean> nullableArguments)
    {
        operatorType.validateSignature(returnType, ImmutableList.copyOf(argumentTypes));

        Signature signature = new Signature(operatorType.name(), returnType, argumentTypes, true);
        return new FunctionInfo(signature, operatorType.getOperator(), true, method, true, nullable, nullableArguments);
    }

    private static class FunctionMap
    {
        private final Multimap<QualifiedName, FunctionInfo> functionsByName;
        private final Map<Signature, FunctionInfo> functionsBySignature;
        private final Multimap<OperatorType, FunctionInfo> byOperator;

        public FunctionMap()
        {
            functionsByName = ImmutableListMultimap.of();
            functionsBySignature = ImmutableMap.of();
            byOperator = ImmutableListMultimap.of();
        }

        public FunctionMap(FunctionMap map, Iterable<FunctionInfo> functions, Multimap<OperatorType, FunctionInfo> operators)
        {
            functionsByName = ImmutableListMultimap.<QualifiedName, FunctionInfo>builder()
                    .putAll(map.functionsByName)
                    .putAll(Multimaps.index(functions, FunctionInfo.nameGetter()))
                    .build();

            functionsBySignature = ImmutableMap.<Signature, FunctionInfo>builder()
                    .putAll(map.functionsBySignature)
                    .putAll(Maps.uniqueIndex(functions, FunctionInfo.handleGetter()))
                    .build();

            byOperator = ImmutableListMultimap.<OperatorType, FunctionInfo>builder()
                    .putAll(map.byOperator)
                    .putAll(operators)
                    .build();

            // Make sure all functions with the same name are aggregations or none of them are
            for (Map.Entry<QualifiedName, Collection<FunctionInfo>> entry : functionsByName.asMap().entrySet()) {
                Collection<FunctionInfo> infos = entry.getValue();
                checkState(Iterables.all(infos, isAggregationPredicate()) || !Iterables.any(infos, isAggregationPredicate()),
                        "'%s' is both an aggregation and a scalar function", entry.getKey());
            }
        }

        public List<FunctionInfo> list()
        {
            return ImmutableList.copyOf(functionsByName.values());
        }

        public Collection<FunctionInfo> get(QualifiedName name)
        {
            return functionsByName.get(name);
        }

        public FunctionInfo get(Signature signature)
        {
            return functionsBySignature.get(signature);
        }

        public Collection<FunctionInfo> getOperators(OperatorType operatorType)
        {
            return byOperator.get(operatorType);
        }
    }
}
