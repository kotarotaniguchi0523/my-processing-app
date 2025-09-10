package com.example;

import com.example.infrastructure.ServiceFactory;
import com.example.domain.services.*;
import com.example.domain.parsers.ICommandParser;

/**
 * Demo program to verify ServiceFactory implementation works correctly
 */
public class ServiceFactoryDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ServiceFactory Demo ===");
        
        try {
            // Get ServiceFactory instance
            ServiceFactory factory = ServiceFactory.getInstance();
            System.out.println("✓ ServiceFactory instance created successfully");
            
            // Show configuration
            var config = factory.getConfiguration();
            System.out.println("✓ Configuration loaded with " + config.size() + " properties");
            System.out.println("  - AI Client Type: " + config.getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE));
            System.out.println("  - Agent Service Type: " + config.getProperty(ServiceFactory.ConfigKeys.AGENT_SERVICE_TYPE));
            
            // Create all services through factory
            System.out.println("\n=== Creating Services ===");
            
            IAgentService agentService = factory.createAgentService();
            System.out.println("✓ AgentService created: " + agentService.getClass().getSimpleName());
            
            IDiffService diffService = factory.createDiffService();
            System.out.println("✓ DiffService created: " + diffService.getClass().getSimpleName());
            
            IStateService stateService = factory.createStateService();
            System.out.println("✓ StateService created: " + stateService.getClass().getSimpleName());
            
            ICommandExecutor commandExecutor = factory.createCommandExecutor();
            System.out.println("✓ CommandExecutor created: " + commandExecutor.getClass().getSimpleName());
            
            ICommandParser commandParser = factory.createCommandParser();
            System.out.println("✓ CommandParser created: " + commandParser.getClass().getSimpleName());
            
            ISessionManager sessionManager = factory.createSessionManager();
            System.out.println("✓ SessionManager created: " + sessionManager.getClass().getSimpleName());
            
            IStateManager stateManager = factory.createStateManager();
            System.out.println("✓ StateManager created: " + stateManager.getClass().getSimpleName());
            
            IHistoryService historyService = factory.createHistoryService();
            System.out.println("✓ HistoryService created: " + historyService.getClass().getSimpleName());
            
            // Test singleton behavior
            System.out.println("\n=== Testing Singleton Behavior ===");
            
            IAgentService agentService2 = factory.createAgentService();
            boolean isSameInstance = (agentService == agentService2);
            System.out.println("✓ AgentService singleton test: " + (isSameInstance ? "PASS" : "FAIL"));
            
            // Test cache clearing
            System.out.println("\n=== Testing Cache Management ===");
            factory.clearCache();
            IAgentService agentService3 = factory.createAgentService();
            boolean isDifferentAfterClear = (agentService != agentService3);
            System.out.println("✓ Cache clear test: " + (isDifferentAfterClear ? "PASS" : "FAIL"));
            
            // Test configuration update
            System.out.println("\n=== Testing Configuration Update ===");
            String originalType = config.getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE);
            factory.updateConfiguration(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE, "openai");
            String newType = factory.getConfiguration().getProperty(ServiceFactory.ConfigKeys.AI_CLIENT_TYPE);
            System.out.println("✓ Configuration update test: " + (!originalType.equals(newType) ? "PASS" : "FAIL"));
            System.out.println("  - Original: " + originalType + ", New: " + newType);
            
            System.out.println("\n=== Demo Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed with error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}