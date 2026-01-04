const BASE_URL = window.BACKEND_URL || '';

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  const config = {
    credentials: 'include',
    ...options
  };

  if (!config.headers) {
    config.headers = {};
  }

  if (config.body && !(config.body instanceof FormData)) {
    config.headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, config);
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('content-type') || '';
  let data = null;
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

export function get(path) {
  return request(path, { method: 'GET' });
}

export function post(path, body) {
  return request(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : null
  });
}

export function put(path, body) {
  return request(path, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : null
  });
}

export function del(path) {
  return request(path, { method: 'DELETE' });
}

export function upload(path, formData) {
  return request(path, {
    method: 'POST',
    body: formData
  });
}

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
