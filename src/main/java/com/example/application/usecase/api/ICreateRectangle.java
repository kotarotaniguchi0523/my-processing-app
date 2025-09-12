package com.example.application.usecase.api;

import com.example.application.usecase.dto.CreateRectangleInput;
import com.example.application.usecase.dto.UseCaseResult;

public interface ICreateRectangle {
  UseCaseResult<String> execute(CreateRectangleInput input);
}
