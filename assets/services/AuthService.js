angular.module('sankhyaJasperMvp')
.factory('AuthService', ['$http', function($http) {
    var BASE_URL = 'http://127.0.0.1:8080/api';

    return {
        login: function(credenciais) {
            return $http({
                method: 'POST',
                url: BASE_URL + '/auth/login',
                data: credenciais,
                headers: {
                    'Content-Type': 'application/json'
                }
            });
        }
    };
}]);