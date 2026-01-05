// Permite configurar el host del backend desde el HTML (window.BACKEND_URL).
// Esto facilita desplegar el mismo bundle contra distintos backends.
const BASE_URL = window.BACKEND_URL || '';

/**
 * Ejecuta una solicitud HTTP con credenciales de sesion y parseo de respuesta.
 *
 * Se compone la URL, se agregan credentials para cookies de sesion,
 * se asigna Content-Type para JSON, se parsea la respuesta segun content-type y
 * se arroja un Error enriquecido si el status no es ok.
 *
 *
 * @param {string} path ruta relativa del backend.
 * @param {RequestInit} [options] opciones de fetch.
 * @returns {Promise<any|null>} JSON parseado, texto o null si 204.
 * @throws {Error} si la respuesta no es ok; incluye status y data.
 */
async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  const config = {
    credentials: 'include',
    ...options
  };

  if (!config.headers) {
    config.headers = {};
  }

  // Para payload JSON, agrega Content-Type automaticamente.
  if (config.body && !(config.body instanceof FormData)) {
    config.headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, config);
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('content-type') || '';
  let data = null;
  // Respuestas JSON vs texto plano.
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const error = new Error('request_failed');
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

/**
 * Wrapper GET.
 *
 * Se delega en request con method=GET.
 *
 *
 * @param {string} path ruta del backend.
 * @returns {Promise<any|null>} respuesta parseada.
 * @throws {Error} si la respuesta no es ok.
 */
export function get(path) {
  return request(path, { method: 'GET' });
}

/**
 * Wrapper POST con body JSON.
 *
 * Se serializa el body a JSON y delega en request.
 *
 *
 * @param {string} path ruta del backend.
 * @param {any} body payload a serializar.
 * @returns {Promise<any|null>} respuesta parseada.
 * @throws {Error} si la respuesta no es ok.
 */
export function post(path, body) {
  return request(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : null
  });
}

/**
 * Wrapper PUT con body JSON.
 *
 * Se serializa el body a JSON y delega en request.
 *
 *
 * @param {string} path ruta del backend.
 * @param {any} body payload a serializar.
 * @returns {Promise<any|null>} respuesta parseada.
 * @throws {Error} si la respuesta no es ok.
 */
export function put(path, body) {
  return request(path, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : null
  });
}

/**
 * Wrapper DELETE.
 *
 * Se delega en request con method=DELETE.
 *
 *
 * @param {string} path ruta del backend.
 * @returns {Promise<any|null>} respuesta parseada.
 * @throws {Error} si la respuesta no es ok.
 */
export function del(path) {
  return request(path, { method: 'DELETE' });
}

/**
 * Wrapper POST para multipart/form-data.
 *
 * Se delega en request sin fijar Content-Type
 * para que el navegador agregue el boundary correcto.
 *
 *
 * @param {string} path ruta del backend.
 * @param {FormData} formData payload multipart.
 * @returns {Promise<any|null>} respuesta parseada.
 * @throws {Error} si la respuesta no es ok.
 */
export function upload(path, formData) {
  return request(path, {
    method: 'POST',
    body: formData
  });
}

/**
 * Construye una URL absoluta para una ruta de archivo almacenada en backend.
 *
 * Se si la ruta ya es absoluta (http), la devuelve tal cual;
 * si es relativa, la concatena con BASE_URL.
 *
 *
 * @param {string} ruta ruta relativa (ej: uploads/uuid.mp3) o URL absoluta.
 * @returns {string|null} URL completa o null si no hay ruta.
 */
export function buildFileUrl(ruta) {
  if (!ruta) {
    return null;
  }
  if (ruta.startsWith('http')) {
    return ruta;
  }
  const cleaned = ruta.replace(/^\/+/, '');
  return `${BASE_URL}/${cleaned}`;
}
