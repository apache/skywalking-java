package org.apache.skywalking.apm.plugin.pulsar.define;

import org.apache.skywalking.apm.plugin.pulsar.common.define.BaseFunctionInstrumentation;

public class FunctionInstrumentation extends BaseFunctionInstrumentation {

    @Override
    protected String[] witnessClasses() {
        return Constants.WITNESS_PULSAR_27X_CLASSES;
    }
}
