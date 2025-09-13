package com.example;

import com.example.application.services.StateService;
import com.example.application.usecase.UseCaseFacade;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.ToolRegistry;
import com.example.infrastructure.ServiceInitializer;

/**
 * Quick system integrity check focusing on the most critical connections.
 */
public class QuickIntegrityCheck {

    public static void main(String[] args) {
        System.out.println("=== Quick System Integrity Check ===");
        
        try {
            // Test basic initialization
            System.out.println("1. Initializing ServiceInitializer...");
            ServiceInitializer.initialize();
            System.out.println("   ✓ ServiceInitializer OK");
            
            // Test core services
            System.out.println("2. Checking core services...");
            StateService stateService = StateService.getInstance();
            if (stateService == null) throw new RuntimeException("StateService is null");
            System.out.println("   ✓ StateService OK");
            
            UseCaseFacade facade = ServiceInitializer.getUseCaseFacade();
            if (facade == null) throw new RuntimeException("UseCaseFacade is null");
            System.out.println("   ✓ UseCaseFacade OK");
            
            // Test tool registry
            System.out.println("3. Checking ToolRegistry...");
            ToolRegistry registry = ToolRegistry.getInstance();
            registry.initializeWithPort(facade);
            if (registry.getToolNames().isEmpty()) {
                throw new RuntimeException("No tools registered");
            }
            System.out.println("   ✓ ToolRegistry OK - " + registry.getToolNames().size() + " tools");
            
            // Test basic tool availability
            System.out.println("4. Checking tool availability...");
            if (!registry.hasTool("create_rectangle")) {
                throw new RuntimeException("create_rectangle tool missing");
            }
            System.out.println("   ✓ create_rectangle tool available");
            
            if (!registry.hasTool("create_circle")) {
                throw new RuntimeException("create_circle tool missing");
            }
            System.out.println("   ✓ create_circle tool available");
            
            if (!registry.hasTool("clear_canvas")) {
                throw new RuntimeException("clear_canvas tool missing");
            }
            System.out.println("   ✓ clear_canvas tool available");
            
            // Test state operations
            System.out.println("5. Testing state operations...");
            DrawingState currentState = stateService.getCurrentState();
            if (currentState == null) throw new RuntimeException("Current state is null");
            System.out.println("   ✓ State operations OK - " + currentState.getShapes().size() + " shapes");
            
            System.out.println();
            System.out.println("✅ QUICK CHECK PASSED - Core system integrity verified");
            System.out.println();
            System.out.println("Available tools: " + registry.getToolNames());
            
        } catch (Exception e) {
            System.out.println("❌ QUICK CHECK FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}