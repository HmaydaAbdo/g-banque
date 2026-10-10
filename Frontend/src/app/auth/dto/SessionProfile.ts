/**
 * Profil de la session courante, renvoyé par /auth/login, /auth/refresh et /auth/profile.
 * Les jetons ne sont jamais exposés au navigateur : ils voyagent en cookies HttpOnly.
 */
export interface SessionProfile {
  username: string;
  authorities: string[];
}
