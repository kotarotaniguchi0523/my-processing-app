package com.example.infrastructure;

import com.example.domain.services.*;
import com.example.infrastructure.adapters.*;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for creating and managing service instances with dependency injection.
 * Provides centralized configuration-based implementation selection and singleton management.
 */
public class ServiceFactory {
    
    private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class.getName());
    
    // Singleton instance
    private static volatile ServiceFactory instance;
    
    // Configuration properties
    private final Properties config;
    
    // Service instances cache for singletons
    private final ConcurrentHashMap<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();
    
    /**
     * Configuration keys for service implementations
     */
    public static class ConfigKeys {
        public static final String AI_CLIENT_TYPE = "ai.client.type";
        public static final String DIFF_SERVICE_TYPE = "diff.service.type";
        public static final String STATE_SERVICE_TYPE = "state.service.type";
        public static final String COMMAND_EXECUTOR_TYPE = "command.executor.type";
        public static final String COMMAND_PARSER_TYPE = "command.parser.type";
        public static final String SESSION_MANAGER_TYPE = "session.manager.type";
        public static final String STATE_MANAGER_TYPE = "state.manager.type";
        public static final String HISTORY_SERVICE_TYPE = "history.service.type";
    }
    
    /**
     * Default implementation types
     */
    public static class Defaults {
        public static final String AI_CLIENT_TYPE = "gemini";
        public static final String DIFF_SERVICE_TYPE = "default";
        public static final String STATE_SERVICE_TYPE = "default";
        public static final String COMMAND_EXECUTOR_TYPE = "default";
        public static final String COMMAND_PARSER_TYPE = "default";
        public static final String SESSION_MANAGER_TYPE = "default";
        public static final String STATE_MANAGER_TYPE = "default";
        public static final String HISTORY_SERVICE_TYPE = "default";
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private ServiceFactory() {
        this.config = loadConfiguration();
        LOGGER.info("ServiceFactory initialized with configuration");
    }
    
    /**
     * Gets the singleton instance of ServiceFactory
     */
    public static ServiceFactory getInstance() {
        if (instance == null) {
            synchronized (ServiceFactory.class) {
                if (instance == null) {
                    instance = new ServiceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Loads configuration from properties file or uses defaults
     */
    private Properties loadConfiguration() {
        Properties props = new Properties();
        
        // Set default values
        props.setProperty(ConfigKeys.AI_CLIENT_TYPE, Defaults.AI_CLIENT_TYPE);
        props.setProperty(ConfigKeys.DIFF_SERVICE_TYPE, Defaults.DIFF_SERVICE_TYPE);
        props.setProperty(ConfigKeys.STATE_SERVICE_TYPE, Defaults.STATE_SERVICE_TYPE);
        props.setProperty(ConfigKeys.COMMAND_EXECUTOR_TYPE, Defaults.COMMAND_EXECUTOR_TYPE);
        props.setProperty(ConfigKeys.COMMAND_PARSER_TYPE, Defaults.COMMAND_PARSER_TYPE);
        props.setProperty(ConfigKeys.SESSION_MANAGER_TYPE, Defaults.SESSION_MANAGER_TYPE);
        props.setProperty(ConfigKeys.STATE_MANAGER_TYPE, Defaults.STATE_MANAGER_TYPE);
        props.setProperty(ConfigKeys.HISTORY_SERVICE_TYPE, Defaults.HISTORY_SERVICE_TYPE);
        
        // Try to load from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("service-config.properties")) {
            if (is != null) {
                props.load(is);
                LOGGER.info("Loaded configuration from service-config.properties");
            } else {
                LOGGER.info("No service-config.properties found, using defaults");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load service configuration, using defaults", e);
        }
        
        return props;
    }
    
    /**
     * Creates or returns cached IStateService implementation
     */
    public IStateService createStateService() {
        return (IStateService) singletonCache.computeIfAbsent(IStateService.class, k -> {
            String type = config.getProperty(ConfigKeys.STATE_SERVICE_TYPE, Defaults.STATE_SERVICE_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    throw new UnsupportedOperationException("StateServiceAdapter was deleted as unused");
            }
        });
    }
    
    /**
     * Creates or returns cached ICommandExecutor implementation
     */
    public ICommandExecutor createCommandExecutor() {
        return (ICommandExecutor) singletonCache.computeIfAbsent(ICommandExecutor.class, k -> {
            String type = config.getProperty(ConfigKeys.COMMAND_EXECUTOR_TYPE, Defaults.COMMAND_EXECUTOR_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new CommandExecutorAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached ISessionManager implementation
     */
    public ISessionManager createSessionManager() {
        return (ISessionManager) singletonCache.computeIfAbsent(ISessionManager.class, k -> {
            String type = config.getProperty(ConfigKeys.SESSION_MANAGER_TYPE, Defaults.SESSION_MANAGER_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    throw new UnsupportedOperationException("SessionManagerAdapter was deleted as unused");
            }
        });
    }
}