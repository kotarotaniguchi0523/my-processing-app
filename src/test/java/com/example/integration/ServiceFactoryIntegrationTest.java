package com.example.integration;

import com.example.infrastructure.ServiceFactory;
import com.example.domain.services.*;
import com.example.domain.parsers.ICommandParser;
import com.example.usecase.UseCaseFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify ServiceFactory works correctly with the application components
 */
class ServiceFactoryIntegrationTest {

    private ServiceFactory serviceFactory;

    @BeforeEach
    void setUp() {
        serviceFactory = ServiceFactory.getInstance();
        serviceFactory.clearCache();
    }

    @Test
    void testServiceFactory_CreatesAllRequiredServices() {
        // Test that all required services can be created without errors
        assertDoesNotThrow(() -> {
            IAgentService agentService = serviceFactory.createAgentService();
            IDiffService diffService = serviceFactory.createDiffService();
            IStateService stateService = serviceFactory.createStateService();
            ICommandExecutor commandExecutor = serviceFactory.createCommandExecutor();
            ICommandParser commandParser = serviceFactory.createCommandParser();
            ISessionManager sessionManager = serviceFactory.createSessionManager();
            IStateManager stateManager = serviceFactory.createStateManager();
            IHistoryService historyService = serviceFactory.createHistoryService();
            
            // Verify all services are not null
            assertNotNull(agentService);
            assertNotNull(diffService);
            assertNotNull(stateService);
            assertNotNull(commandExecutor);
            assertNotNull(commandParser);
            assertNotNull(sessionManager);
            assertNotNull(stateManager);
            assertNotNull(historyService);
        }, "ServiceFactory should create all required services without throwing exceptions");
    }

    @Test
    void testServiceFactory_SupportsConfigurationSwitching() {
        // Test switching to adapter implementations
        serviceFactory.updateConfiguration(
            ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE, "adapter"
        );
        
        assertDoesNotThrow(() -> {
            IAgentService agentService = serviceFactory.createAgentService();
            assertNotNull(agentService);
        }, "ServiceFactory should handle adapter configuration");
    }

    @Test
    void testServiceFactory_HandlesMultipleClients() {
        // Test different AI client configurations
        String[] clientTypes = {"gemini", "openai", "claude"};
        
        for (String clientType : clientTypes) {
            serviceFactory.updateConfiguration(
                ServiceFactory.ConfigKeys.AI_CLIENT_TYPE, clientType
            );
            
            assertDoesNotThrow(() -> {
                IAgentService agentService = serviceFactory.createAgentService();
                assertNotNull(agentService);
            }, "ServiceFactory should handle " + clientType + " client configuration");
        }
    }

    @Test
    void testServiceFactory_WithUseCaseFacade() {
        // This test verifies that the ServiceFactory integration works with UseCaseFacade
        // Note: This is a basic test since UseCaseFacade has complex initialization
        assertDoesNotThrow(() -> {
            // Simulate what UseCaseFacade does
            IAgentService agentService = serviceFactory.createAgentService();
            IDiffService diffService = serviceFactory.createDiffService();
            IStateService stateService = serviceFactory.createStateService();
            ICommandExecutor commandExecutor = serviceFactory.createCommandExecutor();
            ICommandParser commandParser = serviceFactory.createCommandParser();
            ISessionManager sessionManager = serviceFactory.createSessionManager();
            
            // All services should be successfully created and ready for use
            assertNotNull(agentService);
            assertNotNull(diffService);
            assertNotNull(stateService);
            assertNotNull(commandExecutor);
            assertNotNull(commandParser);
            assertNotNull(sessionManager);
            
        }, "ServiceFactory should integrate correctly with UseCaseFacade pattern");
    }

    @Test
    void testServiceFactory_ConsistentInstances() {
        // Verify that the same service instances are returned within the same lifecycle
        IAgentService agentService1 = serviceFactory.createAgentService();
        IAgentService agentService2 = serviceFactory.createAgentService();
        
        assertSame(agentService1, agentService2, 
            "ServiceFactory should return the same singleton instance");
        
        // After clearing cache, new instances should be created
        serviceFactory.clearCache();
        IAgentService agentService3 = serviceFactory.createAgentService();
        
        assertNotSame(agentService1, agentService3, 
            "ServiceFactory should create new instances after cache clear");
    }
    
    @Test
    void testServiceFactory_HandlesMissingConfiguration() {
        // Test that factory works even with missing configuration file
        // The factory should fall back to defaults
        
        assertDoesNotThrow(() -> {
            var config = serviceFactory.getConfiguration();
            
            // Should have default values
            assertNotNull(config.getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE));
            assertNotNull(config.getProperty(ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE));
            
            // Should be able to create services with defaults
            IAgentService service = serviceFactory.createAgentService();
            assertNotNull(service);
            
        }, "ServiceFactory should handle missing configuration gracefully");
    }
}