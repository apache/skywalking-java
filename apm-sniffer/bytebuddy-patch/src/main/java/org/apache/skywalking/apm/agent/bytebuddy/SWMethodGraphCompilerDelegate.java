package org.apache.skywalking.apm.agent.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SWMethodGraphCompilerDelegate implements MethodGraph.Compiler {
    private MethodGraph.Compiler originCompiler;

    public SWMethodGraphCompilerDelegate(MethodGraph.Compiler originCompiler) {
        this.originCompiler = originCompiler;
    }

    @Override
    public MethodGraph.Linked compile(TypeDefinition typeDefinition) {
        MethodGraph.Linked methodGraph = originCompiler.compile(typeDefinition);
        return new SWMethodGraph(methodGraph);
    }

    @Override
    @Deprecated
    public MethodGraph.Linked compile(TypeDescription typeDescription) {
        return originCompiler.compile(typeDescription);
    }

    @Override
    public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
        return originCompiler.compile(typeDefinition, viewPoint);
    }

    @Override
    @Deprecated
    public MethodGraph.Linked compile(TypeDescription typeDefinition, TypeDescription viewPoint) {
        return originCompiler.compile(typeDefinition, viewPoint);
    }

    static class SWMethodGraph implements MethodGraph.Linked {
        private MethodGraph.Linked origin;

        public SWMethodGraph(Linked origin) {
            this.origin = origin;
        }

        @Override
        public MethodGraph getSuperClassGraph() {
            return origin.getSuperClassGraph();
        }

        @Override
        public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
            return origin.getInterfaceGraph(typeDescription);
        }

        @Override
        public Node locate(MethodDescription.SignatureToken token) {
            return origin.locate(token);
        }

        @Override
        public NodeList listNodes() {
            // sort nodes (methods) to generate same cache field order when retransform class
            NodeList nodeList = origin.listNodes();
            List<Node> nodes = nodeList.stream().map(n -> new Pair<Integer, Node>(n.getRepresentative().toString().hashCode(), n))
                    .sorted(Comparator.comparing(p -> p.first))
                    .map(p -> p.second)
                    .collect(Collectors.toList());

//            List<Node> nodes = nodeList.stream()
//                    .sorted(Comparator.comparing(n -> n.getRepresentative().hashCode()))
//                    .collect(Collectors.toList());
            return new NodeList(nodes);
        }
    }

    static class Pair<T, V> {
        private T first;
        private V second;

        public Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
