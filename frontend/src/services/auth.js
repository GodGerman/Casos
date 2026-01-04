import { post } from './api.js';

export function login(nombreUsuario, contrasena) {
  return post('/api/auth/login', {
    nombre_usuario: nombreUsuario,
    contrasena
  });
}

export function logout() {
  return post('/api/auth/logout', {});
}
