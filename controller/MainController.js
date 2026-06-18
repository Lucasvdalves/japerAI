angular.module('sankhyaJasperMvp', [])
.controller('MainController', ['ReportService', '$window', function(ReportService, $window) {
    var vm = this;

    // Recupera dados salvos na sessão do login
    vm.tenantId = $window.localStorage.getItem('tenant_id') || '1001';
    vm.passoAtual = 1;
    vm.statusSql = "Aguardando comando.";
    vm.instrucaoLayout = "";
    vm.carregando = false;

    // Objeto centralizado mapeado com o novo HTML de 6 passos
    vm.relatorio = {
        nomeTecnico: 'RelatorioFaturamento',
        tituloExibido: 'Faturamento por Vendedor',
        categoria: 'Vendas',
        descricao: 'Relatório de faturamento agrupado por vendedor e competência mensal, com filtros de data inicial e final.',
        campos: [
            { nome: 'CODVEND', tipo: 'java.math.BigDecimal', descricao: 'Código do Vendedor' },
            { nome: 'APELIDO', tipo: 'java.lang.String', descricao: 'Apelido do Vendedor' },
            { nome: 'VLRVENDA', tipo: 'java.math.BigDecimal', descricao: 'Valor Total das Vendas' }
        ],
        sql: 'SELECT CAB.CODVEND, VEND.APELIDO, SUM(CAB.VLRNOTA) AS VLRVENDA \nFROM TGFCAB CAB \nINNER JOIN TGFVEN VEND ON VEND.CODVEND = CAB.CODVEND \nWHERE CAB.STATUSNOTA = \'A\' \nGROUP BY CAB.CODVEND, VEND.APELIDO',
        jrxml: '<?xml version="1.0" encoding="UTF-8"?>\n<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports" name="RelatorioFaturamento" language="groovy" pageWidth="595" pageHeight="842" columnWidth="555" leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20">\n  <property name="ireport.zoom" value="1.0"/>\n  \n</jasperReport>'
    };

    // Controles de navegação do Stepper
    vm.mudarPasso = function(passo) { 
        vm.passoAtual = passo; 
        if (vm.passoAtual === 5) {
            vm.executarPreview();
        }
    };
    
    vm.proximoPasso = function() { 
        if (vm.passoAtual < 6) vm.passoAtual++; 
        if (vm.passoAtual === 5) {
            vm.executarPreview();
        }
    };
    
    vm.voltarPasso = function() { 
        if (vm.passoAtual > 1) vm.passoAtual--; 
        if (vm.passoAtual === 5) {
            vm.executarPreview();
        }
    };

    // Lógica para adicionar e remover campos na grade (Passo 2)
    vm.adicionarCampo = function() {
        vm.relatorio.campos.push({ nome: 'NOVO_CAMPO', tipo: 'java.lang.String', descricao: 'Descrição do Campo' });
    };

    vm.removerCampo = function(index) {
        vm.relatorio.campos.splice(index, 1);
    };

    // 1️⃣ Lógica do Passo 1: Solicitar os campos iniciais à IA
    vm.sugerirEstrutura = function() {
        vm.carregando = true;
        vm.statusSql = "IA sugerindo dicionário de dados do ERP...";
        
        var payload = angular.copy(vm.relatorio);
        payload.acao = "ESTRUTURA";

        ReportService.sincronizar(payload, vm.tenantId)
            .then(function(resposta) {
                vm.relatorio.campos = resposta.data.campos;
                vm.passoAtual = 2; // Avança para a grade
            })
            .catch(function(erro) {
                alert("Erro ao sugerir estrutura: " + (erro.data?.mensagem || "Falha de rede."));
            })
            .finally(function() { vm.carregando = false; });
    };

    // 2️⃣ Lógica do Passo 3: Criar / Otimizar a Query SQL Oracle
    vm.gerarSqlComIa = function() {
        vm.carregando = true;
        vm.statusSql = "Consultando base de conhecimento Oracle ERP...";
        
        var payload = angular.copy(vm.relatorio);
        payload.acao = "QUERY";

        ReportService.sincronizar(payload, vm.tenantId)
            .then(function(resposta) {
                vm.relatorio.sql = resposta.data.sql;
                vm.statusSql = resposta.data.statusSql;
            })
            .catch(function(erro) {
                alert("Erro ao gerar SQL: " + (erro.data?.mensagem || "Falha de rede."));
            })
            .finally(function() { vm.carregando = false; });
    };

    // 3️⃣ Lógica do Passo 4: Sincronizar o Layout JRXML (Sob Demanda)
    vm.sincronizarLayout = function() {
        vm.carregando = true;
        vm.statusSql = "IA processando dados através do Hugging Face (Llama)...";
        
        var payload = angular.copy(vm.relatorio);
        payload.acao = "LAYOUT";
        payload.instrucaoLayout = vm.instrucaoLayout; // Vincula o campo de input livre do Passo 4

        ReportService.sincronizar(payload, vm.tenantId)
            .then(function(resposta) {
                vm.relatorio.jrxml = resposta.data.jrxml;
                vm.statusSql = resposta.data.statusSql;
            })
            .catch(function(erro) {
                var msg = (erro.data && erro.data.mensagem) ? erro.data.mensagem : "Erro de rede/comunicação com a porta 8080.";
                alert("Falha na sincronização com o Llama: " + msg);
                vm.statusSql = "Erro na execução da rota.";
            })
            .finally(function() {
                vm.carregando = false;
            });
    };

    // 4️⃣ Lógica do Passo 5: Executar Query real no banco para o Preview
    vm.previewColunas = [];
    vm.previewLinhas = [];
    vm.erroSqlPreview = "";

    vm.executarPreview = function() {
        vm.carregando = true;
        vm.statusSql = "Buscando dados reais no Oracle...";
        vm.erroSqlPreview = "";
        vm.previewColunas = [];
        vm.previewLinhas = [];

        var payload = angular.copy(vm.relatorio);
        payload.acao = "EXECUTE";

        ReportService.sincronizar(payload, vm.tenantId)
            .then(function(resposta) {
                vm.previewColunas = resposta.data.colunas || [];
                vm.previewLinhas = resposta.data.linhas || [];
                vm.statusSql = "Dados reais carregados com sucesso!";
            })
            .catch(function(erro) {
                vm.erroSqlPreview = (erro.data?.mensagem || "Erro de rede/comunicação com a porta 8080.");
                vm.statusSql = "Falha ao carregar preview.";
            })
            .finally(function() {
                vm.carregando = false;
            });
    };

    // 5️⃣ Lógica de Configuração Dinâmica do Banco de Dados
    vm.exibirModalDb = false;
    vm.dbConfig = {
        dbType: 'ORACLE',
        host: 'localhost',
        port: 1522,
        databaseName: 'XE',
        username: 'system',
        password: 'oracle'
    };
    vm.statusConexaoDb = '';
    vm.conexaoSucesso = false;

    vm.abrirModalDb = function() {
        vm.exibirModalDb = true;
        vm.statusConexaoDb = '';
    };

    vm.fecharModalDb = function() {
        vm.exibirModalDb = false;
    };

    vm.testarConfigDb = function() {
        vm.carregando = true;
        vm.statusConexaoDb = 'Testando conexão com o banco...';
        
        var payload = angular.copy(vm.dbConfig);
        payload.acao = 'TEST';

        ReportService.salvarConexao(payload, vm.tenantId)
            .then(function(resposta) {
                vm.conexaoSucesso = true;
                vm.statusConexaoDb = 'Conexão realizada com sucesso!';
            })
            .catch(function(erro) {
                vm.conexaoSucesso = false;
                vm.statusConexaoDb = 'Falha na conexão: ' + (erro.data?.mensagem || 'Erro de rede.');
            })
            .finally(function() {
                vm.carregando = false;
            });
    };

    vm.salvarConfigDb = function() {
        vm.carregando = true;
        vm.statusConexaoDb = 'Verificando e salvando credenciais...';

        var payload = angular.copy(vm.dbConfig);
        payload.acao = 'SAVE';

        ReportService.salvarConexao(payload, vm.tenantId)
            .then(function(resposta) {
                vm.conexaoSucesso = true;
                vm.statusConexaoDb = 'Configurações salvas e conectadas!';
                setTimeout(function() {
                    vm.fecharModalDb();
                    if (vm.passoAtual === 5) {
                        vm.executarPreview();
                    }
                }, 1500);
            })
            .catch(function(erro) {
                vm.conexaoSucesso = false;
                vm.statusConexaoDb = 'Erro ao salvar: ' + (erro.data?.mensagem || 'Erro de rede.');
            })
            .finally(function() {
                vm.carregando = false;
            });
    };

    // Função de download do arquivo físico para injetar no Sankhya Om
    vm.baixarJrxmlFinal = function() {
        var blob = new Blob([vm.relatorio.jrxml], { type: 'text/xml;charset=utf-8;' });
        var downloadLink = angular.element('<a></a>');
        downloadLink.attr('href', window.URL.createObjectURL(blob));
        downloadLink.attr('download', vm.relatorio.nomeTecnico + '.jrxml');
        downloadLink[0].click();
    };
}]);