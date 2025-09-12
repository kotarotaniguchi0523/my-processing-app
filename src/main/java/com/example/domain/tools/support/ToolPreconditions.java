package com.example.domain.tools.support;

import com.example.application.usecase.UseCaseFacade;

/** Lightweight precondition helpers for tools (avoid over-abstracting base classes). */
public final class ToolPreconditions {
  private ToolPreconditions() {}

  public static UseCaseFacade requireFacade(UseCaseFacade facade) {
    if (facade == null) throw new IllegalStateException("UseCaseFacade not injected");
    return facade;
  }
}
