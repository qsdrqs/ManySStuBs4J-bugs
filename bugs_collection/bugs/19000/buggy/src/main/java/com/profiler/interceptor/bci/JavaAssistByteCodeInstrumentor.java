package com.profiler.interceptor.bci;

import com.profiler.interceptor.Interceptor;
import com.profiler.interceptor.StaticBeforeInterceptor;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaAssistByteCodeInstrumentor implements ByteCodeInstrumentor {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private ClassPool classPool;

    public JavaAssistByteCodeInstrumentor() {
        this.classPool = createClassPool(null);
    }

    public JavaAssistByteCodeInstrumentor(String[] pathNames) {
        this.classPool = createClassPool(pathNames);
    }

    public ClassPool getClassPool() {
        return this.classPool;
    }

    private ClassPool createClassPool(String[] pathNames) {
        ClassPool classPool = new ClassPool(null);
        classPool.appendSystemPath();
        if (pathNames != null) {
            for(String path: pathNames) {
                appendClassPath(classPool, path);
            }
        }

        return classPool;
    }

    private void appendClassPath(ClassPool classPool, String pathName) {
        try {
            classPool.appendClassPath(pathName);
        } catch (NotFoundException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "appendClassPath fail. lib not found. " + e.getMessage(), e);
            }
        }
    }

    public void checkLibrary(ClassLoader classLoader, String javassistClassName) {
		// TODO Util로 뽑을까?
		boolean findClass = findClass(javassistClassName);
		if (findClass) {
			return;
		}
		loadClassLoaderLibraries(classLoader);
	}

    @Override
    public InstrumentClass getClass(String javassistClassName) {
        try {
            CtClass cc = classPool.get(javassistClassName);
            return new JavaAssistClass(this, cc);
        } catch (NotFoundException e) {
            // TODO 실패시 더미 객체를 반환해서 잘 에러를 숨길수 있도록 수정필요.
            return null;
        }
    }

    @Override
    public Class defineClass(ClassLoader classLoader, String defineClass, ProtectionDomain protectedDomain) {
        try {
            if(logger.isLoggable(Level.INFO)) {
                logger.info("defineClass classLoader:" + classLoader + " class:" + defineClass);
            }
            CtClass clazz = classPool.get(defineClass);
            return clazz.toClass(classLoader, protectedDomain);
        } catch (NotFoundException e) {
            if(logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "defineClass classLoader:" + classLoader + " " + e.getMessage(), e);
            }
        } catch (CannotCompileException e) {
            if(logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "defineClass classLoader:" + classLoader + " "+ e.getMessage(), e);
            }
        }
        return null;
    }

    public boolean findClass(String javassistClassName) {
		// TODO 원래는 get인데. find는 ctclas를 생성하지 않아 변경. 어차피 아래서 생성하기는 함. 유효성 여부 확인
		// 필요
		URL url = classPool.find(javassistClassName);
		if (url == null) {
			return false;
		}
		return true;
	}

	private void loadClassLoaderLibraries(ClassLoader classLoader) {
		if (classLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
			// TODO classLoader가 가지고 있는 전체 리소스를 모두 로드해야 되는것인지? 테스트 케이스 만들어서
			// 확인해봐야 할듯.
			URL[] urlList = urlClassLoader.getURLs();
			for (URL tempURL : urlList) {
				String filePath = tempURL.getFile();
				try {
					classPool.appendClassPath(filePath);
					// TODO 여기서 로그로 class로더를 찍어보면 어떤 clasdLoader에서 로딩되는지 알수 있을거
					// 것같음.
					// 만약 한개만 로딩해도 된다면. return true 할것
                    if(logger.isLoggable(Level.FINE)) {
					    logger.info("Loaded "+filePath+" library.");
                    }
				} catch (NotFoundException e) {
				}
			}
		}
	}
}
