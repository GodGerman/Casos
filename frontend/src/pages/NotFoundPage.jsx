import React from 'react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="text-center">
      <h2>Pagina no encontrada</h2>
      <p>La ruta solicitada no existe.</p>
      <Link className="btn btn-outline-primary" to="/diagramas">Volver</Link>
    </div>
  );
}
