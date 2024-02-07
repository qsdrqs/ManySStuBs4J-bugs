/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.instrument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.bootstrap.instrument.ClassFilter;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilter;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.NotFoundInstrumentException;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetConstructor;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetConstructors;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetFilter;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetMethod;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.TargetMethods;
import com.navercorp.pinpoint.bootstrap.interceptor.group.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.group.InterceptorGroup;
import com.navercorp.pinpoint.bootstrap.plugin.ObjectRecipe;
import com.navercorp.pinpoint.common.util.Asserts;
import com.navercorp.pinpoint.exception.PinpointException;
import com.navercorp.pinpoint.profiler.instrument.AccessorAnalyzer.AccessorDetails;
import com.navercorp.pinpoint.profiler.instrument.GetterAnalyzer.GetterDetails;
import com.navercorp.pinpoint.profiler.instrument.aspect.AspectWeaverClass;
import com.navercorp.pinpoint.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.objectfactory.AutoBindingObjectFactory;
import com.navercorp.pinpoint.profiler.objectfactory.InterceptorArgumentProvider;
import com.navercorp.pinpoint.profiler.util.JavaAssistUtils;

/**
 * @author emeroad
 * @author netspider
 * @author minwoo.jung
 */
public class JavassistClass implements InstrumentClass {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final Instrumentor pluginContext;
    // private final JavassistClassPool instrumentClassPool;
    private final InterceptorRegistryBinder interceptorRegistryBinder;
    private final ClassLoader classLoader;
    private final CtClass ctClass;

    private static final String FIELD_PREFIX = "_$PINPOINT$_";
    private static final String SETTER_PREFIX = "_$PINPOINT$_set";
    private static final String GETTER_PREFIX = "_$PINPOINT$_get";

    public JavassistClass(Instrumentor pluginContext, InterceptorRegistryBinder interceptorRegistryBinder, ClassLoader classLoader, CtClass ctClass) {
        this.pluginContext = pluginContext;
        this.ctClass = ctClass;
        this.interceptorRegistryBinder = interceptorRegistryBinder;
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public boolean isInterceptable() {
        return !ctClass.isInterface() && !ctClass.isAnnotation() && !ctClass.isModified();
    }

    @Override
    public boolean isInterface() {
        return this.ctClass.isInterface();
    }

    @Override
    public String getName() {
        return this.ctClass.getName();
    }

    @Override
    public String getSuperClass() {
        return this.ctClass.getClassFile2().getSuperclass();
    }

    @Override
    public String[] getInterfaces() {
        return this.ctClass.getClassFile2().getInterfaces();
    }

    private static CtMethod getCtMethod0(CtClass ctClass, String methodName, String[] parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            final String descriptor = method.getMethodInfo2().getDescriptor();
            if (descriptor.startsWith(jvmSignature)) {
                return method;
            }
        }

        return null;
    }

    private CtMethod getCtMethod(String methodName, String[] parameterTypes) throws NotFoundInstrumentException {
        CtMethod method = getCtMethod0(ctClass, methodName, parameterTypes);

        if (method == null) {
            throw new NotFoundInstrumentException(methodName + Arrays.toString(parameterTypes) + " is not found in " + this.getName());
        }

        return method;
    }

    @Override
    public InstrumentMethod getDeclaredMethod(String name, String... parameterTypes) {
        CtMethod method = getCtMethod0(ctClass, name, parameterTypes);
        return method == null ? null : new JavassistMethod(pluginContext, interceptorRegistryBinder, this, method);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods() {
        return getDeclaredMethods(MethodFilters.ACCEPT_ALL);
    }

    @Override
    public List<InstrumentMethod> getDeclaredMethods(MethodFilter methodFilter) {
        if (methodFilter == null) {
            throw new NullPointerException("methodFilter must not be null");
        }
        final CtMethod[] declaredMethod = ctClass.getDeclaredMethods();
        final List<InstrumentMethod> candidateList = new ArrayList<InstrumentMethod>(declaredMethod.length);
        for (CtMethod ctMethod : declaredMethod) {
            final InstrumentMethod method = new JavassistMethod(pluginContext, interceptorRegistryBinder, this, ctMethod);
            if (methodFilter.accept(method)) {
                candidateList.add(method);
            }
        }

        return candidateList;
    }

    private CtConstructor getCtConstructor(String[] parameterTypes) throws NotFoundInstrumentException {
        CtConstructor constructor = getCtConstructor0(parameterTypes);

        if (constructor == null) {
            throw new NotFoundInstrumentException("Constructor" + Arrays.toString(parameterTypes) + " is not found in " + this.getName());
        }

        return constructor;
    }

    private CtConstructor getCtConstructor0(String[] parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        // constructor return type is void
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            final String descriptor = constructor.getMethodInfo2().getDescriptor();
            // skip return type check
            if (descriptor.startsWith(jvmSignature) && constructor.isConstructor()) {
                return constructor;
            }
        }

        return null;
    }

