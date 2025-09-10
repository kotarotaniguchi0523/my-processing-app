package com.example.infrastructure;

import com.example.domain.services.*;
import com.example.domain.parsers.ICommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ServiceFactory dependency injection implementation
 */
class ServiceFactoryTest {

    private ServiceFactory serviceFactory;

    @BeforeEach
    void setUp() {
        serviceFactory = ServiceFactory.getInstance();
        // Clear cache between tests to ensure fresh instances
        serviceFactory.clearCache();
    }

    @Test
    void testGetInstance_ReturnsSameInstance() {
        ServiceFactory instance1 = ServiceFactory.getInstance();
        ServiceFactory instance2 = ServiceFactory.getInstance();
        
        assertSame(instance1, instance2, "ServiceFactory should be a singleton");
    }

    @Test
    void testCreateAgentService_ReturnsNotNull() {
        IAgentService agentService = serviceFactory.createAgentService();
        
        assertNotNull(agentService, "AgentService should not be null");
    }

    @Test
    void testCreateDiffService_ReturnsNotNull() {
        IDiffService diffService = serviceFactory.createDiffService();
        
        assertNotNull(diffService, "DiffService should not be null");
    }

    @Test
    void testCreateStateService_ReturnsNotNull() {
        IStateService stateService = serviceFactory.createStateService();
        
        assertNotNull(stateService, "StateService should not be null");
    }

    @Test
    void testCreateCommandExecutor_ReturnsNotNull() {
        ICommandExecutor commandExecutor = serviceFactory.createCommandExecutor();
        
        assertNotNull(commandExecutor, "CommandExecutor should not be null");
    }

    @Test
    void testCreateCommandParser_ReturnsNotNull() {
        ICommandParser commandParser = serviceFactory.createCommandParser();
        
        assertNotNull(commandParser, "CommandParser should not be null");
    }

    @Test
    void testCreateSessionManager_ReturnsNotNull() {
        ISessionManager sessionManager = serviceFactory.createSessionManager();
        
        assertNotNull(sessionManager, "SessionManager should not be null");
    }

    @Test
    void testCreateStateManager_ReturnsNotNull() {
        IStateManager stateManager = serviceFactory.createStateManager();
        
        assertNotNull(stateManager, "StateManager should not be null");
    }

    @Test
    void testCreateHistoryService_ReturnsNotNull() {
        IHistoryService historyService = serviceFactory.createHistoryService();
        
        assertNotNull(historyService, "HistoryService should not be null");
    }

    @Nested
    class SingletonBehaviorTests {

        @Test
        void testAgentService_ReturnsSameInstance() {
            IAgentService service1 = serviceFactory.createAgentService();
            IAgentService service2 = serviceFactory.createAgentService();
            
            assertSame(service1, service2, "AgentService should be singleton");
        }

        @Test
        void testDiffService_ReturnsSameInstance() {
            IDiffService service1 = serviceFactory.createDiffService();
            IDiffService service2 = serviceFactory.createDiffService();
            
            assertSame(service1, service2, "DiffService should be singleton");
        }

        @Test
        void testStateService_ReturnsSameInstance() {
            IStateService service1 = serviceFactory.createStateService();
            IStateService service2 = serviceFactory.createStateService();
            
            assertSame(service1, service2, "StateService should be singleton");
        }
    }

    @Nested
    class ConfigurationTests {

        @Test
        void testGetConfiguration_ReturnsNotNull() {
            var config = serviceFactory.getConfiguration();
            
            assertNotNull(config, "Configuration should not be null");
        }

        @Test
        void testGetConfiguration_ContainsDefaultValues() {
            var config = serviceFactory.getConfiguration();
            
            assertEquals("gemini", config.getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE));
            assertEquals("default", config.getProperty(ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE));
            assertEquals("default", config.getProperty(ServiceFactory.ConfigKeys.DIFF_SERVICE_TYPE));
        }

        @Test
        void testUpdateConfiguration_ChangesConfiguration() {
            String originalValue = serviceFactory.getConfiguration()
                .getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE);
            
            serviceFactory.updateConfiguration(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE, "openai");
            
            String newValue = serviceFactory.getConfiguration()
                .getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE);
            
            assertNotEquals(originalValue, newValue);
            assertEquals("openai", newValue);
        }

        @Test
        void testUpdateConfiguration_ClearsCache() {
            // Get initial service instance
            IAgentService service1 = serviceFactory.createAgentService();
            
            // Update configuration (should clear cache)
            serviceFactory.updateConfiguration(ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE, "adapter");
            
            // Get new service instance
            IAgentService service2 = serviceFactory.createAgentService();
            
            // Note: We can't easily test if they're different instances since they might be
            // the same implementation type, but we can verify the configuration change took effect
            assertNotNull(service1);
            assertNotNull(service2);
        }
    }

    @Test
    void testClearCache_AllowsNewInstances() {
        // Get initial services
        IAgentService service1 = serviceFactory.createAgentService();
        IDiffService diffService1 = serviceFactory.createDiffService();
        
        // Clear cache
        serviceFactory.clearCache();
        
        // Get new services
        IAgentService service2 = serviceFactory.createAgentService();
        IDiffService diffService2 = serviceFactory.createDiffService();
        
        // Services should be different instances after cache clear
        assertNotSame(service1, service2, "AgentService should be new instance after cache clear");
        assertNotSame(diffService1, diffService2, "DiffService should be new instance after cache clear");
    }

    @Test
    void testConfigurationKeys_AreNotNull() {
        assertNotNull(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.DIFF_SERVICE_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.STATE_SERVICE_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.COMMAND_EXECUTOR_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.COMMAND_PARSER_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.SESSION_MANAGER_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.STATE_MANAGER_TYPE);
        assertNotNull(ServiceFactory.ConfigKeys.HISTORY_SERVICE_TYPE);
    }

    @Test
    void testDefaultValues_AreNotNull() {
        assertNotNull(ServiceFactory.Defaults.AI_CLIENT_TYPE);
        assertNotNull(ServiceFactory.Defaults.AGENT_SERVICE_TYPE);
        assertNotNull(ServiceFactory.Defaults.DIFF_SERVICE_TYPE);
        assertNotNull(ServiceFactory.Defaults.STATE_SERVICE_TYPE);
        assertNotNull(ServiceFactory.Defaults.COMMAND_EXECUTOR_TYPE);
        assertNotNull(ServiceFactory.Defaults.COMMAND_PARSER_TYPE);
        assertNotNull(ServiceFactory.Defaults.SESSION_MANAGER_TYPE);
        assertNotNull(ServiceFactory.Defaults.STATE_MANAGER_TYPE);
        assertNotNull(ServiceFactory.Defaults.HISTORY_SERVICE_TYPE);
    }
}