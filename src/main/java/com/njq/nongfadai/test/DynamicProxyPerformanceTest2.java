package com.njq.nongfadai.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;
import java.util.UUID;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * output:
Create JDK Proxy: 14913 ms

======================================

Create CGLIB Proxy: 3713 ms

======================================

Create JAVAASSIST Proxy: 98837 ms

======================================
 */
public class DynamicProxyPerformanceTest2 {

	static int counter = 10000000;

	public static void main(String[] args) throws Exception {
		CountService delegate = new CountServiceImpl();

		long time = System.currentTimeMillis();
/*		for (int i = 0; i < counter; i++) {
			createJdkDynamicProxy(delegate);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Create JDK Proxy: " + time + " ms");

		printSeparateCharacter();

		time = System.currentTimeMillis();
		for (int i = 0; i < counter; i++) {
			createCglibDynamicProxy(delegate);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Create CGLIB Proxy: " + time + " ms");

		printSeparateCharacter();

		time = System.currentTimeMillis();
		for (int i = 0; i < counter; i++) {
			createJavassistDynamicProxy(delegate);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Create JAVAASSIST Proxy: " + time + " ms");

		printSeparateCharacter();*/

		time = System.currentTimeMillis();
		for (int i = 0; i < counter; i++) {
			createJavassistBytecodeDynamicProxy(delegate);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Create JAVAASSIST Bytecode Proxy: " + time + " ms");

		printSeparateCharacter();

		time = System.currentTimeMillis();
		for (int i = 0; i < counter; i++) {
			createAsmBytecodeDynamicProxy(delegate);
		}
		time = System.currentTimeMillis() - time;
		System.out.println("Create ASM Proxy: " + time + " ms");
	}

	private static void printSeparateCharacter() {
		System.out.println();
		System.out.println("======================================");
		System.out.println();
	}

	private static CountService createJdkDynamicProxy(final CountService delegate) {
		CountService jdkProxy = (CountService) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
				new Class[] { CountService.class }, new JdkHandler(delegate));
		return jdkProxy;
	}

	private static class JdkHandler implements InvocationHandler {

		final Object delegate;

		JdkHandler(Object delegate) {
			this.delegate = delegate;
		}

		public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
			return method.invoke(delegate, objects);
		}
	}

	private static CountService createCglibDynamicProxy(final CountService delegate) throws Exception {
		Enhancer enhancer = new Enhancer();
		enhancer.setCallback(new CglibCallback(delegate));
		enhancer.setInterfaces(new Class[] { CountService.class });
		CountService cglibProxy = (CountService) enhancer.create();
		return cglibProxy;
	}

	private static class CglibCallback implements MethodInterceptor {

		final Object delegate;

		CglibCallback(Object delegate) {
			this.delegate = delegate;
		}

		public Object intercept(Object object, Method method, Object[] objects, MethodProxy methodProxy)
				throws Throwable {
			return methodProxy.invoke(delegate, objects);
		}
	}

	private static CountService createJavassistDynamicProxy(final CountService delegate) throws Exception {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setInterfaces(new Class[] { CountService.class });
		Class<?> proxyClass = proxyFactory.createClass();
		CountService javassistProxy = (CountService) proxyClass.newInstance();
		((ProxyObject) javassistProxy).setHandler(new JavaAssistHandler(delegate));
		return javassistProxy;
	}

	private static class JavaAssistHandler implements MethodHandler {

		final Object delegate;

		JavaAssistHandler(Object delegate) {
			this.delegate = delegate;
		}

		public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {
			return m.invoke(delegate, args);
		}
	}

	private static CountService createJavassistBytecodeDynamicProxy(CountService delegate) throws Exception {
		ClassPool mPool = new ClassPool(true);
		CtClass mCtc = mPool.makeClass(CountService.class.getName() + UUID.randomUUID().toString() + "JavaassistProxy");
		mCtc.addInterface(mPool.get(CountService.class.getName()));
		mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
		mCtc.addField(CtField.make("public " + CountService.class.getName() + " delegate;", mCtc));
		mCtc.addMethod(CtNewMethod.make("public int count() { return delegate.count(); }", mCtc));
		Class<?> pc = mCtc.toClass();
		CountService bytecodeProxy = (CountService) pc.newInstance();
		Field filed = bytecodeProxy.getClass().getField("delegate");
		filed.set(bytecodeProxy, delegate);
		return bytecodeProxy;
	}

	private static CountService createAsmBytecodeDynamicProxy(CountService delegate) throws Exception {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		String className = CountService.class.getName() + "AsmProxy";
		String classPath = className.replace('.', '/');
		String interfacePath = CountService.class.getName().replace('.', '/');
		classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, classPath, null, "java/lang/Object",
				new String[] { interfacePath });

		MethodVisitor initVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		initVisitor.visitCode();
		initVisitor.visitVarInsn(Opcodes.ALOAD, 0);
		initVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		initVisitor.visitInsn(Opcodes.RETURN);
		initVisitor.visitMaxs(0, 0);
		initVisitor.visitEnd();

		FieldVisitor fieldVisitor = classWriter.visitField(Opcodes.ACC_PUBLIC, "delegate", "L" + interfacePath + ";",
				null, null);
		fieldVisitor.visitEnd();

		MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "count", "()I", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
		methodVisitor.visitFieldInsn(Opcodes.GETFIELD, classPath, "delegate", "L" + interfacePath + ";");
		methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, interfacePath, "count", "()I");
		methodVisitor.visitInsn(Opcodes.IRETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();

		classWriter.visitEnd();
		byte[] code = classWriter.toByteArray();
		CountService bytecodeProxy = (CountService) new ByteArrayClassLoader().getClass(className, code).newInstance();
		Field filed = bytecodeProxy.getClass().getField("delegate");
		filed.set(bytecodeProxy, delegate);
		return bytecodeProxy;
	}

	private static class ByteArrayClassLoader extends ClassLoader {

		public ByteArrayClassLoader() {
			super(ByteArrayClassLoader.class.getClassLoader());
		}

		public synchronized Class<?> getClass(String name, byte[] code) {
			if (name == null) {
				throw new IllegalArgumentException("");
			}
			return defineClass(name, code, 0, code.length);
		}

	}
}