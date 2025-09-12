package com.example.application.usecase.api;

import com.example.application.usecase.dto.UseCaseResult;

/**
 * Use case for clearing all shapes from the canvas.
 */
public interface IClearCanvas {
  UseCaseResult<Integer> execute();
}
