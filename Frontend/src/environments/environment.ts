export const environment = {
  production: false,
  // Relatif, comme en production : `ng serve` relaie /gbanque/api vers le backend (proxy.conf.json).
  // Même origine => cookies de session SameSite=Strict et en-tête CSRF Angular fonctionnent en dev.
  apiUrl: '/gbanque/api/'
};