    @Override
    public InstrumentMethod getConstructor(String... parameterTypes) {
        CtConstructor constructor = getCtConstructor0(parameterTypes);
        return constructor == null ? null : new JavassistMethod(pluginContext, interceptorRegistryBinder, this, constructor);
    }

    @Override
    public boolean hasDeclaredMethod(String methodName, String... args) {
        return getCtMethod0(ctClass, methodName, args) != null;
    }

    @Override
    public boolean hasMethod(String methodName, String... parameterTypes) {
        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);

        for (CtMethod method : ctClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            final String descriptor = method.getMethodInfo2().getDescriptor();
            if (descriptor.startsWith(jvmSignature)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasEnclosingMethod(String methodName, String... parameterTypes) {
        CtBehavior behavior;
        try {
            behavior = ctClass.getEnclosingBehavior();
        } catch (NotFoundException ignored) {
            return false;
        }

        if(behavior == null) {
            return false;
        }

        final MethodInfo methodInfo = behavior.getMethodInfo2();
        if (!methodInfo.getName().equals(methodName)) {
            return false;
        }

        final String jvmSignature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypes);
        if (methodInfo.getDescriptor().startsWith(jvmSignature)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean hasConstructor(String... parameterTypeArray) {
        final String signature = JavaAssistUtils.javaTypeToJvmSignature(parameterTypeArray, "void");
        try {
            CtConstructor c = ctClass.getConstructor(signature);
            return c != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean hasField(String name, String type) {
        try {
            String vmType = type == null ? null : JavaAssistUtils.toJvmSignature(type);
            ctClass.getField(name, vmType);
        } catch (NotFoundException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean hasField(String name) {
        return hasField(name, null);
    }

    @Override
    public void weave(String adviceClassName) throws InstrumentException {
        pluginContext.injectClass(classLoader, adviceClassName);

        CtClass adviceClass;
        try {
            adviceClass = ctClass.getClassPool().get(adviceClassName);
        } catch (NotFoundException e) {
            throw new NotFoundInstrumentException(adviceClassName + " not found. Caused:" + e.getMessage(), e);
        }
        try {
            AspectWeaverClass weaverClass = new AspectWeaverClass();
            weaverClass.weaving(ctClass, adviceClass);
        } catch (CannotCompileException e) {
            throw new InstrumentException("weaving fail. sourceClassName:" + ctClass.getName() + " adviceClassName:" + adviceClassName + " Caused:" + e.getMessage(), e);
        } catch (NotFoundException e) {
            throw new InstrumentException("weaving fail. sourceClassName:" + ctClass.getName() + " adviceClassName:" + adviceClassName + " Caused:" + e.getMessage(), e);
        }
    }

    @Override
    public InstrumentMethod addDelegatorMethod(String methodName, String... paramTypes) throws InstrumentException {
        if (getCtMethod0(ctClass, methodName, paramTypes) != null) {
            throw new InstrumentException(getName() + "already have method(" + methodName + ").");
        }

        try {
            final CtClass superClass = ctClass.getSuperclass();
            CtMethod superMethod = getCtMethod0(superClass, methodName, paramTypes);

            if (superMethod == null) {
                throw new NotFoundInstrumentException(methodName + Arrays.toString(paramTypes) + " is not found in " + superClass.getName());
            }

            CtMethod delegatorMethod = CtNewMethod.delegator(superMethod, ctClass);
            ctClass.addMethod(delegatorMethod);

            return new JavassistMethod(pluginContext, interceptorRegistryBinder, this, delegatorMethod);
        } catch (NotFoundException ex) {
            throw new InstrumentException(getName() + "don't have super class(" + getSuperClass() + "). Cause:" + ex.getMessage(), ex);
        } catch (CannotCompileException ex) {
            throw new InstrumentException(methodName + " addDelegatorMethod fail. Cause:" + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] toBytecode() {
        try {
            byte[] bytes = ctClass.toBytecode();
            ctClass.detach();
            return bytes;
        } catch (IOException e) {
            logger.info("IoException class:{} Caused:{}", ctClass.getName(), e.getMessage(), e);
        } catch (CannotCompileException e) {
            logger.info("CannotCompileException class:{} Caused:{}", ctClass.getName(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void addField(String accessorTypeName) throws InstrumentException {
        addField0(accessorTypeName, null);
    }

    @Override
    public void addField(String accessorTypeName, String initValExp) throws InstrumentException {
        addField0(accessorTypeName, initValExp);
    }

    private void addField0(String accessorTypeName, String initValExp) throws InstrumentException {
        try {
            Class<?> accessorType = pluginContext.injectClass(classLoader, accessorTypeName);
            AccessorDetails accessorDetails = new AccessorAnalyzer().analyze(accessorType);

            CtField newField = CtField.make("private " + accessorDetails.getFieldType().getName() + " " + FIELD_PREFIX + accessorTypeName.replace('.', '_').replace('$', '_') + ";", ctClass);

            if (initValExp == null) {
                ctClass.addField(newField);
            } else {
                ctClass.addField(newField, initValExp);
            }

            final CtClass accessorInterface = ctClass.getClassPool().get(accessorTypeName);
            ctClass.addInterface(accessorInterface);

            CtMethod getterMethod = CtNewMethod.getter(accessorDetails.getGetter().getName(), newField);
            ctClass.addMethod(getterMethod);

            CtMethod setterMethod = CtNewMethod.setter(accessorDetails.getSetter().getName(), newField);
            ctClass.addMethod(setterMethod);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add field with accessor [" + accessorTypeName + "]. Cause:" + e.getMessage(), e);
        }
    }

    @Override
    public void addGetter(String getterTypeName, String fieldName) throws InstrumentException {
        try {
            Class<?> getterType = pluginContext.injectClass(classLoader, getterTypeName);

            GetterDetails getterDetails = new GetterAnalyzer().analyze(getterType);

            CtField field = ctClass.getField(fieldName);

            if (!field.getType().getName().equals(getterDetails.getFieldType().getName())) {
                throw new IllegalArgumentException("Return type of the getter is different with the field type. getterMethod: " + getterDetails.getGetter() + ", fieldType: " + field.getType().getName());
            }

            CtMethod getterMethod = CtNewMethod.getter(getterDetails.getGetter().getName(), field);

            if (getterMethod.getDeclaringClass() != ctClass) {
                getterMethod = CtNewMethod.copy(getterMethod, ctClass, null);
            }

            ctClass.addMethod(getterMethod);

            CtClass ctInterface = ctClass.getClassPool().get(getterTypeName);
            ctClass.addInterface(ctInterface);
        } catch (Exception e) {
            throw new InstrumentException("Fail to add getter: " + getterTypeName, e);
        }
    }

    @Override
    public int addInterceptor(String interceptorClassName) throws InstrumentException {
        return addGroupedInterceptor(interceptorClassName, null, null, null);
    }
    @Override
    public int addInterceptor(String interceptorClassName, Object[] constructorArgs) throws InstrumentException {
        return addGroupedInterceptor(interceptorClassName, constructorArgs, null, null);
    }

    @Override
    public int addGroupedInterceptor(String interceptorClassName, InterceptorGroup group) throws InstrumentException {
        return addGroupedInterceptor(interceptorClassName, group, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addGroupedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorGroup group) throws InstrumentException {
        return addGroupedInterceptor(interceptorClassName, constructorArgs, group, ExecutionPolicy.BOUNDARY);
    }

    @Override
    public int addGroupedInterceptor(String interceptorClassName, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException {
        return addGroupedInterceptor(interceptorClassName, null, group, executionPolicy);
    }


    @Override
    public int addGroupedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");

        int interceptorId = -1;
        Class<?> interceptorType = pluginContext.injectClass(classLoader, interceptorClassName);
        
        
        TargetMethods targetMethods = interceptorType.getAnnotation(TargetMethods.class);
        if (targetMethods != null) {
            for (TargetMethod m : targetMethods.value()) {
                interceptorId = addInterceptor0(m, interceptorClassName, constructorArgs, group, executionPolicy);
            }
        }

        TargetMethod targetMethod = interceptorType.getAnnotation(TargetMethod.class);
        if (targetMethod != null) {
            interceptorId = addInterceptor0(targetMethod, interceptorClassName, constructorArgs, group, executionPolicy);
        }

        TargetConstructors targetConstructors = interceptorType.getAnnotation(TargetConstructors.class);
        if (targetConstructors != null) {
            for (TargetConstructor c : targetConstructors.value()) {
                interceptorId = addInterceptor0(c, interceptorClassName, group, executionPolicy, constructorArgs);
            }
        }

        TargetConstructor targetConstructor = interceptorType.getAnnotation(TargetConstructor.class);
        if (targetConstructor != null) {
            interceptorId = addInterceptor0(targetConstructor, interceptorClassName, group, executionPolicy, constructorArgs);
        }

        TargetFilter targetFilter = interceptorType.getAnnotation(TargetFilter.class);
        if (targetFilter != null) {
            interceptorId = addInterceptor0(targetFilter, interceptorClassName, group, executionPolicy, constructorArgs);
        }

        if (interceptorId == -1) {
            throw new PinpointException("No target is specified. At least one of @Targets, @TargetMethod, @TargetConstructor, @TargetFilter must present. interceptor: " + interceptorClassName);
        }

        return interceptorId;
    }
    
    private int addInterceptor0(TargetConstructor c, String interceptorClassName, InterceptorGroup group, ExecutionPolicy executionPolicy, Object... constructorArgs) throws InstrumentException {
        InstrumentMethod constructor = getConstructor(c.value());
        
        if (constructor == null) {
            throw new NotFoundInstrumentException("Cannot find constructor with parameter types: " + Arrays.toString(c.value()));
        }
        
        return constructor.addGroupedInterceptor(interceptorClassName, constructorArgs, group, executionPolicy);
    }

    private int addInterceptor0(TargetMethod m, String interceptorClassName, Object[] constructorArgs, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException {
        InstrumentMethod method = getDeclaredMethod(m.name(), m.paramTypes());

        if (method == null) {
            throw new NotFoundInstrumentException("Cannot find method " + m.name() + " with parameter types: " + Arrays.toString(m.paramTypes()));
        }

        return method.addGroupedInterceptor(interceptorClassName, constructorArgs, group, executionPolicy);
    }

    private int addInterceptor0(TargetFilter annotation, String interceptorClassName, InterceptorGroup group, ExecutionPolicy executionPolicy, Object[] constructorArgs) throws InstrumentException {
        String filterTypeName = annotation.type();
        Asserts.notNull(filterTypeName, "type of @TargetFilter");

        AutoBindingObjectFactory filterFactory = new AutoBindingObjectFactory(pluginContext, classLoader, new InterceptorArgumentProvider(pluginContext.getTraceContext(), this));
        MethodFilter filter = (MethodFilter) filterFactory.createInstance(ObjectRecipe.byConstructor(filterTypeName, (Object[]) annotation.constructorArguments()));

        boolean singleton = annotation.singleton();
        int interceptorId = -1;

        for (InstrumentMethod m : getDeclaredMethods(filter)) {
            if (singleton && interceptorId != -1) {
                m.addInterceptor(interceptorId);
            } else {
                interceptorId = m.addGroupedInterceptor(interceptorClassName, constructorArgs, group, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + ctClass.getName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filterTypeName);
        }

        return interceptorId;
    }

    @Override
    public int addInterceptor(MethodFilter filter, String interceptorClassName) throws InstrumentException {
        return addGroupedInterceptor(filter, interceptorClassName, null, null, null);
    }
    
    @Override
    public int addInterceptor(MethodFilter filter, String interceptorClassName, Object[] constructorArgs) throws InstrumentException {
        return addGroupedInterceptor(filter, interceptorClassName, constructorArgs, null, null);
    }

    @Override
    public int addGroupedInterceptor(MethodFilter filter, String interceptorClassName, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException {
        return addGroupedInterceptor(filter, interceptorClassName, group, executionPolicy);
    }

    @Override
    public int addGroupedInterceptor(MethodFilter filter, String interceptorClassName, Object[] constructorArgs, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException {
        int interceptorId = -1;

        for (InstrumentMethod m : getDeclaredMethods(filter)) {
            if (interceptorId != -1) {
                m.addInterceptor(interceptorId);
            } else {
                interceptorId = m.addGroupedInterceptor(interceptorClassName, constructorArgs, group, executionPolicy);
            }
        }

        if (interceptorId == -1) {
            logger.warn("No methods are intercepted. target: " + ctClass.getName(), ", interceptor: " + interceptorClassName + ", methodFilter: " + filter.getClass().getName());
        }

        return interceptorId;
    }

    @Override
    public List<InstrumentClass> getNestedClasses(ClassFilter filter) {
        List<InstrumentClass> list = new ArrayList<InstrumentClass>();
        CtClass[] nestedClasses;
        try {
            nestedClasses = ctClass.getNestedClasses();
        } catch (NotFoundException ex) {
            return list;
        }

        if (nestedClasses == null || nestedClasses.length == 0) {
            return list;
        }

        for (CtClass nested : nestedClasses) {
            final InstrumentClass clazz = new JavassistClass(pluginContext, interceptorRegistryBinder, classLoader, nested);
            if (filter.accept(clazz)) {
                list.add(clazz);
            }
        }

        return list;
    }
    
}
