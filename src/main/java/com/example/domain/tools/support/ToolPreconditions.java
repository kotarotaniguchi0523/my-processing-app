package com.example.domain.tools.support;

import com.example.application.usecase.api.ToolUseCasePort;

/** Lightweight precondition helpers for tools (avoid over-abstracting base classes). */
public final class ToolPreconditions {
  private ToolPreconditions() {}

  public static ToolUseCasePort requirePort(ToolUseCasePort port) {
    if (port == null)
      throw new IllegalStateException("ToolUseCasePort not injected");
    return port;
  }
}
