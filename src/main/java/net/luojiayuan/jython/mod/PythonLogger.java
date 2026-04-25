package net.luojiayuan.jython.mod;

import org.slf4j.Logger;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * Python日志包装类
 * 将Java的SLF4J Logger暴露给Python使用
 */
public class PythonLogger extends PyObject {

	private final Logger logger;

	public PythonLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Python可调用的info方法
	 */
	public void info(PyObject message) {
		if (logger.isInfoEnabled()) {
			logger.info(message.toString());
		}
	}

	/**
	 * Python可调用的info方法（字符串版本）
	 */
	public void info(String message) {
		if (logger.isInfoEnabled()) {
			logger.info(message);
		}
	}

	/**
	 * Python可调用的warn方法
	 */
	public void warn(PyObject message) {
		if (logger.isWarnEnabled()) {
			logger.warn(message.toString());
		}
	}

	/**
	 * Python可调用的warn方法（字符串版本）
	 */
	public void warn(String message) {
		if (logger.isWarnEnabled()) {
			logger.warn(message);
		}
	}

	/**
	 * Python可调用的warning方法（warn的别名）
	 */
	public void warning(PyObject message) {
		warn(message);
	}

	/**
	 * Python可调用的warning方法（字符串版本）
	 */
	public void warning(String message) {
		warn(message);
	}

	/**
	 * Python可调用的error方法
	 */
	public void error(PyObject message) {
		if (logger.isErrorEnabled()) {
			logger.error(message.toString());
		}
	}

	/**
	 * Python可调用的error方法（字符串版本）
	 */
	public void error(String message) {
		if (logger.isErrorEnabled()) {
			logger.error(message);
		}
	}

	/**
	 * Python可调用的debug方法
	 */
	public void debug(PyObject message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message.toString());
		}
	}

	/**
	 * Python可调用的debug方法（字符串版本）
	 */
	public void debug(String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}
	}
}
