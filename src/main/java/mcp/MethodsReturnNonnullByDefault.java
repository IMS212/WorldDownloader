package mcp;
import javax.annotation.Nonnull;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierDefault;

/**
 *
 * From MCPConfig, copied for use here
 *
 */
@Documented
@Nonnull
@TypeQualifierDefault(ElementType.METHOD) // Note: This is a copy of javax.annotation.ParametersAreNonnullByDefault with target changed to METHOD
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodsReturnNonnullByDefault {}