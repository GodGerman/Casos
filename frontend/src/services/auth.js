import { post } from './api.js';

/**
 * Inicia sesion contra /api/auth/login.
 *
 * Se envia el payload JSON con usuario/contrasena y
 * retorna la respuesta del backend con los datos de sesion.
 *
 *
 * @param {string} nombre_usuario usuario.
 * @param {string} contrasena contrasena.
 * @returns {Promise<object>} payload de autenticacion.
 * @throws {Error} si la respuesta no es ok o el backend rechaza credenciales.
 */
export function login(nombre_usuario, contrasena) {
  return post('/api/auth/login', {
    nombre_usuario: nombre_usuario,
    contrasena
  });
}

/**
 * Cierra sesion actual en el backend.
 *
 * Se llama al endpoint logout usando POST y permite que
 * el servidor invalide la sesion actual.
 *
 *
 * @returns {Promise<object|null>} respuesta del backend.
 * @throws {Error} si la respuesta no es ok.
 */
export function logout() {
  return post('/api/auth/logout', {});
}
