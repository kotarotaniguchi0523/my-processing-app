package com.example.application.usecase.api;

import com.example.application.usecase.dto.MoveShapeInput;
import com.example.application.usecase.dto.UseCaseResult;

public interface IMoveShape {
  UseCaseResult<String> execute(MoveShapeInput input);
}
