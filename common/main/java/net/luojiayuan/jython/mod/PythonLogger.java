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
	 * Python可调用的info方法
	 */
	public void info(String message) {
		if (logger.isInfoEnabled()) {
			logger.info(message);
		}
	}

	/**
	 * Python可调用的warn方法
	 */
	public void warn(String message) {
		if (logger.isWarnEnabled()) {
			logger.warn(message);
		}
	}

	/**
	 * Python可调用的warning方法（warn的别名）
	 */
	public void warning(String message) {
		warn(message);
	}

	/**
	 * Python可调用的error方法
	 */
	public void error(String message) {
		if (logger.isErrorEnabled()) {
			logger.error(message);
		}
	}

	/**
	 * Python可调用的debug方法
	 */
	public void debug(String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}
	}
}
