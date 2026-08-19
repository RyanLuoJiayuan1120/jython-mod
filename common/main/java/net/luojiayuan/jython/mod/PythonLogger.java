package net.luojiayuan.jython.mod;

import org.slf4j.Logger;

/**
 * Python日志包装类
 * 将Java的SLF4J Logger暴露给Python使用
 */
public class PythonLogger {

	private final Logger logger;

	public PythonLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Python可调用的info方法（支持 SLF4J {} 占位符）
	 */
	public void info(String message, Object... args) {
		if (logger.isInfoEnabled()) {
			if (args.length == 0) {
				logger.info(message);
			} else {
				logger.info(message, args);
			}
		}
	}

	/**
	 * Python可调用的warn方法（支持 SLF4J {} 占位符）
	 */
	public void warn(String message, Object... args) {
		if (logger.isWarnEnabled()) {
			if (args.length == 0) {
				logger.warn(message);
			} else {
				logger.warn(message, args);
			}
		}
	}

	/**
	 * Python可调用的warning方法（warn的别名）
	 */
	public void warning(String message, Object... args) {
		warn(message, args);
	}

	/**
	 * Python可调用的error方法（支持 SLF4J {} 占位符）
	 */
	public void error(String message, Object... args) {
		if (logger.isErrorEnabled()) {
			if (args.length == 0) {
				logger.error(message);
			} else {
				logger.error(message, args);
			}
		}
	}

	/**
	 * Python可调用的debug方法（支持 SLF4J {} 占位符）
	 */
	public void debug(String message, Object... args) {
		if (logger.isDebugEnabled()) {
			if (args.length == 0) {
				logger.debug(message);
			} else {
				logger.debug(message, args);
			}
		}
	}
}
