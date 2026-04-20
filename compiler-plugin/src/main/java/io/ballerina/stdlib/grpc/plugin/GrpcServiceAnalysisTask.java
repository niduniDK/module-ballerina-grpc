/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.grpc.plugin;

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.grpc.plugin.endpointyaml.generator.EndpointYamlGenerator;
import io.ballerina.stdlib.grpc.plugin.endpointyaml.generator.ProtoFileExporter;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.io.IOException;
import java.util.Optional;

import static io.ballerina.stdlib.grpc.plugin.GrpcServiceValidator.isBallerinaGrpcService;

public class GrpcServiceAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();

        Optional<Symbol> symbol = context.semanticModel().symbol(serviceNode);
        if (symbol.isEmpty() || !(symbol.get() instanceof ServiceDeclarationSymbol serviceDeclarationSymbol)) {
            return;
        }

        EndpointYamlGenerator endpointYamlGeneratorGrpc = new EndpointYamlGenerator(context);
        ProtoFileExporter protoFileExporter = new ProtoFileExporter(context);

        Project project = context.currentPackage().project();
        BuildOptions buildOptions = project.buildOptions();
        boolean isExportEndpoints = false;

        try {
            isExportEndpoints = buildOptions.exportEndpoints();
        } catch (NoSuchMethodError e) {
            // Used to catch the buildOption not found error for earlier ballerina versions
        }

        if (isBallerinaGrpcService(serviceDeclarationSymbol) && isExportEndpoints) {
            try {
                endpointYamlGeneratorGrpc.writeEndpointYaml();
                protoFileExporter.exportProtoFile();
            } catch (IOException e) {
                context.reportDiagnostic(
                    DiagnosticFactory.createDiagnostic(
                        new DiagnosticInfo(
                                "GRPC_PLUGIN_DEBUG",
                                "[grpc-plugin] " + e.getMessage(),
                                DiagnosticSeverity.ERROR
                        ),
                        serviceNode.location()
                    )
                );
            }
        }
    }
}
