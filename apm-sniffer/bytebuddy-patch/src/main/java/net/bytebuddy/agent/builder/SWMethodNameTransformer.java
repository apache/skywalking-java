package net.bytebuddy.agent.builder;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.utility.RandomString;

/**
 * Generate fixed origin method name
 */
public class SWMethodNameTransformer implements MethodNameTransformer {

    private static final String DEFAULT_PREFIX = "original$";

    private String prefix;

    public SWMethodNameTransformer() {
        this(DEFAULT_PREFIX);
    }

    public SWMethodNameTransformer(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String transform(MethodDescription methodDescription) {
        return prefix + methodDescription.getInternalName() + "$" + RandomString.hashOf(methodDescription.toString().hashCode());
    }

}
