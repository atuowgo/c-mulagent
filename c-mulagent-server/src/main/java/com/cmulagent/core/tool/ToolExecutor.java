package com.cmulagent.core.tool;

import java.util.Map;

@FunctionalInterface
public interface ToolExecutor {

    String execute(Map<String, Object> params);
}