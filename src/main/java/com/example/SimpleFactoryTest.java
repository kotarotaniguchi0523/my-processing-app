package com.example;

import com.example.infrastructure.ServiceFactory;

/**
 * Minimal test to check ServiceFactory basics
 */
public class SimpleFactoryTest {
    
    public static void main(String[] args) {
        System.out.println("Testing ServiceFactory...");
        
        try {
            System.out.println("1. Getting ServiceFactory instance...");
            ServiceFactory factory = ServiceFactory.getInstance();
            System.out.println("   ✓ Success");
            
            System.out.println("2. Getting configuration...");
            var config = factory.getConfiguration();
            System.out.println("   ✓ Config loaded with " + config.size() + " properties");
            
            System.out.println("3. Creating basic services...");
            
            System.out.println("   Creating DiffService...");
            var diffService = factory.createDiffService();
            System.out.println("   ✓ DiffService: " + diffService.getClass().getSimpleName());
            
            System.out.println("   Creating StateService...");
            var stateService = factory.createStateService();
            System.out.println("   ✓ StateService: " + stateService.getClass().getSimpleName());
            
            System.out.println("   Creating CommandExecutor...");
            var commandExecutor = factory.createCommandExecutor();
            System.out.println("   ✓ CommandExecutor: " + commandExecutor.getClass().getSimpleName());
            
            System.out.println("4. Testing AgentService creation...");
            try {
                var agentService = factory.createAgentService();
                System.out.println("   ✓ AgentService: " + agentService.getClass().getSimpleName());
            } catch (Exception e) {
                System.out.println("   ❌ AgentService failed: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n✅ Basic ServiceFactory test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}