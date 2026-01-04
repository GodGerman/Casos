import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AlertMessage from '../components/AlertMessage.jsx';
import Loading from '../components/Loading.jsx';
import { login } from '../services/auth.js';

export default function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [nombreUsuario, setNombreUsuario] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!nombreUsuario.trim() || !contrasena.trim()) {
      setError('Ingresa usuario y contrasena.');
      return;
    }
    setLoading(true);
    try {
      const result = await login(nombreUsuario.trim(), contrasena.trim());
      if (result && result.ok) {
        onLogin({
          id_usuario: result.id_usuario,
          id_rol: result.id_rol,
          nombre_usuario: result.nombre_usuario,
          nombre_rol: result.nombre_rol
        });
        navigate('/diagramas');
      } else {
        setError('Credenciales invalidas.');
      }
    } catch (err) {
      const mensaje = err?.data?.mensaje || 'Error al iniciar sesion.';
      setError(mensaje);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="card shadow-sm login-card">
        <div className="card-body">
          <h3 className="mb-3">Inicio de sesion</h3>
          <AlertMessage type="danger" message={error} />
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="nombre_usuario">Usuario</label>
              <input
                id="nombre_usuario"
                className="form-control"
                type="text"
                value={nombreUsuario}
                onChange={(event) => setNombreUsuario(event.target.value)}
                placeholder="ADMINISTRADOR"
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="contrasena">Contrasena</label>
              <input
                id="contrasena"
                className="form-control"
                type="password"
                value={contrasena}
                onChange={(event) => setContrasena(event.target.value)}
                placeholder="1234"
              />
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={loading}>
              {loading ? 'Ingresando...' : 'Ingresar'}
            </button>
          </form>
          {loading && <div className="mt-3"><Loading text="Validando credenciales..." /></div>}
        </div>
      </div>
    </div>
  );
}
