angular.module('sankhyaJasperMvp')
.factory('ReportService', ['$http', function($http) {
    var BASE_URL = 'http://127.0.0.1:8080/api';

    return {
        sincronizar: function(dadosRelatorio, tenantId) {
            return $http({
                method: 'POST',
                url: BASE_URL + '/relatorios/sincronizar',
                data: dadosRelatorio,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Tenant-ID': tenantId
                }
            });
        },
        salvarConexao: function(dbConfig, tenantId) {
            return $http({
                method: 'POST',
                url: BASE_URL + '/config/database',
                data: dbConfig,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Tenant-ID': tenantId
                }
            });
        }
    };
}]);