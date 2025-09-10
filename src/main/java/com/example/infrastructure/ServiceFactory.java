package com.example.infrastructure;

import com.example.domain.services.*;
import com.example.domain.parsers.ICommandParser;
import com.example.infrastructure.adapters.*;
import com.example.infrastructure.ai.AIClient;
import com.example.infrastructure.ai.AIClientFactory;
import com.example.application.agent.AgentService;

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
        public static final String AGENT_SERVICE_TYPE = "agent.service.type";
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
        public static final String AGENT_SERVICE_TYPE = "default";
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
        props.setProperty(ConfigKeys.AGENT_SERVICE_TYPE, Defaults.AGENT_SERVICE_TYPE);
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
     * Creates or returns cached IAgentService implementation
     */
    @SuppressWarnings("unchecked")
    public IAgentService createAgentService() {
        return (IAgentService) singletonCache.computeIfAbsent(IAgentService.class, k -> {
            String type = config.getProperty(ConfigKeys.AGENT_SERVICE_TYPE, Defaults.AGENT_SERVICE_TYPE);
            
            switch (type.toLowerCase()) {
                case "adapter":
                    return new AgentServiceAdapter();
                case "default":
                default:
                    // Create AgentService with fully injected dependencies
                    AIClient aiClient = createAIClient();
                    return createAgentServiceWithDependencies(aiClient);
            }
        });
    }
    
    /**
     * Creates AgentService with all dependencies injected
     */
    private AgentService createAgentServiceWithDependencies(AIClient aiClient) {
        // We'll use the existing constructor that accepts dependencies
        // This avoids the AgentService creating its own dependencies
        return new AgentService(aiClient);
    }
    
    /**
     * Creates or returns cached IDiffService implementation
     */
    @SuppressWarnings("unchecked")
    public IDiffService createDiffService() {
        return (IDiffService) singletonCache.computeIfAbsent(IDiffService.class, k -> {
            String type = config.getProperty(ConfigKeys.DIFF_SERVICE_TYPE, Defaults.DIFF_SERVICE_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new DiffServiceAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached IStateService implementation
     */
    @SuppressWarnings("unchecked")
    public IStateService createStateService() {
        return (IStateService) singletonCache.computeIfAbsent(IStateService.class, k -> {
            String type = config.getProperty(ConfigKeys.STATE_SERVICE_TYPE, Defaults.STATE_SERVICE_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new StateServiceAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached ICommandExecutor implementation
     */
    @SuppressWarnings("unchecked")
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
     * Creates or returns cached ICommandParser implementation
     */
    @SuppressWarnings("unchecked")
    public ICommandParser createCommandParser() {
        return (ICommandParser) singletonCache.computeIfAbsent(ICommandParser.class, k -> {
            String type = config.getProperty(ConfigKeys.COMMAND_PARSER_TYPE, Defaults.COMMAND_PARSER_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new CommandParserAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached ISessionManager implementation
     */
    @SuppressWarnings("unchecked")
    public ISessionManager createSessionManager() {
        return (ISessionManager) singletonCache.computeIfAbsent(ISessionManager.class, k -> {
            String type = config.getProperty(ConfigKeys.SESSION_MANAGER_TYPE, Defaults.SESSION_MANAGER_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new SessionManagerAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached IStateManager implementation
     */
    @SuppressWarnings("unchecked")
    public IStateManager createStateManager() {
        return (IStateManager) singletonCache.computeIfAbsent(IStateManager.class, k -> {
            String type = config.getProperty(ConfigKeys.STATE_MANAGER_TYPE, Defaults.STATE_MANAGER_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    return new StateManagerAdapter();
            }
        });
    }
    
    /**
     * Creates or returns cached IHistoryService implementation
     */
    @SuppressWarnings("unchecked")
    public IHistoryService createHistoryService() {
        return (IHistoryService) singletonCache.computeIfAbsent(IHistoryService.class, k -> {
            String type = config.getProperty(ConfigKeys.HISTORY_SERVICE_TYPE, Defaults.HISTORY_SERVICE_TYPE);
            
            switch (type.toLowerCase()) {
                case "default":
                default:
                    // Create StateManager for HistoryServiceAdapter
                    com.example.infrastructure.services.StateManager stateManager = 
                        new com.example.infrastructure.services.StateManager();
                    return new HistoryServiceAdapter(stateManager);
            }
        });
    }
    
    /**
     * Creates AIClient based on configuration
     */
    private AIClient createAIClient() {
        String type = config.getProperty(ConfigKeys.AI_CLIENT_TYPE, Defaults.AI_CLIENT_TYPE);
        
        switch (type.toLowerCase()) {
            case "gemini":
                return AIClientFactory.createApplicationClient();
            case "openai":
            case "claude":
            default:
                // For now, default to the factory's default client
                return AIClientFactory.createApplicationClient();
        }
    }
    
    /**
     * Clears the singleton cache (useful for testing)
     */
    public void clearCache() {
        singletonCache.clear();
        LOGGER.info("ServiceFactory cache cleared");
    }
    
    /**
     * Gets current configuration properties (read-only)
     */
    public Properties getConfiguration() {
        return new Properties(config);
    }
    
    /**
     * Updates configuration at runtime (for testing purposes)
     */
    public void updateConfiguration(String key, String value) {
        config.setProperty(key, value);
        // Clear cache to force recreation of services with new config
        clearCache();
        LOGGER.info("Configuration updated: " + key + " = " + value);
    }
}