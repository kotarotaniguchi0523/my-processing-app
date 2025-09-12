package com.example.application.usecase.api;

import com.example.application.usecase.dto.GroupShapesInput;
import com.example.application.usecase.dto.UseCaseResult;
import java.util.Map;

/** Use case for grouping existing shapes by id. */
public interface IGroupShapes {
  UseCaseResult<Map<String, Object>> execute(GroupShapesInput input);
}
