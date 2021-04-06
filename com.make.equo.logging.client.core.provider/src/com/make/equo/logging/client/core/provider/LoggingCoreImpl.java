package com.make.equo.logging.client.core.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.LoggerFactory;

import com.make.equo.logging.client.api.AbstractLogger;
import com.make.equo.logging.client.api.Level;
import com.make.equo.logging.client.api.Logger;

import ch.qos.logback.classic.LoggerContext;

@Component(scope = ServiceScope.PROTOTYPE, service = Logger.class, property = { "service.ranking:Integer=-100" })
public class LoggingCoreImpl extends AbstractLogger {

	private ch.qos.logback.classic.Logger logger;
	private Level privateLevel = null;

	@Override
	public void debug(String message) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Object... args) {
		logger.debug(message, args);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void info(String message, Object... args) {
		logger.info(message, args);
	}

	@Override
	public void info(String message, Throwable throwable) {
		logger.info(message, throwable);
	}

	@Override
	public void warn(String message) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Object... args) {
		logger.warn(message, args);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		logger.warn(message, throwable);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void error(String message, Object... args) {
		logger.error(message, args);
	}

	@Override
	public void error(String message, Throwable throwable) {
		logger.error(message, throwable);
	}

	@Override
	public void trace(String message) {
		logger.trace(message);
	}

	@Override
	public void trace(String message, Object... args) {
		logger.trace(message, args);
	}

	@Override
	public void trace(String message, Throwable throwable) {
		logger.trace(message, throwable);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void init(Class clazz) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		this.logger = loggerContext.getLogger(clazz);
	}

	@Override
	public Level getLoggerLevel() {
		return privateLevel;
	}

	@Override
	public void setLoggerLevel(Level level) {
		this.privateLevel = level;
		if (level != null) {
			this.logger.setLevel(ch.qos.logback.classic.Level.toLevel(level.toString()));
		} else {
			this.logger.setLevel(null);
		}
	}
	
	
}
