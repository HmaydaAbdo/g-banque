/**
 * Configuration unique du frontend (environnement unique : dev = prod).
 * L'API est toujours servie sur la même origine que l'application, sous /gbanque/api :
 * en production par le nginx frontal, en dev par le proxy de `ng serve` (proxy.conf.json).
 * Même origine => cookies de session SameSite=Strict et en-tête CSRF d'Angular fonctionnent partout.
 */
export const environment = {
  apiUrl: '/gbanque/api/'
};
