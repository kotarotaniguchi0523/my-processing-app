package com.example.application.usecase.api;

import com.example.application.usecase.dto.ChangeColorInput;
import com.example.application.usecase.dto.UseCaseResult;

public interface IChangeColor {
  UseCaseResult<String> execute(ChangeColorInput input);
}
