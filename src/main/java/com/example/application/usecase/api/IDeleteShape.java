package com.example.application.usecase.api;

import com.example.application.usecase.dto.DeleteShapeInput;
import com.example.application.usecase.dto.UseCaseResult;

/**
 * Use case for deleting a shape.
 */
public interface IDeleteShape {
  UseCaseResult<String> execute(DeleteShapeInput input);
}
