angular.module('sankhyaJasperMvp', [])
.controller('LoginController', ['AuthService', '$window', '$http', function(AuthService, $window, $http) {
    var vm = this;

    vm.credenciais = {
        tenantId: '',
        username: '',
        password: ''
    };
    vm.carregando = false;
    vm.mensagemErro = '';

    vm.efetuarLogin = function() {
        vm.carregando = true;
        vm.mensagemErro = '';

        AuthService.login(vm.credenciais)
            .then(function(resposta) {
                // Guarda os dados no localStorage do navegador
                $window.localStorage.setItem('token_saas_jasper', resposta.data.token);
                $window.localStorage.setItem('tenant_id', vm.credenciais.tenantId);
                
                // Configura o cabeçalho padrão para que TODAS as próximas requisições enviem o TenantID para o Java Puro
                $http.defaults.headers.common['X-Tenant-ID'] = vm.credenciais.tenantId;

                // Redireciona para o Workspace
                $window.location.href = 'main.html';
            })
            .catch(function(erro) {
                if (erro.data && erro.data.mensagem) {
                    vm.mensagemErro = erro.data.mensagem;
                } else {
                    vm.mensagemErro = 'Não foi possível conectar ao backend Java (Porta 8080).';
                }
            })
            .finally(function() {
                vm.carregando = false;
            });
    };
}]);