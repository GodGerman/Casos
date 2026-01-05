import { post } from './api.js';

export function login(nombre_usuario, contrasena) {
  return post('/api/auth/login', {
    nombre_usuario: nombre_usuario,
    contrasena
  });
}

export function logout() {
  return post('/api/auth/logout', {});
}
