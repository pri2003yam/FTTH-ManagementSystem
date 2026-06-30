package com.aaha.ftth.eoc.delegate;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

/**
 * Base class for all EOC delegates providing common utility methods
 * for reading process variables with null-checking and type conversion.
 */
public abstract class AbstractEocDelegate implements JavaDelegate {

    /**
     * Reads a required variable from the execution context.
     * Throws BpmnError if the variable is null or missing.
     *
     * @param execution the delegate execution context
     * @param name the variable name
     * @param type the expected type class
     * @param <T> the expected type
     * @return the variable value cast to the expected type
     * @throws BpmnError with code ERR_MISSING_VARIABLE if value is null
     */
    protected <T> T getRequiredVariable(DelegateExecution execution, String name, Class<T> type) {
        Object value = execution.getVariable(name);
        if (value == null) {
            throw new BpmnError("ERR_MISSING_VARIABLE",
                "Required variable '" + name + "' is missing or null");
        }
        return type.cast(value);
    }

    /**
     * Reads a required String variable from the execution context.
     *
     * @param execution the delegate execution context
     * @param name the variable name
     * @return the variable value as a String
     * @throws BpmnError with code ERR_MISSING_VARIABLE if value is null
     */
    protected String getRequiredString(DelegateExecution execution, String name) {
        return getRequiredVariable(execution, name, String.class);
    }

    /**
     * Reads a required Long variable from the execution context.
     * Accepts any Number type and converts to Long.
     *
     * @param execution the delegate execution context
     * @param name the variable name
     * @return the variable value as a Long
     * @throws BpmnError with code ERR_MISSING_VARIABLE if value is null
     */
    protected Long getRequiredLong(DelegateExecution execution, String name) {
        return ((Number) getRequiredVariable(execution, name, Number.class)).longValue();
    }

    /**
     * Reads an optional variable from the execution context.
     * Returns null if the variable is not present.
     *
     * @param execution the delegate execution context
     * @param name the variable name
     * @return the variable value, or null if not present
     */
    protected Object getOptionalVariable(DelegateExecution execution, String name) {
        return execution.getVariable(name);
    }
}
